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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.sonar.api.resources.Resource;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;

public class CoverageDecoratorTest {

  private Project project = null;

  @Before
  public void before() {
    project = mock(Project.class);
    when(project.getScope()).thenReturn(Resource.SCOPE_SET);
  }

  @Test
  public void noCoverageWhenStaticAnalysis() {
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.STATIC);
    assertThat(new CoverageDecorator().shouldExecuteOnProject(project), is(false));

    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.REUSE_REPORTS);
    assertThat(new CoverageDecorator().shouldExecuteOnProject(project), is(true));

    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    assertThat(new CoverageDecorator().shouldExecuteOnProject(project), is(true));
  }

  @Test
  public void coverage() {
    DecoratorContext context = mockContext(50, 40, 10, 8);

    new CoverageDecorator().decorate(project, context);

    // (50-40 covered lines + 10-8 covered conditions) / (50 lines + 10 conditions)
    verify(context).saveMeasure(CoreMetrics.COVERAGE, 20.0);
  }

  @Test
  public void coverageCanBe0() {
    DecoratorContext context = mockContext(50, 50, 5, 5);
    new CoverageDecorator().decorate(project, context);

    verify(context).saveMeasure(CoreMetrics.COVERAGE, 0.0);
  }

  @Test
  public void coverageCanBe100() {
    DecoratorContext context = mockContext(50, 0, 5, 0);
    new CoverageDecorator().decorate(project, context);

    verify(context).saveMeasure(CoreMetrics.COVERAGE, 100.0);
  }

  @Test
  public void noCoverageIfNoElements() {
    DecoratorContext context = mockContext(null, null, null, null);
    new CoverageDecorator().decorate(project, context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.COVERAGE), anyDouble());
  }

  private DecoratorContext mockContext(Integer lines, Integer uncoveredLines, Integer conditions, Integer uncoveredConditions) {
    DecoratorContext context = mock(DecoratorContext.class);
    if (lines != null) {
      when(context.getMeasure(CoreMetrics.LINES_TO_COVER)).thenReturn(new Measure(CoreMetrics.LINES_TO_COVER, lines.doubleValue()));
    }
    if (uncoveredLines != null) {
      when(context.getMeasure(CoreMetrics.UNCOVERED_LINES)).thenReturn(new Measure(CoreMetrics.UNCOVERED_LINES, uncoveredLines.doubleValue()));
    }
    if (conditions != null) {
      when(context.getMeasure(CoreMetrics.CONDITIONS_TO_COVER)).thenReturn(new Measure(CoreMetrics.CONDITIONS_TO_COVER, conditions.doubleValue()));
    }
    if (uncoveredConditions != null) {
      when(context.getMeasure(CoreMetrics.UNCOVERED_CONDITIONS)).thenReturn(new Measure(CoreMetrics.UNCOVERED_CONDITIONS, uncoveredConditions.doubleValue()));
    }
    return context;
  }
}
