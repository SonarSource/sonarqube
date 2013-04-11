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
package org.sonar.batch.index;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.sonar.api.BatchComponent;
import org.sonar.api.database.model.Snapshot;

import java.util.Map;

public class SnapshotCache implements BatchComponent {
  // snapshots by component key
  private final Map<String, Snapshot> snapshots = Maps.newHashMap();

  public Snapshot get(String componentKey) {
    return snapshots.get(componentKey);
  }

  public SnapshotCache put(String componentKey, Snapshot snapshot) {
    Preconditions.checkState(!snapshots.containsKey(componentKey), "Component is already registered: " + componentKey);
    snapshots.put(componentKey, snapshot);
    return this;
  }
}
