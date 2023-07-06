/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.issue.index;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeQueueDto.Status;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.server.es.EsIndexSyncInProgressException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.ce.CeActivityDto.Status.FAILED;
import static org.sonar.db.ce.CeActivityDto.Status.SUCCESS;

@RunWith(DataProviderRunner.class)
public class IssueIndexSyncProgressCheckerTest {

  private final System2 system2 = new System2();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final IssueIndexSyncProgressChecker underTest = new IssueIndexSyncProgressChecker(db.getDbClient());

  @Test
  public void return_100_if_there_is_no_tasks_left() {
    IssueSyncProgress issueSyncProgress = underTest.getIssueSyncProgress(db.getSession());
    assertThat(issueSyncProgress.getCompleted()).isZero();
    assertThat(issueSyncProgress.getTotal()).isZero();
    assertThat(issueSyncProgress.toPercentCompleted()).isEqualTo(100);
    assertThat(issueSyncProgress.isCompleted()).isTrue();
    assertThat(issueSyncProgress.hasFailures()).isFalse();
  }

  @Test
  public void return_100_if_all_branches_have_need_issue_sync_set_FALSE() {
    IntStream.range(0, 13).forEach(value -> insertProjectWithBranches(false, 2));
    IntStream.range(0, 14).forEach(value -> insertProjectWithBranches(false, 4));
    IntStream.range(0, 4).forEach(value -> insertProjectWithBranches(false, 10));

    IssueSyncProgress result = underTest.getIssueSyncProgress(db.getSession());
    assertThat(result.getCompleted()).isEqualTo(153);
    assertThat(result.getTotal()).isEqualTo(153);
    assertThat(result.toPercentCompleted()).isEqualTo(100);
    assertThat(result.isCompleted()).isTrue();
  }

  @Test
  public void return_has_failure_true_if_exists_task() {
    assertThat(underTest.getIssueSyncProgress(db.getSession()).hasFailures()).isFalse();

    ProjectData projectData1 = insertProjectWithBranches(false, 0);
    insertCeActivity("TASK_1", projectData1, SUCCESS);

    ProjectData projectData2 = insertProjectWithBranches(false, 0);
    insertCeActivity("TASK_2", projectData2, SUCCESS);

    assertThat(underTest.getIssueSyncProgress(db.getSession()).hasFailures()).isFalse();

    ProjectData projectData3 = insertProjectWithBranches(true, 0);
    insertCeActivity("TASK_3", projectData3, FAILED);

    assertThat(underTest.getIssueSyncProgress(db.getSession()).hasFailures()).isTrue();
  }

  @Test
  @UseDataProvider("various_task_numbers")
  public void return_correct_percent_value_for_branches_to_sync(int toSync, int synced, int expectedPercent) {
    IntStream.range(0, toSync).forEach(value -> insertProjectWithBranches(true, 0));
    IntStream.range(0, synced).forEach(value -> insertProjectWithBranches(false, 0));

    IssueSyncProgress result = underTest.getIssueSyncProgress(db.getSession());
    assertThat(result.getCompleted()).isEqualTo(synced);
    assertThat(result.getTotal()).isEqualTo(toSync + synced);
    assertThat(result.toPercentCompleted()).isEqualTo(expectedPercent);
  }

  @DataProvider
  public static Object[][] various_task_numbers() {
    return new Object[][] {
      // toSync, synced, expected result
      {0, 0, 100},
      {0, 9, 100},
      {10, 0, 0},
      {99, 1, 1},
      {2, 1, 33},
      {6, 4, 40},
      {7, 7, 50},
      {1, 2, 66},
      {4, 10, 71},
      {1, 99, 99},
    };
  }

  @Test
  public void return_0_if_all_branches_have_need_issue_sync_set_true() {
    // only project
    IntStream.range(0, 10).forEach(value -> insertProjectWithBranches(true, 0));

    // project + additional branch
    IntStream.range(0, 10).forEach(value -> insertProjectWithBranches(true, 1));

    IssueSyncProgress result = underTest.getIssueSyncProgress(db.getSession());
    assertThat(result.getCompleted()).isZero();
    assertThat(result.getTotal()).isEqualTo(30);
    assertThat(result.toPercentCompleted()).isZero();
  }

  @Test
  public void return_is_completed_true_if_no_pending_or_in_progress_tasks() {
    // only project
    IntStream.range(0, 10).forEach(value -> insertProjectWithBranches(false, 0));

    // project + additional branch
    IntStream.range(0, 10).forEach(value -> insertProjectWithBranches(false, 1));

    IssueSyncProgress result = underTest.getIssueSyncProgress(db.getSession());
    assertThat(result.isCompleted()).isTrue();
  }

  @Test
  public void return_is_completed_true_if_pending_task_exist_but_all_branches_have_been_synced() {
    insertCeQueue("TASK_1", Status.PENDING);
    // only project
    IntStream.range(0, 10).forEach(value -> insertProjectWithBranches(false, 0));

    // project + additional branch
    IntStream.range(0, 10).forEach(value -> insertProjectWithBranches(false, 1));

    IssueSyncProgress result = underTest.getIssueSyncProgress(db.getSession());
    assertThat(result.isCompleted()).isTrue();
  }

  @Test
  public void return_is_completed_true_if_in_progress_task_exist_but_all_branches_have_been_synced() {
    insertCeQueue("TASK_1", Status.IN_PROGRESS);
    // only project
    IntStream.range(0, 10).forEach(value -> insertProjectWithBranches(false, 0));

    // project + additional branch
    IntStream.range(0, 10).forEach(value -> insertProjectWithBranches(false, 1));

    IssueSyncProgress result = underTest.getIssueSyncProgress(db.getSession());
    assertThat(result.isCompleted()).isTrue();
  }

  @Test
  public void return_is_completed_false_if_pending_task_exist_and_branches_need_issue_sync() {
    insertCeQueue("TASK_1", Status.PENDING);
    // only project
    IntStream.range(0, 10).forEach(value -> insertProjectWithBranches(true, 0));

    // project + additional branch
    IntStream.range(0, 10).forEach(value -> insertProjectWithBranches(false, 1));

    IssueSyncProgress result = underTest.getIssueSyncProgress(db.getSession());
    assertThat(result.isCompleted()).isFalse();
  }

  @Test
  public void return_is_completed_false_if_in_progress_task_exist_and_branches_need_issue_sync() {
    insertCeQueue("TASK_1", Status.IN_PROGRESS);
    // only project
    IntStream.range(0, 10).forEach(value -> insertProjectWithBranches(true, 0));

    // project + additional branch
    IntStream.range(0, 10).forEach(value -> insertProjectWithBranches(false, 1));

    IssueSyncProgress result = underTest.getIssueSyncProgress(db.getSession());
    assertThat(result.isCompleted()).isFalse();
  }

  @Test
  public void checkIfAnyComponentsNeedIssueSync_throws_exception_if_all_components_have_need_issue_sync_TRUE() {
    ProjectData projectData1 = insertProjectWithBranches(true, 0);
    ProjectData projectData2 = insertProjectWithBranches(true, 0);
    DbSession session = db.getSession();
    List<String> projectKeys = Arrays.asList(projectData1.getProjectDto().getKey(), projectData2.getProjectDto().getKey());
    assertThatThrownBy(() -> underTest.checkIfAnyComponentsNeedIssueSync(session, projectKeys))
      .isInstanceOf(EsIndexSyncInProgressException.class)
      .hasFieldOrPropertyWithValue("httpCode", 503)
      .hasMessage("Results are temporarily unavailable. Indexing of issues is in progress.");
  }

  @Test
  public void checkIfAnyComponentsNeedIssueSync_does_not_throw_exception_if_all_components_have_need_issue_sync_FALSE() {
    underTest.checkIfAnyComponentsNeedIssueSync(db.getSession(), Collections.emptyList());
    ProjectData projectData1 = insertProjectWithBranches(false, 0);
    ProjectData projectData2 = insertProjectWithBranches(false, 0);
    underTest.checkIfAnyComponentsNeedIssueSync(db.getSession(), Arrays.asList(projectData1.getProjectDto().getKey(), projectData2.getProjectDto().getKey()));
  }

  @Test
  public void checkIfAnyComponentsNeedIssueSync_throws_exception_if_at_least_one_component_has_need_issue_sync_TRUE() {
    ProjectData projectData1 = insertProjectWithBranches(false, 0);
    ProjectData projectData2 = insertProjectWithBranches(true, 0);

    DbSession session = db.getSession();
    List<String> projectKeys = Arrays.asList(projectData1.getProjectDto().getKey(), projectData2.getProjectDto().getKey());
    assertThatThrownBy(() -> underTest.checkIfAnyComponentsNeedIssueSync(session, projectKeys))
      .isInstanceOf(EsIndexSyncInProgressException.class)
      .hasFieldOrPropertyWithValue("httpCode", 503)
      .hasMessage("Results are temporarily unavailable. Indexing of issues is in progress.");
  }

  @Test
  public void checkIfComponentNeedIssueSync_single_component() {
    ProjectData projectData1 = insertProjectWithBranches(true, 0);
    ProjectData projectData2 = insertProjectWithBranches(false, 0);

    DbSession session = db.getSession();
    // do nothing when need issue sync false
    underTest.checkIfComponentNeedIssueSync(session, projectData2.getProjectDto().getKey());

    // throws if flag set to TRUE
    String key = projectData1.getProjectDto().getKey();
    assertThatThrownBy(() -> underTest.checkIfComponentNeedIssueSync(session, key))
      .isInstanceOf(EsIndexSyncInProgressException.class)
      .hasFieldOrPropertyWithValue("httpCode", 503)
      .hasMessage("Results are temporarily unavailable. Indexing of issues is in progress.");
  }

  @Test
  public void checkIfAnyComponentsNeedIssueSync_single_view_subview_or_app() {
    ProjectData projectData1 = insertProjectWithBranches(true, 0);

    ComponentDto app = db.components().insertPublicApplication().getMainBranchComponent();
    ComponentDto view = db.components().insertPrivatePortfolio();
    ComponentDto subview = db.components().insertSubView(view);

    DbSession session = db.getSession();
    List<String> appViewOrSubviewKeys = Arrays.asList(projectData1.getProjectDto().getKey(), app.getKey(), view.getKey(), subview.getKey());

    // throws if flag set to TRUE
    assertThatThrownBy(() -> underTest.checkIfAnyComponentsNeedIssueSync(session,
      appViewOrSubviewKeys))
        .isInstanceOf(EsIndexSyncInProgressException.class)
        .hasFieldOrPropertyWithValue("httpCode", 503)
        .hasMessage("Results are temporarily unavailable. Indexing of issues is in progress.");
  }

  @Test
  public void checkIfIssueSyncInProgress_throws_exception_if_at_least_one_component_has_need_issue_sync_TRUE() {
    insertProjectWithBranches(false, 0);
    underTest.checkIfIssueSyncInProgress(db.getSession());
    insertProjectWithBranches(true, 0);

    DbSession session = db.getSession();
    assertThatThrownBy(() -> underTest.checkIfIssueSyncInProgress(session))
      .isInstanceOf(EsIndexSyncInProgressException.class)
      .hasFieldOrPropertyWithValue("httpCode", 503)
      .hasMessage("Results are temporarily unavailable. Indexing of issues is in progress.");
  }

  @Test
  public void doProjectNeedIssueSync() {
    ProjectData projectData1 = insertProjectWithBranches(false, 0);
    assertThat(underTest.doProjectNeedIssueSync(db.getSession(), projectData1.getProjectDto().getUuid())).isFalse();
    ProjectData projectData2 = insertProjectWithBranches(true, 0);
    assertThat(underTest.doProjectNeedIssueSync(db.getSession(), projectData2.getProjectDto().getUuid())).isTrue();
  }

  @Test
  public void findProjectUuidsWithIssuesSyncNeed() {
    ProjectData projectData1 = insertProjectWithBranches(false, 0);
    ProjectData projectData2 = insertProjectWithBranches(false, 0);
    ProjectData projectData3 = insertProjectWithBranches(true, 0);
    ProjectData projectData4 = insertProjectWithBranches(true, 0);

    assertThat(underTest.findProjectUuidsWithIssuesSyncNeed(db.getSession(),
      Arrays.asList(projectData1.getProjectDto().getUuid(), projectData2.getProjectDto().getUuid(), projectData3.getProjectDto().getUuid(), projectData4.getProjectDto().getUuid())))
        .containsOnly(projectData3.getProjectDto().getUuid(), projectData4.getProjectDto().getUuid());
  }

  private ProjectData insertProjectWithBranches(boolean needIssueSync, int numberOfBranches) {
    ProjectData projectData = db.components()
      .insertPrivateProject(branchDto -> branchDto.setNeedIssueSync(needIssueSync), c -> {
      }, p -> {
      });
    IntStream.range(0, numberOfBranches).forEach(
      i -> db.components().insertProjectBranch(projectData.getProjectDto(), branchDto -> branchDto.setNeedIssueSync(needIssueSync)));
    return projectData;
  }

  private CeQueueDto insertCeQueue(String uuid, CeQueueDto.Status status) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setUuid(uuid);
    queueDto.setStatus(status);
    queueDto.setTaskType(CeTaskTypes.BRANCH_ISSUE_SYNC);
    db.getDbClient().ceQueueDao().insert(db.getSession(), queueDto);
    return queueDto;
  }

  private CeActivityDto insertCeActivity(String uuid, ProjectData projectData, CeActivityDto.Status status) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setUuid(uuid);
    queueDto.setTaskType(CeTaskTypes.BRANCH_ISSUE_SYNC);

    CeActivityDto dto = new CeActivityDto(queueDto);
    dto.setComponentUuid(projectData.getMainBranchComponent().uuid());
    dto.setEntityUuid(projectData.projectUuid());
    dto.setStatus(status);
    dto.setTaskType(CeTaskTypes.BRANCH_ISSUE_SYNC);
    dto.setAnalysisUuid(uuid + "_AA");
    dto.setCreatedAt(system2.now());
    db.getDbClient().ceActivityDao().insert(db.getSession(), dto);
    return dto;
  }
}
