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

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.USER_UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

class CreateScmAccountsTable extends CreateTableChange {
  static final String SCM_ACCOUNTS_TABLE_NAME = "scm_accounts";
  static final String SCM_ACCOUNT_COLUMN_NAME = "scm_account";
  @VisibleForTesting
  static final String USER_UUID_COLUMN_NAME = "user_uuid";

  @VisibleForTesting
  static final int SCM_ACCOUNT_SIZE = 255;

  public CreateScmAccountsTable(Database db) {
    super(db, SCM_ACCOUNTS_TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(USER_UUID_COLUMN_NAME).setIsNullable(false).setLimit(USER_UUID_SIZE).build())
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(SCM_ACCOUNT_COLUMN_NAME).setIsNullable(false).setLimit(SCM_ACCOUNT_SIZE).build())
      .build());
  }
}
