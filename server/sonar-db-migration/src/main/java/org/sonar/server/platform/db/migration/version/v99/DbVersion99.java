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
package org.sonar.server.platform.db.migration.version.v99;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion99 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(6800, "Add node_name column to ce_activity table", AddNodeNameColumnToCeActivityTable.class)
      .add(6801, "Delete all analysis cache", DeleteAnalysisCache.class)
      .add(6802, "Change user_uuid field size to 255 for audit table", UpdateUserUuidColumnSizeInAuditTable.class)
      .add(6803, "Add ncloc to 'Projects' table", AddNclocToProjects.class)
      .add(6804, "Populate ncloc in 'Projects' table", PopulateNclocForProjects.class)
    ;
  }
}
