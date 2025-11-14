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
package org.sonar.server.platform.db.migration.step;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;

public abstract class CreateTableChange extends DdlChange {
  protected final String tableName;

  protected CreateTableChange(Database db, String tableName) {
    super(db);
    this.tableName = tableName;
  }

  @Override
  public final void execute(Context context) throws SQLException {
    if (!tableExists()) {
      execute(context, tableName);
    }
  }

  public abstract void execute(Context context, String tableName) throws SQLException;

  private boolean tableExists() throws SQLException {
    try (var connection = getDatabase().getDataSource().getConnection()) {
      return DatabaseUtils.tableExists(tableName, connection);
    }
  }
}
