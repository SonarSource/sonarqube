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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;
import org.assertj.core.groups.Tuple;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.rule.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.ce.common.scanner.ScannerReportReaderRule;
import org.sonar.ce.task.projectanalysis.issue.AdHocRuleCreator;
import org.sonar.ce.task.projectanalysis.issue.ProtoIssueCache;
import org.sonar.ce.task.projectanalysis.issue.RuleRepositoryImpl;
import org.sonar.ce.task.projectanalysis.issue.UpdateConflictResolver;
import org.sonar.ce.task.projectanalysis.period.Period;
import org.sonar.ce.task.projectanalysis.period.PeriodHolderRule;
import org.sonar.ce.task.projectanalysis.util.cache.DiskCache.CacheAppender;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.AnticipatedTransitionDto;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.issue.IssueStorage;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.issue.IssueTesting.newCodeReferenceIssue;

public class PersistIssuesStepIT extends BaseStepTest {

  private static final long NOW = 1_400_000_000_000L;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ScannerReportReaderRule reportReader = new ScannerReportReaderRule();
  @Rule
  public PeriodHolderRule periodHolder = new PeriodHolderRule();

  private final System2 system2 = mock(System2.class);
  private final DbSession session = db.getSession();
  private final DbClient dbClient = db.getDbClient();
  private final UpdateConflictResolver conflictResolver = spy(new UpdateConflictResolver());
  private ProtoIssueCache protoIssueCache;
  private ComputationStep underTest;

  private final AdHocRuleCreator adHocRuleCreator = mock(AdHocRuleCreator.class);

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Before
  public void setup() throws Exception {
    periodHolder.setPeriod(new Period(NewCodePeriodType.NUMBER_OF_DAYS.name(), "10", 1000L));

    protoIssueCache = new ProtoIssueCache(temp.newFile(), System2.INSTANCE);
    reportReader.setMetadata(ScannerReport.Metadata.getDefaultInstance());

    underTest = new PersistIssuesStep(dbClient, system2, conflictResolver, new RuleRepositoryImpl(adHocRuleCreator, dbClient), periodHolder,
      protoIssueCache, new IssueStorage(), UuidFactoryImpl.INSTANCE);
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void insert_copied_issue() {
    RuleDto rule = RuleTesting.newRule(RuleKey.of("xoo", "S01"));
    db.rules().insert(rule);
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    when(system2.now()).thenReturn(NOW);
    String issueKey = "ISSUE-1";

    protoIssueCache.newAppender().append(new DefaultIssue()
      .setKey(issueKey)
      .setType(RuleType.CODE_SMELL)
      .setRuleKey(rule.getKey())
      .setComponentUuid(file.uuid())
      .setComponentKey(file.getKey())
      .setProjectUuid(project.uuid())
      .setProjectKey(project.getKey())
      .setSeverity(BLOCKER)
      .setStatus(STATUS_OPEN)
      .setTags(singletonList("test"))
      .setNew(false)
      .setCopied(true)
      .setType(RuleType.BUG)
      .setCreationDate(new Date(NOW))
      .setSelectedAt(NOW)
      .addComment(new DefaultIssueComment()
        .setKey("COMMENT")
        .setIssueKey(issueKey)
        .setUserUuid("john_uuid")
        .setMarkdownText("Some text")
        .setCreatedAt(new Date(NOW))
        .setUpdatedAt(new Date(NOW))
        .setNew(true))
      .setCurrentChange(
        new FieldDiffs()
          .setIssueKey(issueKey)
          .setUserUuid("john_uuid")
          .setDiff("technicalDebt", null, 1L)
          .setCreationDate(new Date(NOW)))
      .addImpact(SoftwareQuality.SECURITY, Severity.MEDIUM)
      .setPrioritizedRule(true))
      .close();

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    IssueDto result = dbClient.issueDao().selectOrFailByKey(session, issueKey);

    assertThat(result.getKey()).isEqualTo(issueKey);
    assertThat(result.getRuleKey()).isEqualTo(rule.getKey());
    assertThat(result.getComponentUuid()).isEqualTo(file.uuid());
    assertThat(result.getProjectUuid()).isEqualTo(project.uuid());
    assertThat(result.getSeverity()).isEqualTo(BLOCKER);
    assertThat(result.getStatus()).isEqualTo(STATUS_OPEN);
    assertThat(result.getType()).isEqualTo(RuleType.BUG.getDbConstant());
    assertThat(result.getTags()).containsExactlyInAnyOrder("test");
    assertThat(result.isNewCodeReferenceIssue()).isFalse();
    assertThat(result.getImpacts())
      .extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity)
      .containsExactlyInAnyOrder(Tuple.tuple(SoftwareQuality.SECURITY, Severity.MEDIUM));
    assertThat(result.isPrioritizedRule()).isTrue();

    List<IssueChangeDto> changes = dbClient.issueChangeDao().selectByIssueKeys(session, Arrays.asList(issueKey));
    assertThat(changes).extracting(IssueChangeDto::getChangeType).containsExactly(IssueChangeDto.TYPE_COMMENT, IssueChangeDto.TYPE_FIELD_CHANGE);
    assertThat(context.getStatistics().getAll()).contains(
      entry("inserts", "1"), entry("updates", "0"), entry("merged", "0"));
  }

  @Test
  public void insert_copied_issue_with_minimal_info() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.REFERENCE_BRANCH.name(), "master", null));

    RuleDto rule = RuleTesting.newRule(RuleKey.of("xoo", "S01"));
    db.rules().insert(rule);
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    when(system2.now()).thenReturn(NOW);
    String issueKey = "ISSUE-2";

    protoIssueCache.newAppender().append(new DefaultIssue()
      .setKey(issueKey)
      .setType(RuleType.CODE_SMELL)
      .setRuleKey(rule.getKey())
      .setComponentUuid(file.uuid())
      .setComponentKey(file.getKey())
      .setProjectUuid(project.uuid())
      .setProjectKey(project.getKey())
      .setSeverity(BLOCKER)
      .setStatus(STATUS_OPEN)
      .setNew(false)
      .setCopied(true)
      .setType(RuleType.BUG)
      .setCreationDate(new Date(NOW))
      .setSelectedAt(NOW)
      .addImpact(SoftwareQuality.SECURITY, Severity.MEDIUM))
      .close();

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    IssueDto result = dbClient.issueDao().selectOrFailByKey(session, issueKey);
    assertThat(result.getKey()).isEqualTo(issueKey);
    assertThat(result.getRuleKey()).isEqualTo(rule.getKey());
    assertThat(result.getComponentUuid()).isEqualTo(file.uuid());
    assertThat(result.getProjectUuid()).isEqualTo(project.uuid());
    assertThat(result.getSeverity()).isEqualTo(BLOCKER);
    assertThat(result.getStatus()).isEqualTo(STATUS_OPEN);
    assertThat(result.getType()).isEqualTo(RuleType.BUG.getDbConstant());
    assertThat(result.getTags()).isEmpty();
    assertThat(result.isNewCodeReferenceIssue()).isFalse();
    assertThat(result.getImpacts())
      .extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity)
      .containsExactlyInAnyOrder(Tuple.tuple(SoftwareQuality.SECURITY, Severity.MEDIUM));

    assertThat(dbClient.issueChangeDao().selectByIssueKeys(session, Arrays.asList(issueKey))).isEmpty();
    assertThat(context.getStatistics().getAll()).contains(
      entry("inserts", "1"), entry("updates", "0"), entry("merged", "0"));
  }

  @Test
  public void insert_merged_issue() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.REFERENCE_BRANCH.name(), "master", null));
    RuleDto rule = RuleTesting.newRule(RuleKey.of("xoo", "S01"));
    db.rules().insert(rule);
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    when(system2.now()).thenReturn(NOW);
    String issueKey = "ISSUE-3";

    protoIssueCache.newAppender().append(new DefaultIssue()
      .setKey(issueKey)
      .setType(RuleType.CODE_SMELL)
      .setRuleKey(rule.getKey())
      .setComponentUuid(file.uuid())
      .setComponentKey(file.getKey())
      .setProjectUuid(project.uuid())
      .setProjectKey(project.getKey())
      .setSeverity(BLOCKER)
      .setStatus(STATUS_OPEN)
      .setNew(true)
      .setIsOnChangedLine(true)
      .setCopied(true)
      .setType(RuleType.BUG)
      .setCreationDate(new Date(NOW))
      .setSelectedAt(NOW)
      .addComment(new DefaultIssueComment()
        .setKey("COMMENT")
        .setIssueKey(issueKey)
        .setUserUuid("john_uuid")
        .setMarkdownText("Some text")
        .setUpdatedAt(new Date(NOW))
        .setCreatedAt(new Date(NOW))
        .setNew(true))
      .setCurrentChange(new FieldDiffs()
        .setIssueKey(issueKey)
        .setUserUuid("john_uuid")
        .setDiff("technicalDebt", null, 1L)
        .setCreationDate(new Date(NOW)))
      .addImpact(SoftwareQuality.SECURITY, Severity.MEDIUM)
      .setPrioritizedRule(true))
      .close();

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    IssueDto result = dbClient.issueDao().selectOrFailByKey(session, issueKey);
    assertThat(result.getKey()).isEqualTo(issueKey);
    assertThat(result.getRuleKey()).isEqualTo(rule.getKey());
    assertThat(result.getComponentUuid()).isEqualTo(file.uuid());
    assertThat(result.getProjectUuid()).isEqualTo(project.uuid());
    assertThat(result.getSeverity()).isEqualTo(BLOCKER);
    assertThat(result.getStatus()).isEqualTo(STATUS_OPEN);
    assertThat(result.getType()).isEqualTo(RuleType.BUG.getDbConstant());
    assertThat(result.isNewCodeReferenceIssue()).isTrue();
    assertThat(result.getImpacts())
      .extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity)
      .containsExactlyInAnyOrder(Tuple.tuple(SoftwareQuality.SECURITY, Severity.MEDIUM));
    assertThat(result.isPrioritizedRule()).isTrue();

    List<IssueChangeDto> changes = dbClient.issueChangeDao().selectByIssueKeys(session, List.of(issueKey));
    assertThat(changes).extracting(IssueChangeDto::getChangeType).containsExactly(IssueChangeDto.TYPE_COMMENT, IssueChangeDto.TYPE_FIELD_CHANGE);
    assertThat(context.getStatistics().getAll()).contains(
      entry("inserts", "1"), entry("updates", "0"), entry("merged", "0"));
  }

  @Test
  public void update_conflicting_issue() {
    RuleDto rule = RuleTesting.newRule(RuleKey.of("xoo", "S01"));
    db.rules().insert(rule);
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    IssueDto issue = db.issues().insert(rule, project, file,
      i -> i.setStatus(STATUS_OPEN)
        .setResolution(null)
        .setCreatedAt(NOW - 1_000_000_000L)
        // simulate the issue has been updated after the analysis ran
        .setUpdatedAt(NOW + 1_000_000_000L));
    issue = dbClient.issueDao().selectByKey(db.getSession(), issue.getKey()).get();
    CacheAppender issueCacheAppender = protoIssueCache.newAppender();
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
    verify(conflictResolver).resolve(eq(defaultIssue), issueDtoCaptor.capture());
    assertThat(issueDtoCaptor.getValue().getKey()).isEqualTo(issue.getKey());
    assertThat(context.getStatistics().getAll()).contains(
      entry("inserts", "0"), entry("updates", "1"), entry("merged", "1"));
  }

  @Test
  public void insert_new_issue() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.REFERENCE_BRANCH.name(), "master", null));
    RuleDto rule = RuleTesting.newRule(RuleKey.of("xoo", "S01"));
    db.rules().insert(rule);
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    session.commit();
    String issueKey = "ISSUE-4";

    protoIssueCache.newAppender().append(new DefaultIssue()
      .setKey(issueKey)
      .setType(RuleType.CODE_SMELL)
      .setRuleKey(rule.getKey())
      .setComponentUuid(file.uuid())
      .setComponentKey(file.getKey())
      .setProjectUuid(project.uuid())
      .setProjectKey(project.getKey())
      .setSeverity(BLOCKER)
      .setStatus(STATUS_OPEN)
      .setCreationDate(new Date(NOW))
      .setNew(true)
      .setIsOnChangedLine(true)
      .addImpact(SoftwareQuality.SECURITY, Severity.MEDIUM)
      .setType(RuleType.BUG)
      .setPrioritizedRule(true)).close();

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    IssueDto result = dbClient.issueDao().selectOrFailByKey(session, issueKey);
    assertThat(result.getKey()).isEqualTo(issueKey);
    assertThat(result.getRuleKey()).isEqualTo(rule.getKey());
    assertThat(result.getComponentUuid()).isEqualTo(file.uuid());
    assertThat(result.getProjectUuid()).isEqualTo(project.uuid());
    assertThat(result.getSeverity()).isEqualTo(BLOCKER);
    assertThat(result.getStatus()).isEqualTo(STATUS_OPEN);
    assertThat(result.getType()).isEqualTo(RuleType.BUG.getDbConstant());
    assertThat(result.getImpacts()).extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity)
      .containsExactly(tuple(SoftwareQuality.SECURITY, Severity.MEDIUM));
    assertThat(context.getStatistics().getAll()).contains(
      entry("inserts", "1"), entry("updates", "0"), entry("merged", "0"));
    assertThat(result.isNewCodeReferenceIssue()).isTrue();
    assertThat(result.isPrioritizedRule()).isTrue();
  }

  @Test
  public void execute_shouldInsertNewIssuesInBatchesWhenGreaterThan500() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.REFERENCE_BRANCH.name(), "master", null));
    RuleDto rule = RuleTesting.newRule(RuleKey.of("xoo", "S01"));
    db.rules().insert(rule);
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    session.commit();
    String issueKey = "ISSUE-";
    CacheAppender<DefaultIssue> appender = protoIssueCache.newAppender();
    IntStream.range(1, 501).forEach(i -> appender.append(new DefaultIssue()
      .setKey(issueKey + i)
      .setType(RuleType.CODE_SMELL)
      .setRuleKey(rule.getKey())
      .setComponentUuid(file.uuid())
      .setComponentKey(file.getKey())
      .setProjectUuid(project.uuid())
      .setProjectKey(project.getKey())
      .setSeverity(BLOCKER)
      .setStatus(STATUS_OPEN)
      .setCreationDate(new Date(NOW))
      .setNew(true)
      .setIsOnChangedLine(true)
      .addImpact(SoftwareQuality.SECURITY, Severity.MEDIUM)
      .setType(RuleType.BUG)));
    appender.close();

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(context.getStatistics().getAll()).contains(
      entry("inserts", "500"), entry("updates", "0"), entry("merged", "0"));
  }

  @Test
  public void execute_shouldUpdateIssuesInBatchesWhenGreaterThan500() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insert();
    List<IssueDto> issues = IntStream.range(1, 501)
      .mapToObj(value -> db.issues().insert(rule, project, file,
        i -> i.setStatus(STATUS_OPEN)
          .setResolution(null)
          .setCreatedAt(NOW - 1_000_000_000L)
          .setUpdatedAt(NOW - 1_000_000_000L)))
      .toList();

    CacheAppender issueCacheAppender = protoIssueCache.newAppender();
    issues.forEach(issue -> issueCacheAppender.append(issue.toDefaultIssue()
      .setStatus(STATUS_CLOSED)
      .setResolution(RESOLUTION_FIXED)
      .setSelectedAt(NOW)
      .addImpact(SoftwareQuality.SECURITY, Severity.MEDIUM)
      .setNew(false)
      .setChanged(true)));

    issueCacheAppender.close();

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(context.getStatistics().getAll()).contains(
      entry("inserts", "0"), entry("updates", "500"), entry("merged", "0"));
  }

  @Test
  public void close_issue() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, file,
      i -> i.setStatus(STATUS_OPEN)
        .setResolution(null)
        .setCreatedAt(NOW - 1_000_000_000L)
        .setUpdatedAt(NOW - 1_000_000_000L));
    CacheAppender issueCacheAppender = protoIssueCache.newAppender();

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
    assertThat(context.getStatistics().getAll()).contains(
      entry("inserts", "0"), entry("updates", "1"), entry("merged", "0"));
  }

  @Test
  public void handle_no_longer_new_issue() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.REFERENCE_BRANCH.name(), "master", null));
    RuleDto rule = RuleTesting.newRule(RuleKey.of("xoo", "S01"));
    db.rules().insert(rule);
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    when(system2.now()).thenReturn(NOW);
    String issueKey = "ISSUE-5";

    DefaultIssue defaultIssue = new DefaultIssue()
      .setKey(issueKey)
      .setType(RuleType.CODE_SMELL)
      .setRuleKey(rule.getKey())
      .setComponentUuid(file.uuid())
      .setComponentKey(file.getKey())
      .setProjectUuid(project.uuid())
      .setProjectKey(project.getKey())
      .setSeverity(BLOCKER)
      .setStatus(STATUS_OPEN)
      .setNew(true)
      .setIsOnChangedLine(true)
      .setIsNewCodeReferenceIssue(true)
      .setIsNoLongerNewCodeReferenceIssue(false)
      .setCopied(false)
      .setType(RuleType.BUG)
      .setCreationDate(new Date(NOW))
      .setSelectedAt(NOW);

    IssueDto issueDto = IssueDto.toDtoForComputationInsert(defaultIssue, rule.getUuid(), NOW);
    dbClient.issueDao().insert(session, issueDto);
    dbClient.issueDao().insertAsNewCodeOnReferenceBranch(session, newCodeReferenceIssue(issueDto));
    session.commit();

    IssueDto result = dbClient.issueDao().selectOrFailByKey(session, issueKey);
    assertThat(result.isNewCodeReferenceIssue()).isTrue();

    protoIssueCache.newAppender().append(defaultIssue.setNew(false)
      .setIsOnChangedLine(false)
      .setIsNewCodeReferenceIssue(false)
      .setIsNoLongerNewCodeReferenceIssue(true))
      .close();

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(context.getStatistics().getAll()).contains(
      entry("inserts", "0"), entry("updates", "1"), entry("merged", "0"));

    result = dbClient.issueDao().selectOrFailByKey(session, issueKey);
    assertThat(result.isNewCodeReferenceIssue()).isFalse();
  }

  @Test
  public void handle_existing_new_code_issue_migration() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.REFERENCE_BRANCH.name(), "master", null));
    RuleDto rule = RuleTesting.newRule(RuleKey.of("xoo", "S01"));
    db.rules().insert(rule);
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    when(system2.now()).thenReturn(NOW);
    String issueKey = "ISSUE-6";

    DefaultIssue defaultIssue = new DefaultIssue()
      .setKey(issueKey)
      .setType(RuleType.CODE_SMELL)
      .setRuleKey(rule.getKey())
      .setComponentUuid(file.uuid())
      .setComponentKey(file.getKey())
      .setProjectUuid(project.uuid())
      .setProjectKey(project.getKey())
      .setSeverity(BLOCKER)
      .setStatus(STATUS_OPEN)
      .setNew(true)
      .setCopied(false)
      .setType(RuleType.BUG)
      .setCreationDate(new Date(NOW))
      .setSelectedAt(NOW);

    IssueDto issueDto = IssueDto.toDtoForComputationInsert(defaultIssue, rule.getUuid(), NOW);
    dbClient.issueDao().insert(session, issueDto);
    session.commit();

    IssueDto result = dbClient.issueDao().selectOrFailByKey(session, issueKey);
    assertThat(result.isNewCodeReferenceIssue()).isFalse();

    protoIssueCache.newAppender().append(defaultIssue.setNew(false)
      .setIsOnChangedLine(true)
      .setIsNewCodeReferenceIssue(false)
      .setIsNoLongerNewCodeReferenceIssue(false))
      .close();

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(context.getStatistics().getAll()).contains(
      entry("inserts", "0"), entry("updates", "1"), entry("merged", "0"));

    result = dbClient.issueDao().selectOrFailByKey(session, issueKey);
    assertThat(result.isNewCodeReferenceIssue()).isTrue();
  }

  @Test
  public void handle_existing_without_need_for_new_code_issue_migration() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.REFERENCE_BRANCH.name(), "master", null));
    RuleDto rule = RuleTesting.newRule(RuleKey.of("xoo", "S01"));
    db.rules().insert(rule);
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    when(system2.now()).thenReturn(NOW);
    String issueKey = "ISSUE-7";

    DefaultIssue defaultIssue = new DefaultIssue()
      .setKey(issueKey)
      .setType(RuleType.CODE_SMELL)
      .setRuleKey(rule.getKey())
      .setComponentUuid(file.uuid())
      .setComponentKey(file.getKey())
      .setProjectUuid(project.uuid())
      .setProjectKey(project.getKey())
      .setSeverity(BLOCKER)
      .setStatus(STATUS_OPEN)
      .setNew(true)
      .setIsOnChangedLine(true)
      .setIsNewCodeReferenceIssue(true)
      .setIsNoLongerNewCodeReferenceIssue(false)
      .setCopied(false)
      .setType(RuleType.BUG)
      .setCreationDate(new Date(NOW))
      .setSelectedAt(NOW);

    IssueDto issueDto = IssueDto.toDtoForComputationInsert(defaultIssue, rule.getUuid(), NOW);
    dbClient.issueDao().insert(session, issueDto);
    dbClient.issueDao().insertAsNewCodeOnReferenceBranch(session, newCodeReferenceIssue(issueDto));
    session.commit();

    IssueDto result = dbClient.issueDao().selectOrFailByKey(session, issueKey);
    assertThat(result.isNewCodeReferenceIssue()).isTrue();

    protoIssueCache.newAppender().append(defaultIssue.setNew(false)
      .setIsOnChangedLine(false)
      .setIsNewCodeReferenceIssue(true)
      .setIsOnChangedLine(true)
      .setIsNoLongerNewCodeReferenceIssue(false))
      .close();

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(context.getStatistics().getAll()).contains(
      entry("inserts", "0"), entry("updates", "0"), entry("merged", "0"));

    result = dbClient.issueDao().selectOrFailByKey(session, issueKey);
    assertThat(result.isNewCodeReferenceIssue()).isTrue();
  }

  @Test
  public void add_comment() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, file,
      i -> i.setStatus(STATUS_OPEN)
        .setResolution(null)
        .setCreatedAt(NOW - 1_000_000_000L)
        .setUpdatedAt(NOW - 1_000_000_000L));
    CacheAppender issueCacheAppender = protoIssueCache.newAppender();

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
          .setUpdatedAt(new Date(NOW))
          .setNew(true)))
      .close();

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    IssueChangeDto issueChangeDto = db.getDbClient().issueChangeDao().selectByIssueKeys(db.getSession(), singletonList(issue.getKey())).get(0);
    assertThat(issueChangeDto)
      .extracting(IssueChangeDto::getChangeType, IssueChangeDto::getUserUuid, IssueChangeDto::getChangeData, IssueChangeDto::getIssueKey,
        IssueChangeDto::getIssueChangeCreationDate)
      .containsOnly(IssueChangeDto.TYPE_COMMENT, "john_uuid", "Some text", issue.getKey(), NOW);
    assertThat(context.getStatistics().getAll()).contains(
      entry("inserts", "0"), entry("updates", "1"), entry("merged", "0"));
  }

  @Test
  public void add_change() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, file,
      i -> i.setStatus(STATUS_OPEN)
        .setResolution(null)
        .setCreatedAt(NOW - 1_000_000_000L)
        .setUpdatedAt(NOW - 1_000_000_000L));
    CacheAppender issueCacheAppender = protoIssueCache.newAppender();

    issueCacheAppender.append(
      issue.toDefaultIssue()
        .setStatus(STATUS_CLOSED)
        .setResolution(RESOLUTION_FIXED)
        .setSelectedAt(NOW)
        .setNew(false)
        .setChanged(true)
        .setIsOnChangedLine(false)
        .setIsNewCodeReferenceIssue(false)
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
    assertThat(context.getStatistics().getAll()).contains(
      entry("inserts", "0"), entry("updates", "1"), entry("merged", "0"));
  }

  @Test
  public void when_anticipatedTransitionIsPresent_ItShouldBeDeleted() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.REFERENCE_BRANCH.name(), "master", null));
    RuleDto rule = RuleTesting.newRule(RuleKey.of("xoo", "S01"));
    db.rules().insert(rule);
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    session.commit();
    String issueKey = "ISSUE-4";

    DefaultIssue newIssue = new DefaultIssue()
      .setKey(issueKey)
      .setType(RuleType.CODE_SMELL)
      .setRuleKey(rule.getKey())
      .setComponentUuid(file.uuid())
      .setComponentKey(file.getKey())
      .setProjectUuid(project.uuid())
      .setProjectKey(project.getKey())
      .setSeverity(BLOCKER)
      .setStatus(STATUS_OPEN)
      .setCreationDate(new Date(NOW))
      .setNew(true)
      .setIsOnChangedLine(true)
      .setType(RuleType.BUG);

    AnticipatedTransitionDto atDto = db.anticipatedTransitions().createForIssue(newIssue, "test_uuid", file.name());
    newIssue.setAnticipatedTransitionUuid(atDto.getUuid());

    var defaultIssueCacheAppender = protoIssueCache.newAppender();
    defaultIssueCacheAppender.append(newIssue).close();

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    IssueDto result = dbClient.issueDao().selectOrFailByKey(session, issueKey);
    assertThat(result.getKey()).isEqualTo(issueKey);
    assertThat(result.getRuleKey()).isEqualTo(rule.getKey());
    assertThat(result.getComponentUuid()).isEqualTo(file.uuid());
    assertThat(result.getProjectUuid()).isEqualTo(project.uuid());
    assertThat(result.getSeverity()).isEqualTo(BLOCKER);
    assertThat(result.getStatus()).isEqualTo(STATUS_OPEN);
    assertThat(result.getType()).isEqualTo(RuleType.BUG.getDbConstant());
    assertThat(context.getStatistics().getAll()).contains(
      entry("inserts", "1"), entry("updates", "0"), entry("merged", "0"));
    assertThat(result.isNewCodeReferenceIssue()).isTrue();

    assertThat(db.anticipatedTransitions().selectByProjectUuid(project.uuid())).isEmpty();
  }
}
