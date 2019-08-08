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

import java.time.Clock;
import java.util.Arrays;
import java.util.Date;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.AvatarResolverImpl;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.issue.index.IssueQueryFactory;
import org.sonar.server.issue.workflow.FunctionExecutor;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.view.index.ViewIndexer;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Issues.Component;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.Issues.SearchWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.resources.Qualifiers.APP;
import static org.sonar.api.utils.DateUtils.addDays;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_02;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.component.BranchType.SHORT;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.component.ComponentTesting.newSubView;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.issue.IssueTesting.newIssue;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_KEYS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_DIRECTORIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_FILE_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_MODULE_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PROJECTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PROJECT_KEYS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PULL_REQUEST;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SINCE_LEAK_PERIOD;

public class SearchActionComponentsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();

  private DbClient dbClient = db.getDbClient();
  private IssueIndex issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSession, new WebAuthorizationTypeSupport(userSession));
  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), dbClient, new IssueIteratorFactory(dbClient));
  private ViewIndexer viewIndexer = new ViewIndexer(dbClient, es.client());
  private IssueQueryFactory issueQueryFactory = new IssueQueryFactory(dbClient, Clock.systemUTC(), userSession);
  private IssueFieldsSetter issueFieldsSetter = new IssueFieldsSetter();
  private IssueWorkflow issueWorkflow = new IssueWorkflow(new FunctionExecutor(issueFieldsSetter), issueFieldsSetter);
  private SearchResponseLoader searchResponseLoader = new SearchResponseLoader(userSession, dbClient, new TransitionService(userSession, issueWorkflow));
  private Languages languages = new Languages();
  private SearchResponseFormat searchResponseFormat = new SearchResponseFormat(new Durations(), languages, new AvatarResolverImpl());
  private PermissionIndexerTester permissionIndexer = new PermissionIndexerTester(es, issueIndexer);

  private WsActionTester ws = new WsActionTester(new SearchAction(userSession, issueIndex, issueQueryFactory, searchResponseLoader, searchResponseFormat,
    new MapSettings().asConfig(), System2.INSTANCE, dbClient));

  @Test
  public void search_all_issues_when_no_parameter() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto projectFile = db.components().insertComponent(newFileDto(project));
    IssueDto issue = db.issues().insertIssue(newIssue(rule, project, projectFile));
    allowAnyoneOnProjects(project);
    indexIssues();

    SearchWsResponse result = ws.newRequest().executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).extracting(Issues.Issue::getKey)
      .containsExactlyInAnyOrder(issue.getKey());
  }

  @Test
  public void issues_on_different_projects() {
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    OrganizationDto organization1 = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization1);
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    IssueDto issue1 = db.issues().insert(rule, project, file);
    OrganizationDto organization2 = db.organizations().insert();
    ComponentDto project2 = db.components().insertPublicProject(organization2);
    ComponentDto file2 = db.components().insertComponent(newFileDto(project2));
    IssueDto issue2 = db.issues().insert(rule, project2, file2);
    allowAnyoneOnProjects(project, project2);
    indexIssues();

    SearchWsResponse response = ws.newRequest().executeProtobuf(SearchWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getKey, Issue::getComponent, Issue::getProject)
      .containsExactlyInAnyOrder(
        tuple(issue1.getKey(), file.getKey(), project.getKey()),
        tuple(issue2.getKey(), file2.getKey(), project2.getKey()));
    assertThat(response.getComponentsList())
      .extracting(Component::getKey, Component::getEnabled)
      .containsExactlyInAnyOrder(tuple(project.getKey(), true), tuple(file.getKey(), true), tuple(project2.getKey(), true), tuple(file2.getKey(), true));
  }

  @Test
  public void search_by_module() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto module1 = db.components().insertComponent(newModuleDto(project));
    ComponentDto file1 = db.components().insertComponent(newFileDto(module1));
    ComponentDto module2 = db.components().insertComponent(newModuleDto(project));
    ComponentDto file2 = db.components().insertComponent(newFileDto(module2));
    RuleDefinitionDto rule = db.rules().insert();
    IssueDto issue1 = db.issues().insert(rule, project, file1);
    IssueDto issue2 = db.issues().insert(rule, project, file2);
    allowAnyoneOnProjects(project);
    indexIssues();

    assertThat(ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, module1.getKey())
      .executeProtobuf(SearchWsResponse.class).getIssuesList()).extracting(Issue::getKey)
        .containsExactlyInAnyOrder(issue1.getKey());

    assertThat(ws.newRequest()
      .setParam(PARAM_MODULE_UUIDS, module1.uuid())
      .executeProtobuf(SearchWsResponse.class).getIssuesList()).extracting(Issue::getKey)
        .containsExactlyInAnyOrder(issue1.getKey());
  }

  @Test
  public void do_not_return_module_key_on_single_module_projects() {
    ComponentDto project = db.components().insertPublicProject(p -> p.setDbKey("PK1"));
    ComponentDto module = db.components().insertComponent(newModuleDto("M1", project).setDbKey("MK1"));
    ComponentDto file = db.components().insertComponent(newFileDto(module, null, "F1").setDbKey("FK1"));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    db.issues().insert(rule, project, file, i -> i.setKee("ISSUE_IN_MODULE"));
    db.issues().insert(rule, project, project, i -> i.setKee("ISSUE_IN_ROOT_MODULE"));
    allowAnyoneOnProjects(project);
    indexIssues();

    SearchWsResponse searchResponse = ws.newRequest().executeProtobuf(SearchWsResponse.class);
    assertThat(searchResponse.getIssuesCount()).isEqualTo(2);

    for (Issue issue : searchResponse.getIssuesList()) {
      assertThat(issue.getProject()).isEqualTo("PK1");
      if (issue.getKey().equals("ISSUE_IN_MODULE")) {
        assertThat(issue.getSubProject()).isEqualTo("MK1");
      } else if (issue.getKey().equals("ISSUE_IN_ROOT_MODULE")) {
        assertThat(issue.hasSubProject()).isFalse();
      }
    }
  }

  @Test
  public void search_since_leak_period_on_project() {
    ComponentDto project = db.components().insertPublicProject(p -> p.setDbKey("PK1"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    db.components().insertSnapshot(project, a -> a.setPeriodDate(parseDateTime("2015-09-03T00:00:00+0100").getTime()));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    IssueDto issueAfterLeak = db.issues().insert(rule, project, file, i -> i.setKee(UUID_EXAMPLE_01)
      .setIssueCreationDate(parseDateTime("2015-09-04T00:00:00+0100"))
      .setIssueUpdateDate(parseDateTime("2015-10-04T00:00:00+0100")));
    IssueDto issueBeforeLeak = db.issues().insert(rule, project, file, i -> i.setKee(UUID_EXAMPLE_02)
      .setIssueCreationDate(parseDateTime("2014-09-04T00:00:00+0100"))
      .setIssueUpdateDate(parseDateTime("2015-10-04T00:00:00+0100")));
    allowAnyoneOnProjects(project);
    indexIssues();

    ws.newRequest()
      .setParam(PARAM_COMPONENT_UUIDS, project.uuid())
      .setParam(PARAM_SINCE_LEAK_PERIOD, "true")
      .execute()
      .assertJson(this.getClass(), "search_since_leak_period.json");
  }

  @Test
  public void search_since_leak_period_on_file_in_module_project() {
    ComponentDto project = db.components().insertPublicProject(p -> p.setDbKey("PK1"));
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto file = db.components().insertComponent(newFileDto(module, null, "F1").setDbKey("FK1"));
    db.components().insertSnapshot(project, a -> a.setPeriodDate(parseDateTime("2015-09-03T00:00:00+0100").getTime()));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    IssueDto issueAfterLeak = db.issues().insert(rule, project, file, i -> i.setKee(UUID_EXAMPLE_01)
      .setIssueCreationDate(parseDateTime("2015-09-04T00:00:00+0100"))
      .setIssueUpdateDate(parseDateTime("2015-10-04T00:00:00+0100")));
    IssueDto issueBeforeLeak = db.issues().insert(rule, project, file, i -> i.setKee(UUID_EXAMPLE_02)
      .setIssueCreationDate(parseDateTime("2014-09-04T00:00:00+0100"))
      .setIssueUpdateDate(parseDateTime("2015-10-04T00:00:00+0100")));
    allowAnyoneOnProjects(project);
    indexIssues();

    ws.newRequest()
      .setParam(PARAM_COMPONENT_UUIDS, project.uuid())
      .setParam(PARAM_FILE_UUIDS, file.uuid())
      .setParam(PARAM_SINCE_LEAK_PERIOD, "true")
      .execute()
      .assertJson(this.getClass(), "search_since_leak_period.json");
  }

  @Test
  public void search_by_file_uuid() {
    ComponentDto project = db.components().insertPublicProject(p -> p.setDbKey("PK1"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    IssueDto issue = db.issues().insert(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    allowAnyoneOnProjects(project);
    indexIssues();

    ws.newRequest()
      .setParam(PARAM_FILE_UUIDS, file.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_file_uuid.json");

    ws.newRequest()
      .setParam(PARAM_FILE_UUIDS, "unknown")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");

    ws.newRequest()
      .setParam(PARAM_COMPONENT_UUIDS, file.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_file_uuid.json");

    ws.newRequest()
      .setParam(PARAM_COMPONENT_UUIDS, "unknown")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");
  }

  @Test
  public void search_by_file_key() {
    ComponentDto project = db.components().insertPublicProject(p -> p.setDbKey("PK1"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    ComponentDto unitTest = db.components().insertComponent(newFileDto(project, null, "F2").setQualifier(Qualifiers.UNIT_TEST_FILE).setDbKey("FK2"));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    IssueDto issueOnFile = db.issues().insert(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    IssueDto issueOnTest = db.issues().insert(rule, project, unitTest, i -> i.setKee("2bd4eac2-b650-4037-80bc-7b1182fd47d4"));
    allowAnyoneOnProjects(project);
    indexIssues();

    ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, file.getKey())
      .execute()
      .assertJson(this.getClass(), "search_by_file_key.json");

    ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, unitTest.getKey())
      .execute()
      .assertJson(this.getClass(), "search_by_test_key.json");
  }

  @Test
  public void search_by_directory_path() {
    ComponentDto project = db.components().insertPublicProject(p -> p.setDbKey("PK1"));
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "D1", "src/main/java/dir"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "F1").setDbKey("FK1").setPath(directory.path() + "/MyComponent.java"));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    db.issues().insert(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    allowAnyoneOnProjects(project);
    indexIssues();

    ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, directory.getKey())
      .execute()
      .assertJson(this.getClass(), "search_by_file_uuid.json");

    ws.newRequest()
      .setParam(PARAM_DIRECTORIES, "unknown")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");

    ws.newRequest()
      .setParam(PARAM_DIRECTORIES, "src/main/java/dir")
      .execute()
      .assertJson(this.getClass(), "search_by_file_uuid.json");

    ws.newRequest()
      .setParam(PARAM_DIRECTORIES, "src/main/java")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");
  }

  @Test
  public void search_by_directory_path_in_different_modules() {
    ComponentDto project = db.components().insertPublicProject(p -> p.setDbKey("PK1"));
    ComponentDto module1 = db.components().insertComponent(newModuleDto("M1", project).setDbKey("MK1"));
    ComponentDto module2 = db.components().insertComponent(newModuleDto("M2", project).setDbKey("MK2"));
    ComponentDto directory1 = db.components().insertComponent(newDirectory(module1, "D1", "src/main/java/dir"));
    ComponentDto directory2 = db.components().insertComponent(newDirectory(module2, "D2", "src/main/java/dir"));
    ComponentDto file1 = db.components().insertComponent(newFileDto(module1, directory1, "F1").setDbKey("FK1").setPath(directory1.path() + "/MyComponent.java"));
    db.components().insertComponent(newFileDto(module2, directory2, "F2").setDbKey("FK2").setPath(directory2.path() + "/MyComponent.java"));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    db.issues().insert(rule, project, file1, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    allowAnyoneOnProjects(project);
    indexIssues();

    ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, directory1.getKey())
      .execute()
      .assertJson(this.getClass(), "search_by_directory_uuid.json");

    ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, directory2.getKey())
      .execute()
      .assertJson(this.getClass(), "no_issue.json");

    ws.newRequest()
      .setParam(PARAM_MODULE_UUIDS, module1.uuid())
      .setParam(PARAM_DIRECTORIES, "src/main/java/dir")
      .execute()
      .assertJson(this.getClass(), "search_by_directory_uuid.json");

    ws.newRequest()
      .setParam(PARAM_MODULE_UUIDS, module2.uuid())
      .setParam(PARAM_DIRECTORIES, "src/main/java/dir")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");

    ws.newRequest()
      .setParam(PARAM_DIRECTORIES, "src/main/java/dir")
      .execute()
      .assertJson(this.getClass(), "search_by_directory_uuid.json");

    ws.newRequest()
      .setParam(PARAM_DIRECTORIES, "src/main/java")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");
  }

  @Test
  public void search_by_view_uuid() {
    ComponentDto project = db.components().insertPublicProject(p -> p.setDbKey("PK1"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    ComponentDto view = db.components().insertComponent(newView(db.getDefaultOrganization(), "V1").setDbKey("MyView"));
    db.components().insertComponent(newProjectCopy(project, view));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    db.issues().insert(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    allowAnyoneOnProjects(project, view);
    indexIssuesAndViews();

    ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, view.getKey())
      .execute()
      .assertJson(this.getClass(), "search_by_view_uuid.json");
  }

  @Test
  public void search_by_sub_view_uuid() {
    ComponentDto project = db.components().insertPublicProject(p -> p.setDbKey("PK1"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    db.issues().insert(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    ComponentDto view = db.components().insertComponent(newView(db.getDefaultOrganization(), "V1").setDbKey("MyView"));
    ComponentDto subView = db.components().insertComponent(newSubView(view, "SV1", "MySubView"));
    db.components().insertComponent(newProjectCopy(project, subView));
    allowAnyoneOnProjects(project, view, subView);
    indexIssuesAndViews();

    ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, subView.getKey())
      .execute()
      .assertJson(this.getClass(), "search_by_view_uuid.json");
  }

  @Test
  public void search_by_sub_view_uuid_return_only_authorized_view() {
    ComponentDto project = db.components().insertPublicProject(p -> p.setDbKey("PK1"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    db.issues().insert(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    ComponentDto view = db.components().insertComponent(newView(db.getDefaultOrganization(), "V1").setDbKey("MyView"));
    ComponentDto subView = db.components().insertComponent(newSubView(view, "SV1", "MySubView"));
    db.components().insertComponent(newProjectCopy(project, subView));
    // User has no permission on the view, no issue will be returned
    allowAnyoneOnProjects(project);
    indexIssuesAndViews();

    ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, subView.getKey())
      .execute()
      .assertJson(this.getClass(), "no_issue.json");
  }

  @Test
  public void search_by_application_key() {
    ComponentDto application = db.components().insertPrivateApplication(db.getDefaultOrganization());
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    db.components().insertComponents(newProjectCopy(project1, application));
    db.components().insertComponents(newProjectCopy(project2, application));
    RuleDefinitionDto rule = db.rules().insert();
    IssueDto issue1 = db.issues().insert(rule, project1, project1);
    IssueDto issue2 = db.issues().insert(rule, project2, project2);
    allowAnyoneOnProjects(project1, project2, application);
    userSession.addProjectPermission(USER, application);
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, application.getDbKey())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issue1.getKey(), issue2.getKey());
  }

  @Test
  public void search_by_application_key_and_branch() {
    ComponentDto application = db.components().insertMainBranch(c -> c.setQualifier(APP).setDbKey("app"));
    ComponentDto applicationBranch1 = db.components().insertProjectBranch(application, a -> a.setKey("app-branch1"));
    ComponentDto applicationBranch2 = db.components().insertProjectBranch(application, a -> a.setKey("app-branch2"));
    ComponentDto project1 = db.components().insertPrivateProject(p -> p.setDbKey("prj1"));
    ComponentDto project1Branch1 = db.components().insertProjectBranch(project1);
    ComponentDto fileOnProject1Branch1 = db.components().insertComponent(newFileDto(project1Branch1));
    ComponentDto project1Branch2 = db.components().insertProjectBranch(project1);
    ComponentDto project2 = db.components().insertPrivateProject(p -> p.setDbKey("prj2"));
    db.components().insertComponents(newProjectCopy(project1Branch1, applicationBranch1));
    db.components().insertComponents(newProjectCopy(project2, applicationBranch1));
    db.components().insertComponents(newProjectCopy(project1Branch2, applicationBranch2));

    RuleDefinitionDto rule = db.rules().insert();
    IssueDto issueOnProject1 = db.issues().insert(rule, project1, project1);
    IssueDto issueOnProject1Branch1 = db.issues().insert(rule, project1Branch1, project1Branch1);
    IssueDto issueOnFileOnProject1Branch1 = db.issues().insert(rule, project1Branch1, fileOnProject1Branch1);
    IssueDto issueOnProject1Branch2 = db.issues().insert(rule, project1Branch2, project1Branch2);
    IssueDto issueOnProject2 = db.issues().insert(rule, project2, project2);
    allowAnyoneOnProjects(project1, project2, application);
    userSession.addProjectPermission(USER, application);
    indexIssuesAndViews();

    // All issues on applicationBranch1
    assertThat(ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, applicationBranch1.getKey())
      .setParam(PARAM_BRANCH, applicationBranch1.getBranch())
      .executeProtobuf(SearchWsResponse.class).getIssuesList())
        .extracting(Issue::getKey, Issue::getComponent, Issue::getProject, Issue::getBranch, Issue::hasBranch)
        .containsExactlyInAnyOrder(
          tuple(issueOnProject1Branch1.getKey(), project1Branch1.getKey(), project1Branch1.getKey(), project1Branch1.getBranch(), true),
          tuple(issueOnFileOnProject1Branch1.getKey(), fileOnProject1Branch1.getKey(), project1Branch1.getKey(), project1Branch1.getBranch(), true),
          tuple(issueOnProject2.getKey(), project2.getKey(), project2.getKey(), "", false));

    // Issues on project1Branch1
    assertThat(ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, applicationBranch1.getKey())
      .setParam(PARAM_PROJECTS, project1.getKey())
      .setParam(PARAM_BRANCH, applicationBranch1.getBranch())
      .executeProtobuf(SearchWsResponse.class).getIssuesList())
        .extracting(Issue::getKey, Issue::getComponent, Issue::getBranch)
        .containsExactlyInAnyOrder(
          tuple(issueOnProject1Branch1.getKey(), project1Branch1.getKey(), project1Branch1.getBranch()),
          tuple(issueOnFileOnProject1Branch1.getKey(), fileOnProject1Branch1.getKey(), project1Branch1.getBranch()));
  }

  @Test
  public void ignore_application_without_browse_permission() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto application = db.components().insertApplication(db.getDefaultOrganization());
    db.components().insertComponents(newProjectCopy("PC1", project, application));
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, project);
    allowAnyoneOnProjects(project);
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, application.getDbKey())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).isEmpty();
  }

  @Test
  public void search_application_without_projects() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto application = db.components().insertApplication(db.getDefaultOrganization());
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, project);
    allowAnyoneOnProjects(project, application);
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, application.getDbKey())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).isEmpty();
  }

  @Test
  public void search_by_application_and_by_leak() {
    Date now = new Date();
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto application = db.components().insertApplication(db.getDefaultOrganization());
    // Project 1
    ComponentDto project1 = db.components().insertPublicProject();
    db.components().insertSnapshot(project1, s -> s.setPeriodDate(addDays(now, -14).getTime()));
    db.components().insertComponents(newProjectCopy("PC1", project1, application));
    IssueDto project1Issue1 = db.issues().insert(rule, project1, project1, i -> i.setIssueCreationDate(addDays(now, -10)));
    IssueDto project1Issue2 = db.issues().insert(rule, project1, project1, i -> i.setIssueCreationDate(addDays(now, -20)));
    // Project 2
    ComponentDto project2 = db.components().insertPublicProject();
    db.components().insertSnapshot(project2, s -> s.setPeriodDate(addDays(now, -25).getTime()));
    db.components().insertComponents(newProjectCopy("PC2", project2, application));
    IssueDto project2Issue1 = db.issues().insert(rule, project2, project2, i -> i.setIssueCreationDate(addDays(now, -15)));
    IssueDto project2Issue2 = db.issues().insert(rule, project2, project2, i -> i.setIssueCreationDate(addDays(now, -30)));
    // Permissions and index
    allowAnyoneOnProjects(project1, project2, application);
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, application.getDbKey())
      .setParam(PARAM_SINCE_LEAK_PERIOD, "true")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).extracting(Issue::getKey)
      .containsExactlyInAnyOrder(project1Issue1.getKey(), project2Issue1.getKey())
      .doesNotContain(project1Issue2.getKey(), project2Issue2.getKey());
  }

  @Test
  public void search_by_application_and_project() {
    ComponentDto project1 = db.components().insertPublicProject();
    ComponentDto project2 = db.components().insertPublicProject();
    ComponentDto application = db.components().insertApplication(db.getDefaultOrganization());
    db.components().insertComponents(newProjectCopy("PC1", project1, application));
    db.components().insertComponents(newProjectCopy("PC2", project2, application));
    RuleDefinitionDto rule = db.rules().insert();
    IssueDto issue1 = db.issues().insert(rule, project1, project1);
    IssueDto issue2 = db.issues().insert(rule, project2, project2);
    allowAnyoneOnProjects(project1, project2, application);
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, application.getDbKey())
      .setParam(PARAM_PROJECT_KEYS, project1.getDbKey())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issue1.getKey())
      .doesNotContain(issue2.getKey());
  }

  @Test
  public void search_by_application_and_project_and_leak() {
    Date now = new Date();
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto application = db.components().insertApplication(db.getDefaultOrganization());
    // Project 1
    ComponentDto project1 = db.components().insertPublicProject();
    db.components().insertSnapshot(project1, s -> s.setPeriodDate(addDays(now, -14).getTime()));
    db.components().insertComponents(newProjectCopy("PC1", project1, application));
    IssueDto project1Issue1 = db.issues().insert(rule, project1, project1, i -> i.setIssueCreationDate(addDays(now, -10)));
    IssueDto project1Issue2 = db.issues().insert(rule, project1, project1, i -> i.setIssueCreationDate(addDays(now, -20)));
    // Project 2
    ComponentDto project2 = db.components().insertPublicProject();
    db.components().insertSnapshot(project2, s -> s.setPeriodDate(addDays(now, -25).getTime()));
    db.components().insertComponents(newProjectCopy("PC2", project2, application));
    IssueDto project2Issue1 = db.issues().insert(rule, project2, project2, i -> i.setIssueCreationDate(addDays(now, -15)));
    IssueDto project2Issue2 = db.issues().insert(rule, project2, project2, i -> i.setIssueCreationDate(addDays(now, -30)));
    // Permissions and index
    allowAnyoneOnProjects(project1, project2, application);
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, application.getDbKey())
      .setParam(PARAM_PROJECT_KEYS, project1.getDbKey())
      .setParam(PARAM_SINCE_LEAK_PERIOD, "true")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).extracting(Issue::getKey)
      .containsExactlyInAnyOrder(project1Issue1.getKey())
      .doesNotContain(project1Issue2.getKey(), project2Issue1.getKey(), project2Issue2.getKey());
  }

  @Test
  public void search_by_application_and_by_leak_when_one_project_has_no_leak() {
    Date now = new Date();
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto application = db.components().insertApplication(db.getDefaultOrganization());
    // Project 1
    ComponentDto project1 = db.components().insertPublicProject();
    db.components().insertSnapshot(project1, s -> s.setPeriodDate(addDays(now, -14).getTime()));
    db.components().insertComponents(newProjectCopy("PC1", project1, application));
    IssueDto project1Issue1 = db.issues().insert(rule, project1, project1, i -> i.setIssueCreationDate(addDays(now, -10)));
    IssueDto project1Issue2 = db.issues().insert(rule, project1, project1, i -> i.setIssueCreationDate(addDays(now, -20)));
    // Project 2, without leak => no issue form it should be returned
    ComponentDto project2 = db.components().insertPublicProject();
    db.components().insertSnapshot(project2, s -> s.setPeriodDate(null));
    db.components().insertComponents(newProjectCopy("PC2", project2, application));
    IssueDto project2Issue1 = db.issues().insert(rule, project2, project2, i -> i.setIssueCreationDate(addDays(now, -15)));
    IssueDto project2Issue2 = db.issues().insert(rule, project2, project2, i -> i.setIssueCreationDate(addDays(now, -30)));
    // Permissions and index
    allowAnyoneOnProjects(project1, project2, application);
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, application.getDbKey())
      .setParam(PARAM_SINCE_LEAK_PERIOD, "true")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).extracting(Issue::getKey)
      .containsExactlyInAnyOrder(project1Issue1.getKey())
      .doesNotContain(project1Issue2.getKey(), project2Issue1.getKey(), project2Issue2.getKey());
  }

  @Test
  public void search_by_branch() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    IssueDto issue = db.issues().insertIssue(newIssue(rule, project, file));

    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(SHORT));
    ComponentDto branchFile = db.components().insertComponent(newFileDto(branch));
    IssueDto branchIssue = db.issues().insertIssue(newIssue(rule, branch, branchFile));
    allowAnyoneOnProjects(project);
    indexIssuesAndViews();

    // On component key + branch
    assertThat(ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, project.getKey())
      .setParam(PARAM_BRANCH, branch.getBranch())
      .executeProtobuf(SearchWsResponse.class).getIssuesList())
        .extracting(Issue::getKey, Issue::getComponent, Issue::getBranch)
        .containsExactlyInAnyOrder(tuple(branchIssue.getKey(), branchFile.getKey(), branchFile.getBranch()));

    // On project key + branch
    assertThat(ws.newRequest()
      .setParam(PARAM_PROJECT_KEYS, project.getKey())
      .setParam(PARAM_BRANCH, branch.getBranch())
      .executeProtobuf(SearchWsResponse.class).getIssuesList())
        .extracting(Issue::getKey, Issue::getComponent, Issue::getBranch)
        .containsExactlyInAnyOrder(tuple(branchIssue.getKey(), branchFile.getKey(), branchFile.getBranch()));

    // On file key + branch
    assertThat(ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, branchFile.getKey())
      .setParam(PARAM_BRANCH, branch.getBranch())
      .executeProtobuf(SearchWsResponse.class).getIssuesList())
        .extracting(Issue::getKey, Issue::getComponent, Issue::getBranch)
        .containsExactlyInAnyOrder(tuple(branchIssue.getKey(), branchFile.getKey(), branchFile.getBranch()));
  }

  @Test
  public void return_branch_in_component_list() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto projectFile = db.components().insertComponent(newFileDto(project));
    IssueDto projectIssue = db.issues().insertIssue(newIssue(rule, project, projectFile));
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(SHORT));
    ComponentDto branchFile = db.components().insertComponent(newFileDto(branch));
    IssueDto branchIssue = db.issues().insertIssue(newIssue(rule, branch, branchFile));
    allowAnyoneOnProjects(project);
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, branch.getKey())
      .setParam(PARAM_BRANCH, branch.getBranch())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getComponentsList())
      .extracting(Issues.Component::getKey, Issues.Component::getBranch)
      .containsExactlyInAnyOrder(
        tuple(branchFile.getKey(), branchFile.getBranch()),
        tuple(branch.getKey(), branch.getBranch()));
  }

  @Test
  public void search_by_pull_request() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto projectFile = db.components().insertComponent(newFileDto(project));
    IssueDto projectIssue = db.issues().insertIssue(newIssue(rule, project, projectFile));
    ComponentDto pullRequest = db.components().insertProjectBranch(project, b -> b.setBranchType(PULL_REQUEST));
    ComponentDto pullRequestFile = db.components().insertComponent(newFileDto(pullRequest));
    IssueDto pullRequestIssue = db.issues().insertIssue(newIssue(rule, pullRequest, pullRequestFile));
    allowAnyoneOnProjects(project);
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, pullRequest.getKey())
      .setParam(PARAM_PULL_REQUEST, pullRequest.getPullRequest())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey, Issue::getComponent, Issue::getPullRequest)
      .containsExactlyInAnyOrder(tuple(pullRequestIssue.getKey(), pullRequestFile.getKey(), pullRequestFile.getPullRequest()));
    assertThat(result.getComponentsList())
      .extracting(Issues.Component::getKey, Issues.Component::getPullRequest)
      .containsExactlyInAnyOrder(
        tuple(pullRequestFile.getKey(), pullRequestFile.getPullRequest()),
        tuple(pullRequest.getKey(), pullRequest.getPullRequest()));
  }

  @Test
  public void search_using_main_branch_name() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto projectFile = db.components().insertComponent(newFileDto(project));
    IssueDto projectIssue = db.issues().insertIssue(newIssue(rule, project, projectFile));
    allowAnyoneOnProjects(project);
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, project.getKey())
      .setParam(PARAM_BRANCH, "master")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey, Issue::getComponent, Issue::hasBranch)
      .containsExactlyInAnyOrder(tuple(projectIssue.getKey(), projectFile.getKey(), false));
    assertThat(result.getComponentsList())
      .extracting(Issues.Component::getKey, Issues.Component::hasBranch)
      .containsExactlyInAnyOrder(
        tuple(projectFile.getKey(), false),
        tuple(project.getKey(), false));
  }

  @Test
  public void does_not_return_branch_issues_on_not_contextualized_search() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto projectFile = db.components().insertComponent(newFileDto(project));
    IssueDto projectIssue = db.issues().insertIssue(newIssue(rule, project, projectFile));
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto branchFile = db.components().insertComponent(newFileDto(branch));
    IssueDto branchIssue = db.issues().insertIssue(newIssue(rule, branch, branchFile));
    allowAnyoneOnProjects(project);
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest().executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).extracting(Issue::getKey)
      .containsExactlyInAnyOrder(projectIssue.getKey())
      .doesNotContain(branchIssue.getKey());
  }

  @Test
  public void does_not_return_branch_issues_when_using_db_key() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto projectFile = db.components().insertComponent(newFileDto(project));
    IssueDto projectIssue = db.issues().insertIssue(newIssue(rule, project, projectFile));
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto branchFile = db.components().insertComponent(newFileDto(branch));
    IssueDto branchIssue = db.issues().insertIssue(newIssue(rule, branch, branchFile));
    allowAnyoneOnProjects(project);
    indexIssues();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, branch.getDbKey())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).isEmpty();
  }

  private void allowAnyoneOnProjects(ComponentDto... projects) {
    userSession.registerComponents(projects);
    Arrays.stream(projects).forEach(p -> permissionIndexer.allowOnlyAnyone(p));
  }

  private void indexIssues() {
    issueIndexer.indexOnStartup(null);
  }

  private void indexIssuesAndViews() {
    indexIssues();
    viewIndexer.indexOnStartup(null);
  }
}
