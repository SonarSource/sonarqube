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
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.issue.index.AsyncIssueIndexing;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.view.index.ViewIndexer;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Issues.AuthorsResponse;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.test.JsonAssert.assertJson;

public class AuthorsActionTest {
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final IssueIndex issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSession, new WebAuthorizationTypeSupport(userSession));
  private final AsyncIssueIndexing asyncIssueIndexing = mock(AsyncIssueIndexing.class);
  private final IssueIndexer issueIndexer = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()), asyncIssueIndexing);
  private final PermissionIndexerTester permissionIndexer = new PermissionIndexerTester(es, issueIndexer);
  private final ViewIndexer viewIndexer = new ViewIndexer(db.getDbClient(), es.client());
  private final IssueIndexSyncProgressChecker issueIndexSyncProgressChecker = mock(IssueIndexSyncProgressChecker.class);
  private final ResourceTypesRule resourceTypes = new ResourceTypesRule().setRootQualifiers(PROJECT);
  private final WsActionTester ws = new WsActionTester(new AuthorsAction(userSession, db.getDbClient(), issueIndex,
    issueIndexSyncProgressChecker, new ComponentFinder(db.getDbClient(), resourceTypes)));

  @Test
  public void search_authors() {
    String leia = "leia.organa";
    String luke = "luke.skywalker";
    ComponentDto project = db.components().insertPrivateProject();
    permissionIndexer.allowOnlyAnyone(project);
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, project, project, issue -> issue.setAuthorLogin(leia));
    db.issues().insertIssue(rule, project, project, issue -> issue.setAuthorLogin(luke));
    indexIssues();
    userSession.logIn();

    AuthorsResponse result = ws.newRequest().executeProtobuf(AuthorsResponse.class);

    assertThat(result.getAuthorsList()).containsExactlyInAnyOrder(leia, luke);
    verify(issueIndexSyncProgressChecker).checkIfIssueSyncInProgress(any());
  }

  @Test
  public void search_authors_by_query() {
    String leia = "leia.organa";
    String luke = "luke.skywalker";
    ComponentDto project = db.components().insertPrivateProject();
    permissionIndexer.allowOnlyAnyone(project);
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, project, project, issue -> issue.setAuthorLogin(leia));
    db.issues().insertIssue(rule, project, project, issue -> issue.setAuthorLogin(luke));
    indexIssues();
    userSession.logIn();

    AuthorsResponse result = ws.newRequest()
      .setParam(TEXT_QUERY, "leia")
      .executeProtobuf(AuthorsResponse.class);

    assertThat(result.getAuthorsList())
      .containsExactlyInAnyOrder(leia)
      .doesNotContain(luke);
  }

  @Test
  public void search_authors_by_project() {
    String leia = "leia.organa";
    String luke = "luke.skywalker";
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    permissionIndexer.allowOnlyAnyone(project1, project2);
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, project1, project1, issue -> issue.setAuthorLogin(leia));
    db.issues().insertIssue(rule, project2, project2, issue -> issue.setAuthorLogin(luke));
    indexIssues();
    userSession.logIn();

    assertThat(ws.newRequest()
      .setParam("project", project1.getKey())
      .executeProtobuf(AuthorsResponse.class).getAuthorsList())
        .containsExactlyInAnyOrder(leia);
    assertThat(ws.newRequest()
      .setParam("project", project1.getKey())
      .setParam(TEXT_QUERY, "eia")
      .executeProtobuf(AuthorsResponse.class).getAuthorsList())
        .containsExactlyInAnyOrder(leia);
    assertThat(ws.newRequest()
      .setParam("project", project1.getKey())
      .setParam(TEXT_QUERY, "luke")
      .executeProtobuf(AuthorsResponse.class).getAuthorsList())
        .isEmpty();

    verify(issueIndexSyncProgressChecker, times(3)).checkIfComponentNeedIssueSync(any(), eq(project1.getKey()));
  }

  @Test
  public void search_authors_by_portfolio() {
    String leia = "leia.organa";
    ComponentDto portfolio = db.components().insertPrivatePortfolio();
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertComponent(newProjectCopy(project, portfolio));
    permissionIndexer.allowOnlyAnyone(project);
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, project, project, issue -> issue.setAuthorLogin(leia));
    indexIssues();
    viewIndexer.indexAll();
    userSession.logIn();

    assertThat(ws.newRequest()
      .setParam("project", portfolio.getKey())
      .executeProtobuf(AuthorsResponse.class).getAuthorsList())
        .containsExactlyInAnyOrder(leia);
  }

  @Test
  public void search_authors_by_application() {
    String leia = "leia.organa";
    ComponentDto application = db.components().insertPrivateApplication();
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertComponent(newProjectCopy(project, application));
    permissionIndexer.allowOnlyAnyone(project);
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, project, project, issue -> issue.setAuthorLogin(leia));
    indexIssues();
    viewIndexer.indexAll();
    userSession.logIn();

    assertThat(ws.newRequest()
      .setParam("project", application.getKey())
      .executeProtobuf(AuthorsResponse.class).getAuthorsList())
        .containsExactlyInAnyOrder(leia);
  }

  @Test
  public void set_page_size() {
    String han = "han.solo";
    String leia = "leia.organa";
    String luke = "luke.skywalker";
    ComponentDto project = db.components().insertPrivateProject();
    permissionIndexer.allowOnlyAnyone(project);
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, project, project, issue -> issue.setAuthorLogin(han));
    db.issues().insertIssue(rule, project, project, issue -> issue.setAuthorLogin(leia));
    db.issues().insertIssue(rule, project, project, issue -> issue.setAuthorLogin(luke));
    indexIssues();
    userSession.logIn();

    AuthorsResponse result = ws.newRequest()
      .setParam(PAGE_SIZE, "2")
      .executeProtobuf(AuthorsResponse.class);

    assertThat(result.getAuthorsList())
      .containsExactlyInAnyOrder(han, leia)
      .doesNotContain(luke);
  }

  @Test
  public void should_ignore_authors_of_hotspot() {
    String luke = "luke.skywalker";
    ComponentDto project = db.components().insertPrivateProject();
    permissionIndexer.allowOnlyAnyone(project);
    db.issues().insertHotspot(project, project, issue -> issue
      .setAuthorLogin(luke));
    indexIssues();
    userSession.logIn();

    AuthorsResponse result = ws.newRequest().executeProtobuf(AuthorsResponse.class);

    assertThat(result.getAuthorsList()).isEmpty();
  }

  private void indexIssues() {
    issueIndexer.indexAllIssues();
  }

  @Test
  public void fail_when_user_is_not_logged() {
    userSession.anonymous();

    assertThatThrownBy(() -> ws.newRequest().execute())
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_project_is_not_a_project() {
    userSession.logIn();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    permissionIndexer.allowOnlyAnyone(project);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("project", file.getKey())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(format("Component '%s' must be a project", file.getKey()));
  }

  @Test
  public void fail_when_project_does_not_exist() {
    userSession.logIn();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("project", "unknown")
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component key 'unknown' not found");
  }

  @Test
  public void json_example() {
    ComponentDto project = db.components().insertPrivateProject();
    permissionIndexer.allowOnlyAnyone(project);
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, project, project, issue -> issue.setAuthorLogin("luke.skywalker"));
    db.issues().insertIssue(rule, project, project, issue -> issue.setAuthorLogin("leia.organa"));
    indexIssues();
    userSession.logIn();

    String result = ws.newRequest().execute().getInput();

    assertThat(ws.getDef().responseExampleAsString()).isNotNull();
    assertJson(result).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("authors");
    assertThat(definition.since()).isEqualTo("5.1");
    assertThat(definition.isPost()).isFalse();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.responseExampleAsString()).isNotEmpty();

    assertThat(definition.params())
      .extracting(Param::key, Param::isRequired, Param::isInternal)
      .containsExactlyInAnyOrder(
        tuple("q", false, false),
        tuple("ps", false, false),
        tuple("project", false, false));

    assertThat(definition.param("ps"))
      .extracting(Param::defaultValue, Param::maximumValue)
      .containsExactly("10", 100);
  }
}
