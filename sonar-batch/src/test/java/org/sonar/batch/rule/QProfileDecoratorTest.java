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
package org.sonar.batch.rule;

import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Scopes;
import org.sonar.api.test.IsMeasure;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class QProfileDecoratorTest {

  static final String JAVA_JSON = "{\"key\":\"J1\",\"language\":\"java\",\"name\":\"Java One\",\"rulesUpdatedAt\":\"2014-01-15T10:00:00+0000\"}";
  static final String JAVA2_JSON = "{\"key\":\"J2\",\"language\":\"java\",\"name\":\"Java Two\",\"rulesUpdatedAt\":\"2014-01-15T10:00:00+0000\"}";
  static final String PHP_JSON = "{\"key\":\"P1\",\"language\":\"php\",\"name\":\"Php One\",\"rulesUpdatedAt\":\"2014-01-15T10:00:00+0000\"}";

  Project project = mock(Project.class);
  Project moduleA = mock(Project.class);
  Project moduleB = mock(Project.class);
  Project moduleC = mock(Project.class);
  DecoratorContext decoratorContext = mock(DecoratorContext.class);

  @Test
  public void don_t_run_on_leaf() throws Exception {
    QProfileDecorator decorator = new QProfileDecorator();
    when(project.getModules()).thenReturn(Collections.<Project>emptyList());
    assertThat(decorator.shouldExecuteOnProject(project)).isFalse();

    when(project.getModules()).thenReturn(Arrays.asList(moduleA, moduleB, moduleC));
    assertThat(decorator.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void aggregate() throws Exception {
    Measure measureModuleA = new Measure(CoreMetrics.QUALITY_PROFILES, "[" + JAVA_JSON + "]");
    Measure measureModuleB = new Measure(CoreMetrics.QUALITY_PROFILES, "[" + JAVA_JSON + "]");
    Measure measureModuleC = new Measure(CoreMetrics.QUALITY_PROFILES, "[" + PHP_JSON + "]");
    when(decoratorContext.getChildrenMeasures(CoreMetrics.QUALITY_PROFILES)).thenReturn(Arrays.asList(measureModuleA, measureModuleB, measureModuleC));

    when(project.getScope()).thenReturn(Scopes.PROJECT);

    QProfileDecorator decorator = new QProfileDecorator();
    decorator.decorate(project, decoratorContext);

    verify(decoratorContext).saveMeasure(
      argThat(new IsMeasure(CoreMetrics.QUALITY_PROFILES, "[" + JAVA_JSON + "," + PHP_JSON + "]")));
  }

  @Test
  public void aggregate_different_profiles_with_same_language() throws Exception {
    Measure measureModuleA = new Measure(CoreMetrics.QUALITY_PROFILES, "[" + JAVA_JSON + "]");
    Measure measureModuleB = new Measure(CoreMetrics.QUALITY_PROFILES, "[" + JAVA2_JSON + "]");
    when(decoratorContext.getChildrenMeasures(CoreMetrics.QUALITY_PROFILES)).thenReturn(Arrays.asList(measureModuleA, measureModuleB));

    when(project.getScope()).thenReturn(Scopes.PROJECT);

    QProfileDecorator decorator = new QProfileDecorator();
    decorator.decorate(project, decoratorContext);

    verify(decoratorContext).saveMeasure(
      argThat(new IsMeasure(CoreMetrics.QUALITY_PROFILES, "[" + JAVA_JSON + "," + JAVA2_JSON + "]")));
  }
}
