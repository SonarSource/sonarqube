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
package org.sonar.ce.task.projectanalysis.qualityprofile;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.server.qualityprofile.QPMeasureData;
import org.sonar.server.qualityprofile.QualityProfile;

import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository.Status.ADDED;
import static org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository.Status.REMOVED;
import static org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository.Status.UNCHANGED;
import static org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository.Status.UPDATED;

public class RegisterQualityProfileStatusStep implements ComputationStep {

  private TreeRootHolder treeRootHolder;
  private MeasureRepository measureRepository;
  private MetricRepository metricRepository;
  private MutableQProfileStatusRepository qProfileStatusRepository;
  private AnalysisMetadataHolder analysisMetadataHolder;

  public RegisterQualityProfileStatusStep(TreeRootHolder treeRootHolder, MeasureRepository measureRepository, MetricRepository metricRepository,
    MutableQProfileStatusRepository qProfileStatusRepository, AnalysisMetadataHolder analysisMetadataHolder) {
    this.treeRootHolder = treeRootHolder;
    this.measureRepository = measureRepository;
    this.metricRepository = metricRepository;
    this.qProfileStatusRepository = qProfileStatusRepository;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Override
  public void execute(Context context) {
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(CrawlerDepthLimit.PROJECT, POST_ORDER) {
        @Override
        public void visitProject(Component tree) {
          executeForProject(tree);
        }
      }).visit(treeRootHolder.getRoot());
  }

  private void executeForProject(Component project) {
    measureRepository.getBaseMeasure(project, metricRepository.getByKey(CoreMetrics.QUALITY_PROFILES_KEY)).ifPresent(baseProfilesMeasure -> {
      Map<String, QualityProfile> baseProfiles = parseJsonData(baseProfilesMeasure);
      Map<String, QualityProfile> rawProfiles = analysisMetadataHolder
        .getQProfilesByLanguage().values().stream()
        .collect(Collectors.toMap(QualityProfile::getQpKey, q -> q));

      registerNoMoreUsedProfiles(baseProfiles, rawProfiles);
      registerNewOrUpdatedProfiles(baseProfiles, rawProfiles);
    });
  }

  private void registerNoMoreUsedProfiles(Map<String, QualityProfile> baseProfiles, Map<String, QualityProfile> rawProfiles) {
    for (QualityProfile baseProfile : baseProfiles.values()) {
      if (!rawProfiles.containsKey(baseProfile.getQpKey())) {
        register(baseProfile, REMOVED);
      }
    }
  }

  private void registerNewOrUpdatedProfiles(Map<String, QualityProfile> baseProfiles, Map<String, QualityProfile> rawProfiles) {
    for (QualityProfile profile : rawProfiles.values()) {
      QualityProfile baseProfile = baseProfiles.get(profile.getQpKey());
      if (baseProfile == null) {
        register(profile, ADDED);
      } else if  (profile.getRulesUpdatedAt().after(baseProfile.getRulesUpdatedAt())) {
        register(baseProfile, UPDATED);
      } else {
        register(baseProfile, UNCHANGED);
      }
    }
  }

  private void register(QualityProfile profile, QProfileStatusRepository.Status status) {
    qProfileStatusRepository.register(profile.getQpKey(), status);
  }

  private static Map<String, QualityProfile> parseJsonData(Measure measure) {
    String data = measure.getStringValue();
    if (data == null) {
      return Collections.emptyMap();
    }
    return QPMeasureData.fromJson(data).getProfilesByKey();
  }

  @Override
  public String getDescription() {
    return "Compute Quality Profile status";
  }

}
