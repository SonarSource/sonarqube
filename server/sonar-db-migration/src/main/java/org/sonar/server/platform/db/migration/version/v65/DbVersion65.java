/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

package org.sonar.server.platform.db.migration.version.v65;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion65 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(1700, "Drop table AUTHORS", DropTableAuthors.class)
      .add(1701, "Clean orphans from USER_ROLES", CleanOrphanRowsInUserRoles.class)
      .add(1702, "Clean orphans from GROUP_ROLES", CleanOrphanRowsInGroupRoles.class)
      .add(1703, "Populate EVENTS.COMPONENT_UUID", PopulateEventsComponentUuid.class)
      .add(1704, "Drop index EVENTS_COMPONENT_UUID", DropIndexEventsComponentUuid.class)
      .add(1705, "Make EVENTS.COMPONENT_UUID not nullable", MakeEventsComponentUuidNotNullable.class)
      .add(1706, "Recreate index EVENTS_COMPONENT_UUID", RecreateIndexEventsComponentUuid.class)
      .add(1707, "Ensure ISSUE.PROJECT_UUID is consistent", EnsureIssueProjectUuidConsistencyOnIssues.class)
      .add(1708, "Clean orphans from PROJECT_LINKS", CleanOrphanRowsInProjectLinks.class)
      .add(1709, "Clean orphans from SETTINGS", CleanOrphanRowsInProperties.class)
      .add(1710, "Clean orphans from MANUAL_MEASURES", CleanOrphanRowsInManualMeasures.class)
      .add(1711, "Drop index MANUAL_MEASURES.COMPONENT_UUID", DropIndexManualMeasuresComponentUuid.class)
      .add(1712, "Make MANUAL_MEASURES.COMPONENT_UUID not nullable", MakeManualMeasuresComponentUuidNotNullable.class)
      .add(1713, "Recreate index MANUAL_MEASURES.COMPONENT_UUID", RecreateIndexManualMeasuresComponentUuid.class)
    ;
  }
}
