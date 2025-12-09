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
package org.sonar.server.platform.db.migration.version.v202502;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class CreateUniqueIndexOnArchitectureGraphs extends DdlChange {

  static final String TABLE_NAME = "architecture_graphs";
  static final String INDEX_NAME = "uq_idx_ag_branch_type_source";
  static final String COLUMN_NAME_BRANCH_UUID = "branch_uuid";
  static final String COLUMN_NAME_TYPE = "type";
  static final String COLUMN_NAME_SOURCE = "source";

  public CreateUniqueIndexOnArchitectureGraphs(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      createIndex(context, connection);
    }
  }

  private void createIndex(Context context, Connection connection) {
    if(!DatabaseUtils.tableColumnExists(connection, TABLE_NAME, COLUMN_NAME_BRANCH_UUID)) {
      return;
    }
    if(!DatabaseUtils.tableColumnExists(connection, TABLE_NAME, COLUMN_NAME_TYPE)) {
      return;
    }
    if(!DatabaseUtils.tableColumnExists(connection, TABLE_NAME, COLUMN_NAME_SOURCE)) {
      return;
    }
    if (DatabaseUtils.indexExistsIgnoreCase(TABLE_NAME, INDEX_NAME, connection)) {
      return;
    }

    context.execute(new CreateIndexBuilder(getDialect())
      .setTable(TABLE_NAME)
      .setName(INDEX_NAME)
      .setUnique(true)
      .addColumn(COLUMN_NAME_BRANCH_UUID, false)
      .addColumn(COLUMN_NAME_TYPE, false)
      .addColumn(COLUMN_NAME_SOURCE, false)
      .build());
  }
}
