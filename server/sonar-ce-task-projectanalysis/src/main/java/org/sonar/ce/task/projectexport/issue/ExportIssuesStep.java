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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectexport.component.ComponentRepository;
import org.sonar.ce.task.projectexport.rule.Rule;
import org.sonar.ce.task.projectexport.rule.RuleRepository;
import org.sonar.ce.task.projectexport.steps.DumpElement;
import org.sonar.ce.task.projectexport.steps.DumpElement.IssueDumpElement;
import org.sonar.ce.task.projectexport.steps.DumpWriter;
import org.sonar.ce.task.projectexport.steps.ProjectHolder;
import org.sonar.ce.task.projectexport.steps.StreamWriter;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.protobuf.DbIssues;

import static java.lang.String.format;
import static org.sonar.ce.task.projectexport.util.ResultSetUtils.defaultIfNull;
import static org.sonar.ce.task.projectexport.util.ResultSetUtils.emptyIfNull;

public class ExportIssuesStep implements ComputationStep {
  private static final String RULE_STATUS_REMOVED = "REMOVED";
  private static final String ISSUE_STATUS_CLOSED = "CLOSED";

  // ordered by rule_id to reduce calls to RuleRepository
  private static final String QUERY = "select" +
    " i.kee, r.uuid, r.plugin_rule_key, r.plugin_name, i.issue_type," +
    " i.component_uuid, i.message, i.line, i.checksum, i.status," +
    " i.resolution, i.severity, i.manual_severity, i.gap, effort," +
    " i.assignee, i.author_login, i.tags, i.issue_creation_date," +
    " i.issue_update_date, i.issue_close_date, i.locations, i.project_uuid," +
    " i.rule_description_context_key, i.message_formattings " +
    " from issues i" +
    " join rules r on r.uuid = i.rule_uuid and r.status <> ?" +
    " join components p on p.uuid = i.project_uuid" +
    " join project_branches pb on pb.uuid = p.uuid" +
    " where pb.project_uuid = ? and pb.branch_type = 'BRANCH' and pb.exclude_from_purge=?" +
    " and i.status <> ?" +
    " order by" +
    " i.rule_uuid asc, i.created_at asc";

  private final DbClient dbClient;
  private final ProjectHolder projectHolder;
  private final DumpWriter dumpWriter;
  private final RuleRegistrar ruleRegistrar;
  private final ComponentRepository componentRepository;

  public ExportIssuesStep(DbClient dbClient, ProjectHolder projectHolder, DumpWriter dumpWriter, RuleRepository ruleRepository,
    ComponentRepository componentRepository) {
    this.dbClient = dbClient;
    this.projectHolder = projectHolder;
    this.dumpWriter = dumpWriter;
    this.componentRepository = componentRepository;
    this.ruleRegistrar = new RuleRegistrar(ruleRepository);
  }

  @Override
  public String getDescription() {
    return "Export issues";
  }

  @Override
  public void execute(Context context) {
    long count = 0;
    try (
      StreamWriter<ProjectDump.Issue> output = dumpWriter.newStreamWriter(DumpElement.ISSUES);
      DbSession dbSession = dbClient.openSession(false);
      PreparedStatement stmt = createStatement(dbSession);
      ResultSet rs = stmt.executeQuery()) {
      ProjectDump.Issue.Builder builder = ProjectDump.Issue.newBuilder();
      while (rs.next()) {
        ProjectDump.Issue issue = toIssue(builder, rs);
        output.write(issue);
        count++;
      }
      Loggers.get(getClass()).debug("{} issues exported", count);
    } catch (Exception e) {
      throw new IllegalStateException(format("Issue export failed after processing %d issues successfully", count), e);
    }
  }

  private PreparedStatement createStatement(DbSession dbSession) throws SQLException {
    PreparedStatement stmt = dbClient.getMyBatis().newScrollingSelectStatement(dbSession, QUERY);
    try {
      stmt.setString(1, RULE_STATUS_REMOVED);
      stmt.setString(2, projectHolder.projectDto().getUuid());
      stmt.setBoolean(3, true);
      stmt.setString(4, ISSUE_STATUS_CLOSED);
      return stmt;
    } catch (Exception t) {
      DatabaseUtils.closeQuietly(stmt);
      throw t;
    }
  }

  private ProjectDump.Issue toIssue(ProjectDump.Issue.Builder builder, ResultSet rs) throws SQLException {
    builder.clear();
    String issueUuid = rs.getString(1);
    setRule(builder, rs);
    builder
      .setUuid(issueUuid)
      .setType(rs.getInt(5))
      .setComponentRef(componentRepository.getRef(rs.getString(6)))
      .setMessage(emptyIfNull(rs, 7))
      .setLine(rs.getInt(8))
      .setChecksum(emptyIfNull(rs, 9))
      .setStatus(emptyIfNull(rs, 10))
      .setResolution(emptyIfNull(rs, 11))
      .setSeverity(emptyIfNull(rs, 12))
      .setManualSeverity(rs.getBoolean(13))
      .setGap(defaultIfNull(rs, 14, IssueDumpElement.NO_GAP))
      .setEffort(defaultIfNull(rs, 15, IssueDumpElement.NO_EFFORT))
      .setAssignee(emptyIfNull(rs, 16))
      .setAuthor(emptyIfNull(rs, 17))
      .setTags(emptyIfNull(rs, 18))
      .setIssueCreatedAt(rs.getLong(19))
      .setIssueUpdatedAt(rs.getLong(20))
      .setIssueClosedAt(rs.getLong(21))
      .setProjectUuid(rs.getString(23));
    Optional.ofNullable(rs.getString(24)).ifPresent(builder::setRuleDescriptionContextKey);
    setLocations(builder, rs, issueUuid);
    setMessageFormattings(builder, rs, issueUuid);
    return builder.build();
  }

  private void setRule(ProjectDump.Issue.Builder builder, ResultSet rs) throws SQLException {
    String ruleUuid = rs.getString(2);
    String ruleKey = rs.getString(3);
    String repositoryKey = rs.getString(4);
    builder.setRuleRef(ruleRegistrar.register(ruleUuid, repositoryKey, ruleKey).ref());
  }

  private static void setLocations(ProjectDump.Issue.Builder builder, ResultSet rs, String issueUuid) throws SQLException {
    try {
      byte[] bytes = rs.getBytes(22);
      if (bytes != null) {
        // fail fast, ensure we can read data from DB
        DbIssues.Locations.parseFrom(bytes);
        builder.setLocations(ByteString.copyFrom(bytes));
      }
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException(format("Fail to read locations from DB for issue %s", issueUuid), e);
    }
  }

  private static void setMessageFormattings(ProjectDump.Issue.Builder builder, ResultSet rs, String issueUuid) throws SQLException {
    try {
      byte[] bytes = rs.getBytes(25);
      if (bytes != null) {
        // fail fast, ensure we can read data from DB
        DbIssues.MessageFormattings messageFormattings = DbIssues.MessageFormattings.parseFrom(bytes);
        if (messageFormattings != null) {
          builder.addAllMessageFormattings(dbToDumpMessageFormatting(messageFormattings.getMessageFormattingList()));
        }
      }
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException(format("Fail to read message formattings from DB for issue %s", issueUuid), e);
    }
  }

  @VisibleForTesting
  static List<ProjectDump.MessageFormatting> dbToDumpMessageFormatting(List<DbIssues.MessageFormatting> messageFormattingList) {
    return messageFormattingList.stream()
      .map(e -> ProjectDump.MessageFormatting.newBuilder()
        .setStart(e.getStart())
        .setEnd(e.getEnd())
        .setType(ProjectDump.MessageFormattingType.valueOf(e.getType().name())).build())
      .toList();
  }

  private static class RuleRegistrar {
    private final RuleRepository ruleRepository;
    private Rule previousRule = null;
    private String previousRuleUuid = null;

    private RuleRegistrar(RuleRepository ruleRepository) {
      this.ruleRepository = ruleRepository;
    }

    public Rule register(String ruleUuid, String repositoryKey, String ruleKey) {
      if (Objects.equals(previousRuleUuid, ruleUuid)) {
        return previousRule;
      }
      return lookup(ruleUuid, RuleKey.of(repositoryKey, ruleKey));
    }

    private Rule lookup(String ruleUuid, RuleKey ruleKey) {
      this.previousRule = ruleRepository.register(ruleUuid, ruleKey);
      this.previousRuleUuid = ruleUuid;
      return previousRule;
    }
  }

}
