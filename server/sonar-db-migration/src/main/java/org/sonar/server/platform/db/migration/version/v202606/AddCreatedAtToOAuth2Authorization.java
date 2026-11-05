/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202606;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.db.DatabaseUtils.tableColumnExists;
import static org.sonar.server.platform.db.migration.def.TimestampColumnDef.DefaultValue.CURRENT_TIMESTAMP;
import static org.sonar.server.platform.db.migration.def.TimestampColumnDef.newTimestampColumnDefBuilder;

/**
 * Adds the database-assigned creation date used to expire the authorizations that carry no token and no code,
 * such as a consent that was never answered. Spring's JDBC service never writes the column, so the default applies
 * to every inserted row.
 */
public class AddCreatedAtToOAuth2Authorization extends DdlChange {

  static final String TABLE_NAME = "oauth2_authorization";
  static final String COLUMN_NAME = "created_at";

  public AddCreatedAtToOAuth2Authorization(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (tableColumnExists(connection, TABLE_NAME, COLUMN_NAME)) {
        return;
      }
    }
    context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
      .addColumn(newTimestampColumnDefBuilder().setColumnName(COLUMN_NAME).setIsNullable(true).setDefaultValue(CURRENT_TIMESTAMP).build())
      .build());
  }
}
