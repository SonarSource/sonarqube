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

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.USER;

public class EditCommentActionIT {

  private static final long NOW = 10_000_000_000L;

  @Rule
  public DbTester dbTester = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private System2 system2 = mock(System2.class);
  private DbClient dbClient = dbTester.getDbClient();
  private IssueDbTester issueDbTester = new IssueDbTester(dbTester);
  private OperationResponseWriter responseWriter = mock(OperationResponseWriter.class);
  private ArgumentCaptor<SearchResponseData> preloadedSearchResponseDataCaptor = ArgumentCaptor.forClass(SearchResponseData.class);

  private WsActionTester tester = new WsActionTester(
    new EditCommentAction(system2, userSession, dbClient, new IssueFinder(dbClient, userSession), responseWriter));

  @Before
  public void setUp() {
    when(system2.now()).thenReturn(NOW);
  }

  @Test
  public void edit_comment() {
    IssueDto issueDto = newIssue();
    UserDto user = dbTester.users().insertUser();
    IssueChangeDto commentDto = issueDbTester.insertComment(issueDto, user, "please fix it");
    loginWithBrowsePermission(user, USER, issueDto);

    call(commentDto.getKey(), "please have a look");

    verify(responseWriter).write(eq(issueDto.getKey()), preloadedSearchResponseDataCaptor.capture(), any(Request.class), any(Response.class));

    verifyContentOfPreloadedSearchResponseData(issueDto);
    IssueChangeDto issueComment = dbClient.issueChangeDao().selectCommentByKey(dbTester.getSession(), commentDto.getKey()).get();
    assertThat(issueComment.getChangeData()).isEqualTo("please have a look");
    assertThat(issueComment.getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  public void fail_when_comment_is_for_hotspot() {
    IssueDto hotspot = issueDbTester.insertHotspot();
    UserDto user = dbTester.users().insertUser();
    IssueChangeDto commentDto = issueDbTester.insertComment(hotspot, user, "please fix it");
    UserDto another = dbTester.users().insertUser();
    loginWithBrowsePermission(another, USER, hotspot);

    String commentDtoKey = commentDto.getKey();
    assertThatThrownBy(() -> call(commentDtoKey, "please have a look"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Issue with key '%s' does not exist", hotspot.getKey());
  }

  @Test
  public void fail_when_comment_does_not_belong_to_current_user() {
    IssueDto issueDto = newIssue();
    UserDto user = dbTester.users().insertUser();
    IssueChangeDto commentDto = issueDbTester.insertComment(issueDto, user, "please fix it");
    UserDto another = dbTester.users().insertUser();
    loginWithBrowsePermission(another, USER, issueDto);

    assertThatThrownBy(() -> call(commentDto.getKey(), "please have a look"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("You can only edit your own comments");
  }

  @Test
  public void fail_when_comment_has_not_user() {
    IssueDto issueDto = newIssue();
    UserDto user = dbTester.users().insertUser();
    IssueChangeDto commentDto = issueDbTester.insertComment(issueDto, null, "please fix it");
    loginWithBrowsePermission(user, USER, issueDto);

    assertThatThrownBy(() ->  call(commentDto.getKey(), "please have a look"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("You can only edit your own comments");
  }

  @Test
  public void fail_when_missing_comment_key() {
    userSession.logIn("john");

    assertThatThrownBy(() -> call(null, "please fix it"))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_when_comment_does_not_exist() {
    userSession.logIn("john");

    assertThatThrownBy(() -> call("ABCD", "please fix it"))
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_when_missing_comment_text() {
    userSession.logIn("john");

    assertThatThrownBy(() -> call("ABCD", null))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_when_empty_comment_text() {
    IssueDto issueDto = newIssue();
    UserDto user = dbTester.users().insertUser();
    IssueChangeDto commentDto = issueDbTester.insertComment(issueDto, user, "please fix it");
    loginWithBrowsePermission(user, USER, issueDto);

    assertThatThrownBy(() -> call(commentDto.getKey(), ""))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_when_not_authenticated() {
    assertThatThrownBy(() -> call("ABCD", "please fix it"))
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_not_enough_permission() {
    IssueDto issueDto = newIssue();
    UserDto user = dbTester.users().insertUser();
    IssueChangeDto commentDto = issueDbTester.insertComment(issueDto, user, "please fix it");
    loginWithBrowsePermission(user, CODEVIEWER, issueDto);

    assertThatThrownBy(() -> call(commentDto.getKey(), "please have a look"))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_NFE_if_security_hotspots() {
    IssueDto issueDto = issueDbTester.insertHotspot();
    UserDto user = dbTester.users().insertUser();
    IssueChangeDto commentDto = issueDbTester.insertComment(issueDto, user, "please fix it");
    loginWithBrowsePermission(user, CODEVIEWER, issueDto);

    assertThatThrownBy(() -> call(commentDto.getKey(), "please have a look"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage(String.format("Issue with key '%s' does not exist", issueDto.getKey()));
  }

  @Test
  public void test_definition() {
    WebService.Action action = tester.getDef();
    assertThat(action.key()).isEqualTo("edit_comment");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(2);
    assertThat(action.responseExample()).isNotNull();
  }

  private TestResponse call(@Nullable String commentKey, @Nullable String commentText) {
    TestRequest request = tester.newRequest();
    ofNullable(commentKey).ifPresent(comment1 -> request.setParam("comment", comment1));
    ofNullable(commentText).ifPresent(comment -> request.setParam("text", comment));
    return request.execute();
  }

  private IssueDto newIssue() {
    return issueDbTester.insertIssue();
  }

  private void loginWithBrowsePermission(UserDto user, String permission, IssueDto issueDto) {
    userSession.logIn(user).addProjectPermission(permission,
      dbClient.componentDao().selectByUuid(dbTester.getSession(), issueDto.getProjectUuid()).get(),
      dbClient.componentDao().selectByUuid(dbTester.getSession(), issueDto.getComponentUuid()).get());
  }

  private void verifyContentOfPreloadedSearchResponseData(IssueDto issue) {
    SearchResponseData preloadedSearchResponseData = preloadedSearchResponseDataCaptor.getValue();
    assertThat(preloadedSearchResponseData.getIssues())
      .extracting(IssueDto::getKey)
      .containsOnly(issue.getKey());
    assertThat(preloadedSearchResponseData.getRules()).isNullOrEmpty();
    assertThat(preloadedSearchResponseData.getComponents()).isNullOrEmpty();
  }
}
