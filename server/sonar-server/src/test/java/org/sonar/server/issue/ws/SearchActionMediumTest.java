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

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.issue.db.*;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.source.db.SnapshotSourceDao;
import org.sonar.core.source.db.SnapshotSourceDto;
import org.sonar.core.user.UserDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.SnapshotTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.issue.filter.IssueFilterParameters;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.QueryContext;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

public class SearchActionMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db;
  DbSession session;
  WsTester wsTester;
  RuleDto rule;
  ComponentDto project;
  ComponentDto file;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    wsTester = tester.get(WsTester.class);
    session = db.openSession(false);

    rule = RuleTesting.newXooX1()
      .setName("Rule name")
      .setDescription("Rule desc")
      .setStatus(RuleStatus.READY);
    tester.get(RuleDao.class).insert(session, rule);

    project = ComponentTesting.newProjectDto().setUuid("ABCD").setProjectUuid("ABCD")
      .setKey("MyProject");
    db.componentDao().insert(session, project);
    db.snapshotDao().insert(session, SnapshotTesting.createForProject(project));

    // project can be seen by anyone
    tester.get(PermissionFacade.class).insertGroupPermission(project.getId(), DefaultGroups.ANYONE, UserRole.USER, session);
    tester.get(PermissionFacade.class).insertGroupPermission(project.getId(), DefaultGroups.ANYONE, UserRole.CODEVIEWER, session);
    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));

    file = ComponentTesting.newFileDto(project).setUuid("BCDE")
      .setKey("MyComponent")
      .setSubProjectId(project.getId());
    db.componentDao().insert(session, file);
    SnapshotDto snapshot = db.snapshotDao().insert(session, SnapshotTesting.createForComponent(file, project));
    SnapshotSourceDto snapshotSource = new SnapshotSourceDto().setSnapshotId(snapshot.getId()).setData("First Line\n"
      + "Second Line\n"
      + "Third Line\n"
      + "Fourth Line\n"
      + "Fifth Line\n");
    tester.get(SnapshotSourceDao.class).insert(snapshotSource );

    UserDto john = new UserDto().setLogin("john").setName("John").setEmail("john@email.com");
    db.userDao().insert(session, john);

    session.commit();

    MockUserSession.set().setLogin("john").addComponentPermission(UserRole.CODEVIEWER, project.getKey(), file.getKey());
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void define_action() throws Exception {
    WebService.Controller controller = wsTester.controller("api/issues");

    WebService.Action show = controller.action("search");
    assertThat(show).isNotNull();
    assertThat(show.handler()).isNotNull();
    assertThat(show.since()).isEqualTo("3.6");
    assertThat(show.isPost()).isFalse();
    assertThat(show.isInternal()).isFalse();
    assertThat(show.responseExampleAsString()).isNotEmpty();
    assertThat(show.params()).hasSize(28);
  }

  @Test
  public void empty_search() throws Exception {
    WsTester.TestRequest request = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION);
    WsTester.Result result = request.execute();

    assertThat(result).isNotNull();
    result.assertJson(this.getClass(), "empty_result.json", false);
  }

  @Test
  public void issue() throws Exception {
    db.userDao().insert(session, new UserDto().setLogin("simon").setName("Simon").setEmail("simon@email.com"));
    db.userDao().insert(session, new UserDto().setLogin("fabrice").setName("Fabrice").setEmail("fabrice@email.com"));

    IssueDto issue = IssueTesting.newDto(rule, file, project)
      .setRule(rule)
      .setDebt(10L)
      .setProject(project)
      .setComponent(file)
      .setStatus("OPEN").setResolution("OPEN")
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssignee("simon")
      .setReporter("fabrice")
      .setActionPlanKey("AP-ABCD")
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2014-12-04"));
    db.issueDao().insert(session, issue);
    session.commit();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION).execute();
    // TODO date assertion is complex to test, and components id are not predictable, that's why strict boolean is set to false
    result.assertJson(this.getClass(), "issue.json", false);
  }

  @Test
  public void issue_with_comment() throws Exception {
    db.userDao().insert(session, new UserDto().setLogin("fabrice").setName("Fabrice").setEmail("fabrice@email.com"));

    IssueDto issue = IssueTesting.newDto(rule, file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    db.issueDao().insert(session, issue);

    tester.get(IssueChangeDao.class).insert(session,
      new IssueChangeDto().setIssueKey(issue.getKey())
        .setKey("COMMENT-ABCD")
        .setChangeData("*My comment*")
        .setChangeType(IssueChangeDto.TYPE_COMMENT)
        .setUserLogin("john")
        .setCreatedAt(DateUtils.parseDate("2014-09-09")));
    tester.get(IssueChangeDao.class).insert(session,
      new IssueChangeDto().setIssueKey(issue.getKey())
        .setKey("COMMENT-ABCE")
        .setChangeData("Another comment")
        .setChangeType(IssueChangeDto.TYPE_COMMENT)
        .setUserLogin("fabrice")
        .setCreatedAt(DateUtils.parseDate("2014-09-10")));
    session.commit();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION).execute();
    result.assertJson(this.getClass(), "issue_with_comment.json", false);
  }

  @Test
  public void issue_with_action_plan() throws Exception {
    db.userDao().insert(session, new UserDto().setLogin("simon").setName("Simon").setEmail("simon@email.com"));
    db.userDao().insert(session, new UserDto().setLogin("fabrice").setName("Fabrice").setEmail("fabrice@email.com"));

    tester.get(ActionPlanDao.class).save(new ActionPlanDto()
      .setKey("AP-ABCD")
      .setName("1.0")
      .setStatus("OPEN")
      .setProjectId(project.getId())
      .setUserLogin("simon")
      .setDeadLine(DateUtils.parseDateTime("2014-01-24T19:10:03+0100"))
      .setCreatedAt(DateUtils.parseDateTime("2014-01-22T19:10:03+0100"))
      .setUpdatedAt(DateUtils.parseDateTime("2014-01-23T19:10:03+0100")));

    IssueDto issue = IssueTesting.newDto(rule, file, project).setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setActionPlanKey("AP-ABCD");
    db.issueDao().insert(session, issue);
    session.commit();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION).execute();
    result.assertJson(this.getClass(), "issue_with_action_plan.json", false);
  }

  @Test
  public void issue_with_attributes() throws Exception {
    IssueDto issue = IssueTesting.newDto(rule, file, project)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setIssueAttributes(KeyValueFormat.format(ImmutableMap.of("jira-issue-key", "SONAR-1234")));
    db.issueDao().insert(session, issue);
    session.commit();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION).execute();
    result.assertJson(this.getClass(), "issue_with_attributes.json", false);
  }

  @Test
  public void issue_with_extra_fields() throws Exception {
    db.userDao().insert(session, new UserDto().setLogin("simon").setName("Simon").setEmail("simon@email.com"));
    db.userDao().insert(session, new UserDto().setLogin("fabrice").setName("Fabrice").setEmail("fabrice@email.com"));

    tester.get(ActionPlanDao.class).save(new ActionPlanDto()
      .setKey("AP-ABCD")
      .setName("1.0")
      .setStatus("OPEN")
      .setProjectId(project.getId())
      .setUserLogin("simon"));

    IssueDto issue = IssueTesting.newDto(rule, file, project)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setAuthorLogin("John")
      .setAssignee("simon")
      .setReporter("fabrice")
      .setActionPlanKey("AP-ABCD");
    db.issueDao().insert(session, issue);
    session.commit();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam("extra_fields", "actions,transitions,assigneeName,reporterName,actionPlanName").execute();
    result.assertJson(this.getClass(), "issue_with_extra_fields.json", false);
  }

  @Test
  public void issue_linked_on_removed_file() throws Exception {
    ComponentDto removedFile = ComponentTesting.newFileDto(project).setUuid("EDCB")
      .setEnabled(false)
      .setKey("RemovedComponent")
      .setSubProjectId(project.getId());
    db.componentDao().insert(session, removedFile);

    IssueDto issue = IssueTesting.newDto(rule, removedFile, project)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setRule(rule)
      .setProject(project)
      .setComponent(removedFile)
      .setStatus("OPEN").setResolution("OPEN")
      .setSeverity("MAJOR")
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2014-12-04"));
    db.issueDao().insert(session, issue);
    session.commit();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION).execute();
    result.assertJson(this.getClass(), "issue_linked_on_removed_file.json", false);
  }

  @Test
  public void issue_contains_component_id_for_eclipse() throws Exception {
    IssueDto issue = IssueTesting.newDto(rule, file, project);
    db.issueDao().insert(session, issue);
    session.commit();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION).execute();
    assertThat(result.outputAsString()).contains("\"componentId\":" + file.getId() + ",");
  }

  @Test
  public void return_full_number_of_issues_when_only_one_component_is_set() throws Exception {
    for (int i = 0; i < QueryContext.MAX_LIMIT + 1; i++) {
      IssueDto issue = IssueTesting.newDto(rule, file, project);
      tester.get(IssueDao.class).insert(session, issue);
    }
    session.commit();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION).setParam(IssueFilterParameters.COMPONENTS, file.getKey()).execute();
    result.assertJson(this.getClass(), "return_full_number_of_issues_when_only_one_component_is_set.json", false);
  }

  @Test
  public void components_contains_sub_projects() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto().setKey("ProjectHavingModule");
    db.componentDao().insert(session, project);
    db.snapshotDao().insert(session, SnapshotTesting.createForProject(project));

    // project can be seen by anyone
    tester.get(PermissionFacade.class).insertGroupPermission(project.getId(), DefaultGroups.ANYONE, UserRole.USER, session);
    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));

    ComponentDto module = ComponentTesting.newFileDto(project).setKey("ModuleHavingFile")
      .setScope("PRJ")
      .setSubProjectId(project.getId());
    db.componentDao().insert(session, module);
    db.snapshotDao().insert(session, SnapshotTesting.createForComponent(module, project));

    ComponentDto file = ComponentTesting.newFileDto(module).setKey("FileLinkedToModule");
    db.componentDao().insert(session, file);
    db.snapshotDao().insert(session, SnapshotTesting.createForComponent(file, project));

    IssueDto issue = IssueTesting.newDto(rule, file, project);
    db.issueDao().insert(session, issue);
    session.commit();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION).execute();
    result.assertJson(this.getClass(), "components_contains_sub_projects.json", false);
  }

  @Test
  public void display_facets() throws Exception {
    IssueDto issue = IssueTesting.newDto(rule, file, project)
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2014-12-04"))
      .setRule(rule)
      .setDebt(10L)
      .setProject(project)
      .setComponent(file)
      .setStatus("OPEN")
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setSeverity("MAJOR");
    db.issueDao().insert(session, issue);
    session.commit();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam("resolved", "false")
      .setParam(SearchAction.PARAM_FACETS, "statuses,severities,resolutions,componentRootUuids,rules,componentUuids,assignees,languages")
      .execute();
    result.assertJson(this.getClass(), "display_facets.json", false);
  }

  @Test
  public void hide_rules() throws Exception {
    IssueDto issue = IssueTesting.newDto(rule, file, project)
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2014-12-04"))
      .setRule(rule)
      .setDebt(10L)
      .setProject(project)
      .setComponent(file)
      .setStatus("OPEN")
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setSeverity("MAJOR");
    db.issueDao().insert(session, issue);
    session.commit();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION).setParam(IssueFilterParameters.HIDE_RULES, "true").execute();
    result.assertJson(this.getClass(), "hide_rules.json", false);
  }

  @Test
  public void paging() throws Exception {
    for (int i = 0; i < 12; i++) {
      IssueDto issue = IssueTesting.newDto(rule, file, project);
      tester.get(IssueDao.class).insert(session, issue);
    }
    session.commit();

    WsTester.TestRequest request = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION);
    request.setParam(SearchAction.PARAM_PAGE, "2");
    request.setParam(SearchAction.PARAM_PAGE_SIZE, "9");

    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "paging.json", false);
  }

  @Test
  public void paging_with_page_size_to_minus_one() throws Exception {
    for (int i = 0; i < 12; i++) {
      IssueDto issue = IssueTesting.newDto(rule, file, project);
      tester.get(IssueDao.class).insert(session, issue);
    }
    session.commit();

    WsTester.TestRequest request = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION);
    request.setParam(SearchAction.PARAM_PAGE, "1");
    request.setParam(SearchAction.PARAM_PAGE_SIZE, "-1");

    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "paging_with_page_size_to_minus_one.json", false);
  }

  @Test
  public void deprecated_paging() throws Exception {
    for (int i = 0; i < 12; i++) {
      IssueDto issue = IssueTesting.newDto(rule, file, project);
      tester.get(IssueDao.class).insert(session, issue);
    }
    session.commit();

    WsTester.TestRequest request = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION);
    request.setParam(IssueFilterParameters.PAGE_INDEX, "2");
    request.setParam(IssueFilterParameters.PAGE_SIZE, "9");

    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "deprecated_paging.json", false);
  }

  @Test
  public void default_page_size_is_100() throws Exception {
    WsTester.TestRequest request = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION);

    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "default_page_size_is_100.json", false);
  }

}
