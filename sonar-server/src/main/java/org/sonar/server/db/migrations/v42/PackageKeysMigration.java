/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.db.migrations.v42;

import org.apache.commons.lang.StringUtils;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.DatabaseMigration;
import org.sonar.server.db.migrations.MassUpdater;
import org.sonar.server.db.migrations.SqlUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Used in Rails migration 490
 *
 * @since 4.2
 */
public class PackageKeysMigration implements DatabaseMigration {

  private final Database db;

  public PackageKeysMigration(Database database) {
    this.db = database;
  }

  @Override
  public void execute() {
    new MassUpdater(db).execute(
      new MassUpdater.InputLoader<Row>() {
        @Override
        public String selectSql() {
          return "SELECT id, kee FROM projects WHERE qualifier='PAC'";
        }

        @Override
        public Row load(ResultSet rs) throws SQLException {
          Row row = new Row();
          row.id = SqlUtil.getLong(rs, 1);
          row.key = rs.getString(2);
          return row;
        }
      },
      new MassUpdater.InputConverter<Row>() {
        @Override
        public String updateSql() {
          return "UPDATE projects SET qualifier='DIR', kee=? WHERE id=?";
        }

        @Override
        public void convert(Row row, PreparedStatement updateStatement) throws SQLException {
          updateStatement.setString(1, convertKey(row.key));
          updateStatement.setLong(2, row.id);
        }
      }
    );
  }

  String convertKey(String packageKey) {
    String prefix = StringUtils.substringBeforeLast(packageKey, ":") + ":";
    String key = StringUtils.substringAfterLast(packageKey, ":");
    if (key.equals("[default]")) {
      return prefix + "[root]";
    }
    return prefix + StringUtils.replace(key, ".", "/");
  }

  private static class Row {
    private Long id;
    private String key;
  }
}
