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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class MigrateProjectMeasuresDeprecatedMetrics extends DataChange {

  private static final String SELECT_QUERY = """
    select m.name, pm.component_uuid , pm.analysis_uuid, pm.text_value
    from project_measures pm
    join  metrics m
      on pm.metric_uuid = m.uuid
      and m.name in (%s)
      and not exists (
        select 1
        from project_measures pm2
        join  metrics m2
          on pm2.metric_uuid = m2.uuid
          and pm.analysis_uuid = pm2.analysis_uuid
          and m2.name in ('software_quality_maintainability_issues')
      )
    order by pm.analysis_uuid
    """.formatted(MeasureMigration.MIGRATION_MAP.keySet().stream().map(s -> "'" + s + "'").collect(Collectors.joining(",")));

  private static final String SELECT_NEW_METRICS_UUID = """
    select m.name, m.uuid
    from metrics m
    where m.name in (%s)
    """.formatted(MeasureMigration.MIGRATION_MAP.values().stream().map(s -> "'" + s + "'").collect(Collectors.joining(",")));

  private final UuidFactory uuidFactory;

  public MigrateProjectMeasuresDeprecatedMetrics(Database db, UuidFactory uuidFactory) {
    super(db);
    this.uuidFactory = uuidFactory;
  }

  @Override
  protected void execute(Context context) throws SQLException {

    Map<String, String> newMetricsUuid = getNewMetricsUuid();

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(SELECT_QUERY);
    massUpdate.update("INSERT INTO project_measures (value, component_uuid, analysis_uuid, uuid, metric_uuid) VALUES (?, ?, ?, ?, ?)");

    massUpdate.execute((row, update, index) -> {
      String metricName = row.getString(1);
      String componentUuid = row.getString(2);
      String analysisUuid = row.getString(3);
      String textValue = row.getString(4);

      Long migratedValue = MeasureMigration.migrate(textValue);
      if (migratedValue != null) {
        update.setDouble(1, migratedValue.doubleValue());
        update.setString(2, componentUuid);
        update.setString(3, analysisUuid);
        update.setString(4, uuidFactory.create());
        String newMetricName = MeasureMigration.MIGRATION_MAP.get(metricName);
        update.setString(5, newMetricsUuid.get(newMetricName));
        return true;
      } else {
        return false;
      }
    });
  }

  private Map<String, String> getNewMetricsUuid() throws SQLException{
    Map<String, String> map = new HashMap<>();
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      try (PreparedStatement statement = connection.prepareStatement(SELECT_NEW_METRICS_UUID)) {
        ResultSet result = statement.executeQuery();
        while (result.next()) {
          map.put(result.getString(1), result.getString(2));
        }
        return map;
      }
    }
  }
}
