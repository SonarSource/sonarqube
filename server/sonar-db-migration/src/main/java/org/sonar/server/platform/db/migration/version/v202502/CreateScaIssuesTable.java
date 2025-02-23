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
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateScaIssuesTable extends CreateTableChange {

  private static final String TABLE_NAME = "sca_issues";
  private static final String COLUMN_UUID_NAME = "uuid";
  private static final String COLUMN_SCA_ISSUE_TYPE = "sca_issue_type";
  private static final int COLUMN_SCA_ISSUE_TYPE_SIZE = 40;
  private static final String COLUMN_PACKAGE_URL = "package_url";
  private static final int COLUMN_PACKAGE_URL_SIZE = 400;
  private static final String COLUMN_VULNERABILITY_ID = "vulnerability_id";
  // the max we have seen in the past as of 2025-02 is 35, though no spec guarantees that
  private static final int COLUMN_VULNERABILITY_ID_SIZE = 63;
  private static final String COLUMN_SPDX_LICENSE_ID = "spdx_license_id";
  // the max we have seen in the past as of 2025-02 is 68, though customers can type custom LicenseRef- names,
  // and we'll probably need to restrict the length of those to this.
  private static final int COLUMN_SPDX_LICENSE_ID_SIZE = 127;
  private static final String COLUMN_CREATED_AT_NAME = "created_at";
  private static final String COLUMN_UPDATED_AT_NAME = "updated_at";

  protected CreateScaIssuesTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID_NAME).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SCA_ISSUE_TYPE).setIsNullable(false).setLimit(COLUMN_SCA_ISSUE_TYPE_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PACKAGE_URL).setIsNullable(false).setLimit(COLUMN_PACKAGE_URL_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_VULNERABILITY_ID).setIsNullable(false).setLimit(COLUMN_VULNERABILITY_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SPDX_LICENSE_ID).setIsNullable(false).setLimit(COLUMN_SPDX_LICENSE_ID_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT_NAME).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT_NAME).setIsNullable(false).build())
      .build());
  }
}
