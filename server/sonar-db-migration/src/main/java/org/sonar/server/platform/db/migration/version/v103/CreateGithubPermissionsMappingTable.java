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
package org.sonar.server.platform.db.migration.version.v103;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateGithubPermissionsMappingTable extends CreateTableChange {
  static final String GITHUB_PERMISSIONS_MAPPING_TABLE_NAME = "github_perms_mapping";
  static final String GITHUB_ROLE_COLUMN = "github_role";
  static final String SONARQUBE_PERMISSION_COLUMN = "sonarqube_permission";

  public CreateGithubPermissionsMappingTable(Database db) {
    super(db, GITHUB_PERMISSIONS_MAPPING_TABLE_NAME);
  }

  @Override
  public void execute(DdlChange.Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName("uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(GITHUB_ROLE_COLUMN).setIsNullable(false).setLimit(100).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(SONARQUBE_PERMISSION_COLUMN).setIsNullable(false).setLimit(64).build())
      .build());
  }
}
