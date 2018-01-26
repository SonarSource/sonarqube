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
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueQueryFactory;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.issue.workflow.FunctionExecutor;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.permission.index.AuthorizationTypeSupport;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.view.index.ViewIndexDefinition;
import org.sonar.server.view.index.ViewIndexer;
import org.sonar.server.ws.WsActionTester;
import org.sonar.server.ws.WsResponseCommonFormat;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Issues.Component;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.Issues.SearchWsResponse;
import org.sonarqube.ws.client.issue.IssuesWsParameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.utils.DateUtils.addDays;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_02;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.component.ComponentTesting.newSubView;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.issue.IssueTesting.newIssue;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_KEYS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PROJECT_KEYS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SINCE_LEAK_PERIOD;

public class SearchActionComponentsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = new EsTester(new IssueIndexDefinition(new MapSettings().asConfig()), new ViewIndexDefinition(new MapSettings().asConfig()));

  private DbClient dbClient = db.getDbClient();
  private IssueIndex issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSession, new AuthorizationTypeSupport(userSession));
  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), dbClient, new IssueIteratorFactory(dbClient));
  private ViewIndexer viewIndexer = new ViewIndexer(dbClient, es.client());
  private IssueQueryFactory issueQueryFactory = new IssueQueryFactory(dbClient, Clock.systemUTC(), userSession);
  private IssueFieldsSetter issueFieldsSetter = new IssueFieldsSetter();
  private IssueWorkflow issueWorkflow = new IssueWorkflow(new FunctionExecutor(issueFieldsSetter), issueFieldsSetter);
  private SearchResponseLoader searchResponseLoader = new SearchResponseLoader(userSession, dbClient, new TransitionService(userSession, issueWorkflow));
  private Languages languages = new Languages();
  private SearchResponseFormat searchResponseFormat = new SearchResponseFormat(new Durations(), new WsResponseCommonFormat(languages), languages, new AvatarResolverImpl());
  private PermissionIndexerTester permissionIndexer = new PermissionIndexerTester(es, issueIndexer);

  private WsActionTester ws = new WsActionTester(new SearchAction(userSession, issueIndex, issueQueryFactory, searchResponseLoader, searchResponseFormat, System2.INSTANCE,
    dbClient));

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
  public void search_by_project_uuid() throws Exception {
    ComponentDto project = db.components().insertPublicProject(p -> p.setDbKey("PK1"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    db.issues().insert(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    allowAnyoneOnProjects(project);
    indexIssues();

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_PROJECT_UUIDS, project.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_project_uuid.json");

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_PROJECT_UUIDS, "unknown")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, project.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_project_uuid.json");

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, "unknown")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");
  }

  @Test
  public void search_since_leak_period_on_project() throws Exception {
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
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, project.uuid())
      .setParam(PARAM_SINCE_LEAK_PERIOD, "true")
      .execute()
      .assertJson(this.getClass(), "search_since_leak_period.json");
  }

  @Test
  public void search_since_leak_period_on_file_in_module_project() throws Exception {
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
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, project.uuid())
      .setParam(IssuesWsParameters.PARAM_FILE_UUIDS, file.uuid())
      .setParam(PARAM_SINCE_LEAK_PERIOD, "true")
      .execute()
      .assertJson(this.getClass(), "search_since_leak_period.json");
  }

  @Test
  public void project_facet_is_sticky() throws Exception {
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();
    OrganizationDto organization3 = db.organizations().insert();
    ComponentDto project1 = db.components().insertPublicProject(organization1, "P1");
    ComponentDto project2 = db.components().insertPublicProject(organization2, "P2");
    ComponentDto project3 = db.components().insertPublicProject(organization3, "P3");
    ComponentDto file1 = db.components().insertComponent(newFileDto(project1, null, "F1").setDbKey("FK1"));
    ComponentDto file2 = db.components().insertComponent(newFileDto(project2, null, "F2").setDbKey("FK2"));
    ComponentDto file3 = db.components().insertComponent(newFileDto(project3, null, "F3").setDbKey("FK3"));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    db.issues().insert(rule, project1, file1, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    db.issues().insert(rule, project2, file2, i -> i.setKee("2bd4eac2-b650-4037-80bc-7b1182fd47d4"));
    db.issues().insert(rule, project3, file3, i -> i.setKee("7b1182fd-b650-4037-80bc-82fd47d4eac2"));
    allowAnyoneOnProjects(project1, project2, project3);
    indexIssues();

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_PROJECT_UUIDS, project1.uuid())
      .setParam(WebService.Param.FACETS, "projectUuids")
      .execute()
      .assertJson(this.getClass(), "display_sticky_project_facet.json");
  }

  @Test
  public void search_by_file_uuid() throws Exception {
    ComponentDto project = db.components().insertPublicProject(p -> p.setDbKey("PK1"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    IssueDto issue = db.issues().insert(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    allowAnyoneOnProjects(project);
    indexIssues();

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_FILE_UUIDS, file.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_file_uuid.json");

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_FILE_UUIDS, "unknown")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, file.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_file_uuid.json");

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, "unknown")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");
  }

  @Test
  public void search_by_file_key() throws Exception {
    ComponentDto project = db.components().insertPublicProject(p -> p.setDbKey("PK1"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    ComponentDto unitTest = db.components().insertComponent(newFileDto(project, null, "F2").setQualifier(Qualifiers.UNIT_TEST_FILE).setDbKey("FK2"));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    IssueDto issueOnFile = db.issues().insert(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    IssueDto issueOnTest = db.issues().insert(rule, project, unitTest, i -> i.setKee("2bd4eac2-b650-4037-80bc-7b1182fd47d4"));
    allowAnyoneOnProjects(project);
    indexIssues();

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_COMPONENTS, file.getDbKey())
      .execute()
      .assertJson(this.getClass(), "search_by_file_key.json");

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_COMPONENTS, unitTest.getDbKey())
      .execute()
      .assertJson(this.getClass(), "search_by_test_key.json");
  }

  @Test
  public void display_file_facet_with_project() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization, p -> p.setDbKey("PK1"));
    ComponentDto file1 = db.components().insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    ComponentDto file2 = db.components().insertComponent(newFileDto(project, null, "F2").setDbKey("FK2"));
    ComponentDto file3 = db.components().insertComponent(newFileDto(project, null, "F3").setDbKey("FK3"));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    db.issues().insert(rule, project, file1, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    db.issues().insert(rule, project, file2, i -> i.setKee("2bd4eac2-b650-4037-80bc-7b1182fd47d4"));
    allowAnyoneOnProjects(project);
    indexIssues();

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_COMPONENT_KEYS, project.getKey())
      .setParam(IssuesWsParameters.PARAM_FILE_UUIDS, file1.uuid() + "," + file3.uuid())
      .setParam(WebService.Param.FACETS, "fileUuids")
      .execute()
      .assertJson(this.getClass(), "display_file_facet.json");
  }

  @Test
  public void display_file_facet_with_organization() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization, p -> p.setDbKey("PK1"));
    ComponentDto file1 = db.components().insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    ComponentDto file2 = db.components().insertComponent(newFileDto(project, null, "F2").setDbKey("FK2"));
    ComponentDto file3 = db.components().insertComponent(newFileDto(project, null, "F3").setDbKey("FK3"));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    db.issues().insert(rule, project, file1, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    db.issues().insert(rule, project, file2, i -> i.setKee("2bd4eac2-b650-4037-80bc-7b1182fd47d4"));
    allowAnyoneOnProjects(project);
    indexIssues();

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_ORGANIZATION, organization.getKey())
      .setParam(IssuesWsParameters.PARAM_FILE_UUIDS, file1.uuid() + "," + file3.uuid())
      .setParam(WebService.Param.FACETS, "fileUuids")
      .execute()
      .assertJson(this.getClass(), "display_file_facet.json");
  }

  @Test
  public void fail_to_display_file_facet_when_no_organization_or_project_is_set() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, file);
    allowAnyoneOnProjects(project);
    indexIssues();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Facet(s) 'fileUuids' require to also filter by project or organization");

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_FILE_UUIDS, file.uuid())
      .setParam(WebService.Param.FACETS, "fileUuids")
      .execute();
  }

  @Test
  public void search_by_directory_path() throws Exception {
    ComponentDto project = db.components().insertPublicProject(p -> p.setDbKey("PK1"));
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "D1", "src/main/java/dir"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "F1").setDbKey("FK1").setPath(directory.path() + "/MyComponent.java"));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    db.issues().insert(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    allowAnyoneOnProjects(project);
    indexIssues();

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, directory.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_file_uuid.json");

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, "unknown")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_DIRECTORIES, "src/main/java/dir")
      .execute()
      .assertJson(this.getClass(), "search_by_file_uuid.json");

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_DIRECTORIES, "src/main/java")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");
  }

  @Test
  public void search_by_directory_path_in_different_modules() throws Exception {
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
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, directory1.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_directory_uuid.json");

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, directory2.uuid())
      .execute()
      .assertJson(this.getClass(), "no_issue.json");

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_MODULE_UUIDS, module1.uuid())
      .setParam(IssuesWsParameters.PARAM_DIRECTORIES, "src/main/java/dir")
      .execute()
      .assertJson(this.getClass(), "search_by_directory_uuid.json");

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_MODULE_UUIDS, module2.uuid())
      .setParam(IssuesWsParameters.PARAM_DIRECTORIES, "src/main/java/dir")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_DIRECTORIES, "src/main/java/dir")
      .execute()
      .assertJson(this.getClass(), "search_by_directory_uuid.json");

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_DIRECTORIES, "src/main/java")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");
  }

  @Test
  public void display_module_facet_using_project() throws Exception {
    ComponentDto project = db.components().insertPublicProject(p -> p.setDbKey("PK1"));
    ComponentDto module = db.components().insertComponent(newModuleDto("M1", project).setDbKey("MK1"));
    ComponentDto subModule1 = db.components().insertComponent(newModuleDto("SUBM1", module).setDbKey("SUBMK1"));
    ComponentDto subModule2 = db.components().insertComponent(newModuleDto("SUBM2", module).setDbKey("SUBMK2"));
    ComponentDto subModule3 = db.components().insertComponent(newModuleDto("SUBM3", module).setDbKey("SUBMK3"));
    ComponentDto file1 = db.components().insertComponent(newFileDto(subModule1, null, "F1").setDbKey("FK1"));
    ComponentDto file2 = db.components().insertComponent(newFileDto(subModule2, null, "F2").setDbKey("FK2"));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    IssueDto issue1 = db.issues().insert(rule, project, file1, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    IssueDto issue2 = db.issues().insert(rule, project, file2, i -> i.setKee("2bd4eac2-b650-4037-80bc-7b1182fd47d4"));
    allowAnyoneOnProjects(project);
    indexIssues();

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_PROJECTS, project.getKey())
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, module.uuid())
      .setParam(IssuesWsParameters.PARAM_MODULE_UUIDS, subModule1.uuid() + "," + subModule3.uuid())
      .setParam(WebService.Param.FACETS, "moduleUuids")
      .execute()
      .assertJson(this.getClass(), "display_module_facet.json");
  }

  @Test
  public void display_module_facet_using_organization() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization, p -> p.setDbKey("PK1"));
    ComponentDto module = db.components().insertComponent(newModuleDto("M1", project).setDbKey("MK1"));
    ComponentDto subModule1 = db.components().insertComponent(newModuleDto("SUBM1", module).setDbKey("SUBMK1"));
    ComponentDto subModule2 = db.components().insertComponent(newModuleDto("SUBM2", module).setDbKey("SUBMK2"));
    ComponentDto subModule3 = db.components().insertComponent(newModuleDto("SUBM3", module).setDbKey("SUBMK3"));
    ComponentDto file1 = db.components().insertComponent(newFileDto(subModule1, null, "F1").setDbKey("FK1"));
    ComponentDto file2 = db.components().insertComponent(newFileDto(subModule2, null, "F2").setDbKey("FK2"));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    IssueDto issue1 = db.issues().insert(rule, project, file1, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    IssueDto issue2 = db.issues().insert(rule, project, file2, i -> i.setKee("2bd4eac2-b650-4037-80bc-7b1182fd47d4"));
    allowAnyoneOnProjects(project);
    indexIssues();

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_ORGANIZATION, organization.getKey())
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, module.uuid())
      .setParam(IssuesWsParameters.PARAM_MODULE_UUIDS, subModule1.uuid() + "," + subModule3.uuid())
      .setParam(WebService.Param.FACETS, "moduleUuids")
      .execute()
      .assertJson(this.getClass(), "display_module_facet.json");
  }

  @Test
  public void fail_to_display_module_facet_when_no_organization_or_project_is_set() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto file = db.components().insertComponent(newFileDto(module, null));
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, file);
    allowAnyoneOnProjects(project);
    indexIssues();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Facet(s) 'moduleUuids' require to also filter by project or organization");

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, module.uuid())
      .setParam(WebService.Param.FACETS, "moduleUuids")
      .execute();
  }

  @Test
  public void display_directory_facet_using_project() throws Exception {
    ComponentDto project = db.components().insertPublicProject(p -> p.setDbKey("PK1"));
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "D1", "src/main/java/dir"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, directory, "F1").setDbKey("FK1").setPath(directory.path() + "/MyComponent.java"));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    IssueDto issue = db.issues().insert(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    allowAnyoneOnProjects(project);
    indexIssues();

    userSession.logIn("john");
    ws.newRequest()
      .setParam("resolved", "false")
      .setParam(IssuesWsParameters.PARAM_COMPONENT_KEYS, project.getKey())
      .setParam(WebService.Param.FACETS, "directories")
      .execute()
      .assertJson(this.getClass(), "display_directory_facet.json");
  }

  @Test
  public void display_directory_facet_using_organization() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization, p -> p.setDbKey("PK1"));
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "D1", "src/main/java/dir"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, directory, "F1").setDbKey("FK1").setPath(directory.path() + "/MyComponent.java"));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    IssueDto issue = db.issues().insert(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    allowAnyoneOnProjects(project);
    indexIssues();

    userSession.logIn("john");
    ws.newRequest()
      .setParam("resolved", "false")
      .setParam(IssuesWsParameters.PARAM_ORGANIZATION, organization.getKey())
      .setParam(WebService.Param.FACETS, "directories")
      .execute()
      .assertJson(this.getClass(), "display_directory_facet.json");
  }

  @Test
  public void fail_to_display_directory_facet_when_no_organization_or_project_is_set() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "src"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, directory));
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, file);
    allowAnyoneOnProjects(project);
    indexIssues();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Facet(s) 'directories' require to also filter by project or organization");

    ws.newRequest()
      .setParam(WebService.Param.FACETS, "directories")
      .execute();
  }

  @Test
  public void search_by_view_uuid() throws Exception {
    ComponentDto project = db.components().insertPublicProject(p -> p.setDbKey("PK1"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    ComponentDto view = db.components().insertComponent(newView(db.getDefaultOrganization(), "V1").setDbKey("MyView"));
    db.components().insertComponent(newProjectCopy(project, view));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    db.issues().insert(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    allowAnyoneOnProjects(project, view);
    indexIssuesAndViews();

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, view.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_view_uuid.json");
  }

  @Test
  public void search_by_sub_view_uuid() throws Exception {
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
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, subView.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_view_uuid.json");
  }

  @Test
  public void search_by_sub_view_uuid_return_only_authorized_view() throws Exception {
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
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, subView.uuid())
      .execute()
      .assertJson(this.getClass(), "no_issue.json");
  }

  @Test
  public void search_by_author() throws Exception {
    ComponentDto project = db.components().insertPublicProject(p -> p.setDbKey("PK1"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    db.issues().insert(rule, project, file, i -> i.setAuthorLogin("leia").setKee("2bd4eac2-b650-4037-80bc-7b112bd4eac2"));
    db.issues().insert(rule, project, file, i -> i.setAuthorLogin("luke@skywalker.name").setKee("82fd47d4-b650-4037-80bc-7b1182fd47d4"));
    allowAnyoneOnProjects(project);
    indexIssues();

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_AUTHORS, "leia")
      .setParam(WebService.Param.FACETS, "authors")
      .execute()
      .assertJson(this.getClass(), "search_by_authors.json");

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_AUTHORS, "unknown")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");
  }

  @Test
  public void search_by_application_key() {
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
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issue1.getKey(), issue2.getKey());
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
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);
    ComponentDto projectFile = db.components().insertComponent(newFileDto(project));
    IssueDto projectIssue = db.issues().insertIssue(newIssue(rule, project, projectFile));
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto branchFile = db.components().insertComponent(newFileDto(branch));
    IssueDto branchIssue = db.issues().insertIssue(newIssue(rule, branch, branchFile));
    allowAnyoneOnProjects(project);
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, branch.getKey())
      .setParam(PARAM_BRANCH, branch.getBranch())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey, Issue::getComponent, Issue::getBranch)
      .containsExactlyInAnyOrder(tuple(branchIssue.getKey(), branchFile.getKey(), branchFile.getBranch()));
    assertThat(result.getComponentsList())
      .extracting(Issues.Component::getKey, Issues.Component::getBranch)
      .containsExactlyInAnyOrder(
        tuple(branchFile.getKey(), branchFile.getBranch()),
        tuple(branch.getKey(), branch.getBranch()));
  }

  @Test
  public void search_using_main_branch_name() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertMainBranch();
    userSession.addProjectPermission(UserRole.USER, project);
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
    userSession.addProjectPermission(UserRole.USER, project);
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
