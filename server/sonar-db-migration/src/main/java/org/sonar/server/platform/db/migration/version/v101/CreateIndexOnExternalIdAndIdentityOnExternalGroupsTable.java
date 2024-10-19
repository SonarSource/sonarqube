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
package org.sonar.server.platform.db.migration.version.v101;

import com.google.common.annotations.VisibleForTesting;
import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.version.v101.CreateExternalGroupsTable.EXTERNAL_GROUP_ID_COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v101.CreateExternalGroupsTable.EXTERNAL_IDENTITY_PROVIDER_COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v101.CreateExternalGroupsTable.TABLE_NAME;

public class CreateIndexOnExternalIdAndIdentityOnExternalGroupsTable extends DdlChange {

  @VisibleForTesting
  static final String INDEX_NAME = "uniq_ext_grp_ext_id_provider";

  public CreateIndexOnExternalIdAndIdentityOnExternalGroupsTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      createIndex(context, connection);
    }
  }

  private void createIndex(Context context, Connection connection) {
    if (!DatabaseUtils.indexExistsIgnoreCase(TABLE_NAME, INDEX_NAME, connection)) {
      context.execute(new CreateIndexBuilder(getDialect())
        .setTable(TABLE_NAME)
        .setName(INDEX_NAME)
        .addColumn(EXTERNAL_IDENTITY_PROVIDER_COLUMN_NAME, false)
        .addColumn(EXTERNAL_GROUP_ID_COLUMN_NAME, false)
        .setUnique(true)
        .build());
    }

  }
}
