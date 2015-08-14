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

package org.sonar.server.computation.component;

import com.google.common.base.Function;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * Cache of database ids for components (component id and snapshot id) based in Maps.
 *
 * This class is intended as a to be used as a delegate in another implementation of MutableDbIdsRepository, hence the
 * final keyword.
 */
public final class MapBasedDbIdsRepository<T> implements MutableDbIdsRepository {

  private final Function<Component, T> componentToKey;
  private final Map<T, Long> componentIdsByRef = new HashMap<>();
  private final Map<T, Long> snapshotIdsByRef = new HashMap<>();

  public MapBasedDbIdsRepository(Function<Component, T> componentToKey) {
    this.componentToKey = componentToKey;
  }

  @Override
  public DbIdsRepository setComponentId(Component component, long componentId) {
    T ref = componentToKey.apply(component);
    Long existingComponentId = componentIdsByRef.get(ref);
    checkState(existingComponentId == null, format("Component ref '%s' has already a component id", ref));
    componentIdsByRef.put(ref, componentId);
    return this;
  }

  @Override
  public long getComponentId(Component component) {
    T ref = componentToKey.apply(component);
    Long componentId = componentIdsByRef.get(ref);
    checkState(componentId != null, format("Component ref '%s' has no component id", ref));
    return componentId;
  }

  @Override
  public DbIdsRepository setSnapshotId(Component component, long snapshotId) {
    T ref = componentToKey.apply(component);
    Long existingSnapshotId = snapshotIdsByRef.get(ref);
    checkState(existingSnapshotId == null, format("Component ref '%s' has already a snapshot id", ref));
    snapshotIdsByRef.put(ref, snapshotId);
    return this;
  }

  @Override
  public long getSnapshotId(Component component) {
    T ref = componentToKey.apply(component);
    Long snapshotId = snapshotIdsByRef.get(ref);
    checkState(snapshotId != null, format("Component ref '%s' has no snapshot id", ref));
    return snapshotId;
  }

}
