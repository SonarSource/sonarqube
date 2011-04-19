/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Project;

public class UnitTestDecoratorTest {

  private UnitTestDecorator decorator;

  @Before
  public void setUp() {
    decorator = new UnitTestDecorator();
  }

  @Test
  public void generatesMetrics() {
    assertThat(decorator.generatesMetrics().size(), is(5));
  }

  @Test
  public void doNotDecorateStaticAnalysis() {
    Project project = mock(Project.class);
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.STATIC);
    assertThat(decorator.shouldExecuteOnProject(project), is(false));

    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    assertThat(decorator.shouldExecuteOnProject(project), is(true));
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2371
   */
  @Test
  public void shouldSaveZeroOnProject() {
    DecoratorContext context = mock(DecoratorContext.class);
    Project project = new Project("");
    project.setAnalysisType(Project.AnalysisType.DYNAMIC);

    decorator.decorate(project, context);

    verify(context).saveMeasure(CoreMetrics.TESTS, 0.0);
  }

}
