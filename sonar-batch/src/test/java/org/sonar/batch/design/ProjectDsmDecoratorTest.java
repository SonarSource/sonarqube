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
package org.sonar.batch.design;

import org.apache.commons.lang.ObjectUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.Project;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProjectDsmDecoratorTest {

  private ProjectDsmDecorator decorator;
  private Project root;
  private DecoratorContext rootContext;
  private SonarIndex index;
  private Project module1;
  private Project module2;

  @Before
  public void prepare() {
    index = mock(SonarIndex.class);
    decorator = new ProjectDsmDecorator(index);
    root = new Project("root");
    rootContext = mock(DecoratorContext.class);

    module1 = new Project("module1").setName("Module1").setParent(root);
    module1.setId(1);
    when(index.getResource(module1)).thenReturn(module1);
    module2 = new Project("module2").setName("Module2").setParent(root);
    module2.setId(2);
    when(index.getResource(module2)).thenReturn(module2);

    DecoratorContext module1Context = mock(DecoratorContext.class);
    when(module1Context.getResource()).thenReturn(module1);
    DecoratorContext module2Context = mock(DecoratorContext.class);
    when(module2Context.getResource()).thenReturn(module2);

    when(rootContext.getChildren()).thenReturn(Arrays.asList(module1Context, module2Context));

  }

  @Test
  public void testProjectDsmDecoratorNoExecution() {
    assertThat(decorator.shouldExecuteOnProject(null)).isTrue();

    // Should not execute on directory
    decorator.decorate(Directory.create("foo"), rootContext);
    // Should not execute on aggregator projects
    Project p = new Project("parent");
    Project child = new Project("child").setParent(p);
    decorator.decorate(p, rootContext);

    verify(rootContext, never()).saveMeasure(any(Measure.class));
  }

  @Test
  public void testProjectDsmDecoratorDependency() {
    Dependency dependency = new Dependency(module1, module2).setWeight(1).setId(51L);
    when(index.getEdge(module1, module2)).thenReturn(dependency);
    when(index.hasEdge(module1, module2)).thenReturn(true);
    when(index.getOutgoingEdges(module1)).thenReturn(Arrays.asList(dependency));
    when(index.getIncomingEdges(module2)).thenReturn(Arrays.asList(dependency));

    decorator.decorate(root, rootContext);

    verify(rootContext, times(1)).saveMeasure(any(Measure.class));

    verify(rootContext).saveMeasure(
      isMeasureWithValue(CoreMetrics.DEPENDENCY_MATRIX,
        "[{\"i\":1,\"n\":\"Module1\",\"q\":\"BRC\",\"v\":[{},{}]},{\"i\":2,\"n\":\"Module2\",\"q\":\"BRC\",\"v\":[{\"i\":51,\"w\":1},{}]}]"));
  }

  Measure isMeasureWithValue(Metric metric, Double value) {
    return argThat(new IsMeasureWithValue(metric, value));
  }

  Measure isMeasureWithValue(Metric metric, String data) {
    return argThat(new IsMeasureWithValue(metric, data));
  }

  class IsMeasureWithValue extends ArgumentMatcher<Measure> {

    private Metric metric;
    private Double value;
    private String data;

    public IsMeasureWithValue(Metric metric, Double value) {
      this.metric = metric;
      this.value = value;
    }

    public IsMeasureWithValue(Metric metric, String data) {
      this.metric = metric;
      this.data = data;
    }

    public boolean matches(Object m) {
      return ((Measure) m).getMetric().equals(metric) && ObjectUtils.equals(((Measure) m).getValue(), value) && ObjectUtils.equals(((Measure) m).getData(), data);
    }
  }
}
