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

import com.google.protobuf.ProtocolStringList;
import java.util.function.Consumer;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.view.index.ViewIndexer;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Issues.TagsResponse;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.test.JsonAssert.assertJson;

public class TagsActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();

  private IssueIndex issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSession, new WebAuthorizationTypeSupport(userSession));
  private IssueIndexSyncProgressChecker issueIndexSyncProgressChecker = mock(IssueIndexSyncProgressChecker.class);
  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()), null);
  private ViewIndexer viewIndexer = new ViewIndexer(db.getDbClient(), es.client());
  private PermissionIndexerTester permissionIndexer = new PermissionIndexerTester(es, issueIndexer);
  private ResourceTypesRule resourceTypes = new ResourceTypesRule().setRootQualifiers(PROJECT);

  private WsActionTester ws = new WsActionTester(new TagsAction(issueIndex, issueIndexSyncProgressChecker, db.getDbClient(), new ComponentFinder(db.getDbClient(), resourceTypes)));

  @Test
  public void search_tags() {
    RuleDto rule = db.rules().insertIssueRule();
    ComponentDto project = db.components().insertPrivateProject();
    db.issues().insertIssue(rule, project, project, issue -> issue.setTags(asList("tag1", "tag2")));
    db.issues().insertIssue(rule, project, project, issue -> issue.setTags(asList("tag3", "tag4", "tag5")));
    indexIssues();
    permissionIndexer.allowOnlyAnyone(project);

    TagsResponse result = ws.newRequest().executeProtobuf(TagsResponse.class);

    assertThat(result.getTagsList()).containsExactly("tag1", "tag2", "tag3", "tag4", "tag5");
    verify(issueIndexSyncProgressChecker).checkIfIssueSyncInProgress(any());
  }

  @Test
  public void search_tags_ignores_hotspots() {
    ComponentDto project = db.components().insertPrivateProject();
    RuleDto issueRule = db.rules().insertIssueRule();
    RuleDto hotspotRule = db.rules().insertHotspotRule();
    Consumer<IssueDto> setTags = issue -> issue.setTags(asList("tag1", "tag2"));
    db.issues().insertIssue(issueRule, project, project, setTags);
    db.issues().insertHotspot(hotspotRule, project, project, setTags);
    indexIssues();
    permissionIndexer.allowOnlyAnyone(project);
    TestRequest testRequest = ws.newRequest();

    assertThat(tagListOf(testRequest)).containsExactly("tag1", "tag2");
  }

  @Test
  public void search_tags_by_query() {
    RuleDto rule = db.rules().insertIssueRule();
    ComponentDto project = db.components().insertPrivateProject();
    db.issues().insertIssue(rule, project, project, issue -> issue.setTags(asList("tag1", "tag2")));
    db.issues().insertIssue(rule, project, project, issue -> issue.setTags(asList("tag12", "tag4", "tag5")));
    indexIssues();
    permissionIndexer.allowOnlyAnyone(project);

    assertThat(tagListOf(ws.newRequest().setParam("q", "ag1"))).containsExactly("tag1", "tag12");
  }

  @Test
  public void search_tags_by_query_ignores_hotspots() {
    RuleDto issueRule = db.rules().insertIssueRule();
    RuleDto hotspotRule = db.rules().insertHotspotRule();
    ComponentDto project = db.components().insertPrivateProject();
    db.issues().insertIssue(issueRule, project, project, issue -> issue.setTags(asList("tag1", "tag2")));
    db.issues().insertHotspot(hotspotRule, project, project, issue -> issue.setTags(asList("tag1", "tag12", "tag4", "tag5")));
    indexIssues();
    permissionIndexer.allowOnlyAnyone(project);
    TestRequest testRequest = ws.newRequest();

    assertThat(tagListOf(testRequest)).containsExactly("tag1", "tag2");
    assertThat(tagListOf(testRequest.setParam("q", "ag1"))).containsExactly("tag1");
    assertThat(tagListOf(testRequest.setParam("q", "tag1"))).containsExactly("tag1");
    assertThat(tagListOf(testRequest.setParam("q", "tag12"))).isEmpty();
    assertThat(tagListOf(testRequest.setParam("q", "tag2"))).containsOnly("tag2");
    assertThat(tagListOf(testRequest.setParam("q", "ag5"))).isEmpty();
  }

  @Test
  public void search_tags_by_project() {
    RuleDto rule = db.rules().insertIssueRule();
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    db.issues().insertIssue(rule, project1, project1, issue -> issue.setTags(singletonList("tag1")));
    db.issues().insertIssue(rule, project2, project2, issue -> issue.setTags(singletonList("tag2")));
    indexIssues();
    permissionIndexer.allowOnlyAnyone(project1, project2);

    assertThat(tagListOf(ws.newRequest()
      .setParam("project", project1.getKey()))).containsExactly("tag1");
    verify(issueIndexSyncProgressChecker).checkIfComponentNeedIssueSync(any(), eq(project1.getKey()));
  }

  @Test
  public void search_tags_by_branch_equals_main_branch() {
    RuleDto rule = db.rules().insertIssueRule();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));
    db.issues().insertIssue(rule, project, project, issue -> issue.setTags(asList("tag1", "tag2")));
    db.issues().insertIssue(rule, branch, branch, issue -> issue.setTags(asList("tag12", "tag4", "tag5")));
    indexIssues();
    permissionIndexer.allowOnlyAnyone(project, branch);

    assertThat(tagListOf(ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", project.uuid()))).containsExactly("tag1", "tag2");
  }

  @Test
  public void search_tags_by_branch() {
    RuleDto rule = db.rules().insertIssueRule();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));
    db.issues().insertIssue(rule, project, project, issue -> issue.setTags(asList("tag1", "tag2")));
    db.issues().insertIssue(rule, branch, branch, issue -> issue.setTags(asList("tag12", "tag4", "tag5")));
    indexIssues();
    permissionIndexer.allowOnlyAnyone(project, branch);

    assertThat(tagListOf(ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "my_branch"))).containsExactly("tag12", "tag4", "tag5");
  }

  @Test
  public void search_tags_by_branch_not_exist_fall_back_to_main_branch() {
    RuleDto rule = db.rules().insertIssueRule();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));
    db.issues().insertIssue(rule, project, project, issue -> issue.setTags(asList("tag1", "tag2")));
    db.issues().insertIssue(rule, branch, branch, issue -> issue.setTags(asList("tag12", "tag4", "tag5")));
    indexIssues();
    permissionIndexer.allowOnlyAnyone(project, branch);

    assertThat(tagListOf(ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "not_exist"))).containsExactly("tag1", "tag2");
  }

  @Test
  public void search_all_tags_by_query() {
    RuleDto rule = db.rules().insertIssueRule();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));
    db.issues().insertIssue(rule, project, project, issue -> issue.setTags(asList("tag1", "tag2")));
    db.issues().insertIssue(rule, branch, branch, issue -> issue.setTags(asList("tag12", "tag4", "tag5")));
    indexIssues();
    permissionIndexer.allowOnlyAnyone(project, branch);

    assertThat(tagListOf(ws.newRequest()
      .setParam("q", "tag1")
      .setParam("all", "true"))).containsExactly("tag1", "tag12");
  }

  @Test
  public void search_tags_by_project_ignores_hotspots() {
    RuleDto issueRule = db.rules().insertIssueRule();
    RuleDto hotspotRule = db.rules().insertHotspotRule();
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    db.issues().insertHotspot(hotspotRule, project1, project1, issue -> issue.setTags(singletonList("tag1")));
    db.issues().insertIssue(issueRule, project1, project1, issue -> issue.setTags(singletonList("tag2")));
    db.issues().insertHotspot(hotspotRule, project2, project2, issue -> issue.setTags(singletonList("tag3")));
    db.issues().insertIssue(issueRule, project2, project2, issue -> issue.setTags(singletonList("tag4")));
    indexIssues();
    permissionIndexer.allowOnlyAnyone(project1, project2);

    assertThat(tagListOf(ws.newRequest()
      .setParam("project", project1.getKey()))).containsExactly("tag2");
  }

  @Test
  public void search_tags_by_portfolio() {
    ComponentDto portfolio = db.components().insertPrivatePortfolio();
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertComponent(newProjectCopy(project, portfolio));
    permissionIndexer.allowOnlyAnyone(project);
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, project, project, issue -> issue.setTags(singletonList("cwe")));
    indexIssues();
    viewIndexer.indexAll();

    assertThat(tagListOf(ws.newRequest().setParam("project", portfolio.getKey()))).containsExactly("cwe");
  }

  @Test
  public void search_tags_by_portfolio_ignores_hotspots() {
    ComponentDto portfolio = db.components().insertPrivatePortfolio();
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertComponent(newProjectCopy(project, portfolio));
    permissionIndexer.allowOnlyAnyone(project);
    RuleDto issueRule = db.rules().insertIssueRule();
    RuleDto hotspotRule = db.rules().insertHotspotRule();
    db.issues().insertHotspot(hotspotRule, project, project, issue -> issue.setTags(singletonList("cwe")));
    db.issues().insertIssue(issueRule, project, project, issue -> issue.setTags(singletonList("foo")));
    indexIssues();
    viewIndexer.indexAll();

    assertThat(tagListOf(ws.newRequest().setParam("project", portfolio.getKey()))).containsExactly("foo");
  }

  @Test
  public void search_tags_by_application() {
    ComponentDto application = db.components().insertPrivateApplication();
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertComponent(newProjectCopy(project, application));
    permissionIndexer.allowOnlyAnyone(project);
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, project, project, issue -> issue.setTags(singletonList("cwe")));
    indexIssues();
    viewIndexer.indexAll();

    assertThat(tagListOf(ws.newRequest().setParam("project", application.getKey()))).containsExactly("cwe");
  }

  @Test
  public void search_tags_by_application_ignores_hotspots() {
    ComponentDto application = db.components().insertPrivateApplication();
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertComponent(newProjectCopy(project, application));
    permissionIndexer.allowOnlyAnyone(project);
    RuleDto issueRule = db.rules().insertIssueRule();
    RuleDto hotspotRule = db.rules().insertHotspotRule();
    db.issues().insertIssue(issueRule, project, project, issue -> issue.setTags(singletonList("cwe")));
    db.issues().insertHotspot(hotspotRule, project, project, issue -> issue.setTags(singletonList("foo")));
    indexIssues();
    viewIndexer.indexAll();

    assertThat(tagListOf(ws.newRequest().setParam("project", application.getKey()))).containsExactly("cwe");
  }

  @Test
  public void return_limited_size() {
    RuleDto rule = db.rules().insertIssueRule();
    ComponentDto project = db.components().insertPrivateProject();
    db.issues().insertIssue(rule, project, project, issue -> issue.setTags(asList("tag1", "tag2")));
    db.issues().insertIssue(rule, project, project, issue -> issue.setTags(asList("tag3", "tag4", "tag5")));
    indexIssues();
    permissionIndexer.allowOnlyAnyone(project);

    TagsResponse result = ws.newRequest()
      .setParam("ps", "2")
      .executeProtobuf(TagsResponse.class);

    assertThat(result.getTagsList()).containsExactly("tag1", "tag2");
  }

  @Test
  public void do_not_return_issues_without_permission() {
    RuleDto rule = db.rules().insertIssueRule();
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    db.issues().insertIssue(rule, project1, project1, issue -> issue.setTags(asList("tag1", "tag2")));
    db.issues().insertIssue(rule, project2, project2, issue -> issue.setTags(asList("tag3", "tag4", "tag5")));
    indexIssues();
    // Project 2 is not visible to current user
    permissionIndexer.allowOnlyAnyone(project1);

    TagsResponse result = ws.newRequest().executeProtobuf(TagsResponse.class);

    assertThat(result.getTagsList()).containsExactly("tag1", "tag2");
  }

  @Test
  public void empty_list() {
    TagsResponse result = ws.newRequest().executeProtobuf(TagsResponse.class);

    assertThat(result.getTagsList()).isEmpty();
  }

  private void indexIssues() {
    issueIndexer.indexAllIssues();
  }

  @Test
  public void fail_when_project_parameter_does_not_match_a_project() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    indexIssues();
    permissionIndexer.allowOnlyAnyone(project, project);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("project", file.getKey())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(format("Component '%s' must be a project", file.getKey()));
  }

  @Test
  public void json_example() {
    RuleDto rule = db.rules().insertIssueRule();
    ComponentDto project = db.components().insertPrivateProject();
    db.issues().insertIssue(rule, project, project, issue -> issue.setTags(asList("convention", "security")));
    db.issues().insertIssue(rule, project, project, issue -> issue.setTags(singletonList("cwe")));
    indexIssues();
    permissionIndexer.allowOnlyAnyone(project);

    String result = ws.newRequest().execute().getInput();

    assertThat(ws.getDef().responseExampleAsString()).isNotNull();
    assertJson(result).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void definition() {
    userSession.logIn();
    Action action = ws.getDef();
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params())
      .extracting(Param::key, Param::defaultValue, Param::since, Param::isRequired, Param::isInternal)
      .containsExactlyInAnyOrder(
        tuple("q", null, null, false, false),
        tuple("ps", "10", null, false, false),
        tuple("project", null, "7.4", false, false),
        tuple("branch", null, "9.2", false, false),
        tuple("all", "false", "9.2", false, false));
  }

  private static ProtocolStringList tagListOf(TestRequest testRequest) {
    return testRequest.executeProtobuf(TagsResponse.class).getTagsList();
  }

}
