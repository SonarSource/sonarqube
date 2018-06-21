/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.component;

import com.google.common.base.Function;
import java.util.HashMap;
import java.util.Map;

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
  private final Map<Developer, Long> developerIdsByKey = new HashMap<>();

  public MapBasedDbIdsRepository(Function<Component, T> componentToKey) {
    this.componentToKey = componentToKey;
  }

  @Override
  public DbIdsRepository setComponentId(Component component, long componentId) {
    T ref = componentToKey.apply(component);
    Long existingComponentId = componentIdsByRef.get(ref);
    checkState(existingComponentId == null,
      "Component id '%s' is already registered in repository for Component '%s', can not set new id '%s'", existingComponentId, component.getKey(), componentId);
    componentIdsByRef.put(ref, componentId);
    return this;
  }

  @Override
  public long getComponentId(Component component) {
    T ref = componentToKey.apply(component);
    Long componentId = componentIdsByRef.get(ref);
    checkState(componentId != null, "No component id registered in repository for Component '%s'", component.getKey());
    return componentId;
  }

  @Override
  public DbIdsRepository setDeveloperId(Developer developer, long developerId) {
    Long existingId = developerIdsByKey.get(developer);
    checkState(existingId == null, "Id '%s' is already registered in repository for Developer '%s', can not set new id '%s'", existingId, developer, developerId);
    developerIdsByKey.put(developer, developerId);
    return this;
  }

  @Override
  public long getDeveloperId(Developer developer) {
    Long devId = developerIdsByKey.get(developer);
    checkState(devId != null, "No id registered in repository for Developer '%s'", developer);
    return devId;
  }
}
