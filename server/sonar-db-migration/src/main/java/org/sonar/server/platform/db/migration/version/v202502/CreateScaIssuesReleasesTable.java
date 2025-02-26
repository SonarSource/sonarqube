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
package org.sonar.server.platform.db.migration.version.v202502;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateScaIssuesReleasesTable extends CreateTableChange {

  static final String COLUMN_SCA_ISSUE_UUID_NAME = "sca_issue_uuid";
  static final String COLUMN_SCA_RELEASE_UUID_NAME = "sca_release_uuid";
  static final String COLUMN_SEVERITY_NAME = "severity";
  static final int COLUMN_SEVERITY_SIZE = 15;
  static final String COLUMN_SEVERITY_SORT_KEY_NAME = "severity_sort_key";
  private static final String TABLE_NAME = "sca_issues_releases";
  private static final String COLUMN_UUID_NAME = "uuid";
  private static final String COLUMN_CREATED_AT_NAME = "created_at";
  private static final String COLUMN_UPDATED_AT_NAME = "updated_at";

  protected CreateScaIssuesReleasesTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID_NAME).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SCA_ISSUE_UUID_NAME).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SCA_RELEASE_UUID_NAME).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SEVERITY_NAME).setIsNullable(false).setLimit(COLUMN_SEVERITY_SIZE).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_SEVERITY_SORT_KEY_NAME).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT_NAME).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT_NAME).setIsNullable(false).build())
      .build());
  }
}
