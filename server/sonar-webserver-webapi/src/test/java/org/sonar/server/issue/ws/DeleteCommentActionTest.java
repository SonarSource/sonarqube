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
package org.sonar.server.issue.ws;

import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDbTester;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.USER;

public class DeleteCommentActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester dbTester = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbClient dbClient = dbTester.getDbClient();
  private IssueDbTester issueDbTester = new IssueDbTester(dbTester);
  private OperationResponseWriter responseWriter = mock(OperationResponseWriter.class);
  private ArgumentCaptor<SearchResponseData> preloadedSearchResponseDataCaptor = ArgumentCaptor.forClass(SearchResponseData.class);

  private WsActionTester tester = new WsActionTester(
    new DeleteCommentAction(userSession, dbClient, new IssueFinder(dbClient, userSession), responseWriter));

  @Test
  public void delete_comment() {
    IssueDto issueDto = issueDbTester.insertIssue();
    UserDto user = dbTester.users().insertUser();
    IssueChangeDto commentDto = issueDbTester.insertComment(issueDto, user, "please fix it");
    loginAndAddProjectPermission(user, issueDto, USER);

    call(commentDto.getKey());

    verify(responseWriter).write(eq(issueDto.getKey()), preloadedSearchResponseDataCaptor.capture(), any(Request.class), any(Response.class));
    assertThat(dbClient.issueChangeDao().selectCommentByKey(dbTester.getSession(), commentDto.getKey())).isNotPresent();
    verifyContentOfPreloadedSearchResponseData(issueDto);
  }

  @Test
  public void delete_comment_using_deprecated_key_parameter() {
    IssueDto issueDto = issueDbTester.insertIssue();
    UserDto user = dbTester.users().insertUser();
    IssueChangeDto commentDto = issueDbTester.insertComment(issueDto, user, "please fix it");
    loginAndAddProjectPermission(user, issueDto, USER);

    tester.newRequest().setParam("key", commentDto.getKey()).setParam("text", "please have a look").execute();

    verify(responseWriter).write(eq(issueDto.getKey()), preloadedSearchResponseDataCaptor.capture(), any(Request.class), any(Response.class));
    assertThat(dbClient.issueChangeDao().selectCommentByKey(dbTester.getSession(), commentDto.getKey())).isNotPresent();
    verifyContentOfPreloadedSearchResponseData(issueDto);
  }

  @Test
  public void fail_when_comment_does_not_belong_to_current_user() {
    IssueDto issueDto = issueDbTester.insertIssue();
    UserDto user = dbTester.users().insertUser();
    IssueChangeDto commentDto = issueDbTester.insertComment(issueDto, user, "please fix it");
    UserDto another = dbTester.users().insertUser();
    loginAndAddProjectPermission(another, issueDto, USER);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("You can only delete your own comments");
    call(commentDto.getKey());
  }

  @Test
  public void fail_when_comment_has_not_user() {
    IssueDto issueDto = issueDbTester.insertIssue();
    UserDto user = dbTester.users().insertUser();
    IssueChangeDto commentDto = issueDbTester.insertComment(issueDto, null, "please fix it");
    loginAndAddProjectPermission(user, issueDto, USER);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("You can only delete your own comments");
    call(commentDto.getKey());
  }

  @Test
  public void fail_when_missing_comment_key() {
    userSession.logIn("john");

    expectedException.expect(IllegalArgumentException.class);
    call(null);
  }

  @Test
  public void fail_when_comment_does_not_exist() {
    userSession.logIn("john");

    expectedException.expect(NotFoundException.class);
    call("ABCD");
  }

  @Test
  public void fail_when_not_authenticated() {
    expectedException.expect(UnauthorizedException.class);
    call("ABCD");
  }

  @Test
  public void fail_when_not_enough_permission() {
    IssueDto issueDto = issueDbTester.insertIssue();
    UserDto user = dbTester.users().insertUser();
    IssueChangeDto commentDto = issueDbTester.insertComment(issueDto, user, "please fix it");
    loginAndAddProjectPermission(user, issueDto, CODEVIEWER);

    expectedException.expect(ForbiddenException.class);
    call(commentDto.getKey());
  }

  @Test
  public void test_definition() {
    WebService.Action action = tester.getDef();
    assertThat(action.key()).isEqualTo("delete_comment");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(1);
    assertThat(action.responseExample()).isNotNull();
  }

  private void verifyContentOfPreloadedSearchResponseData(IssueDto issue) {
    SearchResponseData preloadedSearchResponseData = preloadedSearchResponseDataCaptor.getValue();
    assertThat(preloadedSearchResponseData.getIssues())
      .extracting(IssueDto::getKey)
      .containsOnly(issue.getKey());
    assertThat(preloadedSearchResponseData.getRules()).isNullOrEmpty();
    assertThat(preloadedSearchResponseData.getComponents()).isNullOrEmpty();
  }

  private TestResponse call(@Nullable String commentKey) {
    TestRequest request = tester.newRequest();
    ofNullable(commentKey).ifPresent(comment -> request.setParam("comment", comment));
    return request.execute();
  }

  private void loginAndAddProjectPermission(UserDto user, IssueDto issueDto, String permission) {
    userSession.logIn(user).addProjectPermission(permission, dbClient.componentDao().selectByUuid(dbTester.getSession(), issueDto.getProjectUuid()).get());
  }

}
