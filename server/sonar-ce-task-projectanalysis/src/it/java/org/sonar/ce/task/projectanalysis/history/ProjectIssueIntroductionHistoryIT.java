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
package org.sonar.ce.task.projectanalysis.history;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.issue.fixedissues.FixedIssueForHistoryRepository;
import org.sonar.ce.task.projectanalysis.issue.introducedissues.IntroducedIssueVisitor;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.rule.RuleType;
import org.sonar.db.DBSessionsImpl;
import org.sonar.db.DbTester;
import org.sonarsource.history.model.EntityType;
import org.sonarsource.history.server.db.HistoryDbClient;
import org.sonarsource.history.server.db.HistoryMyBatisConfExtension;
import org.sonarsource.history.server.db.mapper.IssueTtrHistoryMapperFragments;
import org.sonarsource.history.server.db.repository.IssueCountDimensionsRepository;
import org.sonarsource.history.server.db.repository.IssueCountHistoryRepository;
import org.sonarsource.history.server.db.repository.IssueIntroductionHistoryRepository;
import org.sonarsource.history.server.db.repository.IssueTtrHistoryRepository;
import org.sonarsource.history.server.db.repository.MeasureHistoryRepository;
import org.sonarsource.history.server.db.repository.MeasureKeyMappingRepository;
import org.sonarsource.history.server.db.repository.PrResolutionHistoryRepository;
import org.sonarsource.history.server.service.HistoryPurgeService;
import org.sonarsource.history.server.service.IssueCountHistoryRecordingService;
import org.sonarsource.history.server.service.IssueIntroductionHistoryPurgeExtension;
import org.sonarsource.history.server.service.IssueIntroductionHistoryRecordingService;
import org.sonarsource.history.server.service.IssueResolutionHistoryRecordingService;
import org.sonarsource.history.server.service.IssueTtrHistoryRecordingService;
import org.sonarsource.history.server.service.MeasuresHistoryRecordingService;
import org.sonarsource.history.server.service.PrResolutionHistoryRecordingService;
import org.sonarsource.history.server.service.RetryableOrderedBatchWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectIssueIntroductionHistoryIT {
  private static final long ANALYSIS_DATE = Instant.parse("2026-11-12T23:59:59.999Z").toEpochMilli();
  private static final long DAY = Instant.parse("2026-11-12T00:00:00Z").toEpochMilli();
  private static final String[] SEVERITIES = {"INFO", "MINOR", "MAJOR", "CRITICAL", "BLOCKER"};
  private static final RuleType[] TYPES = {RuleType.BUG, RuleType.CODE_SMELL, RuleType.VULNERABILITY};
  // Each worker holds a branch-lock session and a write session; stay within the default test pool of 10.
  private static final int WORKER_COUNT = 4;
  private static final int ANALYSES_PER_WORKER = 10;
  private static final int ISSUES_PER_ANALYSIS = 100;

  @RegisterExtension
  private final DbTester db = DbTester.createWithConfExtension(System2.INSTANCE,
    new HistoryMyBatisConfExtension(IssueTtrHistoryMapperFragments.class));
  private final IssueIntroductionHistoryRepository introductionRepository = new IssueIntroductionHistoryRepository();
  private final PrResolutionHistoryRepository prRepository = new PrResolutionHistoryRepository();
  private final HistoryDbClient historyDb = new HistoryDbClient(db.getDbClient().getMyBatis(), new DBSessionsImpl(db.getDbClient().getMyBatis()),
    List.of(new IssueCountDimensionsRepository(), new IssueCountHistoryRepository(), new IssueTtrHistoryRepository(),
      new MeasureHistoryRepository(), new MeasureKeyMappingRepository(), prRepository, introductionRepository));
  private final RetryableOrderedBatchWriter writer = new RetryableOrderedBatchWriter();

  @Test
  void recordHistory_whenMainBranchIsAnalyzed_recordsNewIssuesOnAnalysisDay() {
    String branch = db.components().insertPrivateProject().getMainBranchComponent().uuid();
    var visitor = newVisitor(branch, true);
    visitor.onIssue(mock(Component.class), newIssue("issue-1", RuleType.BUG, "MAJOR"));
    visitor.onIssue(mock(Component.class), newIssue("issue-2", RuleType.BUG, "MAJOR"));
    visitor.onIssue(mock(Component.class), newIssue("hotspot", RuleType.SECURITY_HOTSPOT, "MAJOR"));
    visitor.onIssue(mock(Component.class), newIssue("existing", RuleType.BUG, "MAJOR").setNew(false));

    newDelegate(visitor).recordHistory(branch, EntityType.PROJECT_BRANCH, Set.of(branch));

    assertThat(db.select("select entity_id, entity_type, recorded_at_epoch, issues_introduced from issue_introduction_history"))
      .singleElement()
      .satisfies(row -> {
        assertThat(row).containsEntry("ENTITY_ID", branch).containsEntry("ENTITY_TYPE", "PROJECT_BRANCH");
        assertThat(((Number) row.get("RECORDED_AT_EPOCH")).longValue()).isEqualTo(DAY);
        assertThat(((Number) row.get("ISSUES_INTRODUCED")).intValue()).isEqualTo(2);
      });
  }

  @Test
  void recordHistory_whenNonMainBranchIsAnalyzed_recordsNothing() {
    String branch = db.components().insertPrivateProject().getMainBranchComponent().uuid();
    var visitor = newVisitor(branch, false);
    visitor.onIssue(mock(Component.class), newIssue("issue-1", RuleType.BUG, "MAJOR"));

    newDelegate(visitor).recordHistory(branch, EntityType.PROJECT_BRANCH, Set.of(branch));

    assertThat(db.countRowsOfTable("issue_introduction_history")).isZero();
  }

  @Test
  void purgeHistoryBefore_deletesIntroductionsOnOrBeforeThreshold() {
    String branch = db.components().insertPrivateProject().getMainBranchComponent().uuid();
    var visitor = newVisitor(branch, true);
    visitor.onIssue(mock(Component.class), newIssue("issue-1", RuleType.BUG, "MAJOR"));
    newDelegate(visitor).recordHistory(branch, EntityType.PROJECT_BRANCH, Set.of(branch));
    var purge = new HistoryPurgeService(historyDb, historyDb.issueCountHistoryRepository(), historyDb.measureHistoryRepository(),
      historyDb.issueTtrHistoryRepository(), List.of(new IssueIntroductionHistoryPurgeExtension(introductionRepository)));

    purge.purgeHistoryBefore(LocalDate.parse("2026-11-11"));
    assertThat(db.countRowsOfTable("issue_introduction_history")).isOne();
    purge.purgeHistoryBefore(LocalDate.parse("2026-11-12"));
    assertThat(db.countRowsOfTable("issue_introduction_history")).isZero();
  }

  @ParameterizedTest(name = "{0} main branches analyzed by concurrent CE workers with overlapping dimensions")
  @ValueSource(ints = {1, WORKER_COUNT})
  void recordHistory_whenWorkersRunConcurrently_keepsExactCountsWithoutDeadlock(int branchCount) throws Exception {
    List<String> branches = IntStream.range(0, branchCount)
      .mapToObj(i -> db.components().insertPrivateProject().getMainBranchComponent().uuid()).toList();
    db.commit();

    runConcurrentAnalyses(branches);

    int expectedPerBranch = WORKER_COUNT / branchCount * ANALYSES_PER_WORKER * ISSUES_PER_ANALYSIS;
    for (int i = 0; i < branchCount; i++) {
      assertThat(((Number) db.selectFirst("select sum(issues_introduced) as total from issue_introduction_history where entity_id = '"
        + branches.get(i) + "' and recorded_at_epoch = " + DAY).get("TOTAL")).intValue()).isEqualTo(expectedPerBranch);
      assertThat(db.countSql("select count(*) from issue_introduction_history where entity_id = '" + branches.get(i) + "'"))
        .isEqualTo(branchCount == 1 || i % 2 == 0 ? 13 : 2);
    }
    assertThat(db.countSql("select count(distinct dimension_id) from issue_introduction_history")).isEqualTo(13);
  }

  private void runConcurrentAnalyses(List<String> branches) throws Exception {
    var start = new CyclicBarrier(WORKER_COUNT);
    List<Callable<Void>> workers = new ArrayList<>();
    for (int worker = 0; worker < WORKER_COUNT; worker++) {
      int workerId = worker;
      String branch = branches.get(worker % branches.size());
      // Odd workers share two of the 13 dimensions, so concurrent analyses of different branches race on the same dimension rows.
      int dimensionCount = worker % 2 == 0 ? 13 : 2;
      workers.add(() -> {
        start.await(10, TimeUnit.SECONDS);
        for (int analysis = 0; analysis < ANALYSES_PER_WORKER; analysis++) {
          var visitor = newVisitor(branch, true);
          for (int issue = 0; issue < ISSUES_PER_ANALYSIS; issue++) {
            int dimension = issue % dimensionCount;
            visitor.onIssue(mock(Component.class), newIssue("w" + workerId + "-a" + analysis + "-i" + issue,
              TYPES[dimension % TYPES.length], SEVERITIES[dimension / TYPES.length]));
          }
          newDelegate(visitor).recordHistory(branch, EntityType.PROJECT_BRANCH, Set.of(branch));
        }
        return null;
      });
    }
    try (var executor = Executors.newFixedThreadPool(WORKER_COUNT)) {
      // A deadlock or pool exhaustion surfaces as a cancelled future.
      for (var result : executor.invokeAll(workers, 60, TimeUnit.SECONDS)) {
        result.get();
      }
    }
  }

  private static IntroducedIssueVisitor newVisitor(String branchUuid, boolean mainBranch) {
    var metadata = mock(AnalysisMetadataHolder.class);
    var branch = mock(Branch.class);
    when(branch.isMain()).thenReturn(mainBranch);
    when(metadata.getBranch()).thenReturn(branch);
    when(metadata.getAnalysisDate()).thenReturn(ANALYSIS_DATE);
    var root = mock(Component.class);
    when(root.getUuid()).thenReturn(branchUuid);
    var treeRootHolder = mock(TreeRootHolder.class);
    when(treeRootHolder.getRoot()).thenReturn(root);
    return new IntroducedIssueVisitor(metadata, treeRootHolder);
  }

  private RecordHistoryDelegateImpl newDelegate(IntroducedIssueVisitor visitor) {
    var resolution = new IssueResolutionHistoryRecordingService(historyDb, new IssueTtrHistoryRecordingService(historyDb, writer),
      new PrResolutionHistoryRecordingService(historyDb, writer, prRepository));
    var recorder = new ProjectIssueTtrHistoryRecorder(new FixedIssueForHistoryRepository(), resolution, visitor,
      new IssueIntroductionHistoryRecordingService(historyDb, writer, introductionRepository));
    return new RecordHistoryDelegateImpl(db.getDbClient(), new IssueCountHistoryRecordingService(historyDb),
      new MeasuresHistoryRecordingService(historyDb, List.of()), recorder, null);
  }

  private static DefaultIssue newIssue(String key, RuleType type, String severity) {
    return new DefaultIssue().setKey(key).setNew(true).setType(type).setSeverity(severity);
  }
}
