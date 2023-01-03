/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v85;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.dialect.MsSql;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class CreateTmpIssueChangesTable extends DdlChange {

  public CreateTmpIssueChangesTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (DatabaseUtils.tableColumnExists(connection, "issue_changes", "project_uuid")) {
        // This migration might already have been done in v84 (#3476) if using SQ >= 8.7.
        return;
      }
      String query;
      if (getDatabase().getDialect().getId().equals(MsSql.ID)) {
        query = "SELECT ic.uuid, ic.kee, ic.issue_key, ic.user_login, ic.change_type, " +
          "ic.change_data, ic.created_at, ic.updated_at, ic.issue_change_creation_date, i.project_uuid " +
          "INTO tmp_issue_changes " +
          "FROM issue_changes AS ic inner join issues i on i.kee = ic.issue_key";
      } else {
        query = "create table tmp_issue_changes " +
          "(uuid, kee, issue_key, user_login, change_type, change_data, created_at, updated_at, issue_change_creation_date, project_uuid)" +
          "as (" +
          "SELECT ic.uuid, ic.kee, ic.issue_key, ic.user_login, ic.change_type, ic.change_data, ic.created_at, ic.updated_at, ic.issue_change_creation_date, i.project_uuid " +
          "FROM issue_changes ic " +
          "inner join issues i on i.kee = ic.issue_key " +
          ")";
      }

      context.execute(query);
    }
  }
}
