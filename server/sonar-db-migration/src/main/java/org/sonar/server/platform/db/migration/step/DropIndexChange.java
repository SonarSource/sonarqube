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
package org.sonar.server.platform.db.migration.step;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;

public abstract class DropIndexChange extends DdlChange {
  private final String indexName;
  private final String tableName;

  public DropIndexChange(Database db, String indexName, String tableName) {
    super(db);
    this.indexName = indexName;
    this.tableName = tableName;
  }

  @Override
  public void execute(Context context) throws SQLException {
    Optional<String> indexName = findExistingIndexName();
    indexName.ifPresent(index -> context.execute(new DropIndexBuilder(getDialect())
      .setTable(tableName)
      .setName(index)
      .build()));
  }

  private Optional<String> findExistingIndexName() throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      return DatabaseUtils.findExistingIndex(connection, tableName, indexName);
    }
  }
}
