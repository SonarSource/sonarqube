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

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FilesDecoratorTest {

  private FilesDecorator decorator;

  @Mock
  private DecoratorContext context;

  @Mock
  private Resource<?> resource;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    decorator = new FilesDecorator();
  }

  @Test
  public void generatesMetrics() {
    assertThat(decorator.generateDirectoriesMetric()).isEqualTo(CoreMetrics.FILES);
  }

  @Test
  public void shouldExecute() {
    assertThat(decorator.shouldExecuteOnProject(mock(Project.class))).isEqualTo(true);
  }

  @Test
  public void shouldNotSaveIfMeasureAlreadyExists() {
    when(context.getMeasure(CoreMetrics.FILES)).thenReturn(new Measure(CoreMetrics.FILES, 1.0));

    decorator.decorate(resource, context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.FILES), anyDouble());
  }

  @Test
  public void shouldSaveOneForFile() {
    when(resource.getQualifier()).thenReturn(Qualifiers.FILE);

    decorator.decorate(resource, context);

    verify(context, times(1)).saveMeasure(eq(CoreMetrics.FILES), eq(1d));
  }

  @Test
  public void shouldSaveOneForClass() {
    when(resource.getQualifier()).thenReturn(Qualifiers.CLASS);

    decorator.decorate(resource, context);

    verify(context, times(1)).saveMeasure(eq(CoreMetrics.FILES), eq(1d));
  }

  @Test
  public void shouldSumChildren() {
    when(context.getChildrenMeasures(CoreMetrics.FILES)).thenReturn(Arrays.asList(new Measure(CoreMetrics.FILES, 2.0), new Measure(CoreMetrics.FILES, 3.0)));

    decorator.decorate(resource, context);

    verify(context).saveMeasure(eq(CoreMetrics.FILES), eq(5.0));
  }

  @Test
  public void shouldFailOnRootProjectIfNoFile() {
    Project project = createMockProject();
    when(context.getChildrenMeasures(CoreMetrics.FILES)).thenReturn(Arrays.asList(new Measure(CoreMetrics.FILES, 0.0)));

    thrown.expect(SonarException.class);
    thrown.expectMessage("Project \"Foo\" does not contain any file in its source folders:\n");
    thrown.expectMessage("- " + new File("target/temp").getAbsolutePath() + "\n");
    thrown.expectMessage("\nPlease check your project configuration.");

    decorator.decorate(project, context);
  }

  @Test
  public void shouldFailOnRootProjectIfNoFileMeasure() {
    Project project = createMockProject();

    thrown.expect(SonarException.class);
    thrown.expectMessage("Project \"Foo\" does not contain any file in its source folders");

    decorator.decorate(project, context);
  }

  private Project createMockProject() {
    Project project = mock(Project.class);
    when(project.getQualifier()).thenReturn(Qualifiers.PROJECT);
    when(project.getName()).thenReturn("Foo");
    ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
    when(fileSystem.getSourceDirs()).thenReturn(Lists.newArrayList(new File("target/temp")));
    when(project.getFileSystem()).thenReturn(fileSystem);
    return project;
  }

  @Test
  public void shouldNotFailOnModuleIfNoFile() {
    when(resource.getQualifier()).thenReturn(Qualifiers.MODULE);
    when(context.getChildrenMeasures(CoreMetrics.FILES)).thenReturn(Arrays.asList(new Measure(CoreMetrics.FILES, 0.0)));

    decorator.decorate(resource, context);

    verify(context).saveMeasure(eq(CoreMetrics.FILES), eq(0.0));
  }

}
