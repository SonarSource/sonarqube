/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202506;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulateIssueStatsByRuleKeyForPortfoliosAndApps extends DataChange {
  private static final String UPDATE_QUERY = """
      insert into issue_stats_by_rule_key
      (aggregation_type, aggregation_id, rule_key, issue_count, rating, mqr_rating, hotspot_count, hotspots_reviewed)
      values (?, ?, ?, ?, ?, ?, ?, ?);
    """;

  private static final String SELECT_QUERY = """
      SELECT (CASE
          WHEN apps.qualifier = 'APP' THEN 'APPLICATION'
          WHEN apps.qualifier = 'VW' OR apps.qualifier = 'SVW' THEN 'PORTFOLIO'
          END),
        apps.uuid,
        i.rule_key,
        SUM(i.issue_count),
        MAX(i.rating),
        MAX(i.mqr_rating),
        SUM(i.hotspot_count),
        SUM(i.hotspots_reviewed)
      FROM issue_stats_by_rule_key i
      JOIN components projects ON projects.branch_uuid = i.aggregation_id
      JOIN components copy_components ON projects.uuid = copy_components.copy_component_uuid
      JOIN components apps ON apps.uuid = copy_components.branch_uuid
      WHERE copy_components.copy_component_uuid IS NOT NULL
      AND apps.qualifier IN ('VW', 'SVW', 'APP')
      AND i.aggregation_type = 'PROJECT'
      GROUP BY i.rule_key, apps.uuid, apps.qualifier;
    """;

  public PopulateIssueStatsByRuleKeyForPortfoliosAndApps(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(SELECT_QUERY);
    massUpdate.update(UPDATE_QUERY);

    massUpdate.execute((row, update, index) -> {
      update
        // aggregation_type
        .setString(1, row.getString(1))
        // aggregation_id
        .setString(2, row.getString(2))
        // rule_key
        .setString(3, row.getString(3))
        // issue_count
        .setInt(4, row.getInt(4))
        // rating
        .setInt(5, row.getInt(5))
        // mqr_rating
        .setInt(6, row.getInt(6))
        // hotspot_count
        .setInt(7, row.getInt(7))
        // hotspots_reviewed
        .setInt(8, row.getInt(8))
      ;
      return true;
    });
  }
}
