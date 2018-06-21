/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.step;

import java.util.HashMap;
import java.util.Map;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.PathAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.PathAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.computation.task.step.ComputationStep;
import org.sonar.server.qualityprofile.QPMeasureData;
import org.sonar.server.qualityprofile.QualityProfile;

import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;

/**
 * Compute quality profile measure per module  based on present languages
 */
public class ComputeQProfileMeasureStep implements ComputationStep {

  private final TreeRootHolder treeRootHolder;
  private final MeasureRepository measureRepository;
  private final MetricRepository metricRepository;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public ComputeQProfileMeasureStep(TreeRootHolder treeRootHolder, MeasureRepository measureRepository, MetricRepository metricRepository,
    AnalysisMetadataHolder analysisMetadataHolder) {
    this.treeRootHolder = treeRootHolder;
    this.measureRepository = measureRepository;
    this.metricRepository = metricRepository;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Override
  public void execute() {
    Metric qProfilesMetric = metricRepository.getByKey(CoreMetrics.QUALITY_PROFILES_KEY);
    new PathAwareCrawler<>(new QProfileAggregationComponentVisitor(qProfilesMetric))
      .visit(treeRootHolder.getRoot());
  }

  private class QProfileAggregationComponentVisitor extends PathAwareVisitorAdapter<QProfiles> {

    private final Metric qProfilesMetric;

    public QProfileAggregationComponentVisitor(Metric qProfilesMetric) {
      super(CrawlerDepthLimit.FILE, POST_ORDER, new SimpleStackElementFactory<QProfiles>() {
        @Override
        public QProfiles createForAny(Component component) {
          return new QProfiles();
        }
      });
      this.qProfilesMetric = qProfilesMetric;
    }

    @Override
    public void visitFile(Component file, Path<QProfiles> path) {
      String languageKey = file.getFileAttributes().getLanguageKey();
      if (languageKey == null) {
        // No qprofile for unknown languages
        return;
      }
      if (!analysisMetadataHolder.getQProfilesByLanguage().containsKey(languageKey)) {
        throw new IllegalStateException("Report contains a file with language '" + languageKey + "' but no matching quality profile");
      }
      path.parent().add(analysisMetadataHolder.getQProfilesByLanguage().get(languageKey));
    }

    @Override
    public void visitDirectory(Component directory, Path<QProfiles> path) {
      QProfiles qProfiles = path.current();
      path.parent().add(qProfiles);
    }

    @Override
    public void visitProject(Component project, Path<QProfiles> path) {
      addMeasure(project, path.current());
    }

    @Override
    public void visitModule(Component module, Path<QProfiles> path) {
      QProfiles qProfiles = path.current();
      addMeasure(module, path.current());
      path.parent().add(qProfiles);
    }

    private void addMeasure(Component component, QProfiles qProfiles) {
      if (!qProfiles.profilesByKey.isEmpty()) {
        measureRepository.add(component, qProfilesMetric, qProfiles.createMeasure());
      }
    }
  }

  private static class QProfiles {
    private final Map<String, QualityProfile> profilesByKey = new HashMap<>();

    public void add(QProfiles qProfiles) {
      profilesByKey.putAll(qProfiles.profilesByKey);
    }

    public void add(QualityProfile qProfile) {
      profilesByKey.put(qProfile.getQpKey(), qProfile);
    }

    public Measure createMeasure() {
      return Measure.newMeasureBuilder().create(QPMeasureData.toJson(new QPMeasureData(profilesByKey.values())));
    }
  }

  @Override
  public String getDescription() {
    return "Compute Quality profile measures";
  }
}
