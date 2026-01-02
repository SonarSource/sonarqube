/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Language;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.event.Event;
import org.sonar.ce.task.projectanalysis.event.EventRepository;
import org.sonar.ce.task.projectanalysis.language.LanguageRepository;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository;
import org.sonar.ce.task.projectanalysis.qualityprofile.QualityProfileRuleChangeResolver;
import org.sonar.ce.task.projectanalysis.qualityprofile.QualityProfileTextGenerator;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.util.UtcDateUtils;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.QPMeasureData;
import org.sonar.server.qualityprofile.QualityProfile;

import static org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository.Status.ADDED;
import static org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository.Status.REMOVED;
import static org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository.Status.UPDATED;

/**
 * Computation of quality profile events
 * As it depends upon {@link CoreMetrics#QUALITY_PROFILES_KEY}, it must be executed after {@link ComputeQProfileMeasureStep}
 */
public class QualityProfileEventsStep implements ComputationStep {
  private static final Logger LOG = LoggerFactory.getLogger(QualityProfileEventsStep.class);
  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;
  private final EventRepository eventRepository;
  private final LanguageRepository languageRepository;
  private final QProfileStatusRepository qProfileStatusRepository;
  private final QualityProfileRuleChangeResolver qualityProfileRuleChangeTextResolver;

  public QualityProfileEventsStep(TreeRootHolder treeRootHolder,
    MetricRepository metricRepository, MeasureRepository measureRepository, LanguageRepository languageRepository,
    EventRepository eventRepository, QProfileStatusRepository qProfileStatusRepository, QualityProfileRuleChangeResolver qualityProfileRuleChangeTextResolver) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.eventRepository = eventRepository;
    this.languageRepository = languageRepository;
    this.qProfileStatusRepository = qProfileStatusRepository;
    this.qualityProfileRuleChangeTextResolver = qualityProfileRuleChangeTextResolver;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    executeForBranch(treeRootHolder.getRoot());
  }

  private void executeForBranch(Component branchComponent) {
    Optional<Measure> baseMeasure = measureRepository.getBaseMeasure(branchComponent, metricRepository.getByKey(CoreMetrics.QUALITY_PROFILES_KEY));
    if (baseMeasure.isEmpty()) {
      // first analysis -> do not generate events
      return;
    }

    // Load profiles used in current analysis for which at least one file of the corresponding language exists
    Optional<Measure> rawMeasure = measureRepository.getRawMeasure(branchComponent, metricRepository.getByKey(CoreMetrics.QUALITY_PROFILES_KEY));
    if (rawMeasure.isEmpty()) {
      // No qualify profile computed on the project
      return;
    }
    Map<String, QualityProfile> rawProfiles = QPMeasureData.fromJson(rawMeasure.get().getStringValue()).getProfilesByKey();

    Map<String, QualityProfile> baseProfiles = parseJsonData(baseMeasure.get());
    detectNewOrUpdatedProfiles(baseProfiles, rawProfiles, branchComponent.getUuid());
    detectNoMoreUsedProfiles(baseProfiles);
  }

  private static Map<String, QualityProfile> parseJsonData(Measure measure) {
    String data = measure.getStringValue();
    if (data == null) {
      return Collections.emptyMap();
    }
    return QPMeasureData.fromJson(data).getProfilesByKey();
  }

  private void detectNoMoreUsedProfiles(Map<String, QualityProfile> baseProfiles) {
    for (QualityProfile baseProfile : baseProfiles.values()) {
      if (qProfileStatusRepository.get(baseProfile.getQpKey()).filter((v) -> v == REMOVED).isPresent()) {
        markAsRemoved(baseProfile);
      }
    }
  }

  private void detectNewOrUpdatedProfiles(Map<String, QualityProfile> baseProfiles, Map<String, QualityProfile> rawProfiles, String componentUuid) {
    for (QualityProfile profile : rawProfiles.values()) {
      qProfileStatusRepository.get(profile.getQpKey()).ifPresent(status -> {
        if (status == ADDED) {
          markAsAdded(profile);
        } else if (status == UPDATED) {
          markAsChanged(baseProfiles.get(profile.getQpKey()), profile, componentUuid);
        }
      });
    }
  }

  private void markAsChanged(QualityProfile baseProfile, QualityProfile profile, String componentUuid) {
    try {
      Map<ActiveRuleChange.Type, Long> changesMappedToNumberOfRules = qualityProfileRuleChangeTextResolver.mapChangeToNumberOfRules(baseProfile, componentUuid);

      if (changesMappedToNumberOfRules.isEmpty()) {
        LOG.debug("No changes found for Quality Profile {}. Quality Profile event skipped.", profile.getQpKey());
        return;
      }

      String data = KeyValueFormat.format(ImmutableSortedMap.of(
        "key", profile.getQpKey(),
        "from", UtcDateUtils.formatDateTime(fixDate(baseProfile.getRulesUpdatedAt())),
        "to", UtcDateUtils.formatDateTime(fixDate(profile.getRulesUpdatedAt())),
        "name", profile.getQpName(),
        "languageKey", profile.getLanguageKey()));
      String ruleChangeText = QualityProfileTextGenerator.generateRuleChangeText(changesMappedToNumberOfRules);

      eventRepository.add(createQProfileEvent(profile, "%s updated with " + ruleChangeText, data, ruleChangeText));
    } catch (Exception e) {
      LOG.error("Failed to generate 'change' event for Quality Profile " + profile.getQpKey(), e);
    }
  }

  private void markAsRemoved(QualityProfile profile) {
    eventRepository.add(createQProfileEvent(profile, "Stop using %s"));
  }

  private void markAsAdded(QualityProfile profile) {
    eventRepository.add(createQProfileEvent(profile, "Use %s"));
  }

  private Event createQProfileEvent(QualityProfile profile, String namePattern) {
    return createQProfileEvent(profile, namePattern, null);
  }

  private Event createQProfileEvent(QualityProfile profile, String namePattern, @Nullable String data) {
    return Event.createProfile(String.format(namePattern, profileLabel(profile)), data, null);
  }

  private Event createQProfileEvent(QualityProfile profile, String namePattern, @Nullable String data, @Nullable String description) {
    return Event.createProfile(String.format(namePattern, profileLabel(profile)), data, description);
  }

  private String profileLabel(QualityProfile profile) {
    Optional<Language> language = languageRepository.find(profile.getLanguageKey());
    String languageName = language.isPresent() ? language.get().getName() : profile.getLanguageKey();
    return String.format("\"%s\" (%s)", profile.getQpName(), languageName);
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
