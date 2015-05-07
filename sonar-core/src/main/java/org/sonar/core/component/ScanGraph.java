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
package org.sonar.core.component;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import org.sonar.api.BatchSide;
import org.sonar.api.component.Component;
import org.sonar.api.resources.Resource;
import org.sonar.core.graph.BeanGraph;
import org.sonar.core.graph.BeanIterable;
import org.sonar.core.graph.GraphUtil;

@BatchSide
public class ScanGraph extends BeanGraph {

  private static final String COMPONENT = "component";
  private final Vertex componentsRoot;

  private ScanGraph(Graph graph) {
    super(graph);
    componentsRoot = graph.addVertex(null);
    componentsRoot.setProperty("root", "components");
  }

  public static ScanGraph create() {
    TinkerGraph graph = new TinkerGraph();
    graph.createKeyIndex("key", Vertex.class);
    return new ScanGraph(graph);
  }

  public ComponentVertex wrapComponent(Vertex vertex) {
    return wrap(vertex, ComponentVertex.class);
  }

  public ComponentVertex getComponent(String key) {
    Vertex vertex = GraphUtil.single(getUnderlyingGraph().getVertices("key", key));
    return vertex != null ? wrapComponent(vertex) : null;
  }

  public ComponentVertex addComponent(Resource resource) {
    return addComponent(new ResourceComponent(resource));
  }

  public void completeComponent(String key, long resourceId, long snapshotId) {
    ComponentVertex component = getComponent(key);
    component.setProperty("sid", snapshotId);
    component.setProperty("rid", resourceId);
  }

  public Iterable<ComponentVertex> getComponents() {
    Iterable<Vertex> componentVertices = componentsRoot.getVertices(Direction.OUT, COMPONENT);
    return new BeanIterable<ComponentVertex>(this, ComponentVertex.class, componentVertices);
  }

  public ComponentVertex addComponent(Component component) {
    Vertex vertex = getUnderlyingGraph().addVertex(null);
    getUnderlyingGraph().addEdge(null, componentsRoot, vertex, COMPONENT);
    ComponentVertex wrapper = wrap(vertex, ComponentVertex.class);
    wrapper.copyFrom(component);
    return wrapper;
  }
}
