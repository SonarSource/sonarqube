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
package org.sonar.server.platform.db.migration.version.v98;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class VariationMigration extends DataChange {
  private final String tableName;
  private final String variationColumnName;

  public VariationMigration(Database db, String tableName, String variationColumnName) {
    super(db);
    this.tableName = tableName;
    this.variationColumnName = variationColumnName;
  }

  @Override
  protected void execute(DataChange.Context context) throws SQLException {
    if (columnExists()) {
      migrateVariation(context,
        "select uuid, " + variationColumnName + " from " + tableName + " where " + variationColumnName + " is not null and value is null",
        "update " + tableName + " set value = ? where uuid = ?");
    }
  }

  static void migrateVariation(DataChange.Context context, String selectQuery, String updateQuery) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(selectQuery);
    massUpdate.update(updateQuery);
    massUpdate.execute((row, update) -> {
      String uuid = row.getString(1);
      double variation = row.getDouble(2);
      update.setDouble(1, variation);
      update.setString(2, uuid);
      return true;
    });
  }

  private boolean columnExists() throws SQLException {
    try (var connection = getDatabase().getDataSource().getConnection()) {
      return DatabaseUtils.tableColumnExists(connection, tableName, variationColumnName);
    }
  }
}
