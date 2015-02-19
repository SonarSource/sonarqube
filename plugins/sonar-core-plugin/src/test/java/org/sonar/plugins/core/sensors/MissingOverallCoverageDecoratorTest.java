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
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class MissingOverallCoverageDecoratorTest {

  private MissingOverallCoverageDecorator decorator;

  @Before
  public void prepare() {
    decorator = new MissingOverallCoverageDecorator();
  }

  @Test
  public void increaseCoverage() {
    assertThat(decorator.provides()).isNotEmpty();
    assertThat(decorator.dependsUpon()).isEqualTo(CoreMetrics.LINES_TO_COVER);
    assertThat(decorator.shouldExecuteOnProject(new Project("foo"))).isTrue();
  }

  @Test
  public void testExecuteOnlyOnMainFile() {
    DecoratorContext context = mock(DecoratorContext.class);
    decorator.decorate(File.create("test/FooTest.java", Java.INSTANCE, true), context);
    decorator.decorate(Directory.create("src"), context);
    decorator.decorate(new Project("foo"), context);
    verifyNoMoreInteractions(context);
  }

  @Test
  public void dontDoAnythingIfOverallCoverageAlreadyDefined() {
    DecoratorContext context = mock(DecoratorContext.class);
    File file = File.create("src/Foo.java");

    when(context.getMeasure(CoreMetrics.OVERALL_LINES_TO_COVER)).thenReturn(new Measure<>(CoreMetrics.OVERALL_LINES_TO_COVER, 0.0));

    decorator.decorate(file, context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.OVERALL_LINES_TO_COVER), anyDouble());
  }

  @Test
  public void testCopyUnitTestMeasures() {
    DecoratorContext context = mock(DecoratorContext.class);
    File file = File.create("src/Foo.java");

    when(context.getMeasure(CoreMetrics.LINES_TO_COVER)).thenReturn(new Measure<>(CoreMetrics.LINES_TO_COVER, 10.0));
    when(context.getMeasure(CoreMetrics.UNCOVERED_LINES)).thenReturn(new Measure<>(CoreMetrics.UNCOVERED_LINES, 5.0));
    when(context.getMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA)).thenReturn(new Measure<>(CoreMetrics.COVERAGE_LINE_HITS_DATA, "1=1;2=2;"));
    when(context.getMeasure(CoreMetrics.CONDITIONS_TO_COVER)).thenReturn(new Measure<>(CoreMetrics.CONDITIONS_TO_COVER, 2.0));
    when(context.getMeasure(CoreMetrics.UNCOVERED_CONDITIONS)).thenReturn(new Measure<>(CoreMetrics.UNCOVERED_CONDITIONS, 1.0));
    when(context.getMeasure(CoreMetrics.CONDITIONS_BY_LINE)).thenReturn(new Measure<>(CoreMetrics.CONDITIONS_BY_LINE, "1=4"));
    when(context.getMeasure(CoreMetrics.COVERED_CONDITIONS_BY_LINE)).thenReturn(new Measure<>(CoreMetrics.COVERED_CONDITIONS_BY_LINE, "1=2"));

    decorator.decorate(file, context);

    verify(context).saveMeasure(CoreMetrics.OVERALL_LINES_TO_COVER, 10.0);
    verify(context).saveMeasure(CoreMetrics.OVERALL_UNCOVERED_LINES, 5.0);
    verify(context).saveMeasure(new Measure(CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA, "1=1;2=2;"));
    verify(context).saveMeasure(CoreMetrics.OVERALL_CONDITIONS_TO_COVER, 2.0);
    verify(context).saveMeasure(CoreMetrics.OVERALL_UNCOVERED_CONDITIONS, 1.0);
    verify(context).saveMeasure(new Measure(CoreMetrics.OVERALL_CONDITIONS_BY_LINE, "1=4"));
    verify(context).saveMeasure(new Measure(CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE, "1=2"));
  }

  @Test
  public void dontFailOnBrokenValues() {
    DecoratorContext context = mock(DecoratorContext.class);
    File file = File.create("src/Foo.java");

    when(context.getMeasure(CoreMetrics.LINES_TO_COVER)).thenReturn(new Measure<>(CoreMetrics.LINES_TO_COVER, 10.0));
    when(context.getMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA)).thenReturn(new Measure<>(CoreMetrics.COVERAGE_LINE_HITS_DATA));

    decorator.decorate(file, context);

    verify(context).saveMeasure(CoreMetrics.OVERALL_LINES_TO_COVER, 10.0);
  }
}
