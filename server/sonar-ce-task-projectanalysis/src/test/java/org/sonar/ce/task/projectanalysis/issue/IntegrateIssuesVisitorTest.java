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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileStatuses;
import org.sonar.ce.task.projectanalysis.component.ReferenceBranchComponentUuids;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.ReportModulesPath;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitor;
import org.sonar.ce.task.projectanalysis.filemove.MovedFilesRepository;
import org.sonar.ce.task.projectanalysis.issue.commonrule.CommonRuleEngineImpl;
import org.sonar.ce.task.projectanalysis.issue.filter.IssueFilter;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRulesHolder;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRulesHolderRule;
import org.sonar.ce.task.projectanalysis.qualityprofile.AlwaysActiveRulesHolderImpl;
import org.sonar.ce.task.projectanalysis.source.NewLinesRepository;
import org.sonar.ce.task.projectanalysis.source.SourceLinesHashRepository;
import org.sonar.ce.task.projectanalysis.source.SourceLinesRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.issue.tracking.Tracker;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueTesting;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.scanner.protocol.Constants;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.workflow.IssueWorkflow;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IntegrateIssuesVisitorTest {

  private static final String FILE_UUID = "FILE_UUID";
  private static final String FILE_UUID_ON_BRANCH = "FILE_UUID_BRANCH";
  private static final String FILE_KEY = "FILE_KEY";
  private static final int FILE_REF = 2;

  private static final Component FILE = ReportComponent.builder(Component.Type.FILE, FILE_REF)
    .setKey(FILE_KEY)
    .setUuid(FILE_UUID)
    .build();

  private static final String PROJECT_KEY = "PROJECT_KEY";
  private static final String PROJECT_UUID = "PROJECT_UUID";
  private static final String PROJECT_UUID_ON_BRANCH = "PROJECT_UUID_BRANCH";
  private static final int PROJECT_REF = 1;
  private static final Component PROJECT = ReportComponent.builder(Component.Type.PROJECT, PROJECT_REF)
    .setKey(PROJECT_KEY)
    .setUuid(PROJECT_UUID)
    .addChildren(FILE)
    .build();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public ActiveRulesHolderRule activeRulesHolderRule = new ActiveRulesHolderRule();
  @Rule
  public RuleRepositoryRule ruleRepositoryRule = new RuleRepositoryRule();

  private final AnalysisMetadataHolder analysisMetadataHolder = mock(AnalysisMetadataHolder.class);
  private final IssueFilter issueFilter = mock(IssueFilter.class);
  private final MovedFilesRepository movedFilesRepository = mock(MovedFilesRepository.class);
  private final IssueChangeContext issueChangeContext = mock(IssueChangeContext.class);
  private final IssueLifecycle issueLifecycle = new IssueLifecycle(analysisMetadataHolder, issueChangeContext, mock(IssueWorkflow.class), new IssueFieldsSetter(),
    mock(DebtCalculator.class), ruleRepositoryRule);
  private final IssueVisitor issueVisitor = mock(IssueVisitor.class);
  private final ReferenceBranchComponentUuids mergeBranchComponentsUuids = mock(ReferenceBranchComponentUuids.class);
  private final SiblingsIssueMerger issueStatusCopier = mock(SiblingsIssueMerger.class);
  private final ReferenceBranchComponentUuids referenceBranchComponentUuids = mock(ReferenceBranchComponentUuids.class);
  private final SourceLinesHashRepository sourceLinesHash = mock(SourceLinesHashRepository.class);
  private final NewLinesRepository newLinesRepository = mock(NewLinesRepository.class);
  private final TargetBranchComponentUuids targetBranchComponentUuids = mock(TargetBranchComponentUuids.class);
  private final SourceLinesRepository sourceLinesRepository = mock(SourceLinesRepository.class);
  private final FileStatuses fileStatuses = mock(FileStatuses.class);
  private ArgumentCaptor<DefaultIssue> defaultIssueCaptor;

  private final ComponentIssuesLoader issuesLoader = new ComponentIssuesLoader(dbTester.getDbClient(), ruleRepositoryRule, activeRulesHolderRule, new MapSettings().asConfig(),
    System2.INSTANCE, mock(IssueChangesToDeleteRepository.class));
  private IssueTrackingDelegator trackingDelegator;
  private TrackerExecution tracker;
  private PullRequestTrackerExecution prBranchTracker;
  private ReferenceBranchTrackerExecution mergeBranchTracker;
  private final ActiveRulesHolder activeRulesHolder = new AlwaysActiveRulesHolderImpl();
  private ProtoIssueCache protoIssueCache;

  private TypeAwareVisitor underTest;

  @Before
  public void setUp() throws Exception {
    IssueVisitors issueVisitors = new IssueVisitors(new IssueVisitor[] {issueVisitor});

    defaultIssueCaptor = ArgumentCaptor.forClass(DefaultIssue.class);
    when(movedFilesRepository.getOriginalFile(any(Component.class))).thenReturn(Optional.empty());

    DbClient dbClient = dbTester.getDbClient();
    TrackerRawInputFactory rawInputFactory = new TrackerRawInputFactory(treeRootHolder, reportReader, sourceLinesHash, new CommonRuleEngineImpl(), issueFilter,
      ruleRepositoryRule, activeRulesHolder);
    TrackerBaseInputFactory baseInputFactory = new TrackerBaseInputFactory(issuesLoader, dbClient, movedFilesRepository, mock(ReportModulesPath.class), analysisMetadataHolder,
      new IssueFieldsSetter(), mock(ComponentsWithUnprocessedIssues.class));
    TrackerTargetBranchInputFactory targetInputFactory = new TrackerTargetBranchInputFactory(issuesLoader, targetBranchComponentUuids, dbClient, movedFilesRepository);
    TrackerReferenceBranchInputFactory mergeInputFactory = new TrackerReferenceBranchInputFactory(issuesLoader, mergeBranchComponentsUuids, dbClient);
    ClosedIssuesInputFactory closedIssuesInputFactory = new ClosedIssuesInputFactory(issuesLoader, dbClient, movedFilesRepository);
    tracker = new TrackerExecution(baseInputFactory, closedIssuesInputFactory, new Tracker<>(), issuesLoader, analysisMetadataHolder);
    mergeBranchTracker = new ReferenceBranchTrackerExecution(mergeInputFactory, new Tracker<>());
    prBranchTracker = new PullRequestTrackerExecution(baseInputFactory, targetInputFactory, new Tracker<>(), newLinesRepository);
    trackingDelegator = new IssueTrackingDelegator(prBranchTracker, mergeBranchTracker, tracker, analysisMetadataHolder);
    treeRootHolder.setRoot(PROJECT);
    protoIssueCache = new ProtoIssueCache(temp.newFile(), System2.INSTANCE);
    when(issueFilter.accept(any(DefaultIssue.class), eq(FILE))).thenReturn(true);
    when(issueChangeContext.date()).thenReturn(new Date());
    underTest = new IntegrateIssuesVisitor(protoIssueCache, rawInputFactory, baseInputFactory, issueLifecycle, issueVisitors, trackingDelegator, issueStatusCopier,
      referenceBranchComponentUuids, mock(PullRequestSourceBranchMerger.class), fileStatuses);
  }

  @Test
  public void process_new_issue() {
    ruleRepositoryRule.add(RuleKey.of("xoo", "S001"));
    when(analysisMetadataHolder.isBranch()).thenReturn(true);
    ScannerReport.Issue reportIssue = ScannerReport.Issue.newBuilder()
      .setMsg("the message")
      .setRuleRepository("xoo")
      .setRuleKey("S001")
      .setSeverity(Constants.Severity.BLOCKER)
      .build();
    reportReader.putIssues(FILE_REF, singletonList(reportIssue));

    underTest.visitAny(FILE);

    assertThat(newArrayList(protoIssueCache.traverse())).hasSize(1);
  }

  @Test
  public void process_existing_issue() {
    RuleKey ruleKey = RuleTesting.XOO_X1;
    // Issue from db has severity major
    addBaseIssue(ruleKey);

    // Issue from report has severity blocker
    ScannerReport.Issue reportIssue = ScannerReport.Issue.newBuilder()
      .setMsg("new message")
      .setRuleRepository(ruleKey.repository())
      .setRuleKey(ruleKey.rule())
      .setSeverity(Constants.Severity.BLOCKER)
      .build();
    reportReader.putIssues(FILE_REF, singletonList(reportIssue));

    underTest.visitAny(FILE);

    List<DefaultIssue> issues = newArrayList(protoIssueCache.traverse());
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.BLOCKER);
  }

  @Test
  public void dont_cache_existing_issue_if_unmodified() {
    RuleKey ruleKey = RuleTesting.XOO_X1;
    // Issue from db has severity major
    addBaseIssue(ruleKey);

    // Issue from report has severity blocker
    ScannerReport.Issue reportIssue = ScannerReport.Issue.newBuilder()
      .setMsg("the message")
      .setRuleRepository(ruleKey.repository())
      .setRuleKey(ruleKey.rule())
      .setSeverity(Constants.Severity.BLOCKER)
      .build();
    reportReader.putIssues(FILE_REF, singletonList(reportIssue));

    underTest.visitAny(FILE);

    List<DefaultIssue> issues = newArrayList(protoIssueCache.traverse());
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.BLOCKER);
  }

  @Test
  public void execute_issue_visitors() {
    ruleRepositoryRule.add(RuleKey.of("xoo", "S001"));
    ScannerReport.Issue reportIssue = ScannerReport.Issue.newBuilder()
      .setMsg("the message")
      .setRuleRepository("xoo")
      .setRuleKey("S001")
      .setSeverity(Constants.Severity.BLOCKER)
      .build();
    reportReader.putIssues(FILE_REF, singletonList(reportIssue));

    underTest.visitAny(FILE);

    verify(issueVisitor).beforeComponent(FILE);
    verify(issueVisitor).afterComponent(FILE);
    verify(issueVisitor).onIssue(eq(FILE), defaultIssueCaptor.capture());
    assertThat(defaultIssueCaptor.getValue().ruleKey().rule()).isEqualTo("S001");
  }

  @Test
  public void close_unmatched_base_issue() {
    RuleKey ruleKey = RuleTesting.XOO_X1;
    addBaseIssue(ruleKey);

    // No issue in the report
    underTest.visitAny(FILE);

    List<DefaultIssue> issues = newArrayList(protoIssueCache.traverse());
    assertThat(issues).isEmpty();
  }

  @Test
  public void remove_uuid_of_original_file_from_componentsWithUnprocessedIssues_if_component_has_one() {
    String originalFileUuid = "original file uuid";
    when(movedFilesRepository.getOriginalFile(FILE))
      .thenReturn(Optional.of(new MovedFilesRepository.OriginalFile(originalFileUuid, "original file key")));

    underTest.visitAny(FILE);
  }

  @Test
  public void reuse_issues_when_data_unchanged() {
    RuleKey ruleKey = RuleTesting.XOO_X1;
    // Issue from db has severity major
    addBaseIssue(ruleKey);

    // Issue from report has severity blocker
    ScannerReport.Issue reportIssue = ScannerReport.Issue.newBuilder()
      .setMsg("new message")
      .setRuleRepository(ruleKey.repository())
      .setRuleKey(ruleKey.rule())
      .setSeverity(Constants.Severity.BLOCKER)
      .build();
    reportReader.putIssues(FILE_REF, singletonList(reportIssue));
    when(fileStatuses.isDataUnchanged(FILE)).thenReturn(true);

    underTest.visitAny(FILE);

    // visitors get called, so measures created from issues should be calculated taking these issues into account
    verify(issueVisitor).onIssue(eq(FILE), defaultIssueCaptor.capture());
    assertThat(defaultIssueCaptor.getValue().ruleKey().rule()).isEqualTo(ruleKey.rule());

    // most issues won't go to the cache since they aren't changed and don't need to be persisted
    // In this test they are being closed but the workflows aren't working (we mock them) so nothing is changed on the issue is not cached.
    assertThat(newArrayList(protoIssueCache.traverse())).isEmpty();
  }

  @Test
  public void copy_issues_when_creating_new_non_main_branch() {
    when(mergeBranchComponentsUuids.getComponentUuid(FILE_KEY)).thenReturn(FILE_UUID_ON_BRANCH);
    when(referenceBranchComponentUuids.getReferenceBranchName()).thenReturn("master");

    when(analysisMetadataHolder.isBranch()).thenReturn(true);
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(true);
    Branch branch = mock(Branch.class);
    when(branch.isMain()).thenReturn(false);
    when(branch.getType()).thenReturn(BranchType.BRANCH);
    when(analysisMetadataHolder.getBranch()).thenReturn(branch);

    RuleKey ruleKey = RuleTesting.XOO_X1;
    // Issue from main branch has severity major
    addBaseIssueOnBranch(ruleKey);

    // Issue from report has severity blocker
    ScannerReport.Issue reportIssue = ScannerReport.Issue.newBuilder()
      .setMsg("the message")
      .setRuleRepository(ruleKey.repository())
      .setRuleKey(ruleKey.rule())
      .setSeverity(Constants.Severity.BLOCKER)
      .build();
    reportReader.putIssues(FILE_REF, singletonList(reportIssue));

    underTest.visitAny(FILE);

    List<DefaultIssue> issues = newArrayList(protoIssueCache.traverse());
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.BLOCKER);
    assertThat(issues.get(0).isNew()).isFalse();
    assertThat(issues.get(0).isCopied()).isTrue();
    assertThat(issues.get(0).changes()).hasSize(1);
    assertThat(issues.get(0).changes().get(0).diffs()).contains(entry(IssueFieldsSetter.FROM_BRANCH, new FieldDiffs.Diff<>("master", null)));
  }

  private void addBaseIssue(RuleKey ruleKey) {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(PROJECT_UUID).setKey(PROJECT_KEY);
    ComponentDto file = ComponentTesting.newFileDto(project, null, FILE_UUID).setKey(FILE_KEY);
    dbTester.components().insertComponents(project, file);

    RuleDto ruleDto = RuleTesting.newDto(ruleKey);
    dbTester.rules().insert(ruleDto);
    ruleRepositoryRule.add(ruleKey);

    IssueDto issue = IssueTesting.newDto(ruleDto, file, project)
      .setKee("ISSUE")
      .setStatus(Issue.STATUS_OPEN)
      .setSeverity(Severity.MAJOR);
    dbTester.getDbClient().issueDao().insert(dbTester.getSession(), issue);
    dbTester.getSession().commit();
  }

  private void addBaseIssueOnBranch(RuleKey ruleKey) {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(PROJECT_UUID_ON_BRANCH).setKey(PROJECT_KEY);
    ComponentDto file = ComponentTesting.newFileDto(project, null, FILE_UUID_ON_BRANCH).setKey(FILE_KEY);
    dbTester.components().insertComponents(project, file);

    RuleDto ruleDto = RuleTesting.newDto(ruleKey);
    dbTester.rules().insert(ruleDto);
    ruleRepositoryRule.add(ruleKey);

    IssueDto issue = IssueTesting.newDto(ruleDto, file, project)
      .setKee("ISSUE")
      .setStatus(Issue.STATUS_OPEN)
      .setSeverity(Severity.MAJOR);
    dbTester.getDbClient().issueDao().insert(dbTester.getSession(), issue);
    dbTester.getSession().commit();
  }
}
