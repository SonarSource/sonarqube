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
package org.sonar.ce.task.projectexport.rule;

import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectexport.steps.DumpElement;
import org.sonar.ce.task.projectexport.steps.DumpWriter;
import org.sonar.ce.task.projectexport.steps.ProjectHolder;
import org.sonar.ce.task.projectexport.steps.StreamWriter;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

import static java.lang.String.format;
import static org.sonar.ce.task.projectexport.util.ResultSetUtils.defaultIfNull;
import static org.sonar.ce.task.projectexport.util.ResultSetUtils.emptyIfNull;

public class ExportAdHocRulesStep implements ComputationStep {
  private static final String RULE_STATUS_REMOVED = "REMOVED";
  private static final String ISSUE_STATUS_CLOSED = "CLOSED";

  private static final String QUERY = "select" +
    " r.uuid, r.plugin_key, r.plugin_rule_key, r.plugin_name, r.name, r.status, r.rule_type, r.scope, r.ad_hoc_name," +
    " r.ad_hoc_description,r.ad_hoc_severity, r.ad_hoc_type" +
    " from rules r" +
    " inner join issues i on r.uuid = i.rule_uuid and r.status <> ? and r.is_ad_hoc = ?" +
    " left join components p on p.uuid = i.project_uuid" +
    " left join project_branches pb on pb.uuid = p.uuid" +
    " where pb.project_uuid = ? and pb.branch_type = 'BRANCH' and pb.exclude_from_purge = ?" +
    " and i.status <> ?" +
    " order by" +
    " i.rule_uuid asc";

  private final DbClient dbClient;
  private final ProjectHolder projectHolder;
  private final DumpWriter dumpWriter;

  public ExportAdHocRulesStep(DbClient dbClient, ProjectHolder projectHolder, DumpWriter dumpWriter) {
    this.dbClient = dbClient;
    this.projectHolder = projectHolder;
    this.dumpWriter = dumpWriter;
  }

  @Override
  public void execute(Context context) {
    long count = 0L;
    try (
      StreamWriter<ProjectDump.AdHocRule> output = dumpWriter.newStreamWriter(DumpElement.AD_HOC_RULES);
      DbSession dbSession = dbClient.openSession(false);
      PreparedStatement stmt = dbClient.getMyBatis().newScrollingSelectStatement(dbSession, QUERY)) {
      stmt.setString(1, RULE_STATUS_REMOVED);
      stmt.setBoolean(2, true);
      stmt.setString(3, projectHolder.projectDto().getUuid());
      stmt.setBoolean(4, true);
      stmt.setString(5, ISSUE_STATUS_CLOSED);
      try (ResultSet rs = stmt.executeQuery()) {
        ProjectDump.AdHocRule.Builder adHocRuleBuilder = ProjectDump.AdHocRule.newBuilder();
        while (rs.next()) {
          ProjectDump.AdHocRule rule = convertToAdHocRule(rs, adHocRuleBuilder);
          output.write(rule);
          count++;
        }
        Loggers.get(getClass()).debug("{} ad-hoc rules exported", count);
      }
    } catch (Exception e) {
      throw new IllegalStateException(format("Ad-hoc rules export failed after processing %d rules successfully", count), e);
    }
  }

  private static ProjectDump.AdHocRule convertToAdHocRule(ResultSet rs, ProjectDump.AdHocRule.Builder builder) throws SQLException {
    return builder
      .clear()
      .setRef(rs.getString(1))
      .setPluginKey(emptyIfNull(rs, 2))
      .setPluginRuleKey(rs.getString(3))
      .setPluginName(rs.getString(4))
      .setName(emptyIfNull(rs, 5))
      .setStatus(emptyIfNull(rs, 6))
      .setType(rs.getInt(7))
      .setScope(rs.getString(8))
      .setMetadata(buildMetadata(rs))
      .build();
  }

  private static ProjectDump.AdHocRule.RuleMetadata buildMetadata(ResultSet rs) throws SQLException {
    return ProjectDump.AdHocRule.RuleMetadata.newBuilder()
      .setAdHocName(emptyIfNull(rs, 9))
      .setAdHocDescription(emptyIfNull(rs, 10))
      .setAdHocSeverity(emptyIfNull(rs, 11))
      .setAdHocType(defaultIfNull(rs, 12, 0))
      .build();
  }

  @Override
  public String getDescription() {
    return "Export ad-hoc rules";
  }
}
