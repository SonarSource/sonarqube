/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.io.IOException;
import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueTesting;
import org.sonar.db.organization.OrganizationDao;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.StartupIndexer;
import org.sonar.server.issue.ActionFinder;
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
import org.sonar.server.permission.index.PermissionIndexer;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.view.index.ViewDoc;
import org.sonar.server.view.index.ViewIndexDefinition;
import org.sonar.server.view.index.ViewIndexer;
import org.sonar.server.ws.WsActionTester;
import org.sonar.server.ws.WsResponseCommonFormat;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.Issues.SearchWsResponse;
import org.sonarqube.ws.client.issue.IssuesWsParameters;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.utils.DateUtils.addDays;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_02;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.issue.IssueTesting.newIssue;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_KEYS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PROJECT_KEYS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SINCE_LEAK_PERIOD;

public class SearchActionComponentsTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = new EsTester(new IssueIndexDefinition(new MapSettings().asConfig()), new ViewIndexDefinition(new MapSettings().asConfig()));

  private DbClient dbClient = db.getDbClient();
  private DbSession session = db.getSession();
  private IssueIndex issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSession, new AuthorizationTypeSupport(userSession));
  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), dbClient, new IssueIteratorFactory(dbClient));
  private ViewIndexer viewIndexer = new ViewIndexer(dbClient, es.client());
  private IssueQueryFactory issueQueryFactory = new IssueQueryFactory(dbClient, System2.INSTANCE, userSession);
  private IssueFieldsSetter issueFieldsSetter = new IssueFieldsSetter();
  private IssueWorkflow issueWorkflow = new IssueWorkflow(new FunctionExecutor(issueFieldsSetter), issueFieldsSetter);
  private SearchResponseLoader searchResponseLoader = new SearchResponseLoader(userSession, dbClient, new ActionFinder(userSession), new TransitionService(userSession, issueWorkflow));
  private Languages languages = new Languages();
  private SearchResponseFormat searchResponseFormat = new SearchResponseFormat(new Durations(), new WsResponseCommonFormat(languages), languages, new AvatarResolverImpl());
  private WsActionTester ws = new WsActionTester(new SearchAction(userSession, issueIndex, issueQueryFactory, searchResponseLoader, searchResponseFormat));
  private OrganizationDto defaultOrganization;
  private OrganizationDto otherOrganization1;
  private OrganizationDto otherOrganization2;
  private PermissionIndexerTester permissionIndexerTester = new PermissionIndexerTester(es, issueIndexer);
  private StartupIndexer permissionIndexer = new PermissionIndexer(dbClient, es.client(), issueIndexer);

  @Before
  public void setUp() {
    session = dbClient.openSession(false);
    OrganizationDao organizationDao = dbClient.organizationDao();
    this.defaultOrganization = db.getDefaultOrganization();
    this.otherOrganization1 = OrganizationTesting.newOrganizationDto().setKey("my-org-1");
    this.otherOrganization2 = OrganizationTesting.newOrganizationDto().setKey("my-org-2");
    organizationDao.insert(session, this.otherOrganization1, false);
    organizationDao.insert(session, this.otherOrganization2, false);
    session.commit();
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void search_all_issues_when_no_parameter() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto projectFile = db.components().insertComponent(newFileDto(project));
    IssueDto issue = db.issues().insertIssue(newIssue(rule, project, projectFile));
    userSession.registerComponents(project);
    permissionIndexerTester.allowOnlyAnyone(project);
    issueIndexer.indexOnStartup(emptySet());

    SearchWsResponse result = ws.newRequest().executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).extracting(Issues.Issue::getKey)
      .containsExactlyInAnyOrder(issue.getKey());
  }

  @Test
  public void issues_on_different_projects() throws Exception {
    RuleDto rule = newRule();
    ComponentDto project = insertComponent(ComponentTesting.newPublicProjectDto(otherOrganization1, "P1").setDbKey("PK1"));
    ComponentDto file = insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    IssueDto issue = IssueTesting.newDto(rule, file, project)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setStatus("OPEN").setResolution("OPEN")
      .setSeverity("MAJOR")
      .setIssueCreationDate(DateUtils.parseDateTime("2014-09-04T00:00:00+0100"))
      .setIssueUpdateDate(DateUtils.parseDateTime("2017-12-04T00:00:00+0100"));
    dbClient.issueDao().insert(session, issue);

    ComponentDto project2 = insertComponent(ComponentTesting.newPublicProjectDto(otherOrganization2, "P2").setDbKey("PK2"));
    ComponentDto file2 = insertComponent(newFileDto(project2, null, "F2").setDbKey("FK2"));
    IssueDto issue2 = IssueTesting.newDto(rule, file2, project2)
      .setKee("92fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setStatus("OPEN").setResolution("OPEN")
      .setSeverity("MAJOR")
      .setIssueCreationDate(DateUtils.parseDateTime("2014-09-04T00:00:00+0100"))
      .setIssueUpdateDate(DateUtils.parseDateTime("2017-12-04T00:00:00+0100"));
    dbClient.issueDao().insert(session, issue2);
    session.commit();
    indexIssues();
    indexPermissions();

    ws.newRequest()
      .execute()
      .assertJson(this.getClass(), "issues_on_different_projects.json");
  }

  @Test
  public void do_not_return_module_key_on_single_module_projects() throws IOException {
    ComponentDto project = insertComponent(ComponentTesting.newPublicProjectDto(defaultOrganization, "P1").setDbKey("PK1"));
    ComponentDto module = insertComponent(newModuleDto("M1", project).setDbKey("MK1"));
    ComponentDto file = insertComponent(newFileDto(module, null, "F1").setDbKey("FK1"));
    RuleDto newRule = newRule();
    IssueDto issueInModule = IssueTesting.newDto(newRule, file, project).setKee("ISSUE_IN_MODULE");
    IssueDto issueInRootModule = IssueTesting.newDto(newRule, project, project).setKee("ISSUE_IN_ROOT_MODULE");
    dbClient.issueDao().insert(session, issueInModule, issueInRootModule);
    session.commit();
    indexIssues();
    indexPermissions();

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
    ComponentDto project = insertComponent(ComponentTesting.newPublicProjectDto(otherOrganization1, "P1").setDbKey("PK1"));
    ComponentDto file = insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    IssueDto issue = IssueTesting.newDto(newRule(), file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    dbClient.issueDao().insert(session, issue);
    session.commit();
    indexIssues();
    indexPermissions();

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
    ComponentDto project = insertComponent(ComponentTesting.newPublicProjectDto(otherOrganization2, "P1").setDbKey("PK1"));
    ComponentDto file = insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    dbClient.snapshotDao().insert(session,
      newAnalysis(project)
        .setPeriodDate(parseDateTime("2015-09-03T00:00:00+0100").getTime()));
    RuleDto rule = newRule();
    IssueDto issueAfterLeak = IssueTesting.newDto(rule, file, project)
      .setKee(UUID_EXAMPLE_01)
      .setIssueCreationDate(parseDateTime("2015-09-04T00:00:00+0100"))
      .setIssueUpdateDate(parseDateTime("2015-10-04T00:00:00+0100"));
    IssueDto issueBeforeLeak = IssueTesting.newDto(rule, file, project)
      .setKee(UUID_EXAMPLE_02)
      .setIssueCreationDate(parseDateTime("2014-09-04T00:00:00+0100"))
      .setIssueUpdateDate(parseDateTime("2015-10-04T00:00:00+0100"));
    dbClient.issueDao().insert(session, issueAfterLeak, issueBeforeLeak);
    session.commit();
    indexIssues();
    indexPermissions();

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, project.uuid())
      .setParam(PARAM_SINCE_LEAK_PERIOD, "true")
      .execute()
      .assertJson(this.getClass(), "search_since_leak_period.json");
  }

  @Test
  public void search_since_leak_period_on_file_in_module_project() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newPublicProjectDto(defaultOrganization, "P1").setDbKey("PK1"));
    ComponentDto module = insertComponent(newModuleDto(project));
    ComponentDto file = insertComponent(newFileDto(module, null, "F1").setDbKey("FK1"));
    dbClient.snapshotDao().insert(session,
      newAnalysis(project).setPeriodDate(parseDateTime("2015-09-03T00:00:00+0100").getTime()));
    RuleDto rule = newRule();
    IssueDto issueAfterLeak = IssueTesting.newDto(rule, file, project)
      .setKee(UUID_EXAMPLE_01)
      .setIssueCreationDate(parseDateTime("2015-09-04T00:00:00+0100"))
      .setIssueUpdateDate(parseDateTime("2015-10-04T00:00:00+0100"));
    IssueDto issueBeforeLeak = IssueTesting.newDto(rule, file, project)
      .setKee(UUID_EXAMPLE_02)
      .setIssueCreationDate(parseDateTime("2014-09-04T00:00:00+0100"))
      .setIssueUpdateDate(parseDateTime("2015-10-04T00:00:00+0100"));
    dbClient.issueDao().insert(session, issueAfterLeak, issueBeforeLeak);
    session.commit();
    indexIssues();
    indexPermissions();

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, project.uuid())
      .setParam(IssuesWsParameters.PARAM_FILE_UUIDS, file.uuid())
      .setParam(PARAM_SINCE_LEAK_PERIOD, "true")
      .execute()
      .assertJson(this.getClass(), "search_since_leak_period.json");
  }

  @Test
  public void project_facet_is_sticky() throws Exception {
    ComponentDto project1 = insertComponent(ComponentTesting.newPublicProjectDto(defaultOrganization, "P1").setDbKey("PK1"));
    ComponentDto project2 = insertComponent(ComponentTesting.newPublicProjectDto(otherOrganization1, "P2").setDbKey("PK2"));
    ComponentDto project3 = insertComponent(ComponentTesting.newPublicProjectDto(otherOrganization2, "P3").setDbKey("PK3"));
    ComponentDto file1 = insertComponent(newFileDto(project1, null, "F1").setDbKey("FK1"));
    ComponentDto file2 = insertComponent(newFileDto(project2, null, "F2").setDbKey("FK2"));
    ComponentDto file3 = insertComponent(newFileDto(project3, null, "F3").setDbKey("FK3"));
    RuleDto rule = newRule();
    IssueDto issue1 = IssueTesting.newDto(rule, file1, project1).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    IssueDto issue2 = IssueTesting.newDto(rule, file2, project2).setKee("2bd4eac2-b650-4037-80bc-7b1182fd47d4");
    IssueDto issue3 = IssueTesting.newDto(rule, file3, project3).setKee("7b1182fd-b650-4037-80bc-82fd47d4eac2");
    dbClient.issueDao().insert(session, issue1, issue2, issue3);
    session.commit();
    indexIssues();
    indexPermissions();

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_PROJECT_UUIDS, project1.uuid())
      .setParam(WebService.Param.FACETS, "projectUuids")
      .execute()
      .assertJson(this.getClass(), "display_sticky_project_facet.json");
  }

  @Test
  public void search_by_file_uuid() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newPublicProjectDto(defaultOrganization, "P1").setDbKey("PK1"));
    ComponentDto file = insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    IssueDto issue = IssueTesting.newDto(newRule(), file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    dbClient.issueDao().insert(session, issue);
    session.commit();
    indexIssues();
    indexPermissions();

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
    ComponentDto project = insertComponent(ComponentTesting.newPublicProjectDto(otherOrganization1, "P1").setDbKey("PK1"));
    ComponentDto file = insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    ComponentDto unitTest = insertComponent(newFileDto(project, null, "F2").setQualifier(Qualifiers.UNIT_TEST_FILE).setDbKey("FK2"));
    RuleDto rule = newRule();
    IssueDto issueOnFile = IssueTesting.newDto(rule, file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    IssueDto issueOnTest = IssueTesting.newDto(rule, unitTest, project).setKee("2bd4eac2-b650-4037-80bc-7b1182fd47d4");
    dbClient.issueDao().insert(session, issueOnFile, issueOnTest);
    session.commit();
    indexIssues();
    indexPermissions();

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
  public void display_file_facet() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newPublicProjectDto(otherOrganization2, "P1").setDbKey("PK1"));
    ComponentDto file1 = insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    ComponentDto file2 = insertComponent(newFileDto(project, null, "F2").setDbKey("FK2"));
    ComponentDto file3 = insertComponent(newFileDto(project, null, "F3").setDbKey("FK3"));
    RuleDto newRule = newRule();
    IssueDto issue1 = IssueTesting.newDto(newRule, file1, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    IssueDto issue2 = IssueTesting.newDto(newRule, file2, project).setKee("2bd4eac2-b650-4037-80bc-7b1182fd47d4");
    dbClient.issueDao().insert(session, issue1, issue2);
    session.commit();
    indexIssues();
    indexPermissions();

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, project.uuid())
      .setParam(IssuesWsParameters.PARAM_FILE_UUIDS, file1.uuid() + "," + file3.uuid())
      .setParam(WebService.Param.FACETS, "fileUuids")
      .execute()
      .assertJson(this.getClass(), "display_file_facet.json");
  }

  @Test
  public void search_by_directory_path() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newPublicProjectDto(defaultOrganization, "P1").setDbKey("PK1"));
    ComponentDto directory = insertComponent(ComponentTesting.newDirectory(project, "D1", "src/main/java/dir"));
    ComponentDto file = insertComponent(newFileDto(project, null, "F1").setDbKey("FK1").setPath(directory.path() + "/MyComponent.java"));
    IssueDto issue = IssueTesting.newDto(newRule(), file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    dbClient.issueDao().insert(session, issue);
    session.commit();
    indexIssues();
    indexPermissions();

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
    ComponentDto project = insertComponent(ComponentTesting.newPublicProjectDto(otherOrganization1, "P1").setDbKey("PK1"));
    ComponentDto module1 = insertComponent(newModuleDto("M1", project).setDbKey("MK1"));
    ComponentDto module2 = insertComponent(newModuleDto("M2", project).setDbKey("MK2"));
    ComponentDto directory1 = insertComponent(ComponentTesting.newDirectory(module1, "D1", "src/main/java/dir"));
    ComponentDto directory2 = insertComponent(ComponentTesting.newDirectory(module2, "D2", "src/main/java/dir"));
    ComponentDto file1 = insertComponent(newFileDto(module1, directory1, "F1").setDbKey("FK1").setPath(directory1.path() + "/MyComponent.java"));
    insertComponent(newFileDto(module2, directory2, "F2").setDbKey("FK2").setPath(directory2.path() + "/MyComponent.java"));
    RuleDto rule = newRule();
    IssueDto issue1 = IssueTesting.newDto(rule, file1, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    dbClient.issueDao().insert(session, issue1);
    session.commit();
    indexIssues();
    indexPermissions();

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
  public void display_module_facet() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newPublicProjectDto(otherOrganization2, "P1").setDbKey("PK1"));
    ComponentDto module = insertComponent(newModuleDto("M1", project).setDbKey("MK1"));
    ComponentDto subModule1 = insertComponent(newModuleDto("SUBM1", module).setDbKey("SUBMK1"));
    ComponentDto subModule2 = insertComponent(newModuleDto("SUBM2", module).setDbKey("SUBMK2"));
    ComponentDto subModule3 = insertComponent(newModuleDto("SUBM3", module).setDbKey("SUBMK3"));
    ComponentDto file1 = insertComponent(newFileDto(subModule1, null, "F1").setDbKey("FK1"));
    ComponentDto file2 = insertComponent(newFileDto(subModule2, null, "F2").setDbKey("FK2"));
    RuleDto newRule = newRule();
    IssueDto issue1 = IssueTesting.newDto(newRule, file1, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    IssueDto issue2 = IssueTesting.newDto(newRule, file2, project).setKee("2bd4eac2-b650-4037-80bc-7b1182fd47d4");
    dbClient.issueDao().insert(session, issue1, issue2);
    session.commit();
    indexIssues();
    indexPermissions();

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, module.uuid())
      .setParam(IssuesWsParameters.PARAM_MODULE_UUIDS, subModule1.uuid() + "," + subModule3.uuid())
      .setParam(WebService.Param.FACETS, "moduleUuids")
      .execute()
      .assertJson(this.getClass(), "display_module_facet.json");
  }

  @Test
  public void display_directory_facet() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newPublicProjectDto(defaultOrganization, "P1").setDbKey("PK1"));
    ComponentDto directory = insertComponent(ComponentTesting.newDirectory(project, "D1", "src/main/java/dir"));
    ComponentDto file = insertComponent(newFileDto(project, directory, "F1").setDbKey("FK1").setPath(directory.path() + "/MyComponent.java"));
    IssueDto issue = IssueTesting.newDto(newRule(), file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    dbClient.issueDao().insert(session, issue);
    session.commit();
    indexIssues();
    indexPermissions();

    userSession.logIn("john");
    ws.newRequest()
      .setParam("resolved", "false")
      .setParam(WebService.Param.FACETS, "directories")
      .execute()
      .assertJson(this.getClass(), "display_directory_facet.json");
  }

  @Test
  public void search_by_view_uuid() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newPublicProjectDto(otherOrganization1, "P1").setDbKey("PK1"));
    ComponentDto file = insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    ComponentDto view = insertComponent(ComponentTesting.newView(defaultOrganization, "V1").setDbKey("MyView"));
    indexView(view.uuid(), newArrayList(project.uuid()));
    indexPermissions();

    insertIssue(IssueTesting.newDto(newRule(), file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));

    userSession.logIn("john")
      .registerComponents(project, file, view);

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, view.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_view_uuid.json");
  }

  @Test
  public void search_by_sub_view_uuid() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newPublicProjectDto(defaultOrganization, "P1").setDbKey("PK1"));
    ComponentDto file = insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    insertIssue(IssueTesting.newDto(newRule(), file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));

    ComponentDto view = insertComponent(ComponentTesting.newView(otherOrganization1, "V1").setDbKey("MyView"));
    indexView(view.uuid(), newArrayList(project.uuid()));
    ComponentDto subView = insertComponent(ComponentTesting.newSubView(view, "SV1", "MySubView"));
    indexView(subView.uuid(), newArrayList(project.uuid()));
    indexPermissions();

    userSession.logIn("john")
      .registerComponents(project, file, view, subView);
    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, subView.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_view_uuid.json");
  }

  @Test
  public void search_by_sub_view_uuid_return_only_authorized_view() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newPublicProjectDto(defaultOrganization, "P1").setDbKey("PK1"));
    ComponentDto file = insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    insertIssue(IssueTesting.newDto(newRule(), file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));

    ComponentDto view = insertComponent(ComponentTesting.newView(otherOrganization1, "V1").setDbKey("MyView"));
    indexView(view.uuid(), newArrayList(project.uuid()));
    ComponentDto subView = insertComponent(ComponentTesting.newSubView(view, "SV1", "MySubView"));
    indexView(subView.uuid(), newArrayList(project.uuid()));

    // User has wrong permission on the view, no issue will be returned
    userSession.logIn("john")
      .registerComponents(project, file, view, subView);

    ws.newRequest()
      .setParam(IssuesWsParameters.PARAM_COMPONENT_UUIDS, subView.uuid())
      .execute()
      .assertJson(this.getClass(), "no_issue.json");
  }

  @Test
  public void search_by_author() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newPublicProjectDto(defaultOrganization, "P1").setDbKey("PK1"));
    ComponentDto file = insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    RuleDto newRule = newRule();
    IssueDto issue1 = IssueTesting.newDto(newRule, file, project).setAuthorLogin("leia").setKee("2bd4eac2-b650-4037-80bc-7b112bd4eac2");
    IssueDto issue2 = IssueTesting.newDto(newRule, file, project).setAuthorLogin("luke@skywalker.name").setKee("82fd47d4-b650-4037-80bc-7b1182fd47d4");
    indexPermissions();

    dbClient.issueDao().insert(session, issue1, issue2);
    session.commit();
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
  public void search_by_application_key() throws Exception {
    ComponentDto project1 = db.components().insertPublicProject();
    ComponentDto project2 = db.components().insertPublicProject();
    ComponentDto application = db.components().insertApplication(db.getDefaultOrganization());
    db.components().insertComponents(newProjectCopy("PC1", project1, application));
    db.components().insertComponents(newProjectCopy("PC2", project2, application));
    RuleDefinitionDto rule = newRule().getDefinition();
    IssueDto issue1 = db.issues().insert(rule, project1, project1);
    IssueDto issue2 = db.issues().insert(rule, project2, project2);
    userSession.registerComponents(application, project1, project2);
    permissionIndexerTester.allowOnlyAnyone(project1);
    permissionIndexerTester.allowOnlyAnyone(project2);
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
    RuleDefinitionDto rule = newRule().getDefinition();
    db.issues().insert(rule, project, project);
    userSession.registerComponents(project);
    permissionIndexerTester.allowOnlyAnyone(project);
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
    RuleDefinitionDto rule = newRule().getDefinition();
    db.issues().insert(rule, project, project);
    userSession.registerComponents(application, project);
    permissionIndexerTester.allowOnlyAnyone(project);
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, application.getDbKey())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).isEmpty();
  }

  @Test
  public void search_by_application_and_by_leak() throws Exception {
    Date now = new Date();
    RuleDefinitionDto rule = newRule().getDefinition();
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
    userSession.registerComponents(application, project1, project2);
    permissionIndexerTester.allowOnlyAnyone(project1);
    permissionIndexerTester.allowOnlyAnyone(project2);
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
  public void search_by_application_and_project() throws Exception {
    ComponentDto project1 = db.components().insertPublicProject();
    ComponentDto project2 = db.components().insertPublicProject();
    ComponentDto application = db.components().insertApplication(db.getDefaultOrganization());
    db.components().insertComponents(newProjectCopy("PC1", project1, application));
    db.components().insertComponents(newProjectCopy("PC2", project2, application));
    RuleDefinitionDto rule = newRule().getDefinition();
    IssueDto issue1 = db.issues().insert(rule, project1, project1);
    IssueDto issue2 = db.issues().insert(rule, project2, project2);
    userSession.registerComponents(application, project1, project2);
    permissionIndexerTester.allowOnlyAnyone(project1);
    permissionIndexerTester.allowOnlyAnyone(project2);
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
  public void search_by_application_and_project_and_leak() throws Exception {
    Date now = new Date();
    RuleDefinitionDto rule = newRule().getDefinition();
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
    userSession.registerComponents(application, project1, project2);
    permissionIndexerTester.allowOnlyAnyone(project1);
    permissionIndexerTester.allowOnlyAnyone(project2);
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
  public void search_by_application_and_by_leak_when_one_project_has_no_leak() throws Exception {
    Date now = new Date();
    RuleDefinitionDto rule = newRule().getDefinition();
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
    userSession.registerComponents(application, project1, project2);
    permissionIndexerTester.allowOnlyAnyone(project1);
    permissionIndexerTester.allowOnlyAnyone(project2);
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
    permissionIndexerTester.allowOnlyAnyone(project);
    userSession.addProjectPermission(UserRole.USER, project);
    ComponentDto projectFile = db.components().insertComponent(newFileDto(project));
    IssueDto projectIssue = db.issues().insertIssue(newIssue(rule, project, projectFile));
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto branchFile = db.components().insertComponent(newFileDto(branch));
    IssueDto branchIssue = db.issues().insertIssue(newIssue(rule, branch, branchFile));
    issueIndexer.indexOnStartup(emptySet());

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, branch.getKey())
      .setParam(PARAM_BRANCH, branch.getBranch())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).extracting(Issues.Issue::getKey)
      .containsExactlyInAnyOrder(branchIssue.getKey())
      .doesNotContain(projectIssue.getKey());
  }

  @Test
  public void does_not_return_branch_issues_on_not_contextualized_search() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    permissionIndexerTester.allowOnlyAnyone(project);
    ComponentDto projectFile = db.components().insertComponent(newFileDto(project));
    IssueDto projectIssue = db.issues().insertIssue(newIssue(rule, project, projectFile));
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto branchFile = db.components().insertComponent(newFileDto(branch));
    IssueDto branchIssue = db.issues().insertIssue(newIssue(rule, branch, branchFile));
    issueIndexer.indexOnStartup(emptySet());

    SearchWsResponse result = ws.newRequest().executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).extracting(Issues.Issue::getKey)
      .containsExactlyInAnyOrder(projectIssue.getKey())
      .doesNotContain(branchIssue.getKey());
  }

  private RuleDto newRule() {
    RuleDto rule = RuleTesting.newXooX1()
      .setName("Rule name")
      .setDescription("Rule desc")
      .setStatus(RuleStatus.READY);
    db.rules().insert(rule.getDefinition());
    session.commit();
    return rule;
  }

  private void indexPermissions() {
    permissionIndexer.indexOnStartup(permissionIndexer.getIndexTypes());
  }

  private IssueDto insertIssue(IssueDto issue) {
    dbClient.issueDao().insert(session, issue);
    session.commit();
    indexIssues();
    return issue;
  }

  private ComponentDto insertComponent(ComponentDto component) {
    dbClient.componentDao().insert(session, component);
    session.commit();
    return component;
  }

  private void indexIssues() {
    issueIndexer.indexOnStartup(issueIndexer.getIndexTypes());
  }

  private void indexView(String viewUuid, List<String> projects) {
    viewIndexer.index(new ViewDoc().setUuid(viewUuid).setProjects(projects));
  }

  private void indexIssuesAndViews() {
    issueIndexer.indexOnStartup(null);
    viewIndexer.indexOnStartup(null);
  }
}
