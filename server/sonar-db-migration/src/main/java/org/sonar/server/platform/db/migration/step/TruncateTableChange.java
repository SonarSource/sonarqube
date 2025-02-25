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
package org.sonar.server.platform.db.migration.step;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;

public abstract class TruncateTableChange extends DataChange {

  protected final String tableName;

  protected TruncateTableChange(Database db, String tableName) {
    super(db);
    this.tableName = tableName;
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (!tableExists()) {
      return;
    }

    try (var upsert = context.prepareUpsert(truncateQuery())) {
      upsert.execute().commit();
    }
  }

  private String truncateQuery() {
    return String.format("TRUNCATE TABLE %s", tableName);
  }

  private boolean tableExists() throws SQLException {
    try (var connection = getDatabase().getDataSource().getConnection()) {
      return DatabaseUtils.tableExists(tableName, connection);
    }
  }
}
