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
package org.sonar.ce.task.projectanalysis.issue;

import com.google.common.base.Optional;
import java.util.Collections;
import java.util.List;
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
import org.sonar.ce.task.projectanalysis.component.MergeBranchComponentUuids;
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
import org.sonar.ce.task.projectanalysis.source.SourceLinesRepositoryRule;
import org.sonar.core.issue.DefaultIssue;
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

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
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
  @Rule
  public SourceLinesRepositoryRule fileSourceRepository = new SourceLinesRepositoryRule();

  private AnalysisMetadataHolder analysisMetadataHolder = mock(AnalysisMetadataHolder.class);
  private IssueFilter issueFilter = mock(IssueFilter.class);
  private MovedFilesRepository movedFilesRepository = mock(MovedFilesRepository.class);
  private IssueLifecycle issueLifecycle = mock(IssueLifecycle.class);
  private IssueVisitor issueVisitor = mock(IssueVisitor.class);
  private MergeBranchComponentUuids mergeBranchComponentsUuids = mock(MergeBranchComponentUuids.class);
  private ShortBranchIssueMerger issueStatusCopier = mock(ShortBranchIssueMerger.class);
  private MergeBranchComponentUuids mergeBranchComponentUuids = mock(MergeBranchComponentUuids.class);
  private SourceLinesHashRepository sourceLinesHash = mock(SourceLinesHashRepository.class);
  private NewLinesRepository newLinesRepository = mock(NewLinesRepository.class);

  private ArgumentCaptor<DefaultIssue> defaultIssueCaptor;

  private ComponentIssuesLoader issuesLoader = new ComponentIssuesLoader(dbTester.getDbClient(), ruleRepositoryRule, activeRulesHolderRule, new MapSettings().asConfig(), System2.INSTANCE);
  private IssueTrackingDelegator trackingDelegator;
  private TrackerExecution tracker;
  private ShortBranchOrPullRequestTrackerExecution shortBranchTracker;
  private MergeBranchTrackerExecution mergeBranchTracker;
  private ActiveRulesHolder activeRulesHolder = new AlwaysActiveRulesHolderImpl();
  private IssueCache issueCache;

  private TypeAwareVisitor underTest;

  @Before
  public void setUp() throws Exception {
    IssueVisitors issueVisitors = new IssueVisitors(new IssueVisitor[] {issueVisitor});

    defaultIssueCaptor = ArgumentCaptor.forClass(DefaultIssue.class);
    when(movedFilesRepository.getOriginalFile(any(Component.class))).thenReturn(Optional.absent());

    DbClient dbClient = dbTester.getDbClient();
    TrackerRawInputFactory rawInputFactory = new TrackerRawInputFactory(treeRootHolder, reportReader, sourceLinesHash, new CommonRuleEngineImpl(),
      issueFilter, ruleRepositoryRule, activeRulesHolder);
    TrackerBaseInputFactory baseInputFactory = new TrackerBaseInputFactory(issuesLoader, dbClient, movedFilesRepository, mock(ReportModulesPath.class), analysisMetadataHolder, new IssueFieldsSetter(), mock(ComponentsWithUnprocessedIssues.class));
    TrackerMergeBranchInputFactory mergeInputFactory = new TrackerMergeBranchInputFactory(issuesLoader, mergeBranchComponentsUuids, dbClient);
    ClosedIssuesInputFactory closedIssuesInputFactory = new ClosedIssuesInputFactory(issuesLoader, dbClient, movedFilesRepository);
    tracker = new TrackerExecution(baseInputFactory, rawInputFactory, closedIssuesInputFactory, new Tracker<>(), issuesLoader, analysisMetadataHolder);
    shortBranchTracker = new ShortBranchOrPullRequestTrackerExecution(baseInputFactory, rawInputFactory, mergeInputFactory, new Tracker<>(), newLinesRepository);
    mergeBranchTracker = new MergeBranchTrackerExecution(rawInputFactory, mergeInputFactory, new Tracker<>());

    trackingDelegator = new IssueTrackingDelegator(shortBranchTracker, mergeBranchTracker, tracker, analysisMetadataHolder);
    treeRootHolder.setRoot(PROJECT);
    issueCache = new IssueCache(temp.newFile(), System2.INSTANCE);
    when(issueFilter.accept(any(DefaultIssue.class), eq(FILE))).thenReturn(true);
    underTest = new IntegrateIssuesVisitor(issueCache, issueLifecycle, issueVisitors, analysisMetadataHolder, trackingDelegator, issueStatusCopier, mergeBranchComponentUuids);
  }

  @Test
  public void process_new_issue() {
    when(analysisMetadataHolder.isLongLivingBranch()).thenReturn(true);
    ScannerReport.Issue reportIssue = ScannerReport.Issue.newBuilder()
      .setMsg("the message")
      .setRuleRepository("xoo")
      .setRuleKey("S001")
      .setSeverity(Constants.Severity.BLOCKER)
      .build();
    reportReader.putIssues(FILE_REF, asList(reportIssue));
    fileSourceRepository.addLine(FILE_REF, "line1");

    underTest.visitAny(FILE);

    verify(issueLifecycle).initNewOpenIssue(defaultIssueCaptor.capture());
    DefaultIssue capturedIssue = defaultIssueCaptor.getValue();
    assertThat(capturedIssue.ruleKey().rule()).isEqualTo("S001");

    verify(issueStatusCopier).tryMerge(FILE, Collections.singletonList(capturedIssue));

    verify(issueLifecycle).doAutomaticTransition(capturedIssue);

    assertThat(newArrayList(issueCache.traverse())).hasSize(1);
  }

  @Test
  public void process_existing_issue() {

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
    reportReader.putIssues(FILE_REF, asList(reportIssue));
    fileSourceRepository.addLine(FILE_REF, "line1");

    underTest.visitAny(FILE);

    ArgumentCaptor<DefaultIssue> rawIssueCaptor = ArgumentCaptor.forClass(DefaultIssue.class);
    ArgumentCaptor<DefaultIssue> baseIssueCaptor = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(issueLifecycle).mergeExistingOpenIssue(rawIssueCaptor.capture(), baseIssueCaptor.capture());
    assertThat(rawIssueCaptor.getValue().severity()).isEqualTo(Severity.BLOCKER);
    assertThat(baseIssueCaptor.getValue().severity()).isEqualTo(Severity.MAJOR);

    verify(issueLifecycle).doAutomaticTransition(defaultIssueCaptor.capture());
    assertThat(defaultIssueCaptor.getValue().ruleKey()).isEqualTo(ruleKey);
    List<DefaultIssue> issues = newArrayList(issueCache.traverse());
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.BLOCKER);

  }

  @Test
  public void execute_issue_visitors() {
    ScannerReport.Issue reportIssue = ScannerReport.Issue.newBuilder()
      .setMsg("the message")
      .setRuleRepository("xoo")
      .setRuleKey("S001")
      .setSeverity(Constants.Severity.BLOCKER)
      .build();
    reportReader.putIssues(FILE_REF, asList(reportIssue));
    fileSourceRepository.addLine(FILE_REF, "line1");

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

    verify(issueLifecycle).doAutomaticTransition(defaultIssueCaptor.capture());
    assertThat(defaultIssueCaptor.getValue().isBeingClosed()).isTrue();
    List<DefaultIssue> issues = newArrayList(issueCache.traverse());
    assertThat(issues).hasSize(1);
  }

  @Test
  public void remove_uuid_of_original_file_from_componentsWithUnprocessedIssues_if_component_has_one() {
    String originalFileUuid = "original file uuid";
    when(movedFilesRepository.getOriginalFile(FILE))
      .thenReturn(Optional.of(new MovedFilesRepository.OriginalFile(4851, originalFileUuid, "original file key")));

    underTest.visitAny(FILE);
  }

  @Test
  public void copy_issues_when_creating_new_long_living_branch() {

    when(mergeBranchComponentsUuids.getUuid(FILE_KEY)).thenReturn(FILE_UUID_ON_BRANCH);
    when(mergeBranchComponentUuids.getMergeBranchName()).thenReturn("master");

    when(analysisMetadataHolder.isLongLivingBranch()).thenReturn(true);
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(true);
    Branch branch = mock(Branch.class);
    when(branch.isMain()).thenReturn(false);
    when(branch.getType()).thenReturn(BranchType.LONG);
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
    reportReader.putIssues(FILE_REF, asList(reportIssue));
    fileSourceRepository.addLine(FILE_REF, "line1");

    underTest.visitAny(FILE);

    ArgumentCaptor<DefaultIssue> rawIssueCaptor = ArgumentCaptor.forClass(DefaultIssue.class);
    ArgumentCaptor<DefaultIssue> baseIssueCaptor = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(issueLifecycle).copyExistingOpenIssueFromLongLivingBranch(rawIssueCaptor.capture(), baseIssueCaptor.capture(), eq("master"));
    assertThat(rawIssueCaptor.getValue().severity()).isEqualTo(Severity.BLOCKER);
    assertThat(baseIssueCaptor.getValue().severity()).isEqualTo(Severity.MAJOR);

    verify(issueLifecycle).doAutomaticTransition(defaultIssueCaptor.capture());
    assertThat(defaultIssueCaptor.getValue().ruleKey()).isEqualTo(ruleKey);
    List<DefaultIssue> issues = newArrayList(issueCache.traverse());
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.BLOCKER);
  }

  private void addBaseIssue(RuleKey ruleKey) {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(dbTester.organizations().insert(), PROJECT_UUID).setDbKey(PROJECT_KEY);
    ComponentDto file = ComponentTesting.newFileDto(project, null, FILE_UUID).setDbKey(FILE_KEY);
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), project, file);

    RuleDto ruleDto = RuleTesting.newDto(ruleKey);
    dbTester.rules().insertRule(ruleDto);
    ruleRepositoryRule.add(ruleKey);

    IssueDto issue = IssueTesting.newDto(ruleDto, file, project)
      .setKee("ISSUE")
      .setStatus(Issue.STATUS_OPEN)
      .setSeverity(Severity.MAJOR);
    dbTester.getDbClient().issueDao().insert(dbTester.getSession(), issue);
    dbTester.getSession().commit();
  }

  private void addBaseIssueOnBranch(RuleKey ruleKey) {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(dbTester.organizations().insert(), PROJECT_UUID_ON_BRANCH).setDbKey(PROJECT_KEY);
    ComponentDto file = ComponentTesting.newFileDto(project, null, FILE_UUID_ON_BRANCH).setDbKey(FILE_KEY);
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), project, file);

    RuleDto ruleDto = RuleTesting.newDto(ruleKey);
    dbTester.rules().insertRule(ruleDto);
    ruleRepositoryRule.add(ruleKey);

    IssueDto issue = IssueTesting.newDto(ruleDto, file, project)
      .setKee("ISSUE")
      .setStatus(Issue.STATUS_OPEN)
      .setSeverity(Severity.MAJOR);
    dbTester.getDbClient().issueDao().insert(dbTester.getSession(), issue);
    dbTester.getSession().commit();
  }
}
