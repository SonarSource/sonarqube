/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.sensors;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Scopes;

import java.util.Collection;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ItCoverageDecoratorTest {
  private final ItCoverageDecorator decorator = new ItCoverageDecorator();
  private final Project project = mock(Project.class);

  @Before
  public void before() {
    when(project.getScope()).thenReturn(Scopes.PROJECT);
  }

  @Test
  public void should_use_metrics() {
    Collection<Metric> metrics = decorator.usedMetrics();

    assertThat(metrics).containsOnly(CoreMetrics.IT_LINES_TO_COVER, CoreMetrics.IT_UNCOVERED_LINES, CoreMetrics.NEW_IT_LINES_TO_COVER,
        CoreMetrics.NEW_IT_UNCOVERED_LINES, CoreMetrics.IT_CONDITIONS_TO_COVER, CoreMetrics.IT_UNCOVERED_CONDITIONS,
        CoreMetrics.NEW_IT_CONDITIONS_TO_COVER, CoreMetrics.NEW_IT_UNCOVERED_CONDITIONS);
  }

  @Test
  public void noCoverageWhenStaticAnalysis() {
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.STATIC);
    assertThat(new CoverageDecorator().shouldExecuteOnProject(project)).isFalse();

    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.REUSE_REPORTS);
    assertThat(new CoverageDecorator().shouldExecuteOnProject(project)).isTrue();

    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    assertThat(new CoverageDecorator().shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void coverage() {
    DecoratorContext context = mockContext(50, 40, 10, 8);

    decorator.decorate(project, context);

    // (50-40 covered lines + 10-8 covered conditions) / (50 lines + 10 conditions)
    verify(context).saveMeasure(CoreMetrics.IT_COVERAGE, 20.0);
  }

  @Test
  public void coverageCanBe0() {
    DecoratorContext context = mockContext(50, 50, 5, 5);

    decorator.decorate(project, context);

    verify(context).saveMeasure(CoreMetrics.IT_COVERAGE, 0.0);
  }

  @Test
  public void coverageCanBe100() {
    DecoratorContext context = mockContext(50, 0, 5, 0);

    decorator.decorate(project, context);

    verify(context).saveMeasure(CoreMetrics.IT_COVERAGE, 100.0);
  }

  @Test
  public void noCoverageIfNoElements() {
    DecoratorContext context = mock(DecoratorContext.class);

    decorator.decorate(project, context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.IT_COVERAGE), anyDouble());
  }

  @Test
  public void should_count_elements_for_new_code() {
    Measure newLines = measureWithVariation(1, 100.0);
    Measure newConditions = measureWithVariation(1, 1.0);
    DecoratorContext context = mockNewContext(newLines, null, null, newConditions);

    long count = decorator.countElementsForNewCode(context, 1);

    assertThat(count).isEqualTo(101).isEqualTo(100 + 1);
  }

  @Test
  public void should_count_covered_elements_for_new_code() {
    Measure newLines = measureWithVariation(1, 100.0);
    Measure newUncoveredConditions = measureWithVariation(1, 10.0);
    Measure newUncoveredLines = measureWithVariation(1, 5.0);
    Measure newConditions = measureWithVariation(1, 1.0);
    DecoratorContext context = mockNewContext(newLines, newUncoveredConditions, newUncoveredLines, newConditions);

    long count = decorator.countCoveredElementsForNewCode(context, 1);

    assertThat(count).isEqualTo(86).isEqualTo(100 + 1 - 10 - 5);
  }

  private static DecoratorContext mockContext(int lines, int uncoveredLines, int conditions, int uncoveredConditions) {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.IT_LINES_TO_COVER)).thenReturn(new Measure(CoreMetrics.IT_LINES_TO_COVER, (double) lines));
    when(context.getMeasure(CoreMetrics.IT_UNCOVERED_LINES)).thenReturn(new Measure(CoreMetrics.IT_UNCOVERED_LINES, (double) uncoveredLines));
    when(context.getMeasure(CoreMetrics.IT_CONDITIONS_TO_COVER)).thenReturn(new Measure(CoreMetrics.IT_CONDITIONS_TO_COVER, (double) conditions));
    when(context.getMeasure(CoreMetrics.IT_UNCOVERED_CONDITIONS)).thenReturn(new Measure(CoreMetrics.IT_UNCOVERED_CONDITIONS, (double) uncoveredConditions));
    return context;
  }

  private static DecoratorContext mockNewContext(Measure newLines, Measure newUncoveredConditions, Measure newUncoveredLines, Measure newConditions) {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.NEW_IT_LINES_TO_COVER)).thenReturn(newLines);
    when(context.getMeasure(CoreMetrics.NEW_IT_UNCOVERED_LINES)).thenReturn(newUncoveredLines);
    when(context.getMeasure(CoreMetrics.NEW_IT_UNCOVERED_CONDITIONS)).thenReturn(newUncoveredConditions);
    when(context.getMeasure(CoreMetrics.NEW_IT_CONDITIONS_TO_COVER)).thenReturn(newConditions);
    return context;
  }

  private static Measure measureWithVariation(int period, double variation) {
    Measure measure = mock(Measure.class);
    when(measure.getVariation(period)).thenReturn(variation);
    return measure;
  }
}
