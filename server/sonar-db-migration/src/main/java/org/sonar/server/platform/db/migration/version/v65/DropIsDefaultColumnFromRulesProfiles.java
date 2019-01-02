/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v65;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.sonar.db.Database;
import org.sonar.db.dialect.MsSql;
import org.sonar.server.platform.db.migration.sql.DropColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static java.lang.String.format;

public class DropIsDefaultColumnFromRulesProfiles extends DdlChange {

  private static final String TABLE_NAME = "rules_profiles";
  private static final String COLUMN_NAME = "is_default";

  public DropIsDefaultColumnFromRulesProfiles(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (getDialect().getId().equals(MsSql.ID)) {
      // this should be handled automatically by DropColumnsBuilder
      dropMssqlConstraints();
    }

    context.execute(new DropColumnsBuilder(getDialect(), TABLE_NAME, COLUMN_NAME).build());
  }

  private void dropMssqlConstraints() throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection();
         PreparedStatement pstmt = connection
           .prepareStatement(format("SELECT d.name " +
             "FROM sys.default_constraints d " +
             "INNER JOIN sys.columns AS c ON d.parent_column_id = c.column_id " +
             "WHERE OBJECT_NAME(d.parent_object_id)='%s' AND c.name='%s'", TABLE_NAME, COLUMN_NAME));
         ResultSet rs = pstmt.executeQuery()) {
      while (rs.next()) {
        String constraintName = rs.getString(1);
        dropMssqlConstraint(connection, constraintName);
      }
    }
  }

  private static void dropMssqlConstraint(Connection connection, String constraintName) throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      stmt.executeUpdate(format("ALTER TABLE %s DROP CONSTRAINT %s", TABLE_NAME, constraintName));
    }
  }
}
