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
package org.sonar.core.graph;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class BeanElementsTest {
  TinkerGraph graph;
  Vertex vertex;
  BeanGraph beanGraph;
  BeanVertex beanVertex;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void before() {
    graph = new TinkerGraph();
    beanGraph = new BeanGraph(graph);

    vertex = graph.addVertex(null);
    beanVertex = new BeanVertex() {
    };
    beanVertex.setElement(vertex);
    beanVertex.setBeanGraph(beanGraph);
  }

  @Test
  public void should_wrap_vertex_into_bean() {
    BeanElements elements = new BeanElements();
    Person bean = elements.wrap(vertex, Person.class, beanGraph);

    assertThat(bean).isNotNull();
    assertThat(bean.element()).isSameAs(vertex);
    assertThat(bean.beanGraph()).isSameAs(beanGraph);
  }

  @Test
  public void should_keep_cache_of_beans() {
    BeanElements elements = new BeanElements();
    Person bean = elements.wrap(vertex, Person.class, beanGraph);
    Person bean2 = elements.wrap(vertex, Person.class, beanGraph);

    assertThat(bean).isSameAs(bean2);
  }

  @Test
  public void should_wrap_different_elements() {
    BeanElements elements = new BeanElements();
    Vertex vertex2 = graph.addVertex(null);

    Person bean = elements.wrap(vertex, Person.class, beanGraph);
    Person bean2 = elements.wrap(vertex2, Person.class, beanGraph);

    assertThat(bean).isNotNull();
    assertThat(bean).isNotSameAs(bean2);
  }

  @Test
  public void should_clear_cache() {
    BeanElements elements = new BeanElements();

    Person bean = elements.wrap(vertex, Person.class, beanGraph);
    assertThat(bean).isNotNull();
    elements.clear();

    Person bean2 = elements.wrap(vertex, Person.class, beanGraph);
    assertThat(bean2).isNotNull();
    assertThat(bean2).isNotSameAs(bean);
  }

  @Test
  public void element_could_be_wrapped_by_several_beans() {
    BeanElements elements = new BeanElements();
    Person person = elements.wrap(vertex, Person.class, beanGraph);
    Job job = elements.wrap(vertex, Job.class, beanGraph);

    assertThat(person).isNotSameAs(job);
  }

  @Test
  public void bean_class_should_have_an_empty_constructor() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Class has no default constructor: org.sonar.core.graph.BeanElementsTest$NoEmptyConstructor");

    BeanElements elements = new BeanElements();
    elements.wrap(vertex, NoEmptyConstructor.class, beanGraph);
  }

  @Test
  public void bean_constructor_should_be_accessible() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Can not access to default constructor: org.sonar.core.graph.BeanElementsTest$PrivateConstructor");

    BeanElements elements = new BeanElements();
    elements.wrap(vertex, PrivateConstructor.class, beanGraph);
  }

  static class Person extends BeanVertex {

  }

  static class Job extends BeanVertex {

  }

  static class NoEmptyConstructor extends BeanVertex {
    private int i;

    NoEmptyConstructor(int i) {
      this.i = i;
    }
  }

  static class PrivateConstructor extends BeanVertex {
    private PrivateConstructor() {

    }
  }
}
