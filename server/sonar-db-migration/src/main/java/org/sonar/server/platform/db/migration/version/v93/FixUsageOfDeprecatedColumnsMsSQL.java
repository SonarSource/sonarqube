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
package org.sonar.server.platform.db.migration.version.v93;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.dialect.MsSql;
import org.sonar.server.platform.db.migration.sql.AlterColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BlobColumnDef.newBlobColumnDefBuilder;

public class FixUsageOfDeprecatedColumnsMsSQL extends DdlChange {

  public FixUsageOfDeprecatedColumnsMsSQL(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (!MsSql.ID.equals(getDatabase().getDialect().getId())) {
      return;
    }

    try (Connection c = getDatabase().getDataSource().getConnection();
      var ps = c.prepareStatement(
        "SELECT table_name, column_name FROM [INFORMATION_SCHEMA].[COLUMNS] WHERE data_type = ?")) {
      ps.setString(1, "image");
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        var tableName = rs.getString(1);
        var columnName = rs.getString(2);
        context.execute(
          new AlterColumnsBuilder(getDatabase().getDialect(), tableName)
            .updateColumn(newBlobColumnDefBuilder().setColumnName(columnName).build())
            .build());
      }
    }

  }
}
