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

import com.google.common.collect.ImmutableMap;
import java.time.Clock;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Languages;
import org.sonar.core.rule.RuleType;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.avatar.AvatarResolverImpl;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.TextRangeResponseFormatter;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.issue.index.IssueQueryFactory;
import org.sonar.server.permission.index.PermissionIndexer;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Common.FacetValue;
import org.sonarqube.ws.Issues.SearchWsResponse;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.sonar.api.server.ws.WebService.Param.FACETS;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.server.tester.UserSessionRule.standalone;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_FILES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PROJECTS;

class SearchActionFacetsIT {

  private static final String[] ISSUE_STATUSES = Issue.STATUSES.stream().filter(s -> !Issue.STATUS_TO_REVIEW.equals(s)).filter(s -> !Issue.STATUS_REVIEWED.equals(s))
    .toArray(String[]::new);

  @RegisterExtension
  private final UserSessionRule userSession = standalone();
  @RegisterExtension
  private final DbTester db = DbTester.create();
  @RegisterExtension
  private final EsTester es = EsTester.create();

  private final Configuration config = mock(Configuration.class);

  private final IssueIndex issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSession,
    new WebAuthorizationTypeSupport(userSession), config);
  private final IssueIndexer issueIndexer = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()),
    null);
  private final PermissionIndexer permissionIndexer = new PermissionIndexer(db.getDbClient(), es.client(), issueIndexer);
  private final IssueQueryFactory issueQueryFactory = new IssueQueryFactory(db.getDbClient(), Clock.systemUTC(), userSession);
  private final SearchResponseLoader searchResponseLoader = new SearchResponseLoader(userSession, db.getDbClient(),
    new TransitionService(userSession, null));
  private final Languages languages = new Languages();
  private final UserResponseFormatter userFormatter = new UserResponseFormatter(new AvatarResolverImpl());
  private final SearchResponseFormat searchResponseFormat = new SearchResponseFormat(new Durations(), languages,
    new TextRangeResponseFormatter(), userFormatter);
  private final IssueIndexSyncProgressChecker issueIndexSyncProgressChecker = new IssueIndexSyncProgressChecker(db.getDbClient());
  private final WsActionTester ws = new WsActionTester(
    new SearchAction(userSession, issueIndex, issueQueryFactory, issueIndexSyncProgressChecker, searchResponseLoader, searchResponseFormat, System2.INSTANCE, db.getDbClient()));

  @Test
  void display_all_facets() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insertIssueRule();
    UserDto user = db.users().insertUser();
    db.issues().insertIssue(rule, project, file, i -> i
      .setSeverity("MAJOR")
      .setStatus("OPEN")
      .setType(RuleType.CODE_SMELL)
      .setEffort(10L)
      .setAssigneeUuid(user.getUuid()));
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENTS, project.getKey())
      .setParam(FACETS, "severities,statuses,resolutions,rules,types,languages,projects,files,assignees")
      .executeProtobuf(SearchWsResponse.class);

    Map<String, Number> expectedStatuses = ImmutableMap.<String, Number>builder().put("OPEN", 1L).put("CONFIRMED", 0L)
      .put("REOPENED", 0L).put("RESOLVED", 0L).put("CLOSED", 0L).build();

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, facet -> facet.getValuesList().stream().collect(toMap(FacetValue::getVal, FacetValue::getCount)))
      .containsExactlyInAnyOrder(
        tuple("severities", of("INFO", 0L, "MINOR", 0L, "MAJOR", 1L, "CRITICAL", 0L, "BLOCKER", 0L)),
        tuple("statuses", expectedStatuses),
        tuple("resolutions", of("", 1L, "FALSE-POSITIVE", 0L, "FIXED", 0L, "REMOVED", 0L, "WONTFIX", 0L)),
        tuple("rules", of(rule.getKey().toString(), 1L)),
        tuple("types", of("CODE_SMELL", 1L, "BUG", 0L, "VULNERABILITY", 0L)),
        tuple("languages", of(rule.getLanguage(), 1L)),
        tuple("projects", of(project.getKey(), 1L)),
        tuple("files", of(file.path(), 1L)),
        tuple("assignees", of("", 0L, user.getLogin(), 1L)));
  }

  @Test
  void display_projects_facet() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, project, file);
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(WebService.Param.FACETS, "projects")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, facet -> facet.getValuesList().stream().collect(toMap(FacetValue::getVal, FacetValue::getCount)))
      .containsExactlyInAnyOrder(tuple("projects", of(project.getKey(), 1L)));
  }

  @Test
  void projects_facet_is_sticky() {
    ComponentDto project1 = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto project3 = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file1 = db.components().insertComponent(newFileDto(project1));
    ComponentDto file2 = db.components().insertComponent(newFileDto(project2));
    ComponentDto file3 = db.components().insertComponent(newFileDto(project3));
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, project1, file1);
    db.issues().insertIssue(rule, project2, file2);
    db.issues().insertIssue(rule, project3, file3);
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_PROJECTS, project1.getKey())
      .setParam(WebService.Param.FACETS, "projects")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, facet -> facet.getValuesList().stream().collect(toMap(FacetValue::getVal, FacetValue::getCount)))
      .containsExactlyInAnyOrder(tuple("projects", of(project1.getKey(), 1L, project2.getKey(), 1L, project3.getKey(), 1L)));
  }

  @Test
  void display_directory_facet_using_project() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "src/main/java/dir"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, directory));
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, project, file);
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam("resolved", "false")
      .setParam(PARAM_COMPONENTS, project.getKey())
      .setParam(WebService.Param.FACETS, "directories")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, facet -> facet.getValuesList().stream().collect(toMap(FacetValue::getVal, FacetValue::getCount)))
      .containsExactlyInAnyOrder(tuple("directories", of(directory.path(), 1L)));
  }

  @Test
  void fail_to_display_directory_facet_when_no_project_is_set() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "src"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, directory));
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, project, file);
    indexPermissions();
    indexIssues();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(WebService.Param.FACETS, "directories")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Facet(s) 'directories' require to also filter by project");
  }

  @Test
  void display_files_facet_with_project() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file1 = db.components().insertComponent(newFileDto(project));
    ComponentDto file2 = db.components().insertComponent(newFileDto(project));
    ComponentDto file3 = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, project, file1);
    db.issues().insertIssue(rule, project, file2);
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENTS, project.getKey())
      .setParam(PARAM_FILES, file1.path())
      .setParam(WebService.Param.FACETS, "files")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, facet -> facet.getValuesList().stream().collect(toMap(FacetValue::getVal, FacetValue::getCount)))
      .containsExactlyInAnyOrder(tuple("files", of(file1.path(), 1L, file2.path(), 1L)));
  }

  @Test
  void fail_to_display_fileUuids_facet_when_no_project_is_set() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, project, file);
    indexPermissions();
    indexIssues();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_FILES, file.path())
        .setParam(WebService.Param.FACETS, "files")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Facet(s) 'files' require to also filter by project");
  }

  @Test
  void check_facets_max_size_for_issues() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    Random random = new Random();
    IntStream.rangeClosed(1, 110)
      .forEach(index -> {
        UserDto user = db.users().insertUser();
        ComponentDto directory = db.components().insertComponent(newDirectory(project, "dir" + index));
        ComponentDto file = db.components().insertComponent(newFileDto(project, directory));

        RuleDto rule = db.rules().insertIssueRule();
        db.issues().insertIssue(rule, project, file, i -> i.setAssigneeUuid(user.getUuid())
          .setStatus(Issue.STATUS_RESOLVED)
          .setResolution(Issue.RESOLUTION_FIXED)
          .setType(rule.getType()));
      });

    // insert some hotspots which should be filtered by default
    IntStream.rangeClosed(201, 230)
      .forEach(index -> {
        UserDto user = db.users().insertUser();
        ComponentDto directory = db.components().insertComponent(newDirectory(project, "dir" + index));
        ComponentDto file = db.components().insertComponent(newFileDto(project, directory));

        db.issues().insertHotspot(project, file, i -> i.setAssigneeUuid(user.getUuid())
          .setStatus(random.nextBoolean() ? Issue.STATUS_TO_REVIEW : Issue.STATUS_REVIEWED));
      });

    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENTS, project.getKey())
      .setParam(FACETS, "files,directories,statuses,resolutions,severities,types,rules,languages,assignees")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, Common.Facet::getValuesCount)
      .containsExactlyInAnyOrder(
        tuple("files", 100),
        tuple("directories", 100),
        tuple("rules", 100),
        tuple("languages", 100),
        // Assignees contains one additional element : it's the empty string that will return number of unassigned issues
        tuple("assignees", 101),
        // Following facets returned fixed number of elements
        tuple("statuses", 5),
        tuple("resolutions", 5),
        tuple("severities", 5),
        tuple("types", 3));
  }

  @Test
  void check_projects_facet_max_size() {
    RuleDto rule = db.rules().insertIssueRule();
    IntStream.rangeClosed(1, 110)
      .forEach(i -> {
        ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
        db.issues().insertIssue(rule, project, project);
      });
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(FACETS, "projects")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getPaging().getTotal()).isEqualTo(110);
    assertThat(response.getFacets().getFacets(0).getValuesCount()).isEqualTo(100);
  }

  @Test
  void display_zero_valued_facets_for_selected_items_having_no_issue() {
    ComponentDto project1 = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file1 = db.components().insertComponent(newFileDto(project1));
    ComponentDto file2 = db.components().insertComponent(newFileDto(project1));
    RuleDto rule1 = db.rules().insertIssueRule();
    RuleDto rule2 = db.rules().insertIssueRule();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.issues().insertIssue(rule1, project1, file1, i -> i
      .setSeverity("MAJOR")
      .setStatus("OPEN")
      .setResolution(null)
      .setType(RuleType.CODE_SMELL)
      .setEffort(10L)
      .setAssigneeUuid(user1.getUuid()));
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_PROJECTS, project1.getKey() + "," + project2.getKey())
      .setParam(PARAM_FILES, file1.path() + "," + file2.path())
      .setParam("rules", rule1.getKey().toString() + "," + rule2.getKey().toString())
      .setParam("severities", "MAJOR,MINOR")
      .setParam("languages", rule1.getLanguage() + "," + rule2.getLanguage())
      .setParam("assignees", user1.getLogin() + "," + user2.getLogin())
      .setParam(FACETS, "severities,statuses,resolutions,rules,types,languages,projects,files,assignees")
      .executeProtobuf(SearchWsResponse.class);

    Map<String, Number> expectedStatuses = ImmutableMap.<String, Number>builder().put("OPEN", 1L).put("CONFIRMED", 0L)
      .put("REOPENED", 0L).put("RESOLVED", 0L).put("CLOSED", 0L).build();

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, facet -> facet.getValuesList().stream().collect(toMap(FacetValue::getVal, FacetValue::getCount)))
      .containsExactlyInAnyOrder(
        tuple("severities", of("INFO", 0L, "MINOR", 0L, "MAJOR", 1L, "CRITICAL", 0L, "BLOCKER", 0L)),
        tuple("statuses", expectedStatuses),
        tuple("resolutions", of("", 1L, "FALSE-POSITIVE", 0L, "FIXED", 0L, "REMOVED", 0L, "WONTFIX", 0L)),
        tuple("rules", of(rule1.getKey().toString(), 1L, rule2.getKey().toString(), 0L)),
        tuple("types", of("CODE_SMELL", 1L, "BUG", 0L, "VULNERABILITY", 0L)),
        tuple("languages", of(rule1.getLanguage(), 1L, rule2.getLanguage(), 0L)),
        tuple("projects", of(project1.getKey(), 1L, project2.getKey(), 0L)),
        tuple("files", of(file1.path(), 1L, file2.path(), 0L)),
        tuple("assignees", of("", 0L, user1.getLogin(), 1L, user2.getLogin(), 0L)));
  }

  @Test
  void assignedToMe_facet_must_escape_login_of_authenticated_user() {
    // login looks like an invalid regexp
    UserDto user = db.users().insertUser(u -> u.setLogin("foo["));
    userSession.logIn(user);

    // should not fail
    SearchWsResponse response = ws.newRequest()
      .setParam(FACETS, "assigned_to_me")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, facet -> facet.getValuesList().stream().collect(toMap(FacetValue::getVal, FacetValue::getCount)))
      .containsExactlyInAnyOrder(
        tuple("assigned_to_me", of("foo[", 0L)));
  }

  @Test
  void assigned_to_me_facet_is_sticky_relative_to_assignees() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insertIssueRule();
    UserDto john = db.users().insertUser();
    UserDto alice = db.users().insertUser();
    db.issues().insertIssue(rule, project, file, i -> i.setAssigneeUuid(john.getUuid()));
    db.issues().insertIssue(rule, project, file, i -> i.setAssigneeUuid(alice.getUuid()));
    db.issues().insertIssue(rule, project, file, i -> i.setAssigneeUuid(null));
    indexIssues();
    userSession.logIn(john);

    SearchWsResponse response = ws.newRequest()
      .setParam("resolved", "false")
      .setParam("assignees", alice.getLogin())
      .setParam(FACETS, "assignees,assigned_to_me")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, facet -> facet.getValuesList().stream().collect(toMap(FacetValue::getVal, FacetValue::getCount)))
      .containsExactlyInAnyOrder(
        tuple("assignees", of(john.getLogin(), 1L, alice.getLogin(), 1L, "", 1L)),
        tuple("assigned_to_me", of(john.getLogin(), 1L)));
  }

  private void indexPermissions() {
    permissionIndexer.indexAll(permissionIndexer.getIndexTypes());
  }

  private void indexIssues() {
    issueIndexer.indexAllIssues();
  }

}
