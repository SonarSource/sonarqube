/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.step;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.server.computation.task.projectanalysis.issue.IssueCache;
import org.sonar.server.computation.task.projectanalysis.issue.RuleRepositoryImpl;
import org.sonar.server.computation.task.projectanalysis.issue.UpdateConflictResolver;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersistIssuesStepTest extends BaseStepTest {

  public static final long NOW = 1400000000000L;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule()
    .setOrganizationUuid("org-1");

  private DbSession session = dbTester.getSession();
  private DbClient dbClient = dbTester.getDbClient();
  private System2 system2;
  private IssueCache issueCache;
  private ComputationStep step;

  @Override
  protected ComputationStep step() {
    return step;
  }

  @Before
  public void setup() throws Exception {
    issueCache = new IssueCache(temp.newFile(), System2.INSTANCE);
    system2 = mock(System2.class);
    when(system2.now()).thenReturn(NOW);
    reportReader.setMetadata(ScannerReport.Metadata.getDefaultInstance());

    step = new PersistIssuesStep(dbClient, system2, new UpdateConflictResolver(), new RuleRepositoryImpl(dbClient, analysisMetadataHolder), issueCache);
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void insert_new_issue() {
    RuleDefinitionDto rule = RuleTesting.newRule(RuleKey.of("xoo", "S01"));
    dbTester.rules().insert(rule);
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto);
    dbClient.componentDao().insert(session, project);
    ComponentDto file = ComponentTesting.newFileDto(project, null);
    dbClient.componentDao().insert(session, file);
    session.commit();

    issueCache.newAppender().append(new DefaultIssue()
      .setKey("ISSUE")
      .setType(RuleType.CODE_SMELL)
      .setRuleKey(rule.getKey())
      .setComponentUuid(file.uuid())
      .setProjectUuid(project.uuid())
      .setSeverity(Severity.BLOCKER)
      .setStatus(Issue.STATUS_OPEN)
      .setNew(true)
      .setType(RuleType.BUG)).close();

    step.execute();

    IssueDto result = dbClient.issueDao().selectOrFailByKey(session, "ISSUE");
    assertThat(result.getKey()).isEqualTo("ISSUE");
    assertThat(result.getRuleKey()).isEqualTo(rule.getKey());
    assertThat(result.getComponentUuid()).isEqualTo(file.uuid());
    assertThat(result.getProjectUuid()).isEqualTo(project.uuid());
    assertThat(result.getSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(result.getStatus()).isEqualTo(Issue.STATUS_OPEN);
    assertThat(result.getType()).isEqualTo(RuleType.BUG.getDbConstant());
  }

  @Test
  public void close_issue() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    issueCache.newAppender().append(new DefaultIssue()
      .setKey("ISSUE")
      .setType(RuleType.CODE_SMELL)
      .setRuleKey(RuleKey.of("xoo", "S01"))
      .setComponentUuid("COMPONENT")
      .setProjectUuid("PROJECT")
      .setSeverity(Severity.BLOCKER)
      .setStatus(Issue.STATUS_CLOSED)
      .setResolution(Issue.RESOLUTION_FIXED)
      .setSelectedAt(NOW)
      .setNew(false)
      .setChanged(true)).close();

    step.execute();

    dbTester.assertDbUnit(getClass(), "close_issue-result.xml", "issues");
  }

  @Test
  public void add_comment() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    issueCache.newAppender().append(new DefaultIssue()
      .setKey("ISSUE")
      .setType(RuleType.CODE_SMELL)
      .setRuleKey(RuleKey.of("xoo", "S01"))
      .setComponentUuid("COMPONENT")
      .setProjectUuid("PROJECT")
      .setSeverity(Severity.BLOCKER)
      .setStatus(Issue.STATUS_CLOSED)
      .setResolution(Issue.RESOLUTION_FIXED)
      .setNew(false)
      .setChanged(true)
      .addComment(new DefaultIssueComment()
        .setKey("COMMENT")
        .setIssueKey("ISSUE")
        .setUserLogin("john")
        .setMarkdownText("Some text")
        .setNew(true)))
      .close();

    step.execute();

    dbTester.assertDbUnit(getClass(), "add_comment-result.xml", new String[] {"id", "created_at", "updated_at"}, "issue_changes");
  }

  @Test
  public void add_change() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    issueCache.newAppender().append(new DefaultIssue()
      .setKey("ISSUE")
      .setType(RuleType.CODE_SMELL)
      .setRuleKey(RuleKey.of("xoo", "S01"))
      .setComponentUuid("COMPONENT")
      .setProjectUuid("PROJECT")
      .setSeverity(Severity.BLOCKER)
      .setStatus(Issue.STATUS_CLOSED)
      .setResolution(Issue.RESOLUTION_FIXED)
      .setNew(false)
      .setChanged(true)
      .setCurrentChange(new FieldDiffs()
        .setIssueKey("ISSUE")
        .setUserLogin("john")
        .setDiff("technicalDebt", null, 1L)))
      .close();

    step.execute();

    dbTester.assertDbUnit(getClass(), "add_change-result.xml", new String[] {"id", "created_at", "updated_at"}, "issue_changes");
  }

}
