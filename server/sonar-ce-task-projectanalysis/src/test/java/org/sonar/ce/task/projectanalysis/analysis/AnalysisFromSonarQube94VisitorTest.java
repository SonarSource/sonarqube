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

import org.junit.Before;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisFromSonarQube94Visitor.AnalysisFromSonarQube94;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.PathAwareVisitor;
import org.sonar.ce.task.projectanalysis.component.ReportAttributes;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.analysis.AnalysisFromSonarQube94Visitor.*;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.ComponentImpl.builder;

public class AnalysisFromSonarQube94VisitorTest {

  private final MeasureRepository measureRepository = mock(MeasureRepository.class);
  private final MetricRepository metricRepository = mock(MetricRepository.class);

  private final Metric metric = mock(Metric.class);

  private AnalysisFromSonarQube94Visitor underTest;

  @Before
  public void before() {
    when(metricRepository.getByKey(anyString())).thenReturn(metric);
    underTest = new AnalysisFromSonarQube94Visitor(metricRepository, measureRepository);
  }

  @Test
  public void visitProject_createMeasureForMetric() {
    Component project = builder(FILE).setUuid("uuid")
      .setKey("dbKey")
      .setName("name")
      .setStatus(Component.Status.SAME)
      .setReportAttributes(mock(ReportAttributes.class))
      .build();

    PathAwareVisitor.Path<AnalysisFromSonarQube94> path = mock(PathAwareVisitor.Path.class);
    when(path.current()).thenReturn(new AnalysisFromSonarQube94());

    underTest.visitProject(project, path);

    Measure expectedMeasure = Measure.newMeasureBuilder().create(true);
    verify(measureRepository).add(project, metric, expectedMeasure);
  }

  @Test
  public void analysisFromSonarQube94StackFactoryCreateForAny_shouldAlwaysReturnAnalysisFromSonarQube94() {
    AnalysisFromSonarQube94StackFactory factory = new AnalysisFromSonarQube94StackFactory();

    AnalysisFromSonarQube94 analysisFromSonarQube94 = factory.createForAny(mock(Component.class));

    assertThat(analysisFromSonarQube94.sonarQube94OrGreater).isTrue();
  }
}
