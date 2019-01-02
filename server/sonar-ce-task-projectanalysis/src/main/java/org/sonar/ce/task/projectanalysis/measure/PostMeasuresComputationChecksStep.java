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
package org.sonar.ce.task.projectanalysis.measure;

import java.util.Optional;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.ComputationStep;

/**
 * Execute {@link PostMeasuresComputationCheck} instances in no specific order.
 * If an extension fails (throws an exception), consecutive extensions
 * won't be called.
 */
@ComputeEngineSide
public class PostMeasuresComputationChecksStep implements ComputationStep {

  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final PostMeasuresComputationCheck[] extensions;

  public PostMeasuresComputationChecksStep(TreeRootHolder treeRootHolder, MetricRepository metricRepository, MeasureRepository measureRepository,
    AnalysisMetadataHolder analysisMetadataHolder, PostMeasuresComputationCheck[] extensions) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.extensions = extensions;
  }

  /**
   * Used when zero {@link PostMeasuresComputationCheck} are registered into container.
   */
  public PostMeasuresComputationChecksStep(TreeRootHolder treeRootHolder, MetricRepository metricRepository, MeasureRepository measureRepository,
    AnalysisMetadataHolder analysisMetadataHolder) {
    this(treeRootHolder, metricRepository, measureRepository, analysisMetadataHolder, new PostMeasuresComputationCheck[0]);
  }

  @Override
  public void execute(ComputationStep.Context context) {
    PostMeasuresComputationCheck.Context extensionContext = new ContextImpl();
    for (PostMeasuresComputationCheck extension : extensions) {
      extension.onCheck(extensionContext);
    }
  }

  @Override
  public String getDescription() {
    return "Checks executed after computation of measures";
  }

  private class ContextImpl implements PostMeasuresComputationCheck.Context {

    @Override
    public String getProjectUuid() {
      return analysisMetadataHolder.getProject().getUuid();
    }

    @Override
    public int getNcloc() {
      Metric nclocMetric = metricRepository.getByKey(CoreMetrics.NCLOC_KEY);
      Optional<Measure> nclocMeasure = measureRepository.getRawMeasure(treeRootHolder.getRoot(), nclocMetric);
      return nclocMeasure.map(Measure::getIntValue).orElse(0);
    }

    @Override
    public String getOrganizationUuid() {
      return analysisMetadataHolder.getOrganization().getUuid();
    }

    @Override
    public String getOrganizationKey() {
      return analysisMetadataHolder.getOrganization().getKey();
    }
  }
}
