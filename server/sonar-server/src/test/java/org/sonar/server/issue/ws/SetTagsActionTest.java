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

import com.google.common.base.Joiner;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueTesting;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
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
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.core.util.stream.MoreCollectors.join;
import static org.sonar.db.component.ComponentTesting.newFileDto;

public class SetTagsActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester esTester = new EsTester(new IssueIndexDefinition(new MapSettings().asConfig()));
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private System2 system2 = mock(System2.class);
  private DbClient dbClient = db.getDbClient();
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private OperationResponseWriter responseWriter = mock(OperationResponseWriter.class);
  private IssueIndexer issueIndexer = new IssueIndexer(esTester.client(), dbClient, new IssueIteratorFactory(dbClient));
  private ArgumentCaptor<SearchResponseData> preloadedSearchResponseDataCaptor = ArgumentCaptor.forClass(SearchResponseData.class);
  private TestIssueChangePostProcessor issueChangePostProcessor = new TestIssueChangePostProcessor();

  private WsActionTester ws = new WsActionTester(new SetTagsAction(userSession, dbClient, new IssueFinder(dbClient, userSession), new IssueFieldsSetter(),
    new IssueUpdater(dbClient,
      new ServerIssueStorage(system2, new DefaultRuleFinder(dbClient, defaultOrganizationProvider), dbClient, issueIndexer), mock(NotificationManager.class), issueChangePostProcessor),
    responseWriter));

  @Test
  public void set_tags() {
    IssueDto issueDto = db.issues().insertIssue(newIssue().setTags(singletonList("old-tag")));
    logIn(issueDto);

    call(issueDto.getKey(), "bug", "todo");

    verify(responseWriter).write(eq(issueDto.getKey()), preloadedSearchResponseDataCaptor.capture(), any(Request.class), any(Response.class));
    verifyContentOfPreloadedSearchResponseData(issueDto);
    IssueDto issueReloaded = dbClient.issueDao().selectByKey(db.getSession(), issueDto.getKey()).get();
    assertThat(issueReloaded.getTags()).containsOnly("bug", "todo");
    assertThat(issueChangePostProcessor.wasCalled()).isFalse();
  }

  @Test
  public void remove_existing_tags_when_value_is_not_set() {
    IssueDto issueDto = db.issues().insertIssue(newIssue().setTags(singletonList("old-tag")));
    logIn(issueDto);

    call(issueDto.getKey());

    IssueDto issueReloaded = dbClient.issueDao().selectByKey(db.getSession(), issueDto.getKey()).get();
    assertThat(issueReloaded.getTags()).isEmpty();
    assertThat(issueChangePostProcessor.wasCalled()).isFalse();
  }

  @Test
  public void remove_existing_tags_when_value_is_empty_string() {
    IssueDto issueDto = db.issues().insertIssue(newIssue().setTags(singletonList("old-tag")));
    logIn(issueDto);

    call(issueDto.getKey(), "");

    IssueDto issueReloaded = dbClient.issueDao().selectByKey(db.getSession(), issueDto.getKey()).get();
    assertThat(issueReloaded.getTags()).isEmpty();
  }

  @Test
  public void set_tags_using_deprecated_key_param() {
    IssueDto issueDto = db.issues().insertIssue(newIssue().setTags(singletonList("old-tag")));
    logIn(issueDto);

    ws.newRequest().setParam("key", issueDto.getKey()).setParam("tags", "bug").execute();

    IssueDto issueReloaded = dbClient.issueDao().selectByKey(db.getSession(), issueDto.getKey()).get();
    assertThat(issueReloaded.getTags()).containsOnly("bug");
  }

  @Test
  public void tags_are_stored_as_lowercase() {
    IssueDto issueDto = db.issues().insertIssue(newIssue().setTags(singletonList("old-tag")));
    logIn(issueDto);

    call(issueDto.getKey(), "bug", "Convention");

    IssueDto issueReloaded = dbClient.issueDao().selectByKey(db.getSession(), issueDto.getKey()).get();
    assertThat(issueReloaded.getTags()).containsOnly("bug", "convention");
  }

  @Test
  public void empty_tags_are_ignored() {
    IssueDto issueDto = db.issues().insertIssue(newIssue().setTags(singletonList("old-tag")));
    logIn(issueDto);

    call(issueDto.getKey(), "security", "", "convention");

    IssueDto issueReloaded = dbClient.issueDao().selectByKey(db.getSession(), issueDto.getKey()).get();
    assertThat(issueReloaded.getTags()).containsOnly("security", "convention");
  }

  @Test
  public void insert_entry_in_changelog_when_setting_tags() {
    IssueDto issueDto = db.issues().insertIssue(newIssue().setTags(singletonList("old-tag")));
    logIn(issueDto);

    call(issueDto.getKey(), "new-tag");

    List<FieldDiffs> fieldDiffs = dbClient.issueChangeDao().selectChangelogByIssue(db.getSession(), issueDto.getKey());
    assertThat(fieldDiffs).hasSize(1);
    assertThat(fieldDiffs.get(0).diffs()).hasSize(1);
    assertThat(fieldDiffs.get(0).diffs().get("tags").oldValue()).isEqualTo("old-tag");
    assertThat(fieldDiffs.get(0).diffs().get("tags").newValue()).isEqualTo("new-tag");
  }

  @Test
  public void fail_when_bad_tag_format() {
    IssueDto issueDto = db.issues().insertIssue(newIssue().setTags(singletonList("old-tag")));
    logIn(issueDto);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Tag 'pol op' is invalid. Rule tags accept only the characters: a-z, 0-9, '+', '-', '#', '.'");

    call(issueDto.getKey(), "pol op");
  }

  @Test
  public void fail_when_not_authenticated() {
    expectedException.expect(UnauthorizedException.class);

    call("ABCD", "bug");
  }

  @Test
  public void fail_when_missing_browse_permission() {
    IssueDto issueDto = db.issues().insertIssue();
    logInAndAddProjectPermission(issueDto, ISSUE_ADMIN);

    expectedException.expect(ForbiddenException.class);

    call(issueDto.getKey(), "bug");
  }

  @Test
  public void test_definition() {
    Action action = ws.getDef();
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(2);

    Param query = action.param("issue");
    assertThat(query.isRequired()).isTrue();
    assertThat(query.description()).isNotEmpty();
    assertThat(query.exampleValue()).isNotEmpty();
    Param pageSize = action.param("tags");
    assertThat(pageSize.isRequired()).isFalse();
    assertThat(pageSize.defaultValue()).isNull();
    assertThat(pageSize.description()).isNotEmpty();
    assertThat(pageSize.exampleValue()).isNotEmpty();
  }

  private TestResponse call(@Nullable String issueKey, String... tags) {
    TestRequest request = ws.newRequest();
    setNullable(issueKey, issue -> request.setParam("issue", issue));
    if (tags.length > 0) {
      request.setParam("tags", Arrays.stream(tags).collect(join(Joiner.on(","))));
    }
    return request.execute();
  }

  private IssueDto newIssue() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    return IssueTesting.newIssue(rule, project, file);
  }

  private void logIn(IssueDto issueDto) {
    userSession.logIn("john").registerComponents(
      dbClient.componentDao().selectByUuid(db.getSession(), issueDto.getProjectUuid()).get(),
      dbClient.componentDao().selectByUuid(db.getSession(), issueDto.getComponentUuid()).get());
  }

  private void logInAndAddProjectPermission(IssueDto issueDto, String permission) {
    userSession.logIn("john").addProjectPermission(permission, dbClient.componentDao().selectByUuid(db.getSession(), issueDto.getProjectUuid()).get());
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
