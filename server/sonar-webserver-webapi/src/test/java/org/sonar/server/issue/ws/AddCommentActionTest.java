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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDbTester;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.issue.TestIssueChangePostProcessor;
import org.sonar.server.issue.WebIssueStorage;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.issue.notification.IssuesChangesNotificationSerializer;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.issue.IssueChangeDto.TYPE_COMMENT;

public class AddCommentActionTest {

  private static final long NOW = 10_000_000_000L;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public EsTester es = EsTester.create();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private System2 system2 = mock(System2.class);

  private DbClient dbClient = dbTester.getDbClient();
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);

  private IssueDbTester issueDbTester = new IssueDbTester(dbTester);

  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), dbClient, new IssueIteratorFactory(dbClient));
  private WebIssueStorage serverIssueStorage = new WebIssueStorage(system2, dbClient, new DefaultRuleFinder(dbClient, defaultOrganizationProvider), issueIndexer);
  private TestIssueChangePostProcessor issueChangePostProcessor = new TestIssueChangePostProcessor();
  private IssueUpdater issueUpdater = new IssueUpdater(dbClient, serverIssueStorage, mock(NotificationManager.class), issueChangePostProcessor, new IssuesChangesNotificationSerializer());
  private OperationResponseWriter responseWriter = mock(OperationResponseWriter.class);
  private ArgumentCaptor<SearchResponseData> preloadedSearchResponseDataCaptor = ArgumentCaptor.forClass(SearchResponseData.class);

  private WsActionTester tester = new WsActionTester(
    new AddCommentAction(system2, userSession, dbClient, new IssueFinder(dbClient, userSession), issueUpdater, new IssueFieldsSetter(), responseWriter));

  @Before
  public void setUp() throws Exception {
    when(system2.now()).thenReturn(NOW);
  }

  @Test
  public void add_comment() {
    IssueDto issueDto = issueDbTester.insertIssue();
    loginWithBrowsePermission(issueDto, USER);

    call(issueDto.getKey(), "please fix it");

    verify(responseWriter).write(eq(issueDto.getKey()), preloadedSearchResponseDataCaptor.capture(), any(Request.class), any(Response.class));
    verifyContentOfPreloadedSearchResponseData(issueDto);

    IssueChangeDto issueComment = dbClient.issueChangeDao().selectByTypeAndIssueKeys(dbTester.getSession(), singletonList(issueDto.getKey()), TYPE_COMMENT).get(0);
    assertThat(issueComment.getKey()).isNotNull();
    assertThat(issueComment.getUserUuid()).isEqualTo(userSession.getUuid());
    assertThat(issueComment.getChangeType()).isEqualTo(TYPE_COMMENT);
    assertThat(issueComment.getChangeData()).isEqualTo("please fix it");
    assertThat(issueComment.getCreatedAt()).isNotNull();
    assertThat(issueComment.getUpdatedAt()).isNotNull();
    assertThat(issueComment.getIssueKey()).isEqualTo(issueDto.getKey());
    assertThat(issueComment.getIssueChangeCreationDate()).isNotNull();

    IssueDto issueReloaded = dbClient.issueDao().selectByKey(dbTester.getSession(), issueDto.getKey()).get();
    assertThat(issueReloaded.getIssueUpdateTime()).isEqualTo(NOW);
    assertThat(issueChangePostProcessor.wasCalled()).isFalse();
  }

  @Test
  public void fail_when_missing_issue_key() {
    userSession.logIn("john");

    expectedException.expect(IllegalArgumentException.class);
    call(null, "please fix it");
  }

  @Test
  public void fail_when_issue_does_not_exist() {
    userSession.logIn("john");

    expectedException.expect(NotFoundException.class);
    call("ABCD", "please fix it");
  }

  @Test
  public void fail_when_missing_comment_text() {
    userSession.logIn("john");

    expectedException.expect(IllegalArgumentException.class);
    call("ABCD", null);
  }

  @Test
  public void fail_when_empty_comment_text() {
    IssueDto issueDto = issueDbTester.insertIssue();
    loginWithBrowsePermission(issueDto, USER);

    expectedException.expect(IllegalArgumentException.class);
    call(issueDto.getKey(), "");
  }

  @Test
  public void fail_when_not_authenticated() {
    expectedException.expect(UnauthorizedException.class);
    call("ABCD", "please fix it");
  }

  @Test
  public void fail_when_not_enough_permission() {
    IssueDto issueDto = issueDbTester.insertIssue();
    loginWithBrowsePermission(issueDto, CODEVIEWER);

    expectedException.expect(ForbiddenException.class);
    call(issueDto.getKey(), "please fix it");
  }

  @Test
  public void test_definition() {
    WebService.Action action = tester.getDef();
    assertThat(action.key()).isEqualTo("add_comment");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(2);
    assertThat(action.responseExampleAsString()).isNotEmpty();
  }

  private void verifyContentOfPreloadedSearchResponseData(IssueDto issue) {
    SearchResponseData preloadedSearchResponseData = preloadedSearchResponseDataCaptor.getValue();
    assertThat(preloadedSearchResponseData.getIssues())
      .extracting(IssueDto::getKey)
      .containsOnly(issue.getKey());
    assertThat(preloadedSearchResponseData.getRules())
      .extracting(RuleDefinitionDto::getKey)
      .containsOnly(issue.getRuleKey());
    assertThat(preloadedSearchResponseData.getComponents())
      .extracting(ComponentDto::uuid)
      .containsOnly(issue.getComponentUuid(), issue.getProjectUuid());
  }

  private TestResponse call(@Nullable String issueKey, @Nullable String commentText) {
    TestRequest request = tester.newRequest();
    ofNullable(issueKey).ifPresent(issue -> request.setParam("issue", issue));
    ofNullable(commentText).ifPresent(text -> request.setParam("text", text));
    return request.execute();
  }

  private void loginWithBrowsePermission(IssueDto issueDto, String permission) {
    UserDto user = dbTester.users().insertUser("john");
    userSession.logIn(user)
      .addProjectPermission(permission,
      dbClient.componentDao().selectByUuid(dbTester.getSession(), issueDto.getProjectUuid()).get(),
      dbClient.componentDao().selectByUuid(dbTester.getSession(), issueDto.getComponentUuid()).get());
  }

}
