/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.util.Collection;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.sonar.core.metric.ScannerMetrics;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.Component.Status;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.computation.task.step.ComputationStep;

import com.google.common.base.Optional;

public class IncrementalMeasureTransitionStep implements ComputationStep {
  private final TreeRootHolder treeRootHolder;
  private final MeasureRepository measureRepository;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final MetricRepository metricRepository;
  private final ScannerMetrics scannerMetrics;

  public IncrementalMeasureTransitionStep(TreeRootHolder treeRootHolder, MeasureRepository measureRepository,
    AnalysisMetadataHolder analysisMetadataHolder, MetricRepository metricRepository, ScannerMetrics scannerMetrics) {
    this.treeRootHolder = treeRootHolder;
    this.measureRepository = measureRepository;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.metricRepository = metricRepository;
    this.scannerMetrics = scannerMetrics;
  }

  @Override
  public void execute() {
    if (!analysisMetadataHolder.isIncrementalAnalysis()) {
      return;
    }
    Component root = treeRootHolder.getRoot();
    Set<String> scannerMetricsKeys = scannerMetrics.getMetrics().stream()
      .map(org.sonar.api.measures.Metric::getKey)
      .collect(MoreCollectors.toSet());

    Collection<Metric> metrics = StreamSupport.stream(metricRepository.getAll().spliterator(), false)
      .filter(m -> scannerMetricsKeys.contains(m.getKey()))
      .collect(MoreCollectors.toList());

    new DepthTraversalTypeAwareCrawler(new ComponentVisitor(metrics)).visit(root);

  }

  @Override
  public String getDescription() {
    return "Incremental measure transition";
  }

  private class ComponentVisitor extends TypeAwareVisitorAdapter {
    private final Collection<Metric> metrics;

    private ComponentVisitor(Collection<Metric> metrics) {
      super(CrawlerDepthLimit.FILE, ComponentVisitor.Order.PRE_ORDER);
      this.metrics = metrics;
    }

    @Override
    public void visitFile(Component file) {
      if (file.getStatus() != Status.SAME) {
        return;
      }

      for (Metric metric : metrics) {
        Optional<Measure> baseMeasure = measureRepository.getBaseMeasure(file, metric);
        if (baseMeasure.isPresent()) {
          measureRepository.add(file, metric, baseMeasure.get());
        }
      }
    }
  }

}
