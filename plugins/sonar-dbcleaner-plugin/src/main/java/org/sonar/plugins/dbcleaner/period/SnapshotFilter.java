/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.plugins.dbcleaner.period;

import java.util.List;
import java.util.ListIterator;

import org.sonar.api.database.model.Snapshot;

abstract class SnapshotFilter {

  final int filter(List<Snapshot> snapshots) {
    int before = snapshots.size();
    ListIterator<Snapshot> iterator = snapshots.listIterator();
    while (iterator.hasNext()) {
      Snapshot snapshot = iterator.next();
      if(filter(snapshot)){
        iterator.remove();
      }
    }
    int after = snapshots.size();
    return before - after;
  }

  abstract boolean filter(Snapshot snapshot);
}
