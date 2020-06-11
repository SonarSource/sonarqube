/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84.issuechanges;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateTableAsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CopyIssueChangesTable extends DdlChange {
  public CopyIssueChangesTable(Database db) {
    super(db);
  }

  @Override public void execute(Context context) throws SQLException {
    CreateTableAsBuilder builder = new CreateTableAsBuilder(getDialect(), "issue_changes_copy", "issue_changes")
      // this will cause the following changes:
      // * Add UUID with values in ID casted to varchar
      .addColumnWithCast(newVarcharColumnDefBuilder().setColumnName("uuid").setLimit(40).setIsNullable(false).build(), "id")
      .addColumn(newVarcharColumnDefBuilder().setColumnName("kee").setLimit(50).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("issue_key").setLimit(50).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("user_login").setLimit(255).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("change_type").setLimit(20).build())
      .addColumn(newClobColumnDefBuilder().setColumnName("change_data").build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("issue_change_creation_date").build());
    context.execute(builder.build());
       /*
        "UUID VARCHAR(40) NOT NULL",
        "KEE VARCHAR(50)",
        "ISSUE_KEY VARCHAR(50) NOT NULL",
        "USER_LOGIN VARCHAR(255)",
        "CHANGE_TYPE VARCHAR(20)",
        "CHANGE_DATA CLOB(2147483647)",
        "CREATED_AT BIGINT",
        "UPDATED_AT BIGINT",
        "ISSUE_CHANGE_CREATION_DATE BIGINT"
        */
  }
}
