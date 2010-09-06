/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;

public class LineCoverageDecoratorTest {

  private Project project = null;

  @Before
  public void before() {
    project = mock(Project.class);
    when(project.getScope()).thenReturn(Project.SCOPE_SET);
  }

  @Test
  public void noCoverageWhenStaticAnalysis() {
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.STATIC);
    assertThat(new LineCoverageDecorator().shouldExecuteOnProject(project), is(false));

    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.REUSE_REPORTS);
    assertThat(new LineCoverageDecorator().shouldExecuteOnProject(project), is(true));

    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    assertThat(new LineCoverageDecorator().shouldExecuteOnProject(project), is(true));
  }

  @Test
  public void lineCoverage() {
    DecoratorContext context = mockContext(50, 10);
    new LineCoverageDecorator().decorate(project, context);

    // 50-10 covered lines / 50 lines
    verify(context).saveMeasure(CoreMetrics.LINE_COVERAGE, 80.0);
  }


  @Test
  public void noLineCoverageIfNoLines() {
    DecoratorContext context = mockContext(null, null);
    new LineCoverageDecorator().decorate(project, context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.LINE_COVERAGE), anyDouble());
  }

  @Test
  public void zeroCoveredLines() {
    DecoratorContext context = mockContext(50, 50);
    new LineCoverageDecorator().decorate(project, context);

    verify(context).saveMeasure(CoreMetrics.LINE_COVERAGE, 0.0);
  }

  @Test
  public void allCoveredLines() {
    DecoratorContext context = mockContext(50, 00);
    new LineCoverageDecorator().decorate(project, context);

    verify(context).saveMeasure(CoreMetrics.LINE_COVERAGE, 100.0);
  }

  private DecoratorContext mockContext(Integer lines, Integer uncoveredLines) {
    DecoratorContext context = mock(DecoratorContext.class);
    if (lines != null) {
      when(context.getMeasure(CoreMetrics.LINES_TO_COVER)).thenReturn(new Measure(CoreMetrics.LINES_TO_COVER, lines.doubleValue()));
    }
    if (uncoveredLines != null) {
      when(context.getMeasure(CoreMetrics.UNCOVERED_LINES)).thenReturn(new Measure(CoreMetrics.UNCOVERED_LINES, uncoveredLines.doubleValue()));
    }
    return context;
  }

}
