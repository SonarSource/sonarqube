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
package org.sonar.server.platform.db.migration.step;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;

public abstract class CreateIndexOnColumn extends DdlChange {

  private final String table;
  private final String columnName;
  private final boolean unique;

  protected CreateIndexOnColumn(Database db, String table, String columnName, boolean unique) {
    super(db);
    this.table = table;
    this.columnName = columnName;
    this.unique = unique;
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (!DatabaseUtils.indexExistsIgnoreCase(table, newIndexName(), connection)) {
        context.execute(new CreateIndexBuilder(getDialect())
          .setTable(table)
          .setName(newIndexName())
          .addColumn(columnName)
          .setUnique(unique)
          .build());
      }
    }
  }

  public String newIndexName() {
    return table + "_" + columnName;
  }
}
