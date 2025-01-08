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
package org.sonar.server.platform.db.migration.version.v102;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulatePurgedColumnInSnapshots extends DataChange {
  private static final String SELECT_QUERY = """
    SELECT s.uuid, s.purge_status
    FROM snapshots s
    WHERE s.purged is null
    """;

  private static final String UPDATE_QUERY = """
    UPDATE snapshots
    SET purged=?
    WHERE uuid=?
    """;

  public PopulatePurgedColumnInSnapshots(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    if (!checkIfColumnExists()) {
      return;
    }

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(SELECT_QUERY);
    massUpdate.update(UPDATE_QUERY);

    massUpdate.execute((row, update, index) -> {
      String snapshotUuid = row.getString(1);
      Integer purgedStatus = row.getNullableInt(2);
      update.setBoolean(1, purgedStatus != null && purgedStatus == 1)
        .setString(2, snapshotUuid);
      return true;
    });
  }

  public boolean checkIfColumnExists() throws SQLException {
    try (var connection = getDatabase().getDataSource().getConnection()) {
      if (DatabaseUtils.tableColumnExists(connection, "snapshots", "purge_status")) {
        return true;
      }
    }
    return false;
  }
}
