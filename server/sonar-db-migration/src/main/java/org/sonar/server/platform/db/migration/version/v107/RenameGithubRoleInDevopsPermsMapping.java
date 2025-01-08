/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v107;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.RenameVarcharColumnChange;

import static org.sonar.server.platform.db.migration.version.v107.RenameGithubPermsMappingTable.DEVOPS_PERMS_MAPPING_TABLE_NAME;

public class RenameGithubRoleInDevopsPermsMapping extends RenameVarcharColumnChange {

  @VisibleForTesting
  static final String OLD_COLUMN_NAME = "github_role";
  @VisibleForTesting
  static final String NEW_COLUMN_NAME = "devops_platform_role";

  public RenameGithubRoleInDevopsPermsMapping(Database db) {
    super(db, DEVOPS_PERMS_MAPPING_TABLE_NAME, OLD_COLUMN_NAME, NEW_COLUMN_NAME);
  }
}
