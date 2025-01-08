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
package org.sonar.server.platform.db.migration.version.v108;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Upsert;
import org.sonar.server.platform.db.migration.version.v00.PopulateInitialSchema;

public class CreateNewSoftwareQualityMetrics extends DataChange {
  private final UuidFactory uuidFactory;

  public CreateNewSoftwareQualityMetrics(Database db, UuidFactory uuidFactory) {
    super(db);
    this.uuidFactory = uuidFactory;
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      for (String metric : MeasureMigration.MIGRATION_MAP.values()) {
        if (!metricExists(connection, metric)) {
          Upsert upsert = context.prepareUpsert(PopulateInitialSchema.createInsertStatement("metrics",
            "name",
            "direction",
            "qualitative",
            "enabled",
            "best_value",
            "optimized_best_value",
            "delete_historical_data",
            "uuid"
          ));
          upsert
            .setString(1, metric)
            .setInt(2, -1)
            .setBoolean(3, metric.startsWith("new_"))
            .setBoolean(4, true)
            .setDouble(5, 0.0)
            .setBoolean(6, true)
            .setBoolean(7, metric.startsWith("new_"))
            .setString(8, uuidFactory.create());
          upsert.execute().commit();
        }
      }
    }
  }

  private static boolean metricExists(Connection connection, String metric) throws SQLException {
    String sql = "SELECT count(1) FROM metrics WHERE name = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, metric);
      ResultSet result = statement.executeQuery();
      return result.next() && result.getInt(1) > 0;
    }
  }
}
