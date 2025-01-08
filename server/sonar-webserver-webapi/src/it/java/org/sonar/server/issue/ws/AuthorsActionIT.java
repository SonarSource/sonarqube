/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.rule.RuleDto;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.test.JsonAssert.assertJson;

class AuthorsActionIT {
  @RegisterExtension
  private final DbTester db = DbTester.create();
  @RegisterExtension
  private final EsTester es = EsTester.create();
  @RegisterExtension
  private final UserSessionRule userSession = UserSessionRule.standalone();

  private final Configuration config = mock(Configuration.class);

  private final IssueIndex issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSession,
    new WebAuthorizationTypeSupport(userSession), config);
  private final AsyncIssueIndexing asyncIssueIndexing = mock(AsyncIssueIndexing.class);
  private final IssueIndexer issueIndexer = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()), asyncIssueIndexing);
  private final PermissionIndexerTester permissionIndexer = new PermissionIndexerTester(es, issueIndexer);
  private final ViewIndexer viewIndexer = new ViewIndexer(db.getDbClient(), es.client());
  private final IssueIndexSyncProgressChecker issueIndexSyncProgressChecker = mock(IssueIndexSyncProgressChecker.class);
  private final WsActionTester ws = new WsActionTester(new AuthorsAction(userSession, db.getDbClient(), issueIndex,
    issueIndexSyncProgressChecker));

  @Test
  void search_authors() {
    String leia = "leia.organa";
    String luke = "luke.skywalker";
    ComponentDto mainBranchComponent = db.components().insertPrivateProject().getMainBranchComponent();
    permissionIndexer.allowOnlyAnyone(db.components().getProjectDtoByMainBranch(mainBranchComponent));
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, mainBranchComponent, mainBranchComponent, issue -> issue.setAuthorLogin(leia));
    db.issues().insertIssue(rule, mainBranchComponent, mainBranchComponent, issue -> issue.setAuthorLogin(luke));
    indexIssues();
    userSession.logIn();

    AuthorsResponse result = ws.newRequest().executeProtobuf(AuthorsResponse.class);

    assertThat(result.getAuthorsList()).containsExactlyInAnyOrder(leia, luke);
    verify(issueIndexSyncProgressChecker).checkIfIssueSyncInProgress(any());
  }

  @Test
  void search_authors_by_query() {
    String leia = "leia.organa";
    String luke = "luke.skywalker";
    ComponentDto mainBranchComponent = db.components().insertPrivateProject().getMainBranchComponent();
    permissionIndexer.allowOnlyAnyone(db.components().getProjectDtoByMainBranch(mainBranchComponent));
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, mainBranchComponent, mainBranchComponent, issue -> issue.setAuthorLogin(leia));
    db.issues().insertIssue(rule, mainBranchComponent, mainBranchComponent, issue -> issue.setAuthorLogin(luke));
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
  void search_authors_by_project() {
    String leia = "leia.organa";
    String luke = "luke.skywalker";
    ComponentDto mainBranch1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto mainBranch2 = db.components().insertPrivateProject().getMainBranchComponent();
    permissionIndexer.allowOnlyAnyone(db.components().getProjectDtoByMainBranch(mainBranch1), db.components().getProjectDtoByMainBranch(mainBranch2));
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, mainBranch1, mainBranch1, issue -> issue.setAuthorLogin(leia));
    db.issues().insertIssue(rule, mainBranch2, mainBranch2, issue -> issue.setAuthorLogin(luke));
    indexIssues();
    userSession.logIn();

    assertThat(ws.newRequest()
      .setParam("project", mainBranch1.getKey())
      .executeProtobuf(AuthorsResponse.class).getAuthorsList())
      .containsExactlyInAnyOrder(leia);
    assertThat(ws.newRequest()
      .setParam("project", mainBranch1.getKey())
      .setParam(TEXT_QUERY, "eia")
      .executeProtobuf(AuthorsResponse.class).getAuthorsList())
      .containsExactlyInAnyOrder(leia);
    assertThat(ws.newRequest()
      .setParam("project", mainBranch1.getKey())
      .setParam(TEXT_QUERY, "luke")
      .executeProtobuf(AuthorsResponse.class).getAuthorsList())
      .isEmpty();

    verify(issueIndexSyncProgressChecker, times(3)).checkIfComponentNeedIssueSync(any(), eq(mainBranch1.getKey()));
  }

  @Test
  void search_authors_by_portfolio() {
    String leia = "leia.organa";
    ComponentDto portfolio = db.components().insertPrivatePortfolio();
    ComponentDto mainBranch = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertComponent(newProjectCopy(mainBranch, portfolio));
    permissionIndexer.allowOnlyAnyone(db.components().getProjectDtoByMainBranch(mainBranch));
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, mainBranch, mainBranch, issue -> issue.setAuthorLogin(leia));
    indexIssues();
    viewIndexer.indexAll();
    userSession.logIn();

    assertThat(ws.newRequest()
      .setParam("project", portfolio.getKey())
      .executeProtobuf(AuthorsResponse.class).getAuthorsList())
      .containsExactlyInAnyOrder(leia);
  }

  @Test
  void search_authors_by_application() {
    String leia = "leia.organa";
    ComponentDto appMainBranch = db.components().insertPrivateApplication().getMainBranchComponent();
    ComponentDto projectMainBranch = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertComponent(newProjectCopy(projectMainBranch, appMainBranch));
    permissionIndexer.allowOnlyAnyone(db.components().getProjectDtoByMainBranch(projectMainBranch));
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, projectMainBranch, projectMainBranch, issue -> issue.setAuthorLogin(leia));
    indexIssues();
    viewIndexer.indexAll();
    userSession.logIn();

    assertThat(ws.newRequest()
      .setParam("project", appMainBranch.getKey())
      .executeProtobuf(AuthorsResponse.class).getAuthorsList())
      .containsExactlyInAnyOrder(leia);
  }

  @Test
  void set_page_size() {
    String han = "han.solo";
    String leia = "leia.organa";
    String luke = "luke.skywalker";
    ComponentDto mainBranch = db.components().insertPrivateProject().getMainBranchComponent();
    permissionIndexer.allowOnlyAnyone(db.components().getProjectDtoByMainBranch(mainBranch));
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, mainBranch, mainBranch, issue -> issue.setAuthorLogin(han));
    db.issues().insertIssue(rule, mainBranch, mainBranch, issue -> issue.setAuthorLogin(leia));
    db.issues().insertIssue(rule, mainBranch, mainBranch, issue -> issue.setAuthorLogin(luke));
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
  void should_ignore_authors_of_hotspot() {
    String luke = "luke.skywalker";
    ComponentDto mainBranch = db.components().insertPrivateProject().getMainBranchComponent();
    permissionIndexer.allowOnlyAnyone(db.components().getProjectDtoByMainBranch(mainBranch));
    db.issues().insertHotspot(mainBranch, mainBranch, issue -> issue
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
  void fail_when_user_is_not_logged() {
    userSession.anonymous();

    assertThatThrownBy(() -> ws.newRequest().execute())
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void fail_when_project_does_not_exist() {
    userSession.logIn();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("project", "unknown")
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Entity not found: unknown");
  }

  @Test
  void json_example() {
    ComponentDto mainBranch = db.components().insertPrivateProject().getMainBranchComponent();
    permissionIndexer.allowOnlyAnyone(db.components().getProjectDtoByMainBranch(mainBranch));
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, mainBranch, mainBranch, issue -> issue.setAuthorLogin("luke.skywalker"));
    db.issues().insertIssue(rule, mainBranch, mainBranch, issue -> issue.setAuthorLogin("leia.organa"));
    indexIssues();
    userSession.logIn();

    String result = ws.newRequest().execute().getInput();

    assertThat(ws.getDef().responseExampleAsString()).isNotNull();
    assertJson(result).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  void definition() {
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
