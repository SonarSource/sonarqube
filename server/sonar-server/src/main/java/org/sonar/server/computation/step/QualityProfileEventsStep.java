/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.step;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSortedMap;
import java.util.Date;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang.time.DateUtils;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Language;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.UtcDateUtils;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.event.Event;
import org.sonar.server.computation.event.EventRepository;
import org.sonar.server.computation.language.LanguageRepository;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.qualityprofile.QPMeasureData;
import org.sonar.server.computation.qualityprofile.QualityProfile;

import static org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor.Order.POST_ORDER;

public class QualityProfileEventsStep implements ComputationStep {
  private final TreeRootHolder treeRootHolder;
  private final MeasureRepository measureRepository;
  private final EventRepository eventRepository;
  private final LanguageRepository languageRepository;

  public QualityProfileEventsStep(TreeRootHolder treeRootHolder,
    MeasureRepository measureRepository, EventRepository eventRepository, LanguageRepository languageRepository) {
    this.treeRootHolder = treeRootHolder;
    this.measureRepository = measureRepository;
    this.eventRepository = eventRepository;
    this.languageRepository = languageRepository;
  }

  @Override
  public void execute() {
    new DepthTraversalTypeAwareVisitor(Component.Type.PROJECT, POST_ORDER) {
      @Override
      public void visitProject(Component tree) {
        executeForProject(tree);
      }
    }.visit(treeRootHolder.getRoot());
  }

  private void executeForProject(Component projectComponent) {
    Optional<MeasureDto> previousMeasure = measureRepository.findPrevious(projectComponent, CoreMetrics.QUALITY_PROFILES);
    if (!previousMeasure.isPresent()) {
      // first analysis -> do not generate events
      return;
    }

    // Load current profiles
    Map<String, QualityProfile> previousProfiles = QPMeasureData.fromJson(previousMeasure.get().getData()).getProfilesByKey();
    Optional<BatchReport.Measure> currentMeasure = measureRepository.findCurrent(projectComponent, CoreMetrics.QUALITY_PROFILES);
    if (!currentMeasure.isPresent()) {
      throw new IllegalStateException("Missing measure " + CoreMetrics.QUALITY_PROFILES + " for component " + projectComponent.getRef());
    }
    Map<String, QualityProfile> currentProfiles = QPMeasureData.fromJson(currentMeasure.get().getStringValue()).getProfilesByKey();

    detectNewOrUpdatedProfiles(projectComponent, previousProfiles, currentProfiles);
    detectNoMoreUsedProfiles(projectComponent, previousProfiles, currentProfiles);
  }

  private void detectNoMoreUsedProfiles(Component context, Map<String, QualityProfile> previousProfiles, Map<String, QualityProfile> currentProfiles) {
    for (QualityProfile previousProfile : previousProfiles.values()) {
      if (!currentProfiles.containsKey(previousProfile.getQpKey())) {
        markAsRemoved(context, previousProfile);
      }
    }
  }

  private void detectNewOrUpdatedProfiles(Component component, Map<String, QualityProfile> previousProfiles, Map<String, QualityProfile> currentProfiles) {
    for (QualityProfile profile : currentProfiles.values()) {
      QualityProfile previousProfile = previousProfiles.get(profile.getQpKey());
      if (previousProfile == null) {
        markAsAdded(component, profile);
      } else if (profile.getRulesUpdatedAt().after(previousProfile.getRulesUpdatedAt())) {
        markAsChanged(component, previousProfile, profile);
      }
    }
  }

  private void markAsChanged(Component component, QualityProfile previousProfile, QualityProfile profile) {
    Date from = previousProfile.getRulesUpdatedAt();

    String data = KeyValueFormat.format(ImmutableSortedMap.of(
      "key", profile.getQpKey(),
      "from", UtcDateUtils.formatDateTime(fixDate(from)),
      "to", UtcDateUtils.formatDateTime(fixDate(profile.getRulesUpdatedAt()))));
    eventRepository.add(component, createQProfileEvent(component, profile, "Changes in %s", data));
  }

  private void markAsRemoved(Component component, QualityProfile profile) {
    eventRepository.add(component, createQProfileEvent(component, profile, "Stop using %s"));
  }

  private void markAsAdded(Component component, QualityProfile profile) {
    eventRepository.add(component, createQProfileEvent(component, profile, "Use %s"));
  }

  private Event createQProfileEvent(Component component, QualityProfile profile, String namePattern) {
    return createQProfileEvent(component, profile, namePattern, null);
  }

  private Event createQProfileEvent(Component component, QualityProfile profile, String namePattern, @Nullable String data) {
    return Event.createProfile(String.format(namePattern, profileLabel(component, profile)), data, null);
  }

  private String profileLabel(Component component, QualityProfile profile) {
    Optional<Language> language = languageRepository.find(profile.getLanguageKey());
    String languageName = language.isPresent() ? language.get().getName() : profile.getLanguageKey();
    return String.format("'%s' (%s)", profile.getQpName(), languageName);
  }

  /**
   * This hack must be done because date precision is millisecond in db/es and date format is select only
   */
  private Date fixDate(Date date) {
    return DateUtils.addSeconds(date, 1);
  }

  @Override
  public String getDescription() {
    return "Compute Quality Profile events";
  }
}
