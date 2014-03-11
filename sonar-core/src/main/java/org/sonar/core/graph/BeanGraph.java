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

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.util.ElementHelper;

import javax.annotation.Nullable;

public class BeanGraph {
  private final Graph graph;
  private final BeanElements beans;

  public BeanGraph(Graph graph) {
    this.graph = graph;
    this.beans = new BeanElements();
  }

  public static BeanGraph createInMemory() {
    return new BeanGraph(new TinkerGraph());
  }

  public final <T extends BeanElement> T wrap(@Nullable Element element, Class<T> beanClass) {
    return element != null ? beans.wrap(element, beanClass, this) : null;
  }

  public final <T extends BeanVertex> T createAdjacentVertex(BeanVertex from, Class<T> beanClass, String edgeLabel, String... edgeProperties) {
    T to = createVertex(beanClass);
    Edge edge = graph.addEdge(null, from.element(), to.element(), edgeLabel);
    ElementHelper.setProperties(edge, edgeProperties);
    return to;
  }

  public final <T extends BeanVertex> T createVertex(Class<T> beanClass) {
    Vertex vertex = graph.addVertex(null);
    return beans.wrap(vertex, beanClass, this);
  }

  public final Graph getUnderlyingGraph() {
    return graph;
  }
}
