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
package org.sonar.ce.task.projectexport.issue;

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
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

import static java.lang.String.format;
import static org.sonar.ce.task.projectexport.util.ResultSetUtils.defaultIfNull;
import static org.sonar.ce.task.projectexport.util.ResultSetUtils.emptyIfNull;

public class ExportIssuesChangelogStep implements ComputationStep {
  private static final String STATUS_CLOSED = "CLOSED";
  private static final String QUERY = "select" +
    " ic.kee, ic.issue_key, ic.change_type, ic.change_data, ic.user_login," +
    " ic.issue_change_creation_date, ic.created_at, p.uuid" +
    " from issue_changes ic" +
    " join issues i on i.kee = ic.issue_key" +
    " join projects p on p.uuid = i.project_uuid" +
    " join project_branches pb on pb.uuid = p.uuid" +
    " where pb.project_uuid = ? and pb.branch_type = 'BRANCH' and pb.exclude_from_purge = ? " +
    " and i.status <> ?" +
    " order by ic.created_at asc";

  private final DbClient dbClient;
  private final ProjectHolder projectHolder;
  private final DumpWriter dumpWriter;

  public ExportIssuesChangelogStep(DbClient dbClient, ProjectHolder projectHolder, DumpWriter dumpWriter) {
    this.dbClient = dbClient;
    this.projectHolder = projectHolder;
    this.dumpWriter = dumpWriter;
  }

  @Override
  public String getDescription() {
    return "Export issues changelog";
  }

  @Override
  public void execute(Context context) {
    long count = 0;
    try (
      StreamWriter<ProjectDump.IssueChange> output = dumpWriter.newStreamWriter(DumpElement.ISSUES_CHANGELOG);
      DbSession dbSession = dbClient.openSession(false);
      PreparedStatement stmt = createStatement(dbSession);
      ResultSet rs = stmt.executeQuery()) {
      ProjectDump.IssueChange.Builder builder = ProjectDump.IssueChange.newBuilder();
      while (rs.next()) {
        ProjectDump.IssueChange issue = toIssuesChange(builder, rs);
        output.write(issue);
        count++;
      }
      Loggers.get(getClass()).debug("{} issue changes exported", count);
    } catch (Exception e) {
      throw new IllegalStateException(format("Issues changelog export failed after processing %d issue changes successfully", count), e);
    }
  }

  private PreparedStatement createStatement(DbSession dbSession) throws SQLException {
    // export oldest entries first, so they can be imported with smallest ids
    PreparedStatement stmt = dbClient.getMyBatis().newScrollingSelectStatement(dbSession, QUERY);
    try {
      stmt.setString(1, projectHolder.projectDto().getUuid());
      stmt.setBoolean(2, true);
      stmt.setString(3, STATUS_CLOSED);
      return stmt;
    } catch (Exception t) {
      DatabaseUtils.closeQuietly(stmt);
      throw t;
    }
  }

  private static ProjectDump.IssueChange toIssuesChange(ProjectDump.IssueChange.Builder builder, ResultSet rs) throws SQLException {
    builder.clear();

    return builder
      .setKey(emptyIfNull(rs, 1))
      .setIssueUuid(rs.getString(2))
      .setChangeType(emptyIfNull(rs, 3))
      .setChangeData(emptyIfNull(rs, 4))
      .setUserUuid(emptyIfNull(rs, 5))
      .setCreatedAt(defaultIfNull(rs, 6, defaultIfNull(rs, 7, 0L)))
      .setProjectUuid(rs.getString(8))
      .build();
  }

}
