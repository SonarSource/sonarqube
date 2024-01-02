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
package org.sonar.server.platform.db.migration.version.v96;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.def.ColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;
import static org.sonar.server.platform.db.migration.version.v96.CreatePushEventsTable.PUSH_EVENTS_TABLE_NAME;

public class AddLanguageColumnToPushEventsTable extends DdlChange {

  static final String COLUMN_LANGUAGE = "language";

  public AddLanguageColumnToPushEventsTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      createLanguageColumn(context, connection);
    }
  }

  private void createLanguageColumn(Context context, Connection connection) {
    VarcharColumnDef contextKeyColumn = newVarcharColumnDefBuilder()
      .setColumnName(COLUMN_LANGUAGE)
      .setIsNullable(true)
      .setLimit(20).build();
    createColumnIfNotExists(context, connection, contextKeyColumn);
  }

  private void createColumnIfNotExists(Context context, Connection connection, ColumnDef columnDef) {
    if (!DatabaseUtils.tableColumnExists(connection, PUSH_EVENTS_TABLE_NAME, columnDef.getName())) {
      context.execute(new AddColumnsBuilder(getDialect(), PUSH_EVENTS_TABLE_NAME)
        .addColumn(columnDef)
        .build());
    }
  }

}
