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
package org.sonar.core.component;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.KeyIndexableGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.util.ElementHelper;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.component.Component;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Resource;
import org.sonar.core.graph.GraphUtil;

public class ComponentGraph implements BatchComponent, ServerComponent {
  private final KeyIndexableGraph graph;
  private final ElementWrappers wrapperCache;
  private final Vertex rootVertex;

  public ComponentGraph() {
    graph = new TinkerGraph();
    graph.createKeyIndex("key", Vertex.class);
    wrapperCache = new ElementWrappers();
    rootVertex = graph.addVertex(null);
    rootVertex.setProperty("root", "components");
  }

  public ComponentGraph(KeyIndexableGraph graph, Vertex rootVertex) {
    this.graph = graph;
    this.rootVertex = rootVertex;
    wrapperCache = new ElementWrappers();
  }

  public <T extends ElementWrapper> T wrap(Element element, Class<T> wrapperClass) {
    return wrapperCache.wrap(element, wrapperClass, this);
  }

  public <T extends ElementWrapper> T wrap(Component component, Class<T> wrapperClass) {
    Vertex vertex = GraphUtil.single(graph.getVertices("key", component.getKey()));
    T wrapper = wrapperCache.wrap(vertex, wrapperClass, this);
    return wrapper;
  }

  public <T extends ElementWrapper<Vertex>> T createVertex(ElementWrapper<Vertex> from, Class<T> classWrapper, String edgeLabel, String... edgeProperties) {
    T to = createVertex(classWrapper);
    Edge edge = graph.addEdge(null, from.element(), to.element(), edgeLabel);
    ElementHelper.setProperties(edge, edgeProperties);
    return to;
  }

  private <T extends ElementWrapper<Vertex>> T createVertex(Class<T> classWrapper) {
    Vertex vertex = graph.addVertex(null);
    return wrapperCache.wrap(vertex, classWrapper, this);
  }

  public <C extends Component<C>> ComponentWrapper<C> createComponent(C component) {
    Vertex componentVertex = graph.addVertex(null);
    graph.addEdge(null, rootVertex, componentVertex, "component");
    ComponentWrapper wrapper = wrapperCache.wrap(componentVertex, ComponentWrapper.class, this);
    wrapper.populate(component);
    return wrapper;
  }

  public ComponentWrapper createComponent(Resource resource, Snapshot snapshot) {
    return createComponent(new ResourceComponent(resource, snapshot));
  }

  public Graph getUnderlyingGraph() {
    return graph;
  }

  public Vertex getRootVertex() {
    return rootVertex;
  }
}
