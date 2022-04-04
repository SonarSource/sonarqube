/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import org.sonar.db.dialect.MsSql;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.AlterColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CopyIssueChangesTable extends DdlChange {
  private static final String COPY_NAME = "issue_changes_copy";

  public CopyIssueChangesTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {

    String query;
    if (getDatabase().getDialect().getId().equals(MsSql.ID)) {
      query = "select cast (ic.id AS VARCHAR(40)) AS uuid, ic.kee, ic.issue_key, ic.user_login, ic.change_type, " +
        "ic.change_data, ic.created_at, ic.updated_at, ic.issue_change_creation_date, i.project_uuid " +
        "INTO issue_changes_copy " +
        "FROM issue_changes AS ic inner join issues i on i.kee = ic.issue_key";
    } else {
      query = "create table issue_changes_copy " +
        "(uuid, kee, issue_key, user_login, change_type, change_data, created_at, updated_at, issue_change_creation_date, project_uuid)" +
        "as (" +
        "SELECT cast (ic.id AS VARCHAR(40)) AS uuid, ic.kee, ic.issue_key, ic.user_login, ic.change_type, ic.change_data, ic.created_at, ic.updated_at, "
        + "ic.issue_change_creation_date, i.project_uuid " +
        "FROM issue_changes ic " +
        "inner join issues i on i.kee = ic.issue_key " +
        ")";
    }

    context.execute(query);
    context.execute(new AlterColumnsBuilder(getDialect(), COPY_NAME).updateColumn(
      newVarcharColumnDefBuilder()
        .setColumnName("project_uuid")
        .setIsNullable(false)
        .setDefaultValue(null)
        .setLimit(VarcharColumnDef.UUID_VARCHAR_SIZE)
        .build()).build());
    context.execute(new AlterColumnsBuilder(getDialect(), COPY_NAME).updateColumn(
      newVarcharColumnDefBuilder()
        .setColumnName("issue_key")
        .setIsNullable(false)
        .setDefaultValue(null)
        .setLimit(VarcharColumnDef.UUID_VARCHAR_SIZE)
        .build()).build());
    context.execute(new AlterColumnsBuilder(getDialect(), COPY_NAME).updateColumn(
      newVarcharColumnDefBuilder()
        .setColumnName("uuid")
        .setIsNullable(false)
        .setDefaultValue(null)
        .setLimit(VarcharColumnDef.UUID_SIZE)
        .build()).build());
  }
}
