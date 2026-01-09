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
package org.sonar.ce.task.projectanalysis.issue;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.ce.common.scanner.ScannerReportReaderRule;
import org.sonar.ce.task.projectanalysis.analysis.Analysis;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.analysis.ScannerPlugin;
import org.sonar.ce.task.projectanalysis.component.BranchComponentUuidsDelegate;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileStatuses;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitor;
import org.sonar.ce.task.projectanalysis.filemove.MovedFilesRepository;
import org.sonar.ce.task.projectanalysis.issue.filter.IssueFilter;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRulesHolder;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRulesHolderRule;
import org.sonar.ce.task.projectanalysis.qualityprofile.AlwaysActiveRulesHolderImpl;
import org.sonar.ce.task.projectanalysis.source.NewLinesRepository;
import org.sonar.ce.task.projectanalysis.source.SourceLinesHashRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.issue.tracking.Input;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IntegrateIssuesVisitorIT {

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

  @TempDir
  File tempDirectory;

  @RegisterExtension
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @RegisterExtension
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @RegisterExtension
  public ScannerReportReaderRule reportReader = new ScannerReportReaderRule();
  @RegisterExtension
  public ActiveRulesHolderRule activeRulesHolderRule = new ActiveRulesHolderRule();
  @RegisterExtension
  public RuleRepositoryRule ruleRepositoryRule = new RuleRepositoryRule();

  private final AnalysisMetadataHolder analysisMetadataHolder = mock(AnalysisMetadataHolder.class);
  private final IssueFilter issueFilter = mock(IssueFilter.class);
  private final MovedFilesRepository movedFilesRepository = mock(MovedFilesRepository.class);
  private final IssueChangeContext issueChangeContext = mock(IssueChangeContext.class);
  private final IssueLifecycle issueLifecycle = new IssueLifecycle(analysisMetadataHolder, issueChangeContext, mock(IssueWorkflow.class), new IssueFieldsSetter(),
    mock(DebtCalculator.class), ruleRepositoryRule);
  private final IssueVisitor issueVisitor = mock(IssueVisitor.class);
  private final BranchComponentUuidsDelegate mergeBranchComponentsUuids = mock(BranchComponentUuidsDelegate.class);
  private final SiblingsIssueMerger issueStatusCopier = mock(SiblingsIssueMerger.class);
  private final BranchComponentUuidsDelegate referenceBranchComponentUuids = mock(BranchComponentUuidsDelegate.class);
  private final SourceLinesHashRepository sourceLinesHash = mock(SourceLinesHashRepository.class);
  private final NewLinesRepository newLinesRepository = mock(NewLinesRepository.class);
  private final TargetBranchComponentUuids targetBranchComponentUuids = mock(TargetBranchComponentUuids.class);
  private final FileStatuses fileStatuses = mock(FileStatuses.class);
  private TrackerRawInputFactory rawInputFactory;
  private ArgumentCaptor<DefaultIssue> defaultIssueCaptor;

  private final ComponentIssuesLoader issuesLoader = new ComponentIssuesLoader(dbTester.getDbClient(), ruleRepositoryRule, activeRulesHolderRule, new MapSettings().asConfig(),
    System2.INSTANCE, mock(IssueChangesToDeleteRepository.class));
  private final ActiveRulesHolder activeRulesHolder = new AlwaysActiveRulesHolderImpl();
  private final LocationHashesService locationHashesService = mock(LocationHashesService.class);
  private ProtoIssueCache protoIssueCache;

  private TypeAwareVisitor underTest;

  @BeforeEach
  void setUp() throws Exception {
    IssueVisitors issueVisitors = new IssueVisitors(new IssueVisitor[]{issueVisitor});

    defaultIssueCaptor = ArgumentCaptor.forClass(DefaultIssue.class);
    when(movedFilesRepository.getOriginalFile(any(Component.class))).thenReturn(Optional.empty());

    DbClient dbClient = dbTester.getDbClient();
    rawInputFactory = spy(new TrackerRawInputFactory(treeRootHolder, reportReader, sourceLinesHash, issueFilter,
      ruleRepositoryRule, activeRulesHolder));
    TrackerBaseInputFactory baseInputFactory = new TrackerBaseInputFactory(issuesLoader, dbClient, movedFilesRepository);
    TrackerTargetBranchInputFactory targetInputFactory = new TrackerTargetBranchInputFactory(issuesLoader, targetBranchComponentUuids, dbClient, movedFilesRepository);
    TrackerReferenceBranchInputFactory mergeInputFactory = new TrackerReferenceBranchInputFactory(issuesLoader, mergeBranchComponentsUuids, dbClient);
    ClosedIssuesInputFactory closedIssuesInputFactory = new ClosedIssuesInputFactory(issuesLoader, dbClient, movedFilesRepository);
    TrackerExecution tracker = new TrackerExecution(baseInputFactory, closedIssuesInputFactory, new Tracker<>(), issuesLoader, analysisMetadataHolder);
    ReferenceBranchTrackerExecution mergeBranchTracker = new ReferenceBranchTrackerExecution(mergeInputFactory, new Tracker<>());
    PullRequestTrackerExecution prBranchTracker = new PullRequestTrackerExecution(baseInputFactory, new Tracker<>(), newLinesRepository);
    IssueTrackingDelegator trackingDelegator = new IssueTrackingDelegator(prBranchTracker, mergeBranchTracker, tracker, analysisMetadataHolder);
    treeRootHolder.setRoot(PROJECT);
    File temp = new File(tempDirectory, "temp");
    protoIssueCache = new ProtoIssueCache(temp, System2.INSTANCE);
    when(issueFilter.accept(any(DefaultIssue.class), eq(FILE))).thenReturn(true);
    when(issueChangeContext.date()).thenReturn(new Date());
    underTest = new IntegrateIssuesVisitor(protoIssueCache, rawInputFactory, baseInputFactory, issueLifecycle, issueVisitors, trackingDelegator, issueStatusCopier,
      referenceBranchComponentUuids, mock(PullRequestSourceBranchMerger.class), fileStatuses, analysisMetadataHolder, targetInputFactory, locationHashesService);
  }

  @Test
  void process_new_issue() {
    // Active rule has severity major
    ruleRepositoryRule.add(RuleTesting.XOO_X1);
    when(analysisMetadataHolder.isBranch()).thenReturn(true);
    ScannerReport.Issue reportIssue = getReportIssue(RuleTesting.XOO_X1);
    reportReader.putIssues(FILE_REF, singletonList(reportIssue));

    underTest.visitAny(FILE);

    List<DefaultIssue> issues = newArrayList(protoIssueCache.traverse());
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).internalTags()).containsExactlyInAnyOrder("internalTag1", "internalTag2");
    assertThat(issues.get(0).codeVariants()).containsExactlyInAnyOrder("foo", "bar");
    assertThat(issues.get(0).severity()).isEqualTo(Severity.MAJOR);
    verify(locationHashesService).computeHashesAndUpdateIssues(anyCollection(), anyCollection(), eq(FILE));
  }

  @Test
  void process_new_issue_with_overridden_severity() {
    // Active rule has severity major
    ruleRepositoryRule.add(RuleTesting.XOO_X1);
    when(analysisMetadataHolder.isBranch()).thenReturn(true);
    ScannerReport.Issue reportIssue = ScannerReport.Issue.newBuilder(getReportIssue(RuleTesting.XOO_X1)).setOverriddenSeverity(Constants.Severity.BLOCKER).build();
    reportReader.putIssues(FILE_REF, singletonList(reportIssue));

    underTest.visitAny(FILE);

    List<DefaultIssue> issues = newArrayList(protoIssueCache.traverse());
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).codeVariants()).containsExactlyInAnyOrder("foo", "bar");
    assertThat(issues.get(0).severity()).isEqualTo(Severity.BLOCKER);
    verify(locationHashesService).computeHashesAndUpdateIssues(anyCollection(), anyCollection(), eq(FILE));
  }

  @Test
  void process_existing_issue() {
    // Active rule has severity major
    RuleKey ruleKey = RuleTesting.XOO_X1;
    // Issue from db has severity minor
    addBaseIssue(ruleKey);

    ScannerReport.Issue reportIssue = getReportIssue(ruleKey);
    reportReader.putIssues(FILE_REF, singletonList(reportIssue));

    underTest.visitAny(FILE);

    List<DefaultIssue> issues = newArrayList(protoIssueCache.traverse());
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.MAJOR);
    verify(locationHashesService).computeHashesAndUpdateIssues(anyCollection(), anyCollection(), eq(FILE));
  }

  @Test
  void execute_issue_visitors() {
    ruleRepositoryRule.add(RuleTesting.XOO_X1);
    ScannerReport.Issue reportIssue = getReportIssue(RuleTesting.XOO_X1);
    reportReader.putIssues(FILE_REF, singletonList(reportIssue));

    underTest.visitAny(FILE);

    verify(issueVisitor).beforeComponent(FILE);
    verify(issueVisitor).afterComponent(FILE);
    verify(issueVisitor).onIssue(eq(FILE), defaultIssueCaptor.capture());
    assertThat(defaultIssueCaptor.getValue().ruleKey().rule()).isEqualTo("x1");
    verify(locationHashesService).computeHashesAndUpdateIssues(anyCollection(), anyCollection(), eq(FILE));
  }

  @Test
  void visitAny_whenIsPullRequest_shouldCallExpectedVisitorsRawIssues() {
    when(analysisMetadataHolder.isPullRequest()).thenReturn(true);
    when(targetBranchComponentUuids.hasTargetBranchAnalysis()).thenReturn(true);

    ruleRepositoryRule.add(RuleTesting.XOO_X1);
    ScannerReport.Issue reportIssue = getReportIssue(RuleTesting.XOO_X1);
    reportReader.putIssues(FILE_REF, singletonList(reportIssue));

    IssueDto otherBranchIssueDto = addBaseIssueOnBranch(RuleTesting.XOO_X1);
    when(targetBranchComponentUuids.getTargetBranchComponentUuid(FILE.getKey())).thenReturn(otherBranchIssueDto.getComponentUuid());

    underTest.visitAny(FILE);

    ArgumentCaptor<Input<DefaultIssue>> targetInputCaptor = ArgumentCaptor.forClass(Input.class);
    ArgumentCaptor<Input<DefaultIssue>> rawInputCaptor = ArgumentCaptor.forClass(Input.class);
    verify(issueVisitor).onRawIssues(eq(FILE), rawInputCaptor.capture(), targetInputCaptor.capture());
    assertThat(rawInputCaptor.getValue().getIssues()).extracting(i -> i.ruleKey().rule()).containsExactly("x1");
    assertThat(targetInputCaptor.getValue().getIssues()).extracting(DefaultIssue::key).containsExactly(otherBranchIssueDto.getKee());
    verify(locationHashesService).computeHashesAndUpdateIssues(anyCollection(), anyCollection(), eq(FILE));
  }

  @Test
  void close_unmatched_base_issue() {
    RuleKey ruleKey = RuleTesting.XOO_X1;
    addBaseIssue(ruleKey);

    // No issue in the report
    underTest.visitAny(FILE);

    List<DefaultIssue> issues = newArrayList(protoIssueCache.traverse());
    assertThat(issues).isEmpty();
    verify(locationHashesService).computeHashesAndUpdateIssues(anyCollection(), anyCollection(), eq(FILE));
  }

  @Test
  void reuse_issues_when_data_unchanged() {
    RuleKey ruleKey = RuleTesting.XOO_X1;
    addBaseIssue(ruleKey);

    // Issue from report has severity blocker
    ScannerReport.Issue reportIssue = getReportIssue(ruleKey);
    reportReader.putIssues(FILE_REF, singletonList(reportIssue));
    when(fileStatuses.isDataUnchanged(FILE)).thenReturn(true);

    underTest.visitAny(FILE);

    // visitors get called, so measures created from issues should be calculated taking these issues into account
    verify(issueVisitor).onIssue(eq(FILE), defaultIssueCaptor.capture());
    assertThat(defaultIssueCaptor.getValue().ruleKey().rule()).isEqualTo(ruleKey.rule());

    // most issues won't go to the cache since they aren't changed and don't need to be persisted
    // In this test they are being closed but the workflows aren't working (we mock them) so nothing is changed on the issue is not cached.
    assertThat(newArrayList(protoIssueCache.traverse())).isEmpty();
    verifyNoInteractions(locationHashesService);
  }

  @Test
  void copy_issues_when_creating_new_non_main_branch() {
    when(mergeBranchComponentsUuids.getComponentUuid(FILE_KEY)).thenReturn(FILE_UUID_ON_BRANCH);
    when(referenceBranchComponentUuids.getReferenceBranchName()).thenReturn("master");

    when(analysisMetadataHolder.isBranch()).thenReturn(true);
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(true);
    Branch branch = mock(Branch.class);
    when(branch.isMain()).thenReturn(false);
    when(branch.getType()).thenReturn(BranchType.BRANCH);
    when(analysisMetadataHolder.getBranch()).thenReturn(branch);

    // Active rule has severity major
    RuleKey ruleKey = RuleTesting.XOO_X1;
    // Issue from main branch has severity minor
    addBaseIssueOnBranch(ruleKey);

    ScannerReport.Issue reportIssue = getReportIssue(ruleKey);
    reportReader.putIssues(FILE_REF, singletonList(reportIssue));

    underTest.visitAny(FILE);

    List<DefaultIssue> issues = newArrayList(protoIssueCache.traverse());
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.MAJOR);
    assertThat(issues.get(0).isNew()).isFalse();
    assertThat(issues.get(0).isCopied()).isTrue();
    assertThat(issues.get(0).changes()).hasSize(1);
    assertThat(issues.get(0).changes().get(0).diffs()).contains(entry(IssueFieldsSetter.FROM_BRANCH, new FieldDiffs.Diff<>("master", null)));
    verify(locationHashesService).computeHashesAndUpdateIssues(anyCollection(), anyCollection(), eq(FILE));
  }

  @Test
  void visitAny_whenCacheFileNotFound_shouldThrowException() {
    tempDirectory.delete();

    assertThatThrownBy(() -> underTest.visitAny(FILE))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Fail to process issues of component 'FILE_KEY'");
  }

  @Test
  void visitAny_whenPluginChangedSinceLastAnalysis_shouldNotExecuteIncrementalAnalysis() {
    RuleKey ruleKey = RuleTesting.XOO_X1;
    addBaseIssue(ruleKey);
    when(fileStatuses.isDataUnchanged(FILE)).thenReturn(true);
    Analysis analysis = mock(Analysis.class);
    when(analysis.getCreatedAt()).thenReturn(1L);
    when(analysisMetadataHolder.getBaseAnalysis()).thenReturn(analysis);
    when(analysisMetadataHolder.getScannerPluginsByKey()).thenReturn(Map.of("xoo", new ScannerPlugin("xoo", "base", 2L)));

    underTest.visitAny(FILE);

    verify(rawInputFactory).create(FILE);
    verify(locationHashesService).computeHashesAndUpdateIssues(anyCollection(), anyCollection(), eq(FILE));
  }

  private void addBaseIssue(RuleKey ruleKey) {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(PROJECT_UUID).setKey(PROJECT_KEY);
    ComponentDto file = ComponentTesting.newFileDto(project, null, FILE_UUID).setKey(FILE_KEY);
    dbTester.components().insertComponents(project, file);

    RuleDto ruleDto = RuleTesting.newRule(ruleKey);
    dbTester.rules().insert(ruleDto);
    ruleRepositoryRule.add(ruleDto);

    IssueDto issue = IssueTesting.newIssue(ruleDto, project, file)
      .setKee("ISSUE")
      .setSeverity(Severity.MINOR);
    dbTester.getDbClient().issueDao().insert(dbTester.getSession(), issue);
    dbTester.getSession().commit();
  }

  private IssueDto addBaseIssueOnBranch(RuleKey ruleKey) {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(PROJECT_UUID_ON_BRANCH).setKey(PROJECT_KEY);
    ComponentDto file = ComponentTesting.newFileDto(project, null, FILE_UUID_ON_BRANCH).setKey(FILE_KEY);
    dbTester.components().insertComponents(project, file);

    RuleDto ruleDto = RuleTesting.newRule(ruleKey);
    dbTester.rules().insert(ruleDto);
    ruleRepositoryRule.add(ruleDto);

    IssueDto issue = IssueTesting.newIssue(ruleDto, project, file)
      .setKee("ISSUE")
      .setSeverity(Severity.MAJOR)
      .setChecksum(null);
    dbTester.getDbClient().issueDao().insert(dbTester.getSession(), issue);
    dbTester.getSession().commit();
    return issue;
  }

  @NotNull
  private static ScannerReport.Issue getReportIssue(RuleKey ruleKey) {
    return ScannerReport.Issue.newBuilder()
      .setMsg("the message")
      .setRuleRepository(ruleKey.repository())
      .setRuleKey(ruleKey.rule())
      .addAllInternalTags(Set.of("internalTag1", "internalTag2"))
      .addAllCodeVariants(Set.of("foo", "bar"))
      .build();
  }
}
