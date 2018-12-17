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

import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDbTester;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.issue.WebIssueStorage;
import org.sonar.server.issue.IssueUpdater;
import org.sonar.server.issue.TestIssueChangePostProcessor;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.rule.DefaultRuleFinder;
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
import static org.mockito.Mockito.when;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.issue.IssueTesting.newDto;
import static org.sonar.db.rule.RuleTesting.newRuleDto;

public class SetTypeActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester dbTester = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private System2 system2 = mock(System2.class);

  private DbClient dbClient = dbTester.getDbClient();

  private IssueDbTester issueDbTester = new IssueDbTester(dbTester);
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);
  private OperationResponseWriter responseWriter = mock(OperationResponseWriter.class);
  private ArgumentCaptor<SearchResponseData> preloadedSearchResponseDataCaptor = ArgumentCaptor.forClass(SearchResponseData.class);

  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), dbClient, new IssueIteratorFactory(dbClient));
  private TestIssueChangePostProcessor issueChangePostProcessor = new TestIssueChangePostProcessor();
  private WsActionTester tester = new WsActionTester(new SetTypeAction(userSession, dbClient, new IssueFinder(dbClient, userSession), new IssueFieldsSetter(),
    new IssueUpdater(dbClient,
      new WebIssueStorage(system2, dbClient, new DefaultRuleFinder(dbClient, defaultOrganizationProvider), issueIndexer), mock(NotificationManager.class),
      issueChangePostProcessor),
    responseWriter, system2));

  @Test
  public void set_type() {
    long now = 1_999_777_234L;
    when(system2.now()).thenReturn(now);
    IssueDto issueDto = issueDbTester.insertIssue(newIssue().setType(CODE_SMELL));
    setUserWithBrowseAndAdministerIssuePermission(issueDto);

    call(issueDto.getKey(), BUG.name());

    verify(responseWriter).write(eq(issueDto.getKey()), preloadedSearchResponseDataCaptor.capture(), any(Request.class), any(Response.class));
    verifyContentOfPreloadedSearchResponseData(issueDto);
    IssueDto issueReloaded = dbClient.issueDao().selectByKey(dbTester.getSession(), issueDto.getKey()).get();
    assertThat(issueReloaded.getType()).isEqualTo(BUG.getDbConstant());

    assertThat(issueChangePostProcessor.calledComponents())
      .extracting(ComponentDto::uuid)
      .containsExactlyInAnyOrder(issueDto.getComponentUuid());
  }

  @Test
  public void prevent_changing_type_security_hotspot() {
    long now = 1_999_777_234L;
    when(system2.now()).thenReturn(now);
    IssueDto issueDto = issueDbTester.insertIssue(newIssue().setType(VULNERABILITY).setIsFromHotspot(true));
    setUserWithBrowseAndAdministerIssuePermission(issueDto);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Changing type of a security hotspot is not permitted");
    call(issueDto.getKey(), BUG.name());
  }

  @Test
  public void insert_entry_in_changelog_when_setting_type() {
    IssueDto issueDto = issueDbTester.insertIssue(newIssue().setType(CODE_SMELL));
    setUserWithBrowseAndAdministerIssuePermission(issueDto);

    call(issueDto.getKey(), BUG.name());

    List<FieldDiffs> fieldDiffs = dbClient.issueChangeDao().selectChangelogByIssue(dbTester.getSession(), issueDto.getKey());
    assertThat(fieldDiffs).hasSize(1);
    assertThat(fieldDiffs.get(0).diffs()).hasSize(1);
    assertThat(fieldDiffs.get(0).diffs().get("type").newValue()).isEqualTo(BUG.name());
    assertThat(fieldDiffs.get(0).diffs().get("type").oldValue()).isEqualTo(CODE_SMELL.name());
  }

  @Test
  public void fail_if_bad_type_value() {
    IssueDto issueDto = issueDbTester.insertIssue(newIssue().setType(CODE_SMELL));
    setUserWithBrowseAndAdministerIssuePermission(issueDto);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value of parameter 'type' (unknown) must be one of: [CODE_SMELL, BUG, VULNERABILITY, SECURITY_HOTSPOT]");
    call(issueDto.getKey(), "unknown");
  }

  @Test
  public void fail_when_not_authenticated() {
    expectedException.expect(UnauthorizedException.class);
    call("ABCD", BUG.name());
  }

  @Test
  public void fail_when_missing_browse_permission() {
    IssueDto issueDto = issueDbTester.insertIssue();
    String login = "john";
    String permission = ISSUE_ADMIN;
    logInAndAddProjectPermission(login, issueDto, permission);

    expectedException.expect(ForbiddenException.class);
    call(issueDto.getKey(), BUG.name());
  }

  @Test
  public void fail_when_missing_administer_issue_permission() {
    IssueDto issueDto = issueDbTester.insertIssue();
    logInAndAddProjectPermission("john", issueDto, USER);

    expectedException.expect(ForbiddenException.class);
    call(issueDto.getKey(), BUG.name());
  }

  @Test
  public void test_definition() {
    WebService.Action action = tester.getDef();
    assertThat(action.key()).isEqualTo("set_type");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(2);
    assertThat(action.responseExample()).isNotNull();
  }

  private TestResponse call(@Nullable String issueKey, @Nullable String type) {
    TestRequest request = tester.newRequest();
    ofNullable(issueKey).ifPresent(issue -> request.setParam("issue", issue));
    ofNullable(type).ifPresent(t -> request.setParam("type", t));
    return request.execute();
  }

  private IssueDto newIssue() {
    RuleDto rule = dbTester.rules().insertRule(newRuleDto());
    ComponentDto project = dbTester.components().insertPrivateProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    return newDto(rule, file, project);
  }

  private void setUserWithBrowseAndAdministerIssuePermission(IssueDto issueDto) {
    ComponentDto project = dbClient.componentDao().selectByUuid(dbTester.getSession(), issueDto.getProjectUuid()).get();
    userSession.logIn("john")
      .addProjectPermission(ISSUE_ADMIN, project)
      .addProjectPermission(USER, project);
  }

  private void logInAndAddProjectPermission(String login, IssueDto issueDto, String permission) {
    userSession.logIn(login).addProjectPermission(permission, dbClient.componentDao().selectByUuid(dbTester.getSession(), issueDto.getProjectUuid()).get());
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
}
