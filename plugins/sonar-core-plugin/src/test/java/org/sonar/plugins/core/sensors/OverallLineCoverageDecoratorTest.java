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
package org.sonar.plugins.core.sensors;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Scopes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OverallLineCoverageDecoratorTest {
  private final OverallLineCoverageDecorator decorator = new OverallLineCoverageDecorator();
  private final Project project = mock(Project.class);

  @Before
  public void before() {
    when(project.getScope()).thenReturn(Scopes.PROJECT);
  }

  @Test
  public void should_depend_on_coverage_metrics() {
    List<Metric> metrics = decorator.dependsUponMetrics();

    assertThat(metrics).containsOnly(CoreMetrics.OVERALL_UNCOVERED_LINES, CoreMetrics.OVERALL_LINES_TO_COVER, CoreMetrics.NEW_OVERALL_UNCOVERED_LINES,
      CoreMetrics.NEW_OVERALL_LINES_TO_COVER);
  }

  @Test
  public void lineCoverage() {
    DecoratorContext context = mockContext(50, 10);

    decorator.decorate(project, context);

    // 50-10 covered lines / 50 lines
    verify(context).saveMeasure(CoreMetrics.OVERALL_LINE_COVERAGE, 80.0);
  }

  @Test
  public void zeroCoveredLines() {
    DecoratorContext context = mockContext(50, 50);

    decorator.decorate(project, context);

    verify(context).saveMeasure(CoreMetrics.OVERALL_LINE_COVERAGE, 0.0);
  }

  @Test
  public void allCoveredLines() {
    DecoratorContext context = mockContext(50, 00);

    decorator.decorate(project, context);

    verify(context).saveMeasure(CoreMetrics.OVERALL_LINE_COVERAGE, 100.0);
  }

  @Test
  public void noLineCoverageIfNoLines() {
    DecoratorContext context = mock(DecoratorContext.class);

    decorator.decorate(project, context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.OVERALL_LINE_COVERAGE), anyDouble());
  }

  private static DecoratorContext mockContext(int lines, int uncoveredLines) {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.OVERALL_LINES_TO_COVER)).thenReturn(new Measure(CoreMetrics.OVERALL_LINES_TO_COVER, (double) lines));
    when(context.getMeasure(CoreMetrics.OVERALL_UNCOVERED_LINES)).thenReturn(new Measure(CoreMetrics.OVERALL_UNCOVERED_LINES, (double) uncoveredLines));
    return context;
  }
}
