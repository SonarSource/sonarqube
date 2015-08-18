/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.filter.IssueFilterParameters;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.permission.PermissionUpdater;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.view.index.ViewDoc;
import org.sonar.server.view.index.ViewIndexer;
import org.sonar.server.ws.WsTester;
import org.sonar.server.ws.WsTester.Result;

import static com.google.common.collect.Lists.newArrayList;

public class SearchActionComponentsMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withStartupTasks().withEsIndexes();

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  DbClient db;
  DbSession session;
  WsTester wsTester;

  @Before
  public void setUp() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    wsTester = tester.get(WsTester.class);
    session = db.openSession(false);
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void issues_on_different_projects() throws Exception {
    RuleDto rule = newRule();
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("P1").setKey("PK1"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "F1").setKey("FK1"));
    IssueDto issue = IssueTesting.newDto(rule, file, project)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setStatus("OPEN").setResolution("OPEN")
      .setSeverity("MAJOR")
      .setIssueCreationDate(DateUtils.parseDateTime("2014-09-04T00:00:00+0100"))
      .setIssueUpdateDate(DateUtils.parseDateTime("2017-12-04T00:00:00+0100"));
    db.issueDao().insert(session, issue);

    ComponentDto project2 = insertComponent(ComponentTesting.newProjectDto("P2").setKey("PK2"));
    setDefaultProjectPermission(project2);
    ComponentDto file2 = insertComponent(ComponentTesting.newFileDto(project2, "F2").setKey("FK2"));
    IssueDto issue2 = IssueTesting.newDto(rule, file2, project2)
      .setKee("92fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setStatus("OPEN").setResolution("OPEN")
      .setSeverity("MAJOR")
      .setIssueCreationDate(DateUtils.parseDateTime("2014-09-04T00:00:00+0100"))
      .setIssueUpdateDate(DateUtils.parseDateTime("2017-12-04T00:00:00+0100"));
    db.issueDao().insert(session, issue2);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION).execute();
    result.assertJson(this.getClass(), "issues_on_different_projects.json");
  }

  @Test
  public void search_by_project_uuid() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("P1").setKey("PK1"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "F1").setKey("FK1"));
    IssueDto issue = IssueTesting.newDto(newRule(), file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    db.issueDao().insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.PROJECT_UUIDS, project.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_project_uuid.json");

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.PROJECT_UUIDS, "unknown")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, project.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_project_uuid.json");

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, "unknown")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");
  }

  @Test
  public void project_facet_is_sticky() throws Exception {
    ComponentDto project1 = insertComponent(ComponentTesting.newProjectDto("P1").setKey("PK1"));
    ComponentDto project2 = insertComponent(ComponentTesting.newProjectDto("P2").setKey("PK2"));
    ComponentDto project3 = insertComponent(ComponentTesting.newProjectDto("P3").setKey("PK3"));
    setDefaultProjectPermission(project1);
    setDefaultProjectPermission(project2);
    setDefaultProjectPermission(project3);
    ComponentDto file1 = insertComponent(ComponentTesting.newFileDto(project1, "F1").setKey("FK1"));
    ComponentDto file2 = insertComponent(ComponentTesting.newFileDto(project2, "F2").setKey("FK2"));
    ComponentDto file3 = insertComponent(ComponentTesting.newFileDto(project3, "F3").setKey("FK3"));
    RuleDto rule = newRule();
    IssueDto issue1 = IssueTesting.newDto(rule, file1, project1).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    IssueDto issue2 = IssueTesting.newDto(rule, file2, project2).setKee("2bd4eac2-b650-4037-80bc-7b1182fd47d4");
    IssueDto issue3 = IssueTesting.newDto(rule, file3, project3).setKee("7b1182fd-b650-4037-80bc-82fd47d4eac2");
    db.issueDao().insert(session, issue1, issue2, issue3);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.PROJECT_UUIDS, project1.uuid())
      .setParam(WebService.Param.FACETS, "projectUuids")
      .execute()
      .assertJson(this.getClass(), "display_sticky_project_facet.json");
  }

  @Test
  public void search_by_file_uuid() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("P1").setKey("PK1"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "F1").setKey("FK1"));
    IssueDto issue = IssueTesting.newDto(newRule(), file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    db.issueDao().insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.FILE_UUIDS, file.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_file_uuid.json");

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.FILE_UUIDS, "unknown")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, file.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_file_uuid.json");

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, "unknown")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");
  }

  @Test
  public void search_by_file_key() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("P1").setKey("PK1"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "F1").setKey("FK1"));
    ComponentDto unitTest = insertComponent(ComponentTesting.newFileDto(project, "F2").setQualifier(Qualifiers.UNIT_TEST_FILE).setKey("FK2"));
    RuleDto rule = newRule();
    IssueDto issueOnFile = IssueTesting.newDto(rule, file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    IssueDto issueOnTest = IssueTesting.newDto(rule, unitTest, project).setKee("2bd4eac2-b650-4037-80bc-7b1182fd47d4");
    db.issueDao().insert(session, issueOnFile, issueOnTest);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENTS, file.key())
      .execute()
      .assertJson(this.getClass(), "search_by_file_key.json");

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENTS, unitTest.key())
      .execute()
      .assertJson(this.getClass(), "search_by_test_key.json");

  }

  @Test
  public void display_file_facet() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("P1").setKey("PK1"));
    setDefaultProjectPermission(project);
    ComponentDto file1 = insertComponent(ComponentTesting.newFileDto(project, "F1").setKey("FK1"));
    ComponentDto file2 = insertComponent(ComponentTesting.newFileDto(project, "F2").setKey("FK2"));
    ComponentDto file3 = insertComponent(ComponentTesting.newFileDto(project, "F3").setKey("FK3"));
    RuleDto newRule = newRule();
    IssueDto issue1 = IssueTesting.newDto(newRule, file1, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    IssueDto issue2 = IssueTesting.newDto(newRule, file2, project).setKee("2bd4eac2-b650-4037-80bc-7b1182fd47d4");
    db.issueDao().insert(session, issue1, issue2);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, project.uuid())
      .setParam(IssueFilterParameters.FILE_UUIDS, file1.uuid() + "," + file3.uuid())
      .setParam(WebService.Param.FACETS, "fileUuids")
      .execute()
      .assertJson(this.getClass(), "display_file_facet.json");
  }

  @Test
  public void search_by_directory_path() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("P1").setKey("PK1"));
    setDefaultProjectPermission(project);
    ComponentDto directory = insertComponent(ComponentTesting.newDirectory(project, "D1", "src/main/java/dir"));
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "F1").setKey("FK1").setPath(directory.path() + "/MyComponent.java"));
    IssueDto issue = IssueTesting.newDto(newRule(), file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    db.issueDao().insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, directory.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_file_uuid.json");

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, "unknown")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.DIRECTORIES, "src/main/java/dir")
      .execute()
      .assertJson(this.getClass(), "search_by_file_uuid.json");

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.DIRECTORIES, "src/main/java")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");
  }

  @Test
  public void search_by_directory_path_in_different_modules() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("P1").setKey("PK1"));
    setDefaultProjectPermission(project);
    ComponentDto module1 = insertComponent(ComponentTesting.newModuleDto("M1", project).setKey("MK1"));
    ComponentDto module2 = insertComponent(ComponentTesting.newModuleDto("M2", project).setKey("MK2"));
    ComponentDto directory1 = insertComponent(ComponentTesting.newDirectory(module1, "D1", "src/main/java/dir"));
    ComponentDto directory2 = insertComponent(ComponentTesting.newDirectory(module2, "D2", "src/main/java/dir"));
    ComponentDto file1 = insertComponent(ComponentTesting.newFileDto(module1, "F1").setKey("FK1").setPath(directory1.path() + "/MyComponent.java"));
    insertComponent(ComponentTesting.newFileDto(module2, "F2").setKey("FK2").setPath(directory2.path() + "/MyComponent.java"));
    RuleDto rule = newRule();
    IssueDto issue1 = IssueTesting.newDto(rule, file1, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    db.issueDao().insert(session, issue1);
    session.commit();

    tester.get(IssueIndexer.class).indexAll();

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, directory1.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_directory_uuid.json");

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, directory2.uuid())
      .execute()
      .assertJson(this.getClass(), "no_issue.json");

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.MODULE_UUIDS, module1.uuid())
      .setParam(IssueFilterParameters.DIRECTORIES, "src/main/java/dir")
      .execute()
      .assertJson(this.getClass(), "search_by_directory_uuid.json");

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.MODULE_UUIDS, module2.uuid())
      .setParam(IssueFilterParameters.DIRECTORIES, "src/main/java/dir")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.DIRECTORIES, "src/main/java/dir")
      .execute()
      .assertJson(this.getClass(), "search_by_directory_uuid.json");

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.DIRECTORIES, "src/main/java")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");
  }

  @Test
  public void display_module_facet() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("P1").setKey("PK1"));
    setDefaultProjectPermission(project);
    ComponentDto module = insertComponent(ComponentTesting.newModuleDto("M1", project).setKey("MK1"));
    ComponentDto subModule1 = insertComponent(ComponentTesting.newModuleDto("SUBM1", module).setKey("SUBMK1"));
    ComponentDto subModule2 = insertComponent(ComponentTesting.newModuleDto("SUBM2", module).setKey("SUBMK2"));
    ComponentDto subModule3 = insertComponent(ComponentTesting.newModuleDto("SUBM3", module).setKey("SUBMK3"));
    ComponentDto file1 = insertComponent(ComponentTesting.newFileDto(subModule1, "F1").setKey("FK1"));
    ComponentDto file2 = insertComponent(ComponentTesting.newFileDto(subModule2, "F2").setKey("FK2"));
    RuleDto newRule = newRule();
    IssueDto issue1 = IssueTesting.newDto(newRule, file1, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    IssueDto issue2 = IssueTesting.newDto(newRule, file2, project).setKee("2bd4eac2-b650-4037-80bc-7b1182fd47d4");
    db.issueDao().insert(session, issue1, issue2);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, module.uuid())
      .setParam(IssueFilterParameters.MODULE_UUIDS, subModule1.uuid() + "," + subModule3.uuid())
      .setParam(WebService.Param.FACETS, "moduleUuids")
      .execute()
      .assertJson(this.getClass(), "display_module_facet.json");
  }

  @Test
  public void display_directory_facet() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("P1").setKey("PK1"));
    setDefaultProjectPermission(project);
    ComponentDto directory = insertComponent(ComponentTesting.newDirectory(project, "D1", "src/main/java/dir"));
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "F1").setKey("FK1").setPath(directory.path() + "/MyComponent.java"));
    IssueDto issue = IssueTesting.newDto(newRule(), file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    db.issueDao().insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    userSessionRule.login("john");
    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam("resolved", "false")
      .setParam(WebService.Param.FACETS, "directories")
      .execute();
    result.assertJson(this.getClass(), "display_directory_facet.json");
  }

  @Test
  public void search_by_view_uuid() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("P1").setKey("PK1"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "F1").setKey("FK1"));
    insertIssue(IssueTesting.newDto(newRule(), file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));

    ComponentDto view = insertComponent(ComponentTesting.newProjectDto("V1").setQualifier(Qualifiers.VIEW).setKey("MyView"));
    indexView(view.uuid(), newArrayList(project.uuid()));

    setAnyoneProjectPermission(view, UserRole.USER);
    userSessionRule.login("john").addProjectUuidPermissions(UserRole.USER, view.uuid());

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, view.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_view_uuid.json");
  }

  @Test
  public void search_by_view_uuid_return_only_authorized_view() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("P1").setKey("PK1"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "F1").setKey("FK1"));
    insertIssue(IssueTesting.newDto(newRule(), file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));

    ComponentDto view = insertComponent(ComponentTesting.newProjectDto("V1").setQualifier(Qualifiers.VIEW).setKey("MyView"));
    indexView(view.uuid(), newArrayList(project.uuid()));

    setAnyoneProjectPermission(view, UserRole.USER);
    // User has wrong permission on the view, no issue will be returned
    userSessionRule.login("john").addProjectUuidPermissions(UserRole.CODEVIEWER, view.uuid());

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, view.uuid())
      .execute()
      .assertJson(this.getClass(), "no_issue.json");
  }

  @Test
  public void search_by_sub_view_uuid() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("P1").setKey("PK1"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "F1").setKey("FK1"));
    insertIssue(IssueTesting.newDto(newRule(), file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));

    ComponentDto view = insertComponent(ComponentTesting.newProjectDto("V1").setQualifier(Qualifiers.VIEW).setKey("MyView"));
    indexView(view.uuid(), newArrayList(project.uuid()));
    ComponentDto subView = insertComponent(ComponentTesting.newProjectDto("SV1").setQualifier(Qualifiers.SUBVIEW).setKey("MySubView"));
    indexView(subView.uuid(), newArrayList(project.uuid()));

    setAnyoneProjectPermission(view, UserRole.USER);
    userSessionRule.login("john").addComponentUuidPermission(UserRole.USER, view.uuid(), subView.uuid());

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, subView.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_view_uuid.json");
  }

  @Test
  public void search_by_sub_view_uuid_return_only_authorized_view() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("P1").setKey("PK1"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "F1").setKey("FK1"));
    insertIssue(IssueTesting.newDto(newRule(), file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));

    ComponentDto view = insertComponent(ComponentTesting.newProjectDto("V1").setQualifier(Qualifiers.VIEW).setKey("MyView"));
    indexView(view.uuid(), newArrayList(project.uuid()));
    ComponentDto subView = insertComponent(ComponentTesting.newProjectDto("SV1").setQualifier(Qualifiers.SUBVIEW).setKey("MySubView"));
    indexView(subView.uuid(), newArrayList(project.uuid()));

    setAnyoneProjectPermission(view, UserRole.USER);
    // User has wrong permission on the view, no issue will be returned
    userSessionRule.login("john").addComponentUuidPermission(UserRole.CODEVIEWER, view.uuid(), subView.uuid());

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, subView.uuid())
      .execute()
      .assertJson(this.getClass(), "no_issue.json");
  }

  @Test
  public void search_by_author() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("P1").setKey("PK1"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "F1").setKey("FK1"));
    RuleDto newRule = newRule();
    IssueDto issue1 = IssueTesting.newDto(newRule, file, project).setAuthorLogin("leia").setKee("2bd4eac2-b650-4037-80bc-7b112bd4eac2");
    IssueDto issue2 = IssueTesting.newDto(newRule, file, project).setAuthorLogin("luke@skywalker.name").setKee("82fd47d4-b650-4037-80bc-7b1182fd47d4");

    db.issueDao().insert(session, issue1, issue2);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.AUTHORS, "leia")
      .setParam(WebService.Param.FACETS, "authors")
      .execute()
      .assertJson(this.getClass(), "search_by_authors.json");

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.AUTHORS, "unknown")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");

  }

  @Test
  public void search_by_developer() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("P1").setKey("PK1"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "F1").setKey("FK1"));
    ComponentDto developer = insertComponent(ComponentTesting.newDeveloper("Anakin Skywalker"));
    db.authorDao().insertAuthor("vader", developer.getId());
    db.authorDao().insertAuthor("anakin@skywalker.name", developer.getId());
    RuleDto newRule = newRule();
    IssueDto issue1 = IssueTesting.newDto(newRule, file, project).setAuthorLogin("vader").setKee("2bd4eac2-b650-4037-80bc-7b112bd4eac2");
    IssueDto issue2 = IssueTesting.newDto(newRule, file, project).setAuthorLogin("anakin@skywalker.name").setKee("82fd47d4-b650-4037-80bc-7b1182fd47d4");

    db.issueDao().insert(session, issue1, issue2);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, developer.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_developer.json");
  }

  @Test
  public void search_by_developer_technical_project() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("P1").setKey("PK1"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "F1").setKey("FK1"));

    ComponentDto otherProject = insertComponent(ComponentTesting.newProjectDto("P2").setKey("PK2"));
    setDefaultProjectPermission(otherProject);
    ComponentDto otherFile = insertComponent(ComponentTesting.newFileDto(otherProject, "F2").setKey("FK2"));

    ComponentDto developer = insertComponent(ComponentTesting.newDeveloper("Anakin Skywalker"));
    ComponentDto technicalProject = insertComponent(ComponentTesting.newDevProjectCopy("COPY_P1", project, developer));
    insertComponent(ComponentTesting.newDevProjectCopy("COPY_P2", otherProject, developer));

    db.authorDao().insertAuthor("vader", developer.getId());
    db.authorDao().insertAuthor("anakin@skywalker.name", developer.getId());
    RuleDto newRule = newRule();

    IssueDto issue1 = IssueTesting.newDto(newRule, file, project).setAuthorLogin("vader").setKee("2bd4eac2-b650-4037-80bc-7b112bd4eac2");
    IssueDto issue2 = IssueTesting.newDto(newRule, file, project).setAuthorLogin("anakin@skywalker.name").setKee("82fd47d4-b650-4037-80bc-7b1182fd47d4");
    IssueDto issueX = IssueTesting.newDto(newRule, otherFile, otherProject).setAuthorLogin("anakin@skywalker.name").setKee("82fd47d4-b650-4037-7b11-80bc82fd47d4");

    db.issueDao().insert(session, issue1, issue2, issueX);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, technicalProject.uuid())
      .execute();
    result
      .assertJson(this.getClass(), "search_by_developer.json");
  }

  private RuleDto newRule() {
    RuleDto rule = RuleTesting.newXooX1()
      .setName("Rule name")
      .setDescription("Rule desc")
      .setStatus(RuleStatus.READY);
    tester.get(RuleDao.class).insert(session, rule);
    session.commit();
    return rule;
  }

  private void setDefaultProjectPermission(ComponentDto project) {
    // project can be seen by anyone
    setAnyoneProjectPermission(project, UserRole.USER);
  }

  private void setAnyoneProjectPermission(ComponentDto project, String permission) {
    userSessionRule.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    tester.get(PermissionUpdater.class).addPermission(new PermissionChange().setComponentKey(project.getKey()).setGroupName(DefaultGroups.ANYONE).setPermission(permission));
  }

  private IssueDto insertIssue(IssueDto issue) {
    db.issueDao().insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();
    return issue;
  }

  private ComponentDto insertComponent(ComponentDto component) {
    db.componentDao().insert(session, component);
    session.commit();
    return component;
  }

  private void indexView(String viewUuid, List<String> projects) {
    tester.get(ViewIndexer.class).index(new ViewDoc().setUuid(viewUuid).setProjects(projects));
  }
}
