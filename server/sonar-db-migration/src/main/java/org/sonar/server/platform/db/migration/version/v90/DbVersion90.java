/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v90;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion90 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(5001, "Drop PK on 'uuid' for 'ce_activity' table", DropPrimaryKeyOnUuidColumnOfCeActivityTable.class)
      .add(5002, "Drop 'ce_activity_uuid' index", DropCeActivityUuidIndex.class)
      .add(5003, "Recreate PK on 'uuid' for 'ce_activity' table", AddPrimaryKeyOnUuidColumnOfCeActivityTable.class)
      .add(5004, "Drop PK on 'uuid' for 'events' table", DropPrimaryKeyOnUuidColumnOfEventsTable.class)
      .add(5005, "Drop 'events_uuid' index", DropEventsUuidIndex.class)
      .add(5006, "Recreate PK on 'uuid' for 'events' table", AddPrimaryKeyOnUuidColumnOfEventsTable.class)
      .add(5007, "Drop PK on 'kee' for 'issues' table", DropPrimaryKeyOnKeeColumnOfIssuesTable.class)
      .add(5008, "Drop 'issues_kee' index", DropIssuesKeeIndex.class)
      .add(5009, "Recreate PK on 'kee' for 'issues' table", AddPrimaryKeyOnKeeColumnOfIssuesTable.class)
      .add(5010, "Drop PK on 'kee' for 'snapshots' table", DropPrimaryKeyOnUuidColumnOfSnapshotsTable.class)
      .add(5011, "Drop 'analyses_uuid' index", DropAnalysesUuidIndex.class)
      .add(5012, "Recreate PK on 'kee' for 'snapshots' table", AddPrimaryKeyOnUuidColumnOfSnapshotsTable.class);
  }
}
