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
package org.sonar.server.platform.db.migration.version.v108;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.history.MigrationHistory;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Upsert;

import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_ENABLED;
import static org.sonar.server.platform.db.migration.version.v00.PopulateInitialSchema.createInsertStatement;

public class EnableSpecificMqrMode extends DataChange {
  private final MigrationHistory migrationHistory;
  private final UuidFactory uuidFactory;
  private final System2 system2;

  public EnableSpecificMqrMode(Database db, MigrationHistory migrationHistory, UuidFactory uuidFactory, System2 system2) {
    super(db);
    this.migrationHistory = migrationHistory;
    this.uuidFactory = uuidFactory;
    this.system2 = system2;
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (!paramExists(connection)) {
        long version = migrationHistory.getInitialDbVersion();
        boolean mqrModeEnabled = version >= 102_000L || version == -1L;
        Upsert upsert = context.prepareUpsert(
          createInsertStatement("properties",
            "uuid",
            "prop_key",
            "is_empty",
            "text_value",
            "created_at"));
        upsert.setString(1, uuidFactory.create())
          .setString(2, MULTI_QUALITY_MODE_ENABLED)
          .setBoolean(3, false)
          .setString(4, String.valueOf(mqrModeEnabled))
          .setLong(5, system2.now());
        upsert.execute().commit();
      }
    }
  }

  private static boolean paramExists(Connection connection) throws SQLException {
    String sql = "SELECT count(1) FROM properties WHERE prop_key = '" + MULTI_QUALITY_MODE_ENABLED + "'";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      ResultSet result = statement.executeQuery();
      return result.next() && result.getInt(1) > 0;
    }
  }
}
