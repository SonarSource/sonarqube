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

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.sql.DropTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.version.v107.RenameGithubPermsMappingTable.DEVOPS_PERMS_MAPPING_TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v107.RenameGithubPermsMappingTable.GITHUB_PERMS_MAPPING_TABLE_NAME;

/**
 * Migration should be reentrant.
 * If migration is rerun from version 103, a new table github_perms_mapping will be created while the table devops_perms_mapping will exist,
 * the rename from github_perms_mapping to devops_perms_mapping will fail and we will be in an inconstant state with the two tables.
 * To avoid this state, we need to drop the table github_perms_mapping if the table devops_perms_mapping exists.
 */
public class DropGithubPermsMappingTableIfDevopsPermsMappingTableExists extends DdlChange {

  public DropGithubPermsMappingTableIfDevopsPermsMappingTableExists(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (var connection = getDatabase().getDataSource().getConnection()) {
      boolean devopsPermsMappingTableExists = DatabaseUtils.tableExists(DEVOPS_PERMS_MAPPING_TABLE_NAME, connection);
      boolean githubPermsMappingTableExists = DatabaseUtils.tableExists(GITHUB_PERMS_MAPPING_TABLE_NAME, connection);
      if (devopsPermsMappingTableExists && githubPermsMappingTableExists) {
        context.execute(new DropTableBuilder(getDialect(), GITHUB_PERMS_MAPPING_TABLE_NAME).build());
      }
    }
  }

}
