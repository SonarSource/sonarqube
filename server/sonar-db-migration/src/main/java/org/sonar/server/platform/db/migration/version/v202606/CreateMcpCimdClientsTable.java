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

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.version.v202606.OAuthJdbcColumnDefs.epochMillis;
import static org.sonar.server.platform.db.migration.version.v202606.OAuthJdbcColumnDefs.text;

public class CreateMcpCimdClientsTable extends DdlChange {

  private static final String TABLE_NAME = "mcp_cimd_clients";
  private static final String INDEX_NAME = "mcp_cimd_clients_reg_uniq";

  public CreateMcpCimdClientsTable(Database database) {
    super(database);
  }

  @Override
  public void execute(Context context) throws SQLException {
    // Check the index separately so a retry can finish an interrupted table creation.
    boolean tableExists;
    boolean indexExists;
    try (var connection = getDatabase().getDataSource().getConnection()) {
      tableExists = DatabaseUtils.tableExists(TABLE_NAME, connection);
      indexExists = tableExists && DatabaseUtils.indexExistsIgnoreCase(TABLE_NAME, INDEX_NAME, connection);
    }
    if (!tableExists) {
      context.execute(new CreateTableBuilder(getDialect(), TABLE_NAME)
        .addPkColumn(text("uuid", 40, false))
        .addColumn(text("registered_client_id", 100, false))
        .addColumn(text("metadata_source", 20, false))
        .addColumn(epochMillis("created_at", false))
        .addColumn(epochMillis("updated_at", false))
        .build());
    }
    if (!indexExists) {
      context.execute(new CreateIndexBuilder(getDialect()).setTable(TABLE_NAME).setName(INDEX_NAME)
        .setUnique(true).addColumn("registered_client_id", false).build());
    }
  }
}
