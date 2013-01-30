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
package org.sonar.core.graph;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class BeanGraphTest {

  @Test
  public void should_get_underlying_graph() {
    TinkerGraph graph = new TinkerGraph();
    BeanGraph beanGraph = new BeanGraph(graph);

    assertThat(beanGraph.getUnderlyingGraph()).isSameAs(graph);
  }

  @Test
  public void should_create_bean_vertex() {
    TinkerGraph graph = new TinkerGraph();
    BeanGraph beanGraph = new BeanGraph(graph);
    Person person = beanGraph.createVertex(Person.class);

    assertThat(person).isNotNull();
    assertThat(person.element()).isNotNull();
    assertThat(person.element().getId()).isNotNull();
    assertThat(person.beanGraph()).isSameAs(beanGraph);
    assertThat(person.age()).isNull();
  }

  @Test
  public void should_wrap_existing_element() {
    TinkerGraph graph = new TinkerGraph();
    BeanGraph beanGraph = new BeanGraph(graph);
    Vertex vertex = graph.addVertex(null);
    vertex.setProperty("age", 42);

    Person person = beanGraph.wrap(vertex, Person.class);
    assertThat(person).isNotNull();
    assertThat(person.element()).isSameAs(vertex);
    assertThat(person.age()).isEqualTo(42);
  }

  @Test
  public void should_create_adjacent_bean_vertex() {
    TinkerGraph graph = new TinkerGraph();
    BeanGraph beanGraph = new BeanGraph(graph);
    Person person = beanGraph.createVertex(Person.class);

    Person adjacent = beanGraph.createAdjacentVertex(person, Person.class, "knows", "type", "family");
    assertThat(adjacent).isNotNull();
    assertThat(person.knows()).hasSize(1);
    assertThat(person.knows().iterator().next()).isSameAs(adjacent);
    assertThat(adjacent.knows()).isEmpty();
  }

  static class Person extends BeanVertex {
    Integer age() {
      return (Integer) getProperty("age");
    }

    Iterable<Person> knows() {
      return getVertices(Person.class, Direction.OUT, "knows");
    }
  }
}
