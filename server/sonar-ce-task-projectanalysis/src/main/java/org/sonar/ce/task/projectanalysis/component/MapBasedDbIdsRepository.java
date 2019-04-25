/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.ce.task.projectanalysis.component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkState;

/**
 * Cache of database ids for components (component id and snapshot id) based in Maps.
 *
 * This class is intended as a to be used as a delegate in another implementation of MutableDbIdsRepository, hence the
 * final keyword.
 */
public final class MapBasedDbIdsRepository<T> implements MutableDbIdsRepository {

  private final Function<Component, T> componentToKey;
  private final Map<T, Long> componentIdsByRef = new HashMap<>();

  public MapBasedDbIdsRepository(Function<Component, T> componentToKey) {
    this.componentToKey = componentToKey;
  }

  @Override
  public DbIdsRepository setComponentId(Component component, long componentId) {
    T ref = componentToKey.apply(component);
    Long existingComponentId = componentIdsByRef.get(ref);
    checkState(existingComponentId == null,
      "Component id '%s' is already registered in repository for Component '%s', can not set new id '%s'", existingComponentId, component.getDbKey(), componentId);
    componentIdsByRef.put(ref, componentId);
    return this;
  }

  @Override
  public long getComponentId(Component component) {
    T ref = componentToKey.apply(component);
    Long componentId = componentIdsByRef.get(ref);
    checkState(componentId != null, "No component id registered in repository for Component '%s'", component.getDbKey());
    return componentId;
  }

}
