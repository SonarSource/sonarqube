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
import java.util.HashMap;
import java.util.Map;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.MessageException;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.PathAwareCrawler;
import org.sonar.server.computation.component.PathAwareVisitorAdapter;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;
import org.sonar.server.computation.qualityprofile.QPMeasureData;
import org.sonar.server.computation.qualityprofile.QualityProfile;

import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;

/**
 * Aggregates quality profile on lower-level module nodes on their parent modules and project
 */
public class ComputeQProfileMeasureStep implements ComputationStep {

  private final TreeRootHolder treeRootHolder;
  private final MeasureRepository measureRepository;
  private final MetricRepository metricRepository;

  public ComputeQProfileMeasureStep(TreeRootHolder treeRootHolder, MeasureRepository measureRepository, MetricRepository metricRepository) {
    this.treeRootHolder = treeRootHolder;
    this.measureRepository = measureRepository;
    this.metricRepository = metricRepository;
  }

  @Override
  public void execute() {
    Metric qProfilesMetric = metricRepository.getByKey(CoreMetrics.QUALITY_PROFILES_KEY);
    new PathAwareCrawler<>(new NewCoverageAggregationComponentVisitor(qProfilesMetric))
      .visit(treeRootHolder.getRoot());
  }

  private class NewCoverageAggregationComponentVisitor extends PathAwareVisitorAdapter<QProfiles> {

    private final Metric qProfilesMetric;

    public NewCoverageAggregationComponentVisitor(Metric qProfilesMetric) {
      super(CrawlerDepthLimit.MODULE, POST_ORDER, new SimpleStackElementFactory<QProfiles>() {
        @Override
        public QProfiles createForAny(Component component) {
          return new QProfiles();
        }
      });
      this.qProfilesMetric = qProfilesMetric;
    }

    @Override
    public void visitProject(Component project, Path<QProfiles> path) {
      addMeasure(project, path.current());
      Optional<Measure> qProfileMeasure = measureRepository.getRawMeasure(project, qProfilesMetric);
      if (!qProfileMeasure.isPresent() || QPMeasureData.fromJson(qProfileMeasure.get().getData()).getProfiles().isEmpty()) {
        throw MessageException.of(String.format("No quality profiles has been found on project '%s', you probably don't have any language plugin suitable for this analysis.",
          project.getKey()));
      }
    }

    @Override
    public void visitModule(Component module, Path<QProfiles> path) {
      Optional<Measure> measure = measureRepository.getRawMeasure(module, qProfilesMetric);
      QProfiles qProfiles = path.current();
      if (measure.isPresent()) {
        qProfiles.add(measure.get());
      } else {
        addMeasure(module, path.current());
      }
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

    public void add(Measure measure) {
      profilesByKey.putAll(QPMeasureData.fromJson(measure.getStringValue()).getProfilesByKey());
    }

    public void add(QProfiles qProfiles) {
      profilesByKey.putAll(qProfiles.profilesByKey);
    }

    public Measure createMeasure() {
      return Measure.newMeasureBuilder().create(QPMeasureData.toJson(new QPMeasureData(profilesByKey.values())));
    }
  }

  @Override
  public String getDescription() {
    return "Computes Quality Profile measures";
  }
}
