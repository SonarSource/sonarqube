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

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.filter.IssueFilterParameters;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.permission.InternalPermissionService;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

public class SearchActionComponentsMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db;
  DbSession session;
  WsTester wsTester;

  @Before
  public void setUp() throws Exception {
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
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));
    IssueDto issue = IssueTesting.newDto(rule, file, project)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setStatus("OPEN").setResolution("OPEN")
      .setSeverity("MAJOR")
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2017-12-04"));
    db.issueDao().insert(session, issue);

    ComponentDto project2 = insertComponent(ComponentTesting.newProjectDto("DBCA").setKey("MyProject2"));
    setDefaultProjectPermission(project2);
    ComponentDto file2 = insertComponent(ComponentTesting.newFileDto(project2, "EDCB").setKey("MyComponent2"));
    IssueDto issue2 = IssueTesting.newDto(rule, file2, project2)
      .setKee("92fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setStatus("OPEN").setResolution("OPEN")
      .setSeverity("MAJOR")
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2017-12-04"));
    db.issueDao().insert(session, issue2);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION).execute();
    result.assertJson(this.getClass(), "issues_on_different_projects.json", false);
  }

  @Test
  public void search_by_project_uuid() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));
    IssueDto issue = IssueTesting.newDto(newRule(), file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    db.issueDao().insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.PROJECT_UUIDS, project.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_project_uuid.json", false);

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.PROJECT_UUIDS, "unknown")
      .execute()
      .assertJson(this.getClass(), "no_issue.json", false);

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, project.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_project_uuid.json", false);

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, "unknown")
      .execute()
      .assertJson(this.getClass(), "no_issue.json", false);
  }

  @Test
  public void search_by_file_uuid() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));
    IssueDto issue = IssueTesting.newDto(newRule(), file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    db.issueDao().insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.FILE_UUIDS, file.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_file_uuid.json", false);

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.FILE_UUIDS, "unknown")
      .execute()
      .assertJson(this.getClass(), "no_issue.json", false);

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, file.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_file_uuid.json", false);

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, "unknown")
      .execute()
      .assertJson(this.getClass(), "no_issue.json", false);
  }

  @Test
  public void search_by_directory_path() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto directory = insertComponent(ComponentTesting.newDirectory(project, "src/main/java/dir"));
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent").setPath(directory.path() + "/MyComponent.java"));
    IssueDto issue = IssueTesting.newDto(newRule(), file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    db.issueDao().insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, directory.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_file_uuid.json", false);

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, "unknown")
      .execute()
      .assertJson(this.getClass(), "no_issue.json", false);

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.DIRECTORIES, "src/main/java/dir")
      .execute()
      .assertJson(this.getClass(), "search_by_file_uuid.json", false);

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.DIRECTORIES, "src/main/java")
      .execute()
      .assertJson(this.getClass(), "no_issue.json", false);
  }

  @Test
  public void search_by_directory_path_in_different_modules() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto module1 = insertComponent(ComponentTesting.newModuleDto(project).setKey("module1"));
    ComponentDto module2 = insertComponent(ComponentTesting.newModuleDto(project).setKey("module2"));
    ComponentDto directory1 = insertComponent(ComponentTesting.newDirectory(module1, "src/main/java/dir"));
    ComponentDto directory2 = insertComponent(ComponentTesting.newDirectory(module2, "src/main/java/dir"));
    ComponentDto file1 = insertComponent(ComponentTesting.newFileDto(module1, "BCDE").setKey("module1:MyComponent").setPath(directory1.path() + "/MyComponent.java"));
    insertComponent(ComponentTesting.newFileDto(module2, "CDEF").setKey("module2:MyComponent").setPath(directory2.path() + "/MyComponent.java"));
    RuleDto rule = newRule();
    IssueDto issue1 = IssueTesting.newDto(rule, file1, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    db.issueDao().insert(session, issue1);
    session.commit();

    tester.get(IssueIndexer.class).indexAll();

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, directory1.uuid())
      .execute()
      .assertJson(this.getClass(), "search_by_directory_uuid.json", false);

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENT_UUIDS, directory2.uuid())
      .execute()
      .assertJson(this.getClass(), "no_issue.json", false);

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.MODULE_UUIDS, module1.uuid())
      .setParam(IssueFilterParameters.DIRECTORIES, "src/main/java/dir")
      .execute()
      .assertJson(this.getClass(), "search_by_directory_uuid.json", false);

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.MODULE_UUIDS, module2.uuid())
      .setParam(IssueFilterParameters.DIRECTORIES, "src/main/java/dir")
      .execute()
      .assertJson(this.getClass(), "no_issue.json", false);

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.DIRECTORIES, "src/main/java/dir")
      .execute()
      .assertJson(this.getClass(), "search_by_directory_uuid.json", false);

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.DIRECTORIES, "src/main/java")
      .execute()
      .assertJson(this.getClass(), "no_issue.json", false);
  }

  @Test
  public void display_directory_facet() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto directory = insertComponent(ComponentTesting.newDirectory(project, "src/main/java/dir"));
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent").setPath(directory.path() + "/MyComponent.java"));
    IssueDto issue = IssueTesting.newDto(newRule(), file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    db.issueDao().insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    MockUserSession.set().setLogin("john");
    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam("resolved", "false")
      .setParam(SearchAction.PARAM_FACETS, "directories")
      .execute();
    result.assertJson(this.getClass(), "display_directory_facet.json", false);
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
    // project can be seen by anyone and by code viewer
    MockUserSession.set().setLogin("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    tester.get(InternalPermissionService.class).addPermission(new PermissionChange().setComponentKey(project.getKey()).setGroup(DefaultGroups.ANYONE).setPermission(UserRole.USER));
    MockUserSession.set();
  }

  private ComponentDto insertComponent(ComponentDto component) {
    db.componentDao().insert(session, component);
    session.commit();
    return component;
  }

}
