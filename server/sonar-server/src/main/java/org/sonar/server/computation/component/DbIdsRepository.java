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

import java.util.HashMap;
import java.util.Map;

/**
 * Cache of persisted component (component id and snapshot id) that can be used in the persistence steps
 */
public class DbIdsRepository {

  private final Map<Integer, Long> componentIdsByRef = new HashMap<>();
  private final Map<Integer, Long> snapshotIdsByRef = new HashMap<>();

  public DbIdsRepository setComponentId(Component component, long componentId) {
    int ref = component.getRef();
    Long existingComponentId = componentIdsByRef.get(ref);
    if (existingComponentId != null) {
      throw new IllegalArgumentException(String.format("Component ref '%s' has already a component id", ref));
    }
    componentIdsByRef.put(ref, componentId);
    return this;
  }

  public long getComponentId(Component component) {
    int ref = component.getRef();
    Long componentId = componentIdsByRef.get(ref);
    if (componentId == null) {
      throw new IllegalArgumentException(String.format("Component ref '%s' has no component id", ref));
    }
    return componentId;
  }

  public DbIdsRepository setSnapshotId(Component component, long snapshotId) {
    int ref = component.getRef();
    Long existingSnapshotId = snapshotIdsByRef.get(ref);
    if (existingSnapshotId != null) {
      throw new IllegalArgumentException(String.format("Component ref '%s' has already a snapshot id", ref));
    }
    snapshotIdsByRef.put(ref, snapshotId);
    return this;
  }

  public long getSnapshotId(Component component) {
    int ref = component.getRef();
    Long snapshotId = snapshotIdsByRef.get(ref);
    if (snapshotId == null) {
      throw new IllegalArgumentException(String.format("Component ref '%s' has no snapshot id", ref));
    }
    return snapshotId;
  }

}
