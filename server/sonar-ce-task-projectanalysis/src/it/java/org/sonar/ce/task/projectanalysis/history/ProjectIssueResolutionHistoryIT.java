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
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.issue.fixedissues.FixedIssueForHistoryRepository;
import org.sonar.ce.task.projectanalysis.issue.fixedissues.FixedIssueVisitor;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.rule.RuleType;
import org.sonar.db.DBSessionsImpl;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonarsource.history.server.db.HistoryDbClient;
import org.sonarsource.history.server.db.HistoryMyBatisConfExtension;
import org.sonarsource.history.server.db.mapper.IssueTtrHistoryMapperFragments;
import org.sonarsource.history.server.db.repository.IssueCountDimensionsRepository;
import org.sonarsource.history.server.db.repository.IssueCountHistoryRepository;
import org.sonarsource.history.server.db.repository.IssueTtrHistoryRepository;
import org.sonarsource.history.server.db.repository.MeasureHistoryRepository;
import org.sonarsource.history.server.db.repository.MeasureKeyMappingRepository;
import org.sonarsource.history.server.db.repository.PrResolutionHistoryRepository;
import org.sonarsource.history.server.service.HistoryPurgeService;
import org.sonarsource.history.server.service.IssueResolutionHistoryRecordingService;
import org.sonarsource.history.server.service.IssueTtrHistoryRecordingService;
import org.sonarsource.history.server.service.PrResolutionHistoryPurgeExtension;
import org.sonarsource.history.server.service.PrResolutionHistoryRecordingService;
import org.sonarsource.history.server.service.RetryableOrderedBatchWriter;
import org.sonarsource.organizations.server.DefaultOrganizationProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectIssueResolutionHistoryIT {
  private static final Instant CLOSED_AT = Instant.parse("2026-11-05T14:30:00Z");
  private static final long DAY = Instant.parse("2026-11-05T00:00:00Z").toEpochMilli();

  @RegisterExtension
  private final DbTester db = DbTester.createWithConfExtension(System2.INSTANCE,
    new HistoryMyBatisConfExtension(IssueTtrHistoryMapperFragments.class));
  private final PrResolutionHistoryRepository prRepository = new PrResolutionHistoryRepository();
  private final HistoryDbClient historyDb = new HistoryDbClient(db.getDbClient().getMyBatis(), new DBSessionsImpl(db.getDbClient().getMyBatis()),
    List.of(new IssueCountDimensionsRepository(), new IssueCountHistoryRepository(), new IssueTtrHistoryRepository(),
      new MeasureHistoryRepository(), new MeasureKeyMappingRepository(), prRepository));

  @Test
  void recordTtrHistory_whenBranchIsAnalyzed_recordsOnlyBranchTtr() {
    var issues = new FixedIssueForHistoryRepository();
    var metadata = mock(AnalysisMetadataHolder.class);
    when(metadata.isPullRequest()).thenReturn(false);
    var visitor = new FixedIssueVisitor(issues, metadata);
    var component = mock(Component.class);
    visitor.onIssue(component, closedIssue("branch-issue", "long-branch"));

    newRecorder(issues).recordTtrHistory("long-branch");

    assertThat(db.countSql("select count(*) from issue_ttr_history")).isOne();
    assertThat(db.countSql("select count(*) from issue_ttr_history where entity_id = 'long-branch' and issues_resolved = 1")).isOne();
    assertThat(db.countSql("select count(*) from pr_resolution_history")).isZero();
  }

  @Test
  void recordTtrHistory_whenPullRequestIsAnalyzed_recordsOnlyBranchAndOrganizationResolutionCounts() {
    var issues = new FixedIssueForHistoryRepository();
    var metadata = mock(AnalysisMetadataHolder.class);
    when(metadata.isPullRequest()).thenReturn(true);
    var visitor = new FixedIssueVisitor(issues, metadata);
    var component = mock(Component.class);
    visitor.onIssue(component, closedIssue("pr-issue-1", "pr-branch"));
    visitor.onIssue(component, closedIssue("pr-issue-2", "pr-branch"));

    newRecorder(issues).recordTtrHistory("pr-branch");

    assertThat(db.countSql("select count(*) from issue_ttr_history")).isZero();
    assertThat(db.countSql("select count(*) from pr_resolution_history")).isEqualTo(2);
    assertThat(db.countSql("select count(*) from pr_resolution_history where entity_id = 'pr-branch' "
      + "and entity_type = 'PROJECT_BRANCH' and issues_resolved = 2 and recorded_at_epoch = " + DAY)).isOne();
    assertThat(db.countSql("select count(*) from pr_resolution_history where entity_id = '" + DefaultOrganizationProvider.ID
      + "' and entity_type = 'ORGANIZATION' and issues_resolved = 2 and recorded_at_epoch = " + DAY)).isOne();
  }

  @Test
  void recordTtrHistory_whenPullRequestAndProjectAreDeleted_preservesCountsUntilRetentionExpires() {
    var project = db.components().insertPrivateProject();
    var pr = db.components().insertProjectBranch(project.getMainBranchComponent(), branch -> branch.setBranchType(BranchType.PULL_REQUEST));
    var issues = new FixedIssueForHistoryRepository();
    var visitor = newPullRequestVisitor(issues);
    var component = mock(Component.class);
    visitor.onIssue(component, closedIssue("pr-issue-1", pr.uuid()).setProjectUuid(project.projectUuid()));
    visitor.onIssue(component, closedIssue("pr-issue-2", pr.uuid()).setProjectUuid(project.projectUuid()));
    db.commit();

    newRecorder(issues).recordTtrHistory(pr.uuid());

    assertPrTotals(pr.uuid(), 2);
    assertThat(db.countRowsOfTable("issue_ttr_history")).isZero();

    db.getDbClient().purgeDao().deleteBranch(db.getSession(), pr.uuid());
    db.commit();
    assertThat(db.getDbClient().branchDao().selectByUuid(db.getSession(), pr.uuid())).isEmpty();
    assertPrTotals(pr.uuid(), 2);

    var projectDto = project.getProjectDto();
    db.getDbClient().purgeDao().deleteProject(db.getSession(), projectDto.getUuid(), projectDto.getQualifier(), projectDto.getName(), projectDto.getKey());
    db.commit();
    assertThat(db.getDbClient().projectDao().selectByUuid(db.getSession(), projectDto.getUuid())).isEmpty();
    assertPrTotals(pr.uuid(), 2);

    var purge = new HistoryPurgeService(historyDb, historyDb.issueCountHistoryRepository(), historyDb.measureHistoryRepository(),
      historyDb.issueTtrHistoryRepository(), List.of(new PrResolutionHistoryPurgeExtension(prRepository)));
    purge.purgeHistoryBefore(LocalDate.parse("2026-11-04"));
    assertPrTotals(pr.uuid(), 2);
    purge.purgeHistoryBefore(LocalDate.parse("2026-11-05"));
    assertThat(db.countRowsOfTable("pr_resolution_history")).isZero();
  }

  @Test
  void recordTtrHistory_whenClosureSnapshotIsDuplicated_countsItOnce() {
    var issues = new FixedIssueForHistoryRepository();
    var visitor = newPullRequestVisitor(issues);
    var component = mock(Component.class);
    visitor.onIssue(component, closedIssue("pr-issue-1", "pr-branch"));
    visitor.onIssue(component, closedIssue("pr-issue-2", "pr-branch"));
    visitor.onIssue(component, closedIssue("pr-issue-2", "pr-branch"));

    newRecorder(issues).recordTtrHistory("pr-branch");

    assertPrTotals("pr-branch", 2);
    assertThat(db.countRowsOfTable("issue_ttr_history")).isZero();
  }

  @Test
  void recordTtrHistory_whenBranchUuidIsMissing_recordsNoContribution() {
    var issues = new FixedIssueForHistoryRepository();
    var visitor = newPullRequestVisitor(issues);
    visitor.onIssue(mock(Component.class), closedIssue("pr-issue", null));

    newRecorder(issues).recordTtrHistory("pr-branch");

    assertThat(db.countRowsOfTable("pr_resolution_history")).isZero();
    assertThat(db.countRowsOfTable("issue_ttr_history")).isZero();
  }

  @Test
  void recordTtrHistory_whenReplayedAfterCommit_addsCountsAgain() {
    var issues = new FixedIssueForHistoryRepository();
    var visitor = newPullRequestVisitor(issues);
    visitor.onIssue(mock(Component.class), closedIssue("pr-issue", "pr-branch"));
    var recorder = newRecorder(issues);

    recorder.recordTtrHistory("pr-branch");
    assertPrTotals("pr-branch", 1);
    recorder.recordTtrHistory("pr-branch");

    assertPrTotals("pr-branch", 2);
    assertThat(db.countRowsOfTable("issue_ttr_history")).isZero();
  }

  private static FixedIssueVisitor newPullRequestVisitor(FixedIssueForHistoryRepository issues) {
    var metadata = mock(AnalysisMetadataHolder.class);
    when(metadata.isPullRequest()).thenReturn(true);
    return new FixedIssueVisitor(issues, metadata);
  }

  private void assertPrTotals(String branchUuid, int expected) {
    assertThat(db.select("select entity_type, entity_id, issues_resolved, recorded_at_epoch from pr_resolution_history"))
      .hasSize(2)
      .allSatisfy(row -> {
        assertThat(((Number) row.get("ISSUES_RESOLVED")).intValue()).isEqualTo(expected);
        assertThat(((Number) row.get("RECORDED_AT_EPOCH")).longValue()).isEqualTo(DAY);
      })
      .anySatisfy(row -> assertThat(row).containsEntry("ENTITY_TYPE", "PROJECT_BRANCH").containsEntry("ENTITY_ID", branchUuid))
      .anySatisfy(row -> assertThat(row).containsEntry("ENTITY_TYPE", "ORGANIZATION")
        .containsEntry("ENTITY_ID", DefaultOrganizationProvider.ID.toString()));
  }

  private ProjectIssueTtrHistoryRecorder newRecorder(FixedIssueForHistoryRepository issues) {
    var writer = new RetryableOrderedBatchWriter();
    var service = new IssueResolutionHistoryRecordingService(historyDb,
      new IssueTtrHistoryRecordingService(historyDb, writer), new PrResolutionHistoryRecordingService(historyDb, writer, prRepository));
    return new ProjectIssueTtrHistoryRecorder(issues, service);
  }

  private static DefaultIssue closedIssue(String key, @Nullable String branchUuid) {
    return new DefaultIssue().setKey(key).setProjectUuid("project").setBranchUuid(branchUuid)
      .setCreationDate(Date.from(CLOSED_AT.minusSeconds(3600)))
      .setCloseDate(Date.from(CLOSED_AT)).setType(RuleType.BUG).setSeverity("MAJOR").setBeingClosed(true);
  }
}
