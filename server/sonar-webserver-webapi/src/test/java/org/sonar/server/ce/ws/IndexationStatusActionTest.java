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
package org.sonar.server.ce.ws;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Ce.IndexationStatusWsResponse;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class IndexationStatusActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private WsActionTester ws = new WsActionTester(new IndexationStatusAction(db.getDbClient()));

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("indexation_status");
    assertThat(def.isInternal()).isTrue();
    assertThat(def.isPost()).isFalse();
    assertThat(def.params()).isEmpty();
  }

  @Test
  public void verify_example_of_response() {
    insertProjectWithBranches(false, 0);
    ws.newRequest().execute().assertJson(ws.getDef().responseExampleAsString());
  }

  @Test
  public void return_100_if_there_is_no_tasks_left() {
    IndexationStatusWsResponse response = ws.newRequest()
      .executeProtobuf(IndexationStatusWsResponse.class);
    assertThat(response.getPercentCompleted()).isEqualTo(100);
    assertThat(response.getIsCompleted()).isTrue();
  }

  @Test
  public void return_100_if_all_branches_have_need_issue_sync_set_FALSE() {
    IntStream.range(0, 13).forEach(value -> insertProjectWithBranches(false, 2));
    IntStream.range(0, 14).forEach(value -> insertProjectWithBranches(false, 4));
    IntStream.range(0, 4).forEach(value -> insertProjectWithBranches(false, 10));

    IndexationStatusWsResponse response = ws.newRequest()
      .executeProtobuf(IndexationStatusWsResponse.class);
    assertThat(response.getPercentCompleted()).isEqualTo(100);
    assertThat(response.getIsCompleted()).isTrue();
  }

  @Test
  @UseDataProvider("various_task_numbers")
  public void return_correct_percent_value_for_branches_to_sync(int toSync, int synced, int expectedPercent, boolean isCompleted) {
    IntStream.range(0, toSync).forEach(value -> insertProjectWithBranches(true, 0));
    IntStream.range(0, synced).forEach(value -> insertProjectWithBranches(false, 0));

    IndexationStatusWsResponse response = ws.newRequest()
      .executeProtobuf(IndexationStatusWsResponse.class);
    assertThat(response.getPercentCompleted()).isEqualTo(expectedPercent);
    assertThat(response.getIsCompleted()).isEqualTo(isCompleted);
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
    IntStream.range(0, 13).forEach(value -> insertProjectWithBranches(true, value));

    IndexationStatusWsResponse response = ws.newRequest()
      .executeProtobuf(IndexationStatusWsResponse.class);
    assertThat(response.getPercentCompleted()).isZero();
    assertThat(response.getIsCompleted()).isFalse();
  }

  private void insertProjectWithBranches(boolean needIssueSync, int numberOfBranches) {
    ProjectDto projectDto = db.components()
      .insertPrivateProjectDto(db.getDefaultOrganization(), branchDto -> branchDto.setNeedIssueSync(needIssueSync));
    IntStream.range(0, numberOfBranches).forEach(
      i -> db.components().insertProjectBranch(projectDto, branchDto -> branchDto.setNeedIssueSync(needIssueSync)));
  }
}
