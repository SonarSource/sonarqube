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
  protected T loadPerspective(Class<T> perspectiveClass, Component component) {
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
}
