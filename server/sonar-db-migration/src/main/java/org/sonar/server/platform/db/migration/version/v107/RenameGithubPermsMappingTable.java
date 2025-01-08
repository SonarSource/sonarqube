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

import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.RenameTableChange;

public class RenameGithubPermsMappingTable extends RenameTableChange {

  static final String DEVOPS_PERMS_MAPPING_TABLE_NAME = "devops_perms_mapping";

  static final String GITHUB_PERMS_MAPPING_TABLE_NAME = "github_perms_mapping";

  protected RenameGithubPermsMappingTable(Database db) {
    super(db, GITHUB_PERMS_MAPPING_TABLE_NAME, DEVOPS_PERMS_MAPPING_TABLE_NAME);
  }
}
