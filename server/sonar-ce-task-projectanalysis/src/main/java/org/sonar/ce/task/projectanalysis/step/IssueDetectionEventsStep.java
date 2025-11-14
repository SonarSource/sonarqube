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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Language;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.ScannerPlugin;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.event.Event;
import org.sonar.ce.task.projectanalysis.event.EventRepository;
import org.sonar.ce.task.projectanalysis.language.LanguageRepository;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.server.qualityprofile.QPMeasureData;
import org.sonar.server.qualityprofile.QualityProfile;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Computation of Issue Detection events
 * As it depends upon {@link CoreMetrics#QUALITY_PROFILES_KEY}, it must be executed after {@link ComputeQProfileMeasureStep}
 */
public class IssueDetectionEventsStep implements ComputationStep {
  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;
  private final EventRepository eventRepository;
  private final LanguageRepository languageRepository;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public IssueDetectionEventsStep(TreeRootHolder treeRootHolder, MetricRepository metricRepository,
    MeasureRepository measureRepository, LanguageRepository languageRepository, EventRepository eventRepository,
    AnalysisMetadataHolder analysisMetadataHolder) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.eventRepository = eventRepository;
    this.languageRepository = languageRepository;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Override
  public void execute(Context context) {
    executeForBranch(treeRootHolder.getRoot());
  }

  private void executeForBranch(Component branchComponent) {
    Metric qualityProfileMetric = metricRepository.getByKey(CoreMetrics.QUALITY_PROFILES_KEY);
    Optional<Measure> baseMeasure = measureRepository.getBaseMeasure(branchComponent, qualityProfileMetric);
    if (baseMeasure.isEmpty()) {
      // first analysis -> do not generate events
      return;
    }

    // Load profiles used in current analysis for which at least one file of the corresponding language exists
    Optional<Measure> rawMeasure = measureRepository.getRawMeasure(branchComponent, qualityProfileMetric);
    if (rawMeasure.isEmpty()) {
      // No qualify profile computed on the project
      return;
    }
    Map<String, QualityProfile> rawProfiles = QPMeasureData.fromJson(rawMeasure.get().getStringValue()).getProfilesByKey();
    detectIssueCapabilitiesChanged(rawProfiles.values());
  }

  private void detectIssueCapabilitiesChanged(Collection<QualityProfile> rawProfiles) {
    Map<String, ScannerPlugin> scannerPluginsByKey = analysisMetadataHolder.getScannerPluginsByKey();
    long lastAnalysisDate = requireNonNull(analysisMetadataHolder.getBaseAnalysis()).getCreatedAt();
    for (QualityProfile profile : rawProfiles) {
      String languageKey = profile.getLanguageKey();
      ScannerPlugin scannerPlugin = scannerPluginsByKey.get(languageKey);
      if (scannerPlugin == null) {
        // nothing to do as the language is not supported
        continue;
      }

      if (scannerPlugin.getUpdatedAt() > lastAnalysisDate) {
        String languageName = languageRepository.find(languageKey)
          .map(Language::getName)
          .orElse(languageKey);
        Event event = Event.createIssueDetection(format("Capabilities have been updated (%s)", languageName));
        eventRepository.add(event);
      }
    }
  }

  @Override
  public String getDescription() {
    return "Generate Issue Detection events";
  }
}
