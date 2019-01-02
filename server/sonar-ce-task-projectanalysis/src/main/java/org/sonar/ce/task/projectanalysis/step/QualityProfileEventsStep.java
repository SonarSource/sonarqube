/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.ce.task.projectanalysis.step;

import com.google.common.collect.ImmutableSortedMap;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.lang.time.DateUtils;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Language;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.event.Event;
import org.sonar.ce.task.projectanalysis.event.EventRepository;
import org.sonar.ce.task.projectanalysis.language.LanguageRepository;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.util.UtcDateUtils;
import org.sonar.server.qualityprofile.QPMeasureData;
import org.sonar.server.qualityprofile.QualityProfile;

import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository.Status.ADDED;
import static org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository.Status.REMOVED;
import static org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository.Status.UPDATED;

/**
 * Computation of quality profile events
 *
 * As it depends upon {@link CoreMetrics#QUALITY_PROFILES_KEY}, it must be executed after {@link ComputeQProfileMeasureStep}
 */
public class QualityProfileEventsStep implements ComputationStep {
  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;
  private final EventRepository eventRepository;
  private final LanguageRepository languageRepository;
  private QProfileStatusRepository qProfileStatusRepository;

  public QualityProfileEventsStep(TreeRootHolder treeRootHolder,
    MetricRepository metricRepository, MeasureRepository measureRepository, LanguageRepository languageRepository,
    EventRepository eventRepository, QProfileStatusRepository qProfileStatusRepository) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.eventRepository = eventRepository;
    this.languageRepository = languageRepository;
    this.qProfileStatusRepository = qProfileStatusRepository;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(CrawlerDepthLimit.PROJECT, POST_ORDER) {
        @Override
        public void visitProject(Component tree) {
          executeForProject(tree);
        }
      }).visit(treeRootHolder.getRoot());
  }

  private void executeForProject(Component projectComponent) {
    Optional<Measure> baseMeasure = measureRepository.getBaseMeasure(projectComponent, metricRepository.getByKey(CoreMetrics.QUALITY_PROFILES_KEY));
    if (!baseMeasure.isPresent()) {
      // first analysis -> do not generate events
      return;
    }

    // Load profiles used in current analysis for which at least one file of the corresponding language exists
    Optional<Measure> rawMeasure = measureRepository.getRawMeasure(projectComponent, metricRepository.getByKey(CoreMetrics.QUALITY_PROFILES_KEY));
    if (!rawMeasure.isPresent()) {
      // No qualify profile computed on the project
      return;
    }
    Map<String, QualityProfile> rawProfiles = QPMeasureData.fromJson(rawMeasure.get().getStringValue()).getProfilesByKey();

    Map<String, QualityProfile> baseProfiles = parseJsonData(baseMeasure.get());
    detectNewOrUpdatedProfiles(projectComponent, baseProfiles, rawProfiles);
    detectNoMoreUsedProfiles(projectComponent, baseProfiles);
  }

  private static Map<String, QualityProfile> parseJsonData(Measure measure) {
    String data = measure.getStringValue();
    if (data == null) {
      return Collections.emptyMap();
    }
    return QPMeasureData.fromJson(data).getProfilesByKey();
  }

  private void detectNoMoreUsedProfiles(Component context, Map<String, QualityProfile> baseProfiles) {
    for (QualityProfile baseProfile : baseProfiles.values()) {
      if (qProfileStatusRepository.get(baseProfile.getQpKey()).filter(REMOVED::equals).isPresent()) {
        markAsRemoved(context, baseProfile);
      }
    }
  }

  private void detectNewOrUpdatedProfiles(Component component, Map<String, QualityProfile> baseProfiles, Map<String, QualityProfile> rawProfiles) {
    for (QualityProfile profile : rawProfiles.values()) {
      qProfileStatusRepository.get(profile.getQpKey()).ifPresent(status -> {
        if (status.equals(ADDED)) {
          markAsAdded(component, profile);
        } else if (status.equals(UPDATED)) {
          markAsChanged(component, baseProfiles.get(profile.getQpKey()), profile);
        }
      });
    }
  }

  private void markAsChanged(Component component, QualityProfile baseProfile, QualityProfile profile) {
    Date from = baseProfile.getRulesUpdatedAt();

    String data = KeyValueFormat.format(ImmutableSortedMap.of(
      "key", profile.getQpKey(),
      "from", UtcDateUtils.formatDateTime(fixDate(from)),
      "to", UtcDateUtils.formatDateTime(fixDate(profile.getRulesUpdatedAt()))));
    eventRepository.add(component, createQProfileEvent(profile, "Changes in %s", data));
  }

  private void markAsRemoved(Component component, QualityProfile profile) {
    eventRepository.add(component, createQProfileEvent(profile, "Stop using %s"));
  }

  private void markAsAdded(Component component, QualityProfile profile) {
    eventRepository.add(component, createQProfileEvent(profile, "Use %s"));
  }

  private Event createQProfileEvent(QualityProfile profile, String namePattern) {
    return createQProfileEvent(profile, namePattern, null);
  }

  private Event createQProfileEvent(QualityProfile profile, String namePattern, @Nullable String data) {
    return Event.createProfile(String.format(namePattern, profileLabel(profile)), data, null);
  }

  private String profileLabel(QualityProfile profile) {
    Optional<Language> language = languageRepository.find(profile.getLanguageKey());
    String languageName = language.isPresent() ? language.get().getName() : profile.getLanguageKey();
    return String.format("'%s' (%s)", profile.getQpName(), languageName);
  }

  /**
   * This hack must be done because date precision is millisecond in db/es and date format is select only
   */
  private static Date fixDate(Date date) {
    return DateUtils.addSeconds(date, 1);
  }

  @Override
  public String getDescription() {
    return "Generate Quality profile events";
  }
}
