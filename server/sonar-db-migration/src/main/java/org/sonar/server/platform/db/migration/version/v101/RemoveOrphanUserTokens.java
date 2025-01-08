/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class RemoveOrphanUserTokens extends DataChange {
  private static final String SELECT_QUERY = """
    SELECT ut.uuid as tokenUuid
    FROM user_tokens ut
    LEFT OUTER JOIN projects p ON ut.project_key = p.kee
    WHERE p.uuid is null and ut.project_key IS NOT NULL
    """;

  public RemoveOrphanUserTokens(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (DatabaseUtils.tableColumnExists(connection, "user_tokens", "project_key")) {
        MassUpdate massUpdate = context.prepareMassUpdate();
        massUpdate.select(SELECT_QUERY);
        massUpdate.update("delete from user_tokens where uuid = ?");
        massUpdate.execute((row, update) -> {
          String uuid = row.getString(1);
          update.setString(1, uuid);
          return true;
        });
      }
    }
  }
}
