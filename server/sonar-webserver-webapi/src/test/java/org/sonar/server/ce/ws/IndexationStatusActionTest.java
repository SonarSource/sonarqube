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
package org.sonar.server.ce.ws;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueSyncProgress;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Ce.IndexationStatusWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class IndexationStatusActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  public IssueIndexSyncProgressChecker issueIndexSyncProgressCheckerMock = mock(IssueIndexSyncProgressChecker.class);

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private WsActionTester ws = new WsActionTester(new IndexationStatusAction(db.getDbClient(), issueIndexSyncProgressCheckerMock));

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
    when(issueIndexSyncProgressCheckerMock.getIssueSyncProgress(any())).thenReturn(new IssueSyncProgress(true,0, 0, false));
    ws.newRequest().execute().assertJson(ws.getDef().responseExampleAsString());
  }

  @Test
  public void return_100_if_there_is_no_tasks_left() {
    when(issueIndexSyncProgressCheckerMock.getIssueSyncProgress(any())).thenReturn(new IssueSyncProgress(true, 10, 10, false));
    IndexationStatusWsResponse response = ws.newRequest()
      .executeProtobuf(IndexationStatusWsResponse.class);
    assertThat(response.getPercentCompleted()).isEqualTo(100);
    assertThat(response.getIsCompleted()).isTrue();
    assertThat(response.getHasFailures()).isFalse();
  }

  @Test
  public void return_0_if_all_branches_have_need_issue_sync_set_TRUE() {
    when(issueIndexSyncProgressCheckerMock.getIssueSyncProgress(any())).thenReturn(new IssueSyncProgress(false,0, 10, false));

    IndexationStatusWsResponse response = ws.newRequest()
      .executeProtobuf(IndexationStatusWsResponse.class);
    assertThat(response.getPercentCompleted()).isZero();
    assertThat(response.getIsCompleted()).isFalse();
    assertThat(response.getHasFailures()).isFalse();
  }
}
