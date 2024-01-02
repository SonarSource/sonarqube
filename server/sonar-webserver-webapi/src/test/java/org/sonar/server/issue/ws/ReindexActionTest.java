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
package org.sonar.server.issue.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.index.AsyncIssueIndexing;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ReindexActionTest {

  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final AsyncIssueIndexing mock = mock(AsyncIssueIndexing.class);
  private final IssueIndexer issueIndexer = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()), mock);
  private final ReindexAction underTest = new ReindexAction(db.getDbClient(), issueIndexer, userSession);
  private final WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void test_definition() {
    WebService.Action action = tester.getDef();

    assertThat(action.key()).isEqualTo("reindex");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).extracting(WebService.Param::key).containsExactly("project");
  }

  @Test
  public void reindex_project() {
    ProjectDto project = db.components().insertPrivateProjectDto();
    userSession.logIn().setSystemAdministrator();
    userSession.addProjectPermission(UserRole.ADMIN, project);

    TestResponse response = tester.newRequest()
      .setParam("project", project.getKey())
      .execute();

    assertThat(response.getStatus()).isEqualTo(204);
    verify(mock, times(1)).triggerForProject(project.getUuid());
  }

  @Test
  public void fail_if_project_does_not_exist() {
    userSession.logIn().setSystemAdministrator();

    TestRequest testRequest = tester.newRequest().setParam("project", "some-key");
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("project not found");
  }

  @Test
  public void fail_if_parameter_not_present() {
    userSession.logIn().setSystemAdministrator();
    TestRequest testRequest = tester.newRequest();
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'project' parameter is missing");
  }

  @Test
  public void fail_if_not_authorized() {
    ProjectDto project = db.components().insertPrivateProjectDto();
    userSession.addProjectPermission(UserRole.USER, project);

    TestRequest testRequest = tester.newRequest().setParam("project", project.getKey());
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

}
