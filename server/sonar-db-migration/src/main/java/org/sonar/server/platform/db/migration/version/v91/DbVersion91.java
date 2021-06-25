/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v91;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;
import org.sonar.server.platform.db.migration.version.v90.AddPrimaryKeyOnKeeColumnOfIssuesTable;
import org.sonar.server.platform.db.migration.version.v90.AddPrimaryKeyOnUuidColumnOfCeActivityTable;
import org.sonar.server.platform.db.migration.version.v90.AddPrimaryKeyOnUuidColumnOfEventsTable;
import org.sonar.server.platform.db.migration.version.v90.AddPrimaryKeyOnUuidColumnOfSnapshotsTable;
import org.sonar.server.platform.db.migration.version.v90.DropAnalysesUuidIndex;
import org.sonar.server.platform.db.migration.version.v90.DropCeActivityUuidIndex;
import org.sonar.server.platform.db.migration.version.v90.DropEventsUuidIndex;
import org.sonar.server.platform.db.migration.version.v90.DropIssuesKeeIndex;
import org.sonar.server.platform.db.migration.version.v90.DropPrimaryKeyOnKeeColumnOfIssuesTable;
import org.sonar.server.platform.db.migration.version.v90.DropPrimaryKeyOnUuidColumnOfCeActivityTable;
import org.sonar.server.platform.db.migration.version.v90.DropPrimaryKeyOnUuidColumnOfEventsTable;
import org.sonar.server.platform.db.migration.version.v90.DropPrimaryKeyOnUuidColumnOfSnapshotsTable;

public class DbVersion91 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(6001, "Drop 'manual_measures_component_uuid' index", DropManualMeasuresComponentUuidIndex.class)
      .add(6002, "Drop 'manual_measures' table", DropManualMeasuresTable.class);
  }
}
