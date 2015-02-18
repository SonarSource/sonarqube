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
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DirectoryDsmDecoratorTest {

  private DirectoryDsmDecorator decorator;
  private Directory dir;
  private DecoratorContext dirContext;
  private SonarIndex index;
  private File file1;
  private File file2;
  private DecoratorContext file1Context;
  private DecoratorContext file2Context;

  @Before
  public void prepare() {
    index = mock(SonarIndex.class);
    decorator = new DirectoryDsmDecorator(index);
    dir = Directory.create("src");
    dirContext = mock(DecoratorContext.class);

    file1 = File.create("src/Foo1.java", null, false);
    file1.setId(1);
    file2 = File.create("src/Foo2.java", null, false);
    file2.setId(2);

    file1Context = mock(DecoratorContext.class);
    when(file1Context.getResource()).thenReturn(file1);
    file2Context = mock(DecoratorContext.class);
    when(file2Context.getResource()).thenReturn(file2);

    when(dirContext.getChildren()).thenReturn(Arrays.asList(file1Context, file2Context));

  }

  @Test
  public void testDirectoryDsmDecoratorNoExecution() {
    assertThat(decorator.shouldExecuteOnProject(null)).isTrue();

    // Should not execute on project
    decorator.decorate(new Project("foo"), dirContext);

    // Should not do anything if dir has no files
    when(dirContext.getChildren()).thenReturn(Collections.emptyList());
    decorator.decorate(dir, dirContext);

    verify(dirContext, never()).saveMeasure(any(Measure.class));
  }

  @Test
  public void testDirectoryDsmDecoratorNoDependency() {
    decorator.decorate(dir, dirContext);

    verify(dirContext, times(5)).saveMeasure(any(Measure.class));

    verify(dirContext).saveMeasure(isMeasureWithValue(CoreMetrics.FILE_CYCLES, 0.0));
    verify(dirContext).saveMeasure(isMeasureWithValue(CoreMetrics.FILE_FEEDBACK_EDGES, 0.0));
    verify(dirContext).saveMeasure(isMeasureWithValue(CoreMetrics.FILE_TANGLES, 0.0));
    verify(dirContext).saveMeasure(isMeasureWithValue(CoreMetrics.FILE_EDGES_WEIGHT, 0.0));
    verify(dirContext).saveMeasure(
      isMeasureWithValue(CoreMetrics.DEPENDENCY_MATRIX,
        "[{\"i\":1,\"n\":\"Foo1.java\",\"q\":\"FIL\",\"v\":[{},{}]},{\"i\":2,\"n\":\"Foo2.java\",\"q\":\"FIL\",\"v\":[{},{}]}]"));

  }

  @Test
  public void testDirectoryDsmDecoratorDependency() {
    Dependency dependency = new Dependency(file1, file2).setWeight(1).setId(51L);
    when(index.getEdge(file1, file2)).thenReturn(dependency);
    when(index.hasEdge(file1, file2)).thenReturn(true);
    when(index.getOutgoingEdges(file1)).thenReturn(Arrays.asList(dependency));
    when(index.getIncomingEdges(file2)).thenReturn(Arrays.asList(dependency));

    decorator.decorate(dir, dirContext);

    verify(dirContext, times(5)).saveMeasure(any(Measure.class));

    verify(dirContext).saveMeasure(isMeasureWithValue(CoreMetrics.FILE_CYCLES, 0.0));
    verify(dirContext).saveMeasure(isMeasureWithValue(CoreMetrics.FILE_FEEDBACK_EDGES, 0.0));
    verify(dirContext).saveMeasure(isMeasureWithValue(CoreMetrics.FILE_TANGLES, 0.0));
    verify(dirContext).saveMeasure(isMeasureWithValue(CoreMetrics.FILE_EDGES_WEIGHT, 1.0));

    verify(dirContext).saveMeasure(
      isMeasureWithValue(CoreMetrics.DEPENDENCY_MATRIX,
        "[{\"i\":1,\"n\":\"Foo1.java\",\"q\":\"FIL\",\"v\":[{},{}]},{\"i\":2,\"n\":\"Foo2.java\",\"q\":\"FIL\",\"v\":[{\"i\":51,\"w\":1},{}]}]"));
  }

  @Test
  public void testDirectoryDsmDecoratorNoDSMIfMoreThan200Components() {
    Dependency dependency = new Dependency(file1, file2).setWeight(1).setId(51L);
    when(index.getEdge(file1, file2)).thenReturn(dependency);
    when(index.hasEdge(file1, file2)).thenReturn(true);
    when(index.getOutgoingEdges(file1)).thenReturn(Arrays.asList(dependency));
    when(index.getIncomingEdges(file2)).thenReturn(Arrays.asList(dependency));

    List<DecoratorContext> contexts = new ArrayList<DecoratorContext>(201);
    contexts.add(file1Context);
    contexts.add(file2Context);
    for (int i = 0; i < 199; i++) {
      DecoratorContext fileContext = mock(DecoratorContext.class);
      when(fileContext.getResource()).thenReturn(File.create("file" + i));
      contexts.add(fileContext);
    }

    when(dirContext.getChildren()).thenReturn(contexts);

    decorator.decorate(dir, dirContext);

    verify(dirContext, times(4)).saveMeasure(any(Measure.class));

    verify(dirContext).saveMeasure(isMeasureWithValue(CoreMetrics.FILE_CYCLES, 0.0));
    verify(dirContext).saveMeasure(isMeasureWithValue(CoreMetrics.FILE_FEEDBACK_EDGES, 0.0));
    verify(dirContext).saveMeasure(isMeasureWithValue(CoreMetrics.FILE_TANGLES, 0.0));
    verify(dirContext).saveMeasure(isMeasureWithValue(CoreMetrics.FILE_EDGES_WEIGHT, 1.0));
  }

  @Test
  public void testDirectoryDsmDecoratorCycleDependency() {
    Dependency dependency1to2 = new Dependency(file1, file2).setWeight(1).setId(50L);
    when(index.getEdge(file1, file2)).thenReturn(dependency1to2);
    when(index.hasEdge(file1, file2)).thenReturn(true);
    when(index.getOutgoingEdges(file1)).thenReturn(Arrays.asList(dependency1to2));
    when(index.getIncomingEdges(file2)).thenReturn(Arrays.asList(dependency1to2));
    Dependency dependency2to1 = new Dependency(file2, file1).setWeight(2).setId(51L);
    when(index.getEdge(file2, file1)).thenReturn(dependency2to1);
    when(index.hasEdge(file2, file1)).thenReturn(true);
    when(index.getOutgoingEdges(file2)).thenReturn(Arrays.asList(dependency2to1));
    when(index.getIncomingEdges(file1)).thenReturn(Arrays.asList(dependency2to1));

    decorator.decorate(dir, dirContext);

    verify(dirContext, times(5)).saveMeasure(any(Measure.class));

    verify(dirContext).saveMeasure(isMeasureWithValue(CoreMetrics.FILE_CYCLES, 1.0));
    verify(dirContext).saveMeasure(isMeasureWithValue(CoreMetrics.FILE_FEEDBACK_EDGES, 1.0));
    verify(dirContext).saveMeasure(isMeasureWithValue(CoreMetrics.FILE_TANGLES, 1.0));
    verify(dirContext).saveMeasure(isMeasureWithValue(CoreMetrics.FILE_EDGES_WEIGHT, 3.0));

    verify(dirContext).saveMeasure(
      isMeasureWithValue(CoreMetrics.DEPENDENCY_MATRIX,
        "[{\"i\":2,\"n\":\"Foo2.java\",\"q\":\"FIL\",\"v\":[{},{\"i\":50,\"w\":1}]},{\"i\":1,\"n\":\"Foo1.java\",\"q\":\"FIL\",\"v\":[{\"i\":51,\"w\":2},{}]}]"));
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
