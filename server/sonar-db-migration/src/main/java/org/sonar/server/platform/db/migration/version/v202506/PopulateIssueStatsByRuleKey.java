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
package org.sonar.server.platform.db.migration.version.v202506;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulateIssueStatsByRuleKey extends DataChange {
  private static final String SELECT_QUERY = """
      select
        b.uuid as "branchName",
        r.plugin_name as "RuleRepository",
        r.plugin_rule_key as "RuleKey",
        sum(case when i.issue_type = 4 then 0 else 1 end) as "issueCount",
        max(case when i.issue_type = 4 then 0 else
          case
            when i.severity = 'INFO' then 1
            when i.severity = 'MINOR' then 2
            when i.severity = 'MAJOR' then 3
            when i.severity = 'CRITICAL' then 4
            when i.severity = 'BLOCKER' then 5
          end
        end) as "issueRating",
        max(case when i.issue_type = 4 then 0 else
          case
            when ii.severity is null then 1
            when ii.severity = 'INFO' then 1
            when ii.severity = 'LOW' then 2
            when ii.severity = 'MEDIUM' then 3
            when ii.severity = 'HIGH' then 4
            when ii.severity = 'BLOCKER' then 5
          end
        end) as "mqrIssueRating",
        sum(case when i.issue_type = 4 and i.status = 'TO_REVIEW' then 1 else 0 end) as "hotspotCount",
        sum(case when i.issue_type = 4 and i.status = 'REVIEWED' then 1 else 0 end) as "hotspotsReviewed"
      from issues i
      join rules r on i.rule_uuid = r.uuid
      join project_branches b on b.uuid = i.project_uuid
      left outer join issues_impacts ii on i.kee = ii.issue_key and ii.software_quality = 'SECURITY'
      where b.branch_type = 'BRANCH' and i.resolution is null
      group by b.uuid, r.plugin_name, r.plugin_rule_key
    """;
  private static final String UPDATE_QUERY = """
      insert into issue_stats_by_rule_key
      (aggregation_type, aggregation_id, rule_key, issue_count, rating, mqr_rating, hotspot_count, hotspots_reviewed)
      values (?, ?, ?, ?, ?, ?, ?, ?);
    """;

  private static final String DELETE_QUERY = """
    delete from issue_stats_by_rule_key;
    """;

  public PopulateIssueStatsByRuleKey(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    context.prepareUpsert(DELETE_QUERY).execute().commit();

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(SELECT_QUERY);
    massUpdate.update(UPDATE_QUERY);

    massUpdate.execute((row, update, index) -> {
      update
        // aggregation_type
        .setString(1, "PROJECT")
        // aggregation_id
        .setString(2, row.getString(1))
        // rule_key
        .setString(3, row.getString(2) + ":" + row.getString(3))
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
