/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.IssueProducer;
import org.sonar.core.rule.RuleType;
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
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRule;
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
import org.sonar.server.issue.TaintChecker;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflow;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowActionsFactory;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowDefinition;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class IntegrateIssuesVisitorIT {

  private static final long NOW = 1_704_067_200_000L;
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
  private final IssueVisitor measureVisitor = mock(IssueVisitor.class, withSettings().extraInterfaces(IssueMeasureVisitor.class));
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
    IssueVisitors issueVisitors = new IssueVisitors(new IssueVisitor[]{issueVisitor, measureVisitor});

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
    when(issueChangeContext.date()).thenReturn(new Date(NOW));
    underTest = new IntegrateIssuesVisitor(protoIssueCache, rawInputFactory, baseInputFactory, issueLifecycle, issueVisitors, trackingDelegator, issueStatusCopier,
      referenceBranchComponentUuids, mock(PullRequestSourceBranchMerger.class), fileStatuses, analysisMetadataHolder, targetInputFactory, locationHashesService, issuesLoader, treeRootHolder);
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

  // Issue tracking of findings migrated from Security Hotspots (MMF-5734 / USER-2694): the next analysis must match them
  // to the raw issue of the same rule instead of closing them and creating a new one. These are the 3 states the
  // migration leaves behind: To review (Open), Acknowledged (Confirmed), and Safe or Fixed (both Accepted).
  @ParameterizedTest
  @CsvSource(value = {
    "OPEN,NULL",
    "CONFIRMED,NULL",
    "RESOLVED,WONTFIX"
  }, nullValues = "NULL")
  void migrated_hotspot_is_tracked_by_next_analysis(String status, String resolution) {
    RuleKey ruleKey = RuleTesting.XOO_X1;
    addMigratedBaseIssue(ruleKey, status, resolution);
    reportReader.putIssues(FILE_REF, singletonList(getReportIssue(ruleKey)));

    underTest.visitAny(FILE);

    verify(issueVisitor, atLeastOnce()).onIssue(eq(FILE), defaultIssueCaptor.capture());
    List<DefaultIssue> issues = defaultIssueCaptor.getAllValues();
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).key()).isEqualTo("ISSUE");
    assertThat(issues.get(0).isNew()).isFalse();
    assertThat(issues.get(0).isBeingClosed()).isFalse();
  }

  @Test
  void migrated_hotspot_in_closed_status_is_not_tracked_and_is_recreated_as_new_issue() {
    // Documents why Reviewed/Safe must not be migrated to CLOSED: closed base issues are excluded from tracking, so the
    // raw issue of the same rule on the same code becomes a brand new open issue (USER-2694, step 4).
    RuleKey ruleKey = RuleTesting.XOO_X1;
    addMigratedBaseIssue(ruleKey, Issue.STATUS_CLOSED, Issue.RESOLUTION_WONT_FIX);
    reportReader.putIssues(FILE_REF, singletonList(getReportIssue(ruleKey)));

    underTest.visitAny(FILE);

    verify(issueVisitor, atLeastOnce()).onIssue(eq(FILE), defaultIssueCaptor.capture());
    List<DefaultIssue> issues = defaultIssueCaptor.getAllValues();
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).key()).isNotEqualTo("ISSUE");
    assertThat(issues.get(0).isNew()).isTrue();
  }

  @Test
  void migrated_hotspot_is_closed_and_replaced_when_the_analysis_reports_a_different_rule_key() {
    // Signature of "all previous issues closed, new ones created": the migrated row keeps the old rule, the analyzer now
    // reports another rule key for the same code. Tracking matches on rule key, so nothing can match.
    addMigratedBaseIssue(RuleTesting.XOO_X2, Issue.STATUS_OPEN, null);
    ruleRepositoryRule.add(RuleTesting.XOO_X1);
    reportReader.putIssues(FILE_REF, singletonList(getReportIssue(RuleTesting.XOO_X1)));

    underTest.visitAny(FILE);

    verify(issueVisitor, atLeastOnce()).onIssue(eq(FILE), defaultIssueCaptor.capture());
    List<DefaultIssue> issues = defaultIssueCaptor.getAllValues();
    assertThat(issues).hasSize(2);
    assertThat(issues).filteredOn(DefaultIssue::isNew).hasSize(1);
    assertThat(issues).filteredOn(DefaultIssue::isBeingClosed).extracting(DefaultIssue::key).containsExactly("ISSUE");
  }

  /** USER-2694: a repaired row is tracked when no analysis ran between the migration and the repair. */
  @ParameterizedTest
  @ValueSource(ints = {0, 3})
  void repaired_row_is_tracked_when_the_file_is_analysed_after_the_repair(int linesAddedAbove) {
    RuleKey ruleKey = RuleTesting.XOO_X1;
    insertBaseFile(fileHashes(0));
    insertMigratedBaseIssue(ruleKey, "REPAIRED", Issue.STATUS_RESOLVED, Issue.RESOLUTION_WONT_FIX, FINDING_LINE);
    analyse(ruleKey, linesAddedAbove);

    List<DefaultIssue> issues = analysedIssues();

    assertThat(issues).singleElement().satisfies(issue -> {
      assertThat(issue.key()).isEqualTo("REPAIRED");
      assertThat(issue.isNew()).isFalse();
      assertThat(issue.isBeingClosed()).isFalse();
    });
  }

  /** With an Open duplicate on the current line, tracking keeps the duplicate and closes the repaired row. */
  @ParameterizedTest
  @ValueSource(ints = {0, 3})
  void when_an_open_duplicate_exists_the_duplicate_is_kept_and_the_repaired_row_is_closed(int linesAddedAbove) {
    RuleKey ruleKey = RuleTesting.XOO_X1;
    // The file as of the analysis that created the duplicate: 3 lines were added above the finding, so it moved from
    // line 24 to line 27, and line 24 now holds other code.
    insertBaseFile(fileHashes(3));
    insertMigratedBaseIssue(ruleKey, "REPAIRED", Issue.STATUS_RESOLVED, Issue.RESOLUTION_WONT_FIX, FINDING_LINE);
    insertBaseIssue(ruleKey, "DUPLICATE", Issue.STATUS_OPEN, null, FINDING_LINE + 3, List.of());
    analyse(ruleKey, 3 + linesAddedAbove);

    List<DefaultIssue> issues = analysedIssues();

    assertThat(issues).hasSize(2);
    assertThat(issues).filteredOn(issue -> "DUPLICATE".equals(issue.key())).singleElement().satisfies(issue -> {
      assertThat(issue.isNew()).isFalse();
      assertThat(issue.isBeingClosed()).isFalse();
    });
    assertThat(issues).filteredOn(issue -> "REPAIRED".equals(issue.key())).singleElement()
      .satisfies(issue -> assertThat(issue.isBeingClosed()).isTrue());
    assertThat(issues).noneMatch(DefaultIssue::isNew);
  }

  /** Uses the real issue workflow: a closed former hotspot whose code comes back is restored with its review. */
  @ParameterizedTest
  @CsvSource(value = {
    // status before closing, resolution before closing, expected status, expected resolution, expected IssueStatus
    "TO_REVIEW,NULL,OPEN,NULL,OPEN",
    "REVIEWED,ACKNOWLEDGED,CONFIRMED,NULL,CONFIRMED",
    "REVIEWED,SAFE,RESOLVED,WONTFIX,ACCEPTED",
    "REVIEWED,FIXED,RESOLVED,WONTFIX,ACCEPTED"
  }, nullValues = "NULL")
  void closed_former_hotspot_whose_code_comes_back_is_restored_by_the_real_workflow(String statusBeforeClosing, String resolutionBeforeClosing,
    String expectedStatus, String expectedResolution, IssueStatus expectedIssueStatus) {
    RuleKey ruleKey = RuleTesting.XOO_X1;
    insertBaseFile(fileHashes(0));
    insertClosedFormerHotspot(ruleKey, "CLOSED_HS", statusBeforeClosing, resolutionBeforeClosing, "REMOVED");

    List<DefaultIssue> issues = analyseWithRealWorkflow(ruleKey, 0);

    assertThat(issues).singleElement().satisfies(issue -> {
      assertThat(issue.key()).isEqualTo("CLOSED_HS");
      assertThat(issue.isNew()).isFalse();
      assertThat(issue.status()).isEqualTo(expectedStatus);
      assertThat(issue.resolution()).isEqualTo(expectedResolution);
      assertThat(issue.issueStatus()).isEqualTo(expectedIssueStatus);
      assertThat(issue.closeDate()).isNull();
    });
  }

  @Test
  void closed_former_hotspot_whose_code_comes_back_on_another_line_is_reported_as_a_new_issue() {
    // Closed issues are only recovered when rule, line, line hash and message all match, which holds for every closed
    // issue, not only former hotspots: when lines were added above, the finding is reported as a new open issue, so it
    // is never hidden. The closed row simply stays closed.
    RuleKey ruleKey = RuleTesting.XOO_X1;
    insertBaseFile(fileHashes(0));
    insertClosedFormerHotspot(ruleKey, "CLOSED_HS", "REVIEWED", "SAFE", "FIXED");

    List<DefaultIssue> issues = analyseWithRealWorkflow(ruleKey, 3);

    assertThat(issues).filteredOn(DefaultIssue::isNew).singleElement().satisfies(issue -> assertThat(issue.status()).isEqualTo(Issue.STATUS_OPEN));
    assertThat(issues).filteredOn(issue -> "CLOSED_HS".equals(issue.key())).singleElement()
      .satisfies(issue -> assertThat(issue.status()).isEqualTo(Issue.STATUS_CLOSED));
  }

  @Test
  void closed_former_hotspot_is_restored_even_when_the_migration_added_its_own_change_after_closing() {
    // The migration writes one change (type, tags, ...) dated after the closing; it has no status diff, so the closing
    // must remain the change the workflow reads.
    RuleKey ruleKey = RuleTesting.XOO_X1;
    insertBaseFile(fileHashes(0));
    IssueDto closed = insertClosedFormerHotspot(ruleKey, "CLOSED_HS", "REVIEWED", "ACKNOWLEDGED", "REMOVED");
    dbTester.issues().insertFieldDiffs(closed, new FieldDiffs().setIssueKey(closed.getKee()).setCreationDate(new Date(System.currentTimeMillis() - 1_000))
      .setDiff("type", "SECURITY_HOTSPOT", "VULNERABILITY").setDiff("tags", null, "former-hotspot"));

    List<DefaultIssue> issues = analyseWithRealWorkflow(ruleKey, 0);

    assertThat(issues).singleElement().satisfies(issue -> assertThat(issue.issueStatus()).isEqualTo(IssueStatus.CONFIRMED));
  }

  @Test
  void closed_former_hotspot_with_an_unknown_review_stays_closed_and_no_new_issue_is_created() {
    RuleKey ruleKey = RuleTesting.XOO_X1;
    insertBaseFile(fileHashes(0));
    insertClosedFormerHotspot(ruleKey, "CLOSED_HS", "REVIEWED", "SOMETHING_ELSE", "REMOVED");

    List<DefaultIssue> issues = analyseWithRealWorkflow(ruleKey, 0);

    assertThat(issues).singleElement().satisfies(issue -> {
      assertThat(issue.key()).isEqualTo("CLOSED_HS");
      assertThat(issue.status()).isEqualTo("CLOSED");
    });
  }

  @Test
  void accepted_former_hotspot_is_not_reopened_when_still_detected() {
    RuleKey ruleKey = RuleTesting.XOO_X1;
    insertBaseFile(fileHashes(0));
    insertMigratedBaseIssue(ruleKey, "ACCEPTED_HS", Issue.STATUS_RESOLVED, Issue.RESOLUTION_WONT_FIX, FINDING_LINE);

    List<DefaultIssue> detected = analyseWithRealWorkflow(ruleKey, 0);

    assertThat(detected).singleElement().satisfies(issue -> {
      assertThat(issue.key()).isEqualTo("ACCEPTED_HS");
      assertThat(issue.issueStatus()).isEqualTo(IssueStatus.ACCEPTED);
    });
  }

  @Test
  void accepted_former_hotspot_is_closed_when_it_is_no_longer_detected() {
    RuleKey ruleKey = RuleTesting.XOO_X1;
    insertBaseFile(fileHashes(0));
    insertMigratedBaseIssue(ruleKey, "ACCEPTED_HS", Issue.STATUS_RESOLVED, Issue.RESOLUTION_WONT_FIX, FINDING_LINE);

    List<DefaultIssue> issues = analyseWithRealWorkflow(ruleKey, 0, false);

    assertThat(issues).singleElement().satisfies(issue -> {
      assertThat(issue.key()).isEqualTo("ACCEPTED_HS");
      assertThat(issue.isBeingClosed()).isTrue();
      assertThat(issue.status()).isEqualTo("CLOSED");
    });
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

  @Test
  void visitAny_whenHunterAgentIssueExists_shouldDispatchToMeasureVisitorOnly() {
    addHunterAgentIssue();

    underTest.visitAny(FILE);

    verify(measureVisitor).onIssue(eq(FILE), defaultIssueCaptor.capture());
    assertThat(defaultIssueCaptor.getValue().key()).isEqualTo("HA_ISSUE");
    verify(issueVisitor, never()).onIssue(eq(FILE), defaultIssueCaptor.capture());
  }

  @Test
  void visitAny_whenHunterAgentIssueExists_shouldNotAppendToCache() {
    addHunterAgentIssue();

    underTest.visitAny(FILE);

    assertThat(newArrayList(protoIssueCache.traverse())).isEmpty();
  }

  private RuleDto addHunterAgentIssue() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(PROJECT_UUID).setKey(PROJECT_KEY);
    ComponentDto file = ComponentTesting.newFileDto(project, null, FILE_UUID).setKey(FILE_KEY);
    dbTester.components().insertComponents(project, file);

    RuleDto ruleDto = RuleTesting.newRule(RuleKey.of("hunter-agent", "sql-injection")).setIsExternal(true).setIsAdHoc(true);
    dbTester.rules().insert(ruleDto);
    ruleRepositoryRule.add(ruleDto);

    IssueDto issue = IssueTesting.newIssue(ruleDto, project, file)
      .setKee("HA_ISSUE")
      .setIssueProducer(IssueProducer.HUNTER_AGENT);
    dbTester.getDbClient().issueDao().insert(dbTester.getSession(), issue);
    dbTester.getSession().commit();
    return ruleDto;
  }

  private static final int FINDING_LINE = 24;
  private static final String FINDING_HASH = "finding-line";

  /**
   * Line hashes of the file after {@code linesAddedAbove} new lines were added above its original line 1. The
   * finding is on original line {@link #FINDING_LINE}; every other line has a hash of its own, so a hash identifies
   * the code of that line.
   */
  private static List<String> fileHashes(int linesAddedAbove) {
    List<String> hashes = new ArrayList<>();
    for (int i = 1; i <= linesAddedAbove; i++) {
      hashes.add("added-" + i);
    }
    for (int line = 1; line <= 30; line++) {
      hashes.add(line == FINDING_LINE ? FINDING_HASH : "line-" + line);
    }
    return hashes;
  }

  private void insertBaseFile(List<String> lineHashes) {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(PROJECT_UUID).setKey(PROJECT_KEY);
    ComponentDto file = ComponentTesting.newFileDto(project, null, FILE_UUID).setKey(FILE_KEY);
    dbTester.components().insertComponents(project, file);
    dbTester.fileSources().insertFileSource(file, dto -> dto.setLineHashes(lineHashes));
  }

  private void insertMigratedBaseIssue(RuleKey ruleKey, String kee, String status, String resolution, int line) {
    insertBaseIssue(ruleKey, kee, status, resolution, line, List.of("former-hotspot"));
  }

  private void insertBaseIssue(RuleKey ruleKey, String kee, String status, @Nullable String resolution, int line, List<String> tags) {
    RuleDto ruleDto = dbTester.getDbClient().ruleDao().selectByKey(dbTester.getSession(), ruleKey).orElse(null);
    if (ruleDto == null) {
      ruleDto = RuleTesting.newRule(ruleKey).setType(RuleType.VULNERABILITY);
      dbTester.rules().insert(ruleDto);
      ruleRepositoryRule.add(ruleDto);
      activeRulesHolderRule.put(new ActiveRule(ruleKey, Severity.MAJOR, emptyMap(), 1_000L, null, "qp", Map.of()));
    }
    ComponentDto project = dbTester.getDbClient().componentDao().selectByUuid(dbTester.getSession(), PROJECT_UUID).orElseThrow();
    ComponentDto file = dbTester.getDbClient().componentDao().selectByUuid(dbTester.getSession(), FILE_UUID).orElseThrow();
    IssueDto issue = IssueTesting.newIssue(ruleDto, project, file)
      .setKee(kee)
      .setType(RuleType.VULNERABILITY)
      .setStatus(status)
      .setResolution(resolution)
      .setLine(line)
      .setMessage("the message")
      .setChecksum(null)
      .setTags(tags);
    dbTester.getDbClient().issueDao().insert(dbTester.getSession(), issue);
    dbTester.getSession().commit();
  }

  /** Runs the analysis of the file as it is {@code linesAddedAbove} lines after its original version, finding included. */
  private void analyse(RuleKey ruleKey, int linesAddedAbove) {
    when(sourceLinesHash.getLineHashesMatchingDBVersion(FILE)).thenReturn(fileHashes(linesAddedAbove));
    int line = FINDING_LINE + linesAddedAbove;
    reportReader.putIssues(FILE_REF, singletonList(ScannerReport.Issue.newBuilder(getReportIssue(ruleKey))
      .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(line).setEndLine(line).setStartOffset(0).setEndOffset(5).build())
      .build()));

    underTest.visitAny(FILE);
  }

  private List<DefaultIssue> analysedIssues() {
    verify(issueVisitor, atLeastOnce()).onIssue(eq(FILE), defaultIssueCaptor.capture());
    return defaultIssueCaptor.getAllValues();
  }

  /** A hotspot the analyzer closed before it was migrated: its changelog holds the status change recorded when it closed. */
  private IssueDto insertClosedFormerHotspot(RuleKey ruleKey, String kee, String statusBeforeClosing, @Nullable String resolutionBeforeClosing,
    String closeResolution) {
    RuleDto ruleDto = RuleTesting.newRule(ruleKey).setType(RuleType.VULNERABILITY);
    dbTester.rules().insert(ruleDto);
    ruleRepositoryRule.add(ruleDto);
    activeRulesHolderRule.put(new ActiveRule(ruleKey, Severity.MAJOR, emptyMap(), 1_000L, null, "qp", Map.of()));
    ComponentDto project = dbTester.getDbClient().componentDao().selectByUuid(dbTester.getSession(), PROJECT_UUID).orElseThrow();
    ComponentDto file = dbTester.getDbClient().componentDao().selectByUuid(dbTester.getSession(), FILE_UUID).orElseThrow();
    long closedAt = System.currentTimeMillis() - 86_400_000L;
    IssueDto issue = IssueTesting.newIssue(ruleDto, project, file)
      .setKee(kee)
      .setType(RuleType.VULNERABILITY)
      .setStatus("CLOSED")
      .setResolution(closeResolution)
      .setLine(null)
      .setMessage("the message")
      .setChecksum(FINDING_HASH)
      .setIssueCloseDate(new Date(closedAt))
      .setTags(List.of("former-hotspot"));
    dbTester.getDbClient().issueDao().insert(dbTester.getSession(), issue);
    dbTester.getSession().commit();
    // What the workflow recorded when it closed the hotspot: status, resolution and the line it was on. A resolution
    // that does not change (reviewed as Fixed, closed as FIXED) has no diff.
    FieldDiffs closing = new FieldDiffs().setIssueKey(kee).setCreationDate(new Date(closedAt))
      .setDiff("status", statusBeforeClosing, "CLOSED")
      .setDiff("line", String.valueOf(FINDING_LINE), "");
    if (!closeResolution.equals(resolutionBeforeClosing)) {
      closing.setDiff("resolution", resolutionBeforeClosing == null ? "" : resolutionBeforeClosing, closeResolution);
    }
    dbTester.issues().insertFieldDiffs(issue, closing);
    return issue;
  }

  /** Analyses the file with the real issue workflow and returns the issues it produced, as the visitors saw them. */
  private List<DefaultIssue> analyseWithRealWorkflow(RuleKey ruleKey, int linesAddedAbove) {
    return analyseWithRealWorkflow(ruleKey, linesAddedAbove, true);
  }

  /** {@code reportFinding} false is an analysis in which the analyzer no longer reports the finding. */
  private List<DefaultIssue> analyseWithRealWorkflow(RuleKey ruleKey, int linesAddedAbove, boolean reportFinding) {
    IssueWorkflow realWorkflow = new IssueWorkflow(new CodeQualityIssueWorkflow(new CodeQualityIssueWorkflowActionsFactory(new IssueFieldsSetter()),
      new CodeQualityIssueWorkflowDefinition(), mock(TaintChecker.class)), null);
    IssueLifecycle realLifecycle = new IssueLifecycle(analysisMetadataHolder, issueChangeContext, realWorkflow, new IssueFieldsSetter(),
      mock(DebtCalculator.class), ruleRepositoryRule);
    DbClient dbClient = dbTester.getDbClient();
    TrackerBaseInputFactory baseInputFactory = new TrackerBaseInputFactory(issuesLoader, dbClient, movedFilesRepository);
    ClosedIssuesInputFactory closedIssuesInputFactory = new ClosedIssuesInputFactory(issuesLoader, dbClient, movedFilesRepository);
    TrackerExecution tracker = new TrackerExecution(baseInputFactory, closedIssuesInputFactory, new Tracker<>(), issuesLoader, analysisMetadataHolder);
    IssueTrackingDelegator trackingDelegator = new IssueTrackingDelegator(
      new PullRequestTrackerExecution(baseInputFactory, new Tracker<>(), newLinesRepository),
      new ReferenceBranchTrackerExecution(new TrackerReferenceBranchInputFactory(issuesLoader, mergeBranchComponentsUuids, dbClient), new Tracker<>()),
      tracker, analysisMetadataHolder);
    TypeAwareVisitor visitor = new IntegrateIssuesVisitor(protoIssueCache, rawInputFactory, baseInputFactory, realLifecycle,
      new IssueVisitors(new IssueVisitor[] {issueVisitor, measureVisitor}), trackingDelegator, issueStatusCopier, referenceBranchComponentUuids,
      mock(PullRequestSourceBranchMerger.class), fileStatuses, analysisMetadataHolder,
      new TrackerTargetBranchInputFactory(issuesLoader, targetBranchComponentUuids, dbClient, movedFilesRepository), locationHashesService,
      issuesLoader, treeRootHolder);

    when(sourceLinesHash.getLineHashesMatchingDBVersion(FILE)).thenReturn(fileHashes(linesAddedAbove));
    int line = FINDING_LINE + linesAddedAbove;
    reportReader.putIssues(FILE_REF, reportFinding
      ? singletonList(ScannerReport.Issue.newBuilder(getReportIssue(ruleKey))
        .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(line).setEndLine(line).setStartOffset(0).setEndOffset(5).build())
        .build())
      : emptyList());

    visitor.visitAny(FILE);

    verify(issueVisitor, atLeastOnce()).onIssue(eq(FILE), defaultIssueCaptor.capture());
    return defaultIssueCaptor.getAllValues();
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

  private void addMigratedBaseIssue(RuleKey ruleKey, String status, @Nullable String resolution) {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(PROJECT_UUID).setKey(PROJECT_KEY);
    ComponentDto file = ComponentTesting.newFileDto(project, null, FILE_UUID).setKey(FILE_KEY);
    dbTester.components().insertComponents(project, file);

    RuleDto ruleDto = RuleTesting.newRule(ruleKey).setType(RuleType.VULNERABILITY);
    dbTester.rules().insert(ruleDto);
    ruleRepositoryRule.add(ruleDto);

    // Same shape as a finding converted in place by HotspotsToIssuesMigrator: type switched, mapped status, former-hotspot tag.
    IssueDto issue = IssueTesting.newIssue(ruleDto, project, file)
      .setKee("ISSUE")
      .setType(RuleType.VULNERABILITY)
      .setStatus(status)
      .setResolution(resolution)
      .setLine(null)
      .setMessage("the message")
      .setChecksum(null)
      .setTags(List.of("former-hotspot"));
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
