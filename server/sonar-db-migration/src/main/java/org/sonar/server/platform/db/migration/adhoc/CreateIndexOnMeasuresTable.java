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
package org.sonar.server.platform.db.migration.adhoc;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.api.server.ServerSide;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.adhoc.CreateMeasuresTable.COLUMN_BRANCH_UUID;
import static org.sonar.server.platform.db.migration.adhoc.CreateMeasuresTable.MEASURES_TABLE_NAME;

@ServerSide
public class CreateIndexOnMeasuresTable extends DdlChange {

  static final String INDEX_NAME = "measures_branch_uuid";

  public CreateIndexOnMeasuresTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (!DatabaseUtils.indexExistsIgnoreCase(MEASURES_TABLE_NAME, INDEX_NAME, connection)) {
        context.execute(new CreateIndexBuilder()
          .setTable(MEASURES_TABLE_NAME)
          .setName(INDEX_NAME)
          .addColumn(COLUMN_BRANCH_UUID)
          .setUnique(false)
          .build());
      }
    }
  }
}
