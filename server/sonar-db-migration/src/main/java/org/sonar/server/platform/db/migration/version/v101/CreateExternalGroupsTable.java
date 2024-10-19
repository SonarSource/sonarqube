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
package org.sonar.server.platform.db.migration.version.v101;

import com.google.common.annotations.VisibleForTesting;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateExternalGroupsTable extends CreateTableChange {

  static final String TABLE_NAME = "external_groups";
  @VisibleForTesting
  static final String GROUP_UUID_COLUMN_NAME = "group_uuid";
  static final String EXTERNAL_GROUP_ID_COLUMN_NAME = "external_group_id";
  static final String EXTERNAL_IDENTITY_PROVIDER_COLUMN_NAME = "external_identity_provider";

  public CreateExternalGroupsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(GROUP_UUID_COLUMN_NAME).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(EXTERNAL_GROUP_ID_COLUMN_NAME).setIsNullable(false).setLimit(255).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(EXTERNAL_IDENTITY_PROVIDER_COLUMN_NAME).setIsNullable(false).setLimit(100).build())
      .build());
  }
}
