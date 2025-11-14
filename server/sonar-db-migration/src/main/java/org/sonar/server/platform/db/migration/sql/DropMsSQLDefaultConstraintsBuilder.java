/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.sql;

import com.google.common.base.Preconditions;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.db.Database;
import org.sonar.db.dialect.MsSql;

import static java.lang.String.format;

/**
 * See SONAR-13948. Some columns created in SQ < 5.6 were created with the default command which generated constraints on MS SQL server.
 */
public class DropMsSQLDefaultConstraintsBuilder {
  private final Database db;
  private String tableName;
  private String[] columns;

  public DropMsSQLDefaultConstraintsBuilder(Database db) {
    this.db = db;
  }

  public DropMsSQLDefaultConstraintsBuilder setTable(String s) {
    this.tableName = s;
    return this;
  }

  public DropMsSQLDefaultConstraintsBuilder setColumns(String... columns) {
    this.columns = columns;
    return this;
  }

  public List<String> build() throws SQLException {
    Preconditions.checkArgument(columns.length > 0, "At least one column expected.");
    Preconditions.checkArgument(MsSql.ID.equals(db.getDialect().getId()), "Expected MsSql dialect was: " + db.getDialect().getId());
    return getMsSqlDropDefaultConstraintQueries();
  }

  private List<String> getMsSqlDropDefaultConstraintQueries() throws SQLException {
    List<String> dropQueries = new LinkedList<>();
    if (MsSql.ID.equals(db.getDialect().getId())) {
      List<String> defaultConstraints = getMssqlDefaultConstraints();
      for (String defaultConstraintName : defaultConstraints) {
        dropQueries.add("ALTER TABLE " + tableName + " DROP CONSTRAINT " + defaultConstraintName);
      }
    }
    return dropQueries;
  }

  private List<String> getMssqlDefaultConstraints() throws SQLException {
    List<String> defaultConstrainNames = new LinkedList<>();
    String commaSeparatedListOfColumns = Arrays.stream(columns).map(s -> "'" + s + "'")
      .collect(Collectors.joining(","));
    try (Connection connection = db.getDataSource().getConnection();
      PreparedStatement pstmt = connection
        .prepareStatement(format("SELECT d.name FROM sys.tables t "
          + "JOIN sys.default_constraints d ON d.parent_object_id = t.object_id "
          + "JOIN sys.columns c ON c.object_id = t.object_id AND c.column_id = d.parent_column_id "
          + "JOIN sys.schemas s ON s.schema_id = t.schema_id "
          + "WHERE t.name = '%s' AND c.name in (%s) AND s.name = '%s'", tableName, commaSeparatedListOfColumns, connection.getSchema()));
      ResultSet rs = pstmt.executeQuery()) {
      while (rs.next()) {
        defaultConstrainNames.add(rs.getString(1));
      }
    }
    return defaultConstrainNames;
  }

}
