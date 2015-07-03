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

package org.sonar.db.purge;

import org.sonar.api.utils.DateUtils;

public final class DbCleanerTestUtils {

  private DbCleanerTestUtils() {
  }

  public static PurgeableSnapshotDto createSnapshotWithDate(long snapshotId, String date) {
    PurgeableSnapshotDto snapshot = new PurgeableSnapshotDto();
    snapshot.setSnapshotId(snapshotId);
    snapshot.setDate(DateUtils.parseDate(date).getTime());
    return snapshot;
  }

  public static PurgeableSnapshotDto createSnapshotWithDateTime(long snapshotId, String datetime) {
    PurgeableSnapshotDto snapshot = new PurgeableSnapshotDto();
    snapshot.setSnapshotId(snapshotId);
    snapshot.setDate(DateUtils.parseDateTime(datetime).getTime());
    return snapshot;
  }

}
