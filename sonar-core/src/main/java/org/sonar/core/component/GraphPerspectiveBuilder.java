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

import org.sonar.api.component.Component;
import org.sonar.api.component.Perspective;
import org.sonar.core.graph.EdgePath;

public abstract class GraphPerspectiveBuilder<T extends Perspective> extends PerspectiveBuilder<T> {

  protected final ScanGraph graph;
  protected final EdgePath path;
  protected final GraphPerspectiveLoader<T> perspectiveLoader;

  protected GraphPerspectiveBuilder(ScanGraph graph, Class<T> perspectiveClass, EdgePath path,
    GraphPerspectiveLoader<T> perspectiveLoader) {
    super(perspectiveClass);
    this.graph = graph;
    this.path = path;
    this.perspectiveLoader = perspectiveLoader;
  }

  public T create(ComponentVertex component) {
    return (T) component.beanGraph().createAdjacentVertex(component, perspectiveLoader.getBeanClass(),
      perspectiveLoader.getPerspectiveKey());
  }

  public EdgePath path() {
    return path;
  }

  public GraphPerspectiveLoader<T> getPerspectiveLoader() {
    return perspectiveLoader;
  }

  @Override
  public T loadPerspective(Class<T> perspectiveClass, Component component) {
    ComponentVertex vertex;
    if (component instanceof ComponentVertex) {
      vertex = (ComponentVertex) component;
    } else {
      vertex = graph.getComponent(component.key());
    }

    if (vertex != null) {
      T perspective = perspectiveLoader.load(vertex);
      if (perspective == null) {
        perspective = create(vertex);
      }
      return perspective;
    }
    return null;
  }

  public T get(Class<T> perspectiveClass, String componentKey) {
    ComponentVertex vertex = graph.getComponent(componentKey);
    if (vertex != null) {
      T perspective = perspectiveLoader.load(vertex);
      if (perspective != null) {
        return perspective;
      }
    }
    return null;
  }
}
