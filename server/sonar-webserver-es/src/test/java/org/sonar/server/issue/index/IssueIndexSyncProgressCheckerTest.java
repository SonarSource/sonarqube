/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class IssueIndexSyncProgressCheckerTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private IssueIndexSyncProgressChecker underTest = new IssueIndexSyncProgressChecker(db.getDbClient());

  @Test
  public void return_100_if_there_is_no_tasks_left() {
    IssueSyncProgress issueSyncProgress = underTest.getIssueSyncProgress(db.getSession());
    assertThat(issueSyncProgress.getCompleted()).isZero();
    assertThat(issueSyncProgress.getTotal()).isZero();
    assertThat(issueSyncProgress.toPercentCompleted()).isEqualTo(100);
    assertThat(issueSyncProgress.isCompleted()).isTrue();
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
  @UseDataProvider("various_task_numbers")
  public void return_correct_percent_value_for_branches_to_sync(int toSync, int synced, int expectedPercent, boolean isCompleted) {
    IntStream.range(0, toSync).forEach(value -> insertProjectWithBranches(true, 0));
    IntStream.range(0, synced).forEach(value -> insertProjectWithBranches(false, 0));

    IssueSyncProgress result = underTest.getIssueSyncProgress(db.getSession());
    assertThat(result.getCompleted()).isEqualTo(synced);
    assertThat(result.getTotal()).isEqualTo(toSync + synced);
    assertThat(result.toPercentCompleted()).isEqualTo(expectedPercent);
    assertThat(result.isCompleted()).isEqualTo(isCompleted);
  }

  @DataProvider
  public static Object[][] various_task_numbers() {
    return new Object[][] {
      // toSync, synced, expected result, expectedCompleted
      {0, 0, 100, true},
      {0, 9, 100, true},
      {10, 0, 0, false},
      {99, 1, 1, false},
      {2, 1, 33, false},
      {6, 4, 40, false},
      {7, 7, 50, false},
      {1, 2, 66, false},
      {4, 10, 71, false},
      {1, 99, 99, false},
    };
  }

  @Test
  public void return_0_if_all_branches_have_need_issue_sync_set_TRUE() {
    // only project
    IntStream.range(0, 10).forEach(value -> insertProjectWithBranches(true, 0));

    // project + additional branch
    IntStream.range(0, 10).forEach(value -> insertProjectWithBranches(true, 1));

    IssueSyncProgress result = underTest.getIssueSyncProgress(db.getSession());
    assertThat(result.getCompleted()).isZero();
    assertThat(result.getTotal()).isEqualTo(30);
    assertThat(result.toPercentCompleted()).isZero();
    assertThat(result.isCompleted()).isFalse();
  }

  private void insertProjectWithBranches(boolean needIssueSync, int numberOfBranches) {
    ProjectDto projectDto = db.components()
      .insertPrivateProjectDto(db.getDefaultOrganization(), branchDto -> branchDto.setNeedIssueSync(needIssueSync));
    IntStream.range(0, numberOfBranches).forEach(
      i -> db.components().insertProjectBranch(projectDto, branchDto -> branchDto.setNeedIssueSync(needIssueSync)));
  }
}
