/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.issue.IssueUpdater;
import org.sonar.server.issue.ServerIssueStorage;
import org.sonar.server.issue.TestIssueChangePostProcessor;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.USER;

public class AssignActionTest {

  private static final String PREVIOUS_ASSIGNEE = "previous";
  private static final String CURRENT_USER_LOGIN = "john";

  private static final long PAST = 10_000_000_000L;
  private static final long NOW = 50_000_000_000L;

  private TestSystem2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public EsTester es = new EsTester(new IssueIndexDefinition(new MapSettings().asConfig()));
  @Rule
  public DbTester db = DbTester.create(system2);

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()));
  private OperationResponseWriter responseWriter = mock(OperationResponseWriter.class);
  private TestIssueChangePostProcessor issueChangePostProcessor = new TestIssueChangePostProcessor();
  private AssignAction underTest = new AssignAction(system2, userSession, db.getDbClient(), new IssueFinder(db.getDbClient(), userSession), new IssueFieldsSetter(),
    new IssueUpdater(db.getDbClient(),
      new ServerIssueStorage(system2, new DefaultRuleFinder(db.getDbClient(), defaultOrganizationProvider), db.getDbClient(), issueIndexer),
      mock(NotificationManager.class), issueChangePostProcessor),
    responseWriter);
  private ArgumentCaptor<SearchResponseData> preloadedSearchResponseDataCaptor = ArgumentCaptor.forClass(SearchResponseData.class);
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void assign_to_someone() {
    IssueDto issue = newIssueWithBrowsePermission();
    insertUser("arthur");

    ws.newRequest()
      .setParam("issue", issue.getKey())
      .setParam("assignee", "arthur")
      .execute();

    checkIssueAssignee(issue.getKey(), "arthur");
    verify(responseWriter).write(eq(issue.getKey()), preloadedSearchResponseDataCaptor.capture(), any(Request.class), any(Response.class));
    verifyContentOfPreloadedSearchResponseData(issue);
    assertThat(issueChangePostProcessor.wasCalled()).isFalse();
  }

  @Test
  public void assign_to_me() {
    IssueDto issue = newIssueWithBrowsePermission();

    ws.newRequest()
      .setParam("issue", issue.getKey())
      .setParam("assignee", "_me")
      .execute();

    checkIssueAssignee(issue.getKey(), CURRENT_USER_LOGIN);
    verify(responseWriter).write(eq(issue.getKey()), preloadedSearchResponseDataCaptor.capture(), any(Request.class), any(Response.class));
    verifyContentOfPreloadedSearchResponseData(issue);
    assertThat(issueChangePostProcessor.wasCalled()).isFalse();
  }

  @Test
  public void assign_to_me_using_deprecated_me_param() {
    IssueDto issue = newIssueWithBrowsePermission();

    ws.newRequest()
      .setParam("issue", issue.getKey())
      .setParam("me", "true")
      .execute();

    checkIssueAssignee(issue.getKey(), CURRENT_USER_LOGIN);
    verify(responseWriter).write(eq(issue.getKey()), preloadedSearchResponseDataCaptor.capture(), any(Request.class), any(Response.class));
    verifyContentOfPreloadedSearchResponseData(issue);
  }

  @Test
  public void unassign() {
    IssueDto issue = newIssueWithBrowsePermission();

    ws.newRequest()
      .setParam("issue", issue.getKey())
      .execute();

    checkIssueAssignee(issue.getKey(), null);
    verify(responseWriter).write(eq(issue.getKey()), preloadedSearchResponseDataCaptor.capture(), any(Request.class), any(Response.class));
    verifyContentOfPreloadedSearchResponseData(issue);
    assertThat(issueChangePostProcessor.wasCalled()).isFalse();
  }

  @Test
  public void unassign_with_empty_assignee_param() {
    IssueDto issue = newIssueWithBrowsePermission();

    ws.newRequest()
      .setParam("issue", issue.getKey())
      .setParam("assignee", "")
      .execute();

    checkIssueAssignee(issue.getKey(), null);
    verify(responseWriter).write(eq(issue.getKey()), preloadedSearchResponseDataCaptor.capture(), any(Request.class), any(Response.class));
    verifyContentOfPreloadedSearchResponseData(issue);
  }

  @Test
  public void nothing_to_do_when_new_assignee_is_same_as_old_one() {
    IssueDto issue = newIssueWithBrowsePermission();
    insertUser(PREVIOUS_ASSIGNEE);

    ws.newRequest()
      .setParam("issue", issue.getKey())
      .setParam("assignee", PREVIOUS_ASSIGNEE)
      .execute();

    IssueDto issueReloaded = db.getDbClient().issueDao().selectByKey(db.getSession(), issue.getKey()).get();
    assertThat(issueReloaded.getAssignee()).isEqualTo(PREVIOUS_ASSIGNEE);
    assertThat(issueReloaded.getUpdatedAt()).isEqualTo(PAST);
    assertThat(issueReloaded.getIssueUpdateTime()).isEqualTo(PAST);
  }

  @Test
  public void fail_when_assignee_does_not_exist() {
    IssueDto issue = newIssueWithBrowsePermission();

    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setParam("issue", issue.getKey())
      .setParam("assignee", "unknown")
      .execute();
  }

  @Test
  public void fail_when_assignee_is_disabled() {
    IssueDto issue = newIssueWithBrowsePermission();
    db.users().insertUser(user -> user.setActive(false));

    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setParam("issue", issue.getKey())
      .setParam("assignee", "unknown")
      .execute();
  }

  @Test
  public void fail_when_not_authenticated() {
    IssueDto issue = newIssue();
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);

    ws.newRequest()
      .setParam("issue", issue.getKey())
      .setParam("assignee", "_me")
      .execute();
  }

  @Test
  public void fail_when_missing_browse_permission() {
    IssueDto issue = newIssue();
    setUserWithPermission(issue, CODEVIEWER);

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam("issue", issue.getKey())
      .setParam("assignee", "_me")
      .execute();
  }

  @Test
  public void fail_when_assignee_is_not_member_of_organization_of_project_issue() {
    OrganizationDto org = db.organizations().insert(organizationDto -> organizationDto.setKey("Organization key"));
    IssueDto issueDto = db.issues().insertIssue(org);
    setUserWithBrowsePermission(issueDto);
    OrganizationDto otherOrganization = db.organizations().insert();
    UserDto assignee = db.users().insertUser("arthur");
    db.organizations().addMember(otherOrganization, assignee);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("User 'arthur' is not member of organization 'Organization key'");

    ws.newRequest()
      .setParam("issue", issueDto.getKey())
      .setParam("assignee", "arthur")
      .execute();
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

  private UserDto insertUser(String login) {
    UserDto user = db.users().insertUser(login);
    db.organizations().addMember(db.getDefaultOrganization(), user);
    return user;
  }

  private IssueDto newIssue() {
    IssueDto issue = db.issues().insertIssue(
      issueDto -> issueDto
        .setAssignee(PREVIOUS_ASSIGNEE)
        .setCreatedAt(PAST).setIssueCreationTime(PAST)
        .setUpdatedAt(PAST).setIssueUpdateTime(PAST));
    return issue;
  }

  private IssueDto newIssueWithBrowsePermission() {
    IssueDto issue = newIssue();
    setUserWithBrowsePermission(issue);
    return issue;
  }

  private void setUserWithBrowsePermission(IssueDto issue) {
    setUserWithPermission(issue, USER);
  }

  private void setUserWithPermission(IssueDto issue, String permission) {
    insertUser(CURRENT_USER_LOGIN);
    userSession.logIn(CURRENT_USER_LOGIN)
      .addProjectPermission(permission,
        db.getDbClient().componentDao().selectByUuid(db.getSession(), issue.getProjectUuid()).get(),
        db.getDbClient().componentDao().selectByUuid(db.getSession(), issue.getComponentUuid()).get());
  }

  private void checkIssueAssignee(String issueKey, @Nullable String expectedAssignee) {
    IssueDto issueReloaded = db.getDbClient().issueDao().selectByKey(db.getSession(), issueKey).get();
    assertThat(issueReloaded.getAssignee()).isEqualTo(expectedAssignee);
    assertThat(issueReloaded.getIssueUpdateTime()).isEqualTo(NOW);
    assertThat(issueReloaded.getUpdatedAt()).isEqualTo(NOW);
  }
}
