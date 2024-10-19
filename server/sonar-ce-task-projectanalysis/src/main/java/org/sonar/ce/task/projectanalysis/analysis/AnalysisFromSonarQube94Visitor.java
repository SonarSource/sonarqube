/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.analysis;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.PathAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;

public class AnalysisFromSonarQube94Visitor extends PathAwareVisitorAdapter<AnalysisFromSonarQube94Visitor.AnalysisFromSonarQube94> {

  private final MeasureRepository measureRepository;
  private final Metric analysisFromSonarQube94Metric;

  public AnalysisFromSonarQube94Visitor(MetricRepository metricRepository, MeasureRepository measureRepository) {
    super(CrawlerDepthLimit.PROJECT, Order.PRE_ORDER, new AnalysisFromSonarQube94StackFactory());

    this.measureRepository = measureRepository;
    this.analysisFromSonarQube94Metric = metricRepository.getByKey(CoreMetrics.ANALYSIS_FROM_SONARQUBE_9_4_KEY);
  }

  @Override
  public void visitProject(Component project, Path<AnalysisFromSonarQube94Visitor.AnalysisFromSonarQube94> path) {
    measureRepository.add(project, analysisFromSonarQube94Metric, Measure.newMeasureBuilder().create(path.current().sonarQube94OrGreater));
  }

  public static final class AnalysisFromSonarQube94StackFactory extends SimpleStackElementFactory<AnalysisFromSonarQube94> {

    @Override
    public AnalysisFromSonarQube94 createForAny(Component component) {
      return new AnalysisFromSonarQube94();
    }

    /** Stack item is not used at ProjectView level, saves on instantiating useless objects */
    @Override
    public AnalysisFromSonarQube94 createForProjectView(Component projectView) {
      return null;
    }
  }

  public static final class AnalysisFromSonarQube94 {
    final boolean sonarQube94OrGreater = true;
  }
}
