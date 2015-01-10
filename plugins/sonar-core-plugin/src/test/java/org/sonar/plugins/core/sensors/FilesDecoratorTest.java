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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
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
  private Resource resource;

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

}
