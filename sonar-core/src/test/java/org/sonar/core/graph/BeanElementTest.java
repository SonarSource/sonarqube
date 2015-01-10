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
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BeanElementTest {

  BeanGraph beanGraph;
  BeanVertex beanVertex;

  @Before
  public void before() {
    TinkerGraph graph = new TinkerGraph();
    beanGraph = new BeanGraph(graph);

    Vertex vertex = graph.addVertex(null);
    beanVertex = new BeanVertex() {
    };
    beanVertex.setElement(vertex);
    beanVertex.setBeanGraph(beanGraph);
  }

  @Test
  public void should_set_required_fields() {
    assertThat(beanVertex.beanGraph()).isSameAs(beanGraph);
    assertThat(beanVertex.element()).isInstanceOf(Vertex.class);
  }

  @Test
  public void no_properties_by_default() {
    assertThat(beanVertex.getProperty("color")).isNull();
    assertThat(beanVertex.getProperty("code")).isNull();
    assertThat(beanVertex.getProperty("alive")).isNull();
    assertThat(beanVertex.getPropertyKeys()).isEmpty();
  }

  @Test
  public void should_set_properties() {
    beanVertex.setProperty("color", "red");
    beanVertex.setProperty("code", 123);
    beanVertex.setProperty("alive", true);

    assertThat(beanVertex.getProperty("color")).isEqualTo("red");
    assertThat(beanVertex.getProperty("code")).isEqualTo(123);
    assertThat(beanVertex.getProperty("alive")).isEqualTo(true);
    assertThat(beanVertex.getPropertyKeys()).containsOnly("color", "code", "alive");
  }

  @Test
  public void should_unset_properties_with_null_values() {
    beanVertex.setProperty("color", "red");
    beanVertex.setProperty("code", 123);
    beanVertex.setProperty("alive", true);

    beanVertex.setProperty("color", null);
    beanVertex.setProperty("code", null);
    beanVertex.setProperty("alive", null);
    beanVertex.setProperty("other", null);

    assertThat(beanVertex.getProperty("color")).isNull();
    assertThat(beanVertex.getProperty("code")).isNull();
    assertThat(beanVertex.getProperty("alive")).isNull();
    assertThat(beanVertex.getPropertyKeys()).isEmpty();
  }

  @Test
  public void should_remove_property() {
    beanVertex.setProperty("color", "red");
    beanVertex.setProperty("code", 123);
    beanVertex.setProperty("alive", true);

    beanVertex.removeProperty("color");
    beanVertex.removeProperty("code");
    beanVertex.removeProperty("alive");
    beanVertex.removeProperty("other");

    assertThat(beanVertex.getProperty("color")).isNull();
    assertThat(beanVertex.getProperty("code")).isNull();
    assertThat(beanVertex.getProperty("alive")).isNull();
    assertThat(beanVertex.getPropertyKeys()).isEmpty();
  }
}
