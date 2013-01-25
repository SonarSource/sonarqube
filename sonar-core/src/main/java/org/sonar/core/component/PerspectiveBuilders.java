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

import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.component.Component;
import org.sonar.api.component.Perspective;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Resource;

import javax.annotation.CheckForNull;

import java.util.Map;

public class PerspectiveBuilders implements ResourcePerspectives, BatchComponent, ServerComponent {
  private final ComponentGraph graph;
  private Map<Class<?>, PerspectiveBuilder<?>> builders = Maps.newHashMap();
  private final Map<Component, Map<Class<Perspective>, Perspective>> components = new MapMaker().weakValues().makeMap();
  private final SonarIndex resourceIndex;

  public PerspectiveBuilders(ComponentGraph graph, PerspectiveBuilder[] builders, SonarIndex resourceIndex) {
    this.graph = graph;
    this.resourceIndex = resourceIndex;
    for (PerspectiveBuilder builder : builders) {
      // TODO check duplications
      this.builders.put(builder.getPerspectiveClass(), builder);
    }
  }

  @CheckForNull
  public <P extends Perspective> P as(Component component, Class<P> toClass) {
    if (component.getKey() == null) {
      return null;
    }
    Map<Class<Perspective>, Perspective> perspectives = components.get(component);
    if (perspectives == null) {
      perspectives = Maps.newHashMap();
      components.put(component, perspectives);
    }
    P perspective = (P) perspectives.get(toClass);
    if (perspective == null) {
      ComponentWrapper componentWrapper = graph.wrap(component, ComponentWrapper.class);
      perspective = builderFor(toClass).build(componentWrapper);
      perspectives.put((Class) toClass, perspective);
    }
    return perspective;
  }

  public <P extends Perspective> P as(Resource resource, Class<P> toClass) {
    Resource indexedResource = resourceIndex.getResource(resource);
    if (indexedResource != null) {
      return as(new ResourceComponent(indexedResource), toClass);
    }
    return null;
  }

  <T extends Perspective> PerspectiveBuilder<T> builderFor(Class<T> clazz) {
    PerspectiveBuilder<T> builder = (PerspectiveBuilder<T>) builders.get(clazz);
    if (builder == null) {
      throw new PerspectiveNotFoundException("Perspective class is not registered: " + clazz);
    }
    return builder;
  }
}
