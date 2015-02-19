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
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.core.config.CorePropertyDefinitions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class MissingCoverageDecoratorTest {

  private Settings settings;
  private MissingCoverageDecorator decorator;

  @Before
  public void prepare() {
    settings = new Settings(new PropertyDefinitions(CorePropertyDefinitions.all()));
    decorator = new MissingCoverageDecorator(settings);
  }

  @Test
  public void increaseCoverage() {
    assertThat(decorator.provides()).isEqualTo(CoreMetrics.LINES_TO_COVER);
    assertThat(decorator.dependsUpon()).isEqualTo(CoreMetrics.NCLOC);
  }

  @Test
  public void testShouldExecute() {
    assertThat(decorator.shouldExecuteOnProject(new Project("foo"))).isTrue();
    settings.setProperty(CoreProperties.COVERAGE_UNFORCED_KEY, "true");
    assertThat(decorator.shouldExecuteOnProject(new Project("foo"))).isFalse();
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
  public void dontDoAnythingIfLinesToCoverAlreadyDefined() {
    DecoratorContext context = mock(DecoratorContext.class);
    File file = File.create("src/Foo.java");

    when(context.getMeasure(CoreMetrics.LINES_TO_COVER)).thenReturn(new Measure<>(CoreMetrics.LINES_TO_COVER, 0.0));
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure<>(CoreMetrics.NCLOC, 10.0));

    decorator.decorate(file, context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.LINES_TO_COVER), anyDouble());
    verify(context, never()).saveMeasure(eq(CoreMetrics.UNCOVERED_LINES), anyDouble());
  }

  @Test
  public void testUseNclocDataIfPossible() {
    DecoratorContext context = mock(DecoratorContext.class);
    File file = File.create("src/Foo.java");

    when(context.getMeasure(CoreMetrics.LINES_TO_COVER)).thenReturn(null);
    when(context.getMeasure(CoreMetrics.NCLOC_DATA)).thenReturn(new Measure<>(CoreMetrics.NCLOC_DATA, "1=0;2=1;3=0;4=1"));

    decorator.decorate(file, context);

    verify(context).saveMeasure(new Measure(CoreMetrics.LINES_TO_COVER, 2.0));
    verify(context).saveMeasure(new Measure(CoreMetrics.UNCOVERED_LINES, 2.0));
    verify(context).saveMeasure(new Measure(CoreMetrics.COVERAGE_LINE_HITS_DATA, "2=0;4=0"));
  }

  @Test
  public void testUseNclocAsLinesToCover() {
    DecoratorContext context = mock(DecoratorContext.class);
    File file = File.create("src/Foo.java");

    when(context.getMeasure(CoreMetrics.LINES_TO_COVER)).thenReturn(null);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure<>(CoreMetrics.NCLOC, 10.0));

    decorator.decorate(file, context);

    verify(context).saveMeasure(CoreMetrics.LINES_TO_COVER, 10.0);
    verify(context).saveMeasure(CoreMetrics.UNCOVERED_LINES, 10.0);
  }
}
