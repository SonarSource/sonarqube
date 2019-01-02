/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.issue.AdHocRuleCreator;
import org.sonar.ce.task.projectanalysis.issue.IssueCache;
import org.sonar.ce.task.projectanalysis.issue.RuleRepositoryImpl;
import org.sonar.ce.task.projectanalysis.issue.UpdateConflictResolver;
import org.sonar.ce.task.projectanalysis.util.cache.DiskCache;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueMapper;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.issue.IssueStorage;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.db.component.ComponentTesting.newFileDto;

public class PersistIssuesStepTest extends BaseStepTest {

  private static final long NOW = 1_400_000_000_000L;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule()
    .setOrganizationUuid("org-1", "qg-uuid-1");

  private System2 system2 = mock(System2.class);
  private DbSession session = db.getSession();
  private DbClient dbClient = db.getDbClient();
  private UpdateConflictResolver conflictResolver = mock(UpdateConflictResolver.class);
  private IssueCache issueCache;
  private ComputationStep underTest;

  private AdHocRuleCreator adHocRuleCreator = mock(AdHocRuleCreator.class);

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Before
  public void setup() throws Exception {
    issueCache = new IssueCache(temp.newFile(), System2.INSTANCE);
    reportReader.setMetadata(ScannerReport.Metadata.getDefaultInstance());

    underTest = new PersistIssuesStep(dbClient, system2, conflictResolver, new RuleRepositoryImpl(adHocRuleCreator, dbClient, analysisMetadataHolder), issueCache,
      new IssueStorage());
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void insert_copied_issue() {
    RuleDefinitionDto rule = RuleTesting.newRule(RuleKey.of("xoo", "S01"));
    db.rules().insert(rule);
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organizationDto);
    ComponentDto file = db.components().insertComponent(newFileDto(project, null));
    when(system2.now()).thenReturn(NOW);

    issueCache.newAppender().append(new DefaultIssue()
      .setKey("ISSUE")
      .setType(RuleType.CODE_SMELL)
      .setRuleKey(rule.getKey())
      .setComponentUuid(file.uuid())
      .setProjectUuid(project.uuid())
      .setSeverity(BLOCKER)
      .setStatus(STATUS_OPEN)
      .setNew(false)
      .setCopied(true)
      .setType(RuleType.BUG)
      .setSelectedAt(NOW)
      .addComment(new DefaultIssueComment()
        .setKey("COMMENT")
        .setIssueKey("ISSUE")
        .setUserUuid("john_uuid")
        .setMarkdownText("Some text")
        .setCreatedAt(new Date(NOW))
        .setNew(true))
      .setCurrentChange(
        new FieldDiffs()
          .setIssueKey("ISSUE")
          .setUserUuid("john_uuid")
          .setDiff("technicalDebt", null, 1L)
          .setCreationDate(new Date(NOW))))
      .close();

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    IssueDto result = dbClient.issueDao().selectOrFailByKey(session, "ISSUE");
    assertThat(result.getKey()).isEqualTo("ISSUE");
    assertThat(result.getRuleKey()).isEqualTo(rule.getKey());
    assertThat(result.getComponentUuid()).isEqualTo(file.uuid());
    assertThat(result.getProjectUuid()).isEqualTo(project.uuid());
    assertThat(result.getSeverity()).isEqualTo(BLOCKER);
    assertThat(result.getStatus()).isEqualTo(STATUS_OPEN);
    assertThat(result.getType()).isEqualTo(RuleType.BUG.getDbConstant());

    List<IssueChangeDto> changes = dbClient.issueChangeDao().selectByIssueKeys(session, Arrays.asList("ISSUE"));
    assertThat(changes).extracting(IssueChangeDto::getChangeType).containsExactly(IssueChangeDto.TYPE_COMMENT, IssueChangeDto.TYPE_FIELD_CHANGE);
    assertThat(context.getStatistics().getAll()).containsOnly(
      entry("inserts", "1"), entry("updates", "0"), entry("merged", "0"), entry("untouched", "0"));
  }

  @Test
  public void insert_merged_issue() {
    RuleDefinitionDto rule = RuleTesting.newRule(RuleKey.of("xoo", "S01"));
    db.rules().insert(rule);
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organizationDto);
    ComponentDto file = db.components().insertComponent(newFileDto(project, null));
    when(system2.now()).thenReturn(NOW);

    issueCache.newAppender().append(new DefaultIssue()
      .setKey("ISSUE")
      .setType(RuleType.CODE_SMELL)
      .setRuleKey(rule.getKey())
      .setComponentUuid(file.uuid())
      .setProjectUuid(project.uuid())
      .setSeverity(BLOCKER)
      .setStatus(STATUS_OPEN)
      .setNew(true)
      .setCopied(true)
      .setType(RuleType.BUG)
      .setSelectedAt(NOW)
      .addComment(new DefaultIssueComment()
        .setKey("COMMENT")
        .setIssueKey("ISSUE")
        .setUserUuid("john_uuid")
        .setMarkdownText("Some text")
        .setCreatedAt(new Date(NOW))
        .setNew(true))
      .setCurrentChange(new FieldDiffs()
        .setIssueKey("ISSUE")
        .setUserUuid("john_uuid")
        .setDiff("technicalDebt", null, 1L)
        .setCreationDate(new Date(NOW))))
      .close();

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    IssueDto result = dbClient.issueDao().selectOrFailByKey(session, "ISSUE");
    assertThat(result.getKey()).isEqualTo("ISSUE");
    assertThat(result.getRuleKey()).isEqualTo(rule.getKey());
    assertThat(result.getComponentUuid()).isEqualTo(file.uuid());
    assertThat(result.getProjectUuid()).isEqualTo(project.uuid());
    assertThat(result.getSeverity()).isEqualTo(BLOCKER);
    assertThat(result.getStatus()).isEqualTo(STATUS_OPEN);
    assertThat(result.getType()).isEqualTo(RuleType.BUG.getDbConstant());

    List<IssueChangeDto> changes = dbClient.issueChangeDao().selectByIssueKeys(session, Arrays.asList("ISSUE"));
    assertThat(changes).extracting(IssueChangeDto::getChangeType).containsExactly(IssueChangeDto.TYPE_COMMENT, IssueChangeDto.TYPE_FIELD_CHANGE);
    assertThat(context.getStatistics().getAll()).containsOnly(
      entry("inserts", "1"), entry("updates", "0"), entry("merged", "0"), entry("untouched", "0"));
  }

  @Test
  public void update_conflicting_issue() {
    RuleDefinitionDto rule = RuleTesting.newRule(RuleKey.of("xoo", "S01"));
    db.rules().insert(rule);
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organizationDto);
    ComponentDto file = db.components().insertComponent(newFileDto(project, null));
    IssueDto issue = db.issues().insert(rule, project, file,
      i -> i.setStatus(STATUS_OPEN)
        .setResolution(null)
        .setCreatedAt(NOW - 1_000_000_000L)
        // simulate the issue has been updated after the analysis ran
        .setUpdatedAt(NOW + 1_000_000_000L));
    issue = dbClient.issueDao().selectByKey(db.getSession(), issue.getKey()).get();
    DiskCache<DefaultIssue>.DiskAppender issueCacheAppender = issueCache.newAppender();
    when(system2.now()).thenReturn(NOW);

    DefaultIssue defaultIssue = issue.toDefaultIssue()
      .setStatus(STATUS_CLOSED)
      .setResolution(RESOLUTION_FIXED)
      .setSelectedAt(NOW)
      .setNew(false)
      .setChanged(true);
    issueCacheAppender.append(defaultIssue).close();

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    ArgumentCaptor<IssueDto> issueDtoCaptor = ArgumentCaptor.forClass(IssueDto.class);
    verify(conflictResolver).resolve(eq(defaultIssue), issueDtoCaptor.capture(), any(IssueMapper.class));
    assertThat(issueDtoCaptor.getValue().getId()).isEqualTo(issue.getId());
    assertThat(context.getStatistics().getAll()).containsOnly(
      entry("inserts", "0"), entry("updates", "1"), entry("merged", "1"), entry("untouched", "0"));

  }

  @Test
  public void insert_new_issue() {
    RuleDefinitionDto rule = RuleTesting.newRule(RuleKey.of("xoo", "S01"));
    db.rules().insert(rule);
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organizationDto);
    ComponentDto file = db.components().insertComponent(newFileDto(project, null));
    session.commit();

    issueCache.newAppender().append(new DefaultIssue()
      .setKey("ISSUE")
      .setType(RuleType.CODE_SMELL)
      .setRuleKey(rule.getKey())
      .setComponentUuid(file.uuid())
      .setProjectUuid(project.uuid())
      .setSeverity(BLOCKER)
      .setStatus(STATUS_OPEN)
      .setNew(true)
      .setType(RuleType.BUG)).close();

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    IssueDto result = dbClient.issueDao().selectOrFailByKey(session, "ISSUE");
    assertThat(result.getKey()).isEqualTo("ISSUE");
    assertThat(result.getRuleKey()).isEqualTo(rule.getKey());
    assertThat(result.getComponentUuid()).isEqualTo(file.uuid());
    assertThat(result.getProjectUuid()).isEqualTo(project.uuid());
    assertThat(result.getSeverity()).isEqualTo(BLOCKER);
    assertThat(result.getStatus()).isEqualTo(STATUS_OPEN);
    assertThat(result.getType()).isEqualTo(RuleType.BUG.getDbConstant());
    assertThat(context.getStatistics().getAll()).containsOnly(
      entry("inserts", "1"), entry("updates", "0"), entry("merged", "0"), entry("untouched", "0"));
  }

  @Test
  public void close_issue() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, file,
      i -> i.setStatus(STATUS_OPEN)
        .setResolution(null)
        .setCreatedAt(NOW - 1_000_000_000L)
        .setUpdatedAt(NOW - 1_000_000_000L));
    DiskCache<DefaultIssue>.DiskAppender issueCacheAppender = issueCache.newAppender();

    issueCacheAppender.append(
      issue.toDefaultIssue()
        .setStatus(STATUS_CLOSED)
        .setResolution(RESOLUTION_FIXED)
        .setSelectedAt(NOW)
        .setNew(false)
        .setChanged(true))
      .close();

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    IssueDto issueReloaded = db.getDbClient().issueDao().selectByKey(db.getSession(), issue.getKey()).get();
    assertThat(issueReloaded.getStatus()).isEqualTo(STATUS_CLOSED);
    assertThat(issueReloaded.getResolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(context.getStatistics().getAll()).containsOnly(
      entry("inserts", "0"), entry("updates", "1"), entry("merged", "0"), entry("untouched", "0"));
  }

  @Test
  public void add_comment() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, file,
      i -> i.setStatus(STATUS_OPEN)
        .setResolution(null)
        .setCreatedAt(NOW - 1_000_000_000L)
        .setUpdatedAt(NOW - 1_000_000_000L));
    DiskCache<DefaultIssue>.DiskAppender issueCacheAppender = issueCache.newAppender();

    issueCacheAppender.append(
      issue.toDefaultIssue()
        .setStatus(STATUS_CLOSED)
        .setResolution(RESOLUTION_FIXED)
        .setSelectedAt(NOW)
        .setNew(false)
        .setChanged(true)
        .addComment(new DefaultIssueComment()
          .setKey("COMMENT")
          .setIssueKey(issue.getKey())
          .setUserUuid("john_uuid")
          .setMarkdownText("Some text")
          .setCreatedAt(new Date(NOW))
          .setNew(true)))
      .close();

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    IssueChangeDto issueChangeDto = db.getDbClient().issueChangeDao().selectByIssueKeys(db.getSession(), singletonList(issue.getKey())).get(0);
    assertThat(issueChangeDto)
      .extracting(IssueChangeDto::getChangeType, IssueChangeDto::getUserUuid, IssueChangeDto::getChangeData, IssueChangeDto::getIssueKey,
        IssueChangeDto::getIssueChangeCreationDate)
      .containsOnly(IssueChangeDto.TYPE_COMMENT, "john_uuid", "Some text", issue.getKey(), NOW);
    assertThat(context.getStatistics().getAll()).containsOnly(
      entry("inserts", "0"), entry("updates", "1"), entry("merged", "0"), entry("untouched", "0"));
  }

  @Test
  public void add_change() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, file,
      i -> i.setStatus(STATUS_OPEN)
        .setResolution(null)
        .setCreatedAt(NOW - 1_000_000_000L)
        .setUpdatedAt(NOW - 1_000_000_000L));
    DiskCache<DefaultIssue>.DiskAppender issueCacheAppender = issueCache.newAppender();

    issueCacheAppender.append(
      issue.toDefaultIssue()
        .setStatus(STATUS_CLOSED)
        .setResolution(RESOLUTION_FIXED)
        .setSelectedAt(NOW)
        .setNew(false)
        .setChanged(true)
        .setCurrentChange(new FieldDiffs()
          .setIssueKey("ISSUE")
          .setUserUuid("john_uuid")
          .setDiff("technicalDebt", null, 1L)
          .setCreationDate(new Date(NOW))))
      .close();

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    IssueChangeDto issueChangeDto = db.getDbClient().issueChangeDao().selectByIssueKeys(db.getSession(), singletonList(issue.getKey())).get(0);
    assertThat(issueChangeDto)
      .extracting(IssueChangeDto::getChangeType, IssueChangeDto::getUserUuid, IssueChangeDto::getChangeData, IssueChangeDto::getIssueKey,
        IssueChangeDto::getIssueChangeCreationDate)
      .containsOnly(IssueChangeDto.TYPE_FIELD_CHANGE, "john_uuid", "technicalDebt=1", issue.getKey(), NOW);
    assertThat(context.getStatistics().getAll()).containsOnly(
      entry("inserts", "0"), entry("updates", "1"), entry("merged", "0"), entry("untouched", "0"));
  }

}
