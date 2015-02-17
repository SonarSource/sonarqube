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

import edu.emory.mathcs.backport.java.util.Collections;
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

public class SubProjectDsmDecoratorTest {

  private SubProjectDsmDecorator decorator;
  private Project module;
  private DecoratorContext moduleContext;
  private SonarIndex index;
  private Directory dir1;
  private Directory dir2;

  @Before
  public void prepare() {
    index = mock(SonarIndex.class);
    decorator = new SubProjectDsmDecorator(index);
    module = new Project("foo");
    moduleContext = mock(DecoratorContext.class);

    dir1 = Directory.create("src/foo1");
    dir1.setId(1);
    dir2 = Directory.create("src/foo2");
    dir2.setId(2);

    DecoratorContext dir1Context = mock(DecoratorContext.class);
    when(dir1Context.getResource()).thenReturn(dir1);
    DecoratorContext dir2Context = mock(DecoratorContext.class);
    when(dir2Context.getResource()).thenReturn(dir2);

    when(moduleContext.getChildren()).thenReturn(Arrays.asList(dir1Context, dir2Context));

  }

  @Test
  public void testSubProjectDsmDecoratorNoExecution() {
    assertThat(decorator.shouldExecuteOnProject(null)).isTrue();

    // Should not execute on directory
    decorator.decorate(Directory.create("foo"), moduleContext);
    // Should not execute on aggregator projects
    Project p = new Project("parent");
    Project child = new Project("child").setParent(p);
    decorator.decorate(p, moduleContext);

    // Should not do anything if module has no dir
    when(moduleContext.getChildren()).thenReturn(Collections.emptyList());
    decorator.decorate(module, moduleContext);

    verify(moduleContext, never()).saveMeasure(any(Measure.class));
  }

  @Test
  public void testSubProjectDsmDecoratorNoDependency() {
    decorator.decorate(module, moduleContext);

    verify(moduleContext, times(5)).saveMeasure(any(Measure.class));

    verify(moduleContext).saveMeasure(isMeasureWithValue(CoreMetrics.DIRECTORY_CYCLES, 0.0));
    verify(moduleContext).saveMeasure(isMeasureWithValue(CoreMetrics.DIRECTORY_FEEDBACK_EDGES, 0.0));
    verify(moduleContext).saveMeasure(isMeasureWithValue(CoreMetrics.DIRECTORY_TANGLES, 0.0));
    verify(moduleContext).saveMeasure(isMeasureWithValue(CoreMetrics.DIRECTORY_EDGES_WEIGHT, 0.0));
    verify(moduleContext).saveMeasure(isMeasureWithValue(CoreMetrics.DEPENDENCY_MATRIX,
      "[{\"i\":1,\"n\":\"src/foo1\",\"q\":\"DIR\",\"v\":[{},{}]},{\"i\":2,\"n\":\"src/foo2\",\"q\":\"DIR\",\"v\":[{},{}]}]"));
  }

  @Test
  public void testSubProjectDsmDecoratorDependency() {
    Dependency dependency = new Dependency(dir1, dir2).setWeight(1).setId(51L);
    when(index.getEdge(dir1, dir2)).thenReturn(dependency);
    when(index.hasEdge(dir1, dir2)).thenReturn(true);
    when(index.getOutgoingEdges(dir1)).thenReturn(Arrays.asList(dependency));
    when(index.getIncomingEdges(dir2)).thenReturn(Arrays.asList(dependency));

    decorator.decorate(module, moduleContext);

    verify(moduleContext, times(5)).saveMeasure(any(Measure.class));

    verify(moduleContext).saveMeasure(isMeasureWithValue(CoreMetrics.DIRECTORY_CYCLES, 0.0));
    verify(moduleContext).saveMeasure(isMeasureWithValue(CoreMetrics.DIRECTORY_FEEDBACK_EDGES, 0.0));
    verify(moduleContext).saveMeasure(isMeasureWithValue(CoreMetrics.DIRECTORY_TANGLES, 0.0));
    verify(moduleContext).saveMeasure(isMeasureWithValue(CoreMetrics.DIRECTORY_EDGES_WEIGHT, 1.0));

    verify(moduleContext).saveMeasure(
      isMeasureWithValue(CoreMetrics.DEPENDENCY_MATRIX,
        "[{\"i\":1,\"n\":\"src/foo1\",\"q\":\"DIR\",\"v\":[{},{}]},{\"i\":2,\"n\":\"src/foo2\",\"q\":\"DIR\",\"v\":[{\"i\":51,\"w\":1},{}]}]"));
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
