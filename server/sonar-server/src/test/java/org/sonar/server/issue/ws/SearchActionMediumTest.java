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
import org.sonar.core.issue.db.*;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.user.UserDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.issue.filter.IssueFilterParameters;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.permission.InternalPermissionService;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.QueryContext;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchActionMediumTest {

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
  public void define_action() throws Exception {
    WebService.Controller controller = wsTester.controller("api/issues");

    WebService.Action show = controller.action("search");
    assertThat(show).isNotNull();
    assertThat(show.handler()).isNotNull();
    assertThat(show.since()).isEqualTo("3.6");
    assertThat(show.isPost()).isFalse();
    assertThat(show.isInternal()).isFalse();
    assertThat(show.responseExampleAsString()).isNotEmpty();
    assertThat(show.params()).hasSize(40);
  }

  @Test
  public void empty_search() throws Exception {
    WsTester.TestRequest request = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION);
    WsTester.Result result = request.execute();

    assertThat(result).isNotNull();
    result.assertJson(this.getClass(), "empty_result.json");
  }

  @Test
  public void issue() throws Exception {
    db.userDao().insert(session, new UserDto().setLogin("simon").setName("Simon").setEmail("simon@email.com"));
    db.userDao().insert(session, new UserDto().setLogin("fabrice").setName("Fabrice").setEmail("fabrice@email.com"));

    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));
    IssueDto issue = IssueTesting.newDto(newRule(), file, project)
      .setDebt(10L)
      .setStatus("OPEN").setResolution("OPEN")
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssignee("simon")
      .setReporter("fabrice")
      .setActionPlanKey("AP-ABCD")
      .setIssueCreationDate(DateUtils.parseDateTime("2014-09-04T00:00:00+0100"))
      .setIssueUpdateDate(DateUtils.parseDateTime("2017-12-04T00:00:00+0100"));
    db.issueDao().insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION).execute();
    result.assertJson(this.getClass(), "issue.json");
  }

  @Test
  public void issue_with_comment() throws Exception {
    db.userDao().insert(session, new UserDto().setLogin("john").setName("John").setEmail("john@email.com"));
    db.userDao().insert(session, new UserDto().setLogin("fabrice").setName("Fabrice").setEmail("fabrice@email.com"));

    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));
    IssueDto issue = IssueTesting.newDto(newRule(), file, project)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    db.issueDao().insert(session, issue);

    tester.get(IssueChangeDao.class).insert(session,
      new IssueChangeDto().setIssueKey(issue.getKey())
        .setKey("COMMENT-ABCD")
        .setChangeData("*My comment*")
        .setChangeType(IssueChangeDto.TYPE_COMMENT)
        .setUserLogin("john")
        .setCreatedAt(DateUtils.parseDate("2014-09-09").getTime()));
    tester.get(IssueChangeDao.class).insert(session,
      new IssueChangeDto().setIssueKey(issue.getKey())
        .setKey("COMMENT-ABCE")
        .setChangeData("Another comment")
        .setChangeType(IssueChangeDto.TYPE_COMMENT)
        .setUserLogin("fabrice")
        .setCreatedAt(DateUtils.parseDate("2014-09-10").getTime()));
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    MockUserSession.set().setLogin("john");
    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION).execute();
    result.assertJson(this.getClass(), "issue_with_comment.json");
  }

  @Test
  public void issue_with_comment_hidden() throws Exception {
    db.userDao().insert(session, new UserDto().setLogin("john").setName("John").setEmail("john@email.com"));
    db.userDao().insert(session, new UserDto().setLogin("fabrice").setName("Fabrice").setEmail("fabrice@email.com"));

    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));
    IssueDto issue = IssueTesting.newDto(newRule(), file, project)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2");
    db.issueDao().insert(session, issue);

    tester.get(IssueChangeDao.class).insert(session,
      new IssueChangeDto().setIssueKey(issue.getKey())
        .setKey("COMMENT-ABCD")
        .setChangeData("*My comment*")
        .setChangeType(IssueChangeDto.TYPE_COMMENT)
        .setUserLogin("john")
        .setCreatedAt(DateUtils.parseDate("2014-09-09").getTime()));
    tester.get(IssueChangeDao.class).insert(session,
      new IssueChangeDto().setIssueKey(issue.getKey())
        .setKey("COMMENT-ABCE")
        .setChangeData("Another comment")
        .setChangeType(IssueChangeDto.TYPE_COMMENT)
        .setUserLogin("fabrice")
        .setCreatedAt(DateUtils.parseDate("2014-09-10").getTime()));
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    MockUserSession.set().setLogin("john");
    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION).setParam(IssueFilterParameters.HIDE_COMMENTS, "true").execute();
    result.assertJson(this.getClass(), "issue_with_comment_hidden.json");
    assertThat(result.outputAsString()).doesNotContain("fabrice");
  }

  @Test
  public void issue_with_action_plan() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));

    tester.get(ActionPlanDao.class).save(new ActionPlanDto()
      .setKey("AP-ABCD")
      .setName("1.0")
      .setStatus("OPEN")
      .setProjectId(project.getId())
      .setUserLogin("simon")
      .setDeadLine(DateUtils.parseDateTime("2014-01-24T19:10:03+0000"))
      .setCreatedAt(DateUtils.parseDateTime("2014-01-22T19:10:03+0000"))
      .setUpdatedAt(DateUtils.parseDateTime("2014-01-23T19:10:03+0000")));

    IssueDto issue = IssueTesting.newDto(newRule(), file, project)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setActionPlanKey("AP-ABCD");
    db.issueDao().insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION).execute();
    result.assertJson(this.getClass(), "issue_with_action_plan.json");
  }

  @Test
  public void issue_with_attributes() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));
    IssueDto issue = IssueTesting.newDto(newRule(), file, project)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setIssueAttributes(KeyValueFormat.format(ImmutableMap.of("jira-issue-key", "SONAR-1234")));
    db.issueDao().insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION).execute();
    result.assertJson(this.getClass(), "issue_with_attributes.json");
  }

  @Test
  public void issue_with_extra_fields() throws Exception {
    db.userDao().insert(session, new UserDto().setLogin("simon").setName("Simon").setEmail("simon@email.com"));
    db.userDao().insert(session, new UserDto().setLogin("fabrice").setName("Fabrice").setEmail("fabrice@email.com"));
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));

    tester.get(ActionPlanDao.class).save(new ActionPlanDto()
      .setKey("AP-ABCD")
      .setName("1.0")
      .setStatus("OPEN")
      .setProjectId(project.getId())
      .setUserLogin("simon"));

    IssueDto issue = IssueTesting.newDto(newRule(), file, project)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setAuthorLogin("John")
      .setAssignee("simon")
      .setReporter("fabrice")
      .setActionPlanKey("AP-ABCD");
    db.issueDao().insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    MockUserSession.set().setLogin("john");
    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam("extra_fields", "actions,transitions,assigneeName,reporterName,actionPlanName").execute();
    result.assertJson(this.getClass(), "issue_with_extra_fields.json");
  }

  @Test
  public void issue_linked_on_removed_file() throws Exception {
    RuleDto rule = newRule();
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto removedFile = insertComponent(ComponentTesting.newFileDto(project).setUuid("EDCB")
      .setEnabled(false)
      .setKey("RemovedComponent"));

    IssueDto issue = IssueTesting.newDto(rule, removedFile, project)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setComponent(removedFile)
      .setStatus("OPEN").setResolution("OPEN")
      .setSeverity("MAJOR")
      .setIssueCreationDate(DateUtils.parseDateTime("2014-09-04T00:00:00+0100"))
      .setIssueUpdateDate(DateUtils.parseDateTime("2017-12-04T00:00:00+0100"));
    db.issueDao().insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION).execute();
    result.assertJson(this.getClass(), "issue_linked_on_removed_file.json");
  }

  @Test
  public void issue_contains_component_id_for_eclipse() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));
    IssueDto issue = IssueTesting.newDto(newRule(), file, project);
    db.issueDao().insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION).execute();
    assertThat(result.outputAsString()).contains("\"componentId\":" + file.getId() + ",");
  }

  @Test
  public void ignore_paging_with_one_component() throws Exception {
    RuleDto rule = newRule();
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));
    for (int i = 0; i < QueryContext.MAX_LIMIT + 1; i++) {
      IssueDto issue = IssueTesting.newDto(rule, file, project);
      tester.get(IssueDao.class).insert(session, issue);
    }
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENTS, file.getKey())
      .setParam(IssueFilterParameters.IGNORE_PAGING, "true")
      .execute();
    result.assertJson(this.getClass(), "ignore_paging_with_one_component.json");
  }

  @Test
  public void apply_paging_with_multiple_components() throws Exception {
    RuleDto rule = newRule();
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));
    for (int i = 0; i < QueryContext.MAX_LIMIT + 1; i++) {
      IssueDto issue = IssueTesting.newDto(rule, file, project);
      tester.get(IssueDao.class).insert(session, issue);
    }
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    ComponentDto otherFile = insertComponent(ComponentTesting.newFileDto(project).setUuid("FEDC").setKey("OtherComponent"));

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam(IssueFilterParameters.COMPONENTS, file.getKey() + "," + otherFile.getKey())
      .setParam(IssueFilterParameters.IGNORE_PAGING, "true")
      .execute();
    result.assertJson(this.getClass(), "apply_paging_with_multiple_components.json");
  }

  @Test
  public void apply_paging_with_one_component() throws Exception {
    RuleDto rule = newRule();
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));
    for (int i = 0; i < QueryContext.MAX_LIMIT + 1; i++) {
      IssueDto issue = IssueTesting.newDto(rule, file, project);
      tester.get(IssueDao.class).insert(session, issue);
    }
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION).setParam(IssueFilterParameters.COMPONENTS, file.getKey()).execute();
    result.assertJson(this.getClass(), "apply_paging_with_one_component.json");
  }

  @Test
  public void components_contains_sub_projects() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("ProjectHavingModule"));
    setDefaultProjectPermission(project);
    ComponentDto module = insertComponent(ComponentTesting.newModuleDto(project).setKey("ModuleHavingFile"));
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(module, "BCDE").setKey("FileLinkedToModule"));
    IssueDto issue = IssueTesting.newDto(newRule(), file, project);
    db.issueDao().insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION).execute();
    result.assertJson(this.getClass(), "components_contains_sub_projects.json");
  }

  @Test
  public void display_facets() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));
    IssueDto issue = IssueTesting.newDto(newRule(), file, project)
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2017-12-04"))
      .setDebt(10L)
      .setStatus("OPEN")
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setSeverity("MAJOR");
    db.issueDao().insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    MockUserSession.set().setLogin("john");
    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam("resolved", "false")
      .setParam(WebService.Param.FACETS, "statuses,severities,resolutions,projectUuids,rules,fileUuids,assignees,languages,actionPlans")
      .execute();
    result.assertJson(this.getClass(), "display_facets.json");
  }

  @Test
  public void display_zero_valued_facets_for_selected_items() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));
    IssueDto issue = IssueTesting.newDto(newRule(), file, project)
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2017-12-04"))
      .setDebt(10L)
      .setStatus("OPEN")
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setSeverity("MAJOR");
    db.issueDao().insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    MockUserSession.set().setLogin("john");
    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam("resolved", "false")
      .setParam("severities", "MAJOR,MINOR")
      .setParam("languages", "xoo,polop,palap")
      .setParam(WebService.Param.FACETS, "statuses,severities,resolutions,projectUuids,rules,fileUuids,assignees,assigned_to_me,languages,actionPlans")
      .execute();
    result.assertJson(this.getClass(), "display_zero_facets.json");
  }

  @Test
  public void filter_by_assigned_to_me() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));
    RuleDto rule = newRule();
    IssueDto issue1 = IssueTesting.newDto(rule, file, project)
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2017-12-04"))
      .setDebt(10L)
      .setStatus("OPEN")
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setSeverity("MAJOR")
      .setAssignee("john");
    IssueDto issue2 = IssueTesting.newDto(rule, file, project)
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2017-12-04"))
      .setDebt(10L)
      .setStatus("OPEN")
      .setKee("7b112bd4-b650-4037-80bc-82fd47d4eac2")
      .setSeverity("MAJOR")
      .setAssignee("alice");
    IssueDto issue3 = IssueTesting.newDto(rule, file, project)
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2017-12-04"))
      .setDebt(10L)
      .setStatus("OPEN")
      .setKee("82fd47d4-4037-b650-80bc-7b112bd4eac2")
      .setSeverity("MAJOR");
    db.issueDao().insert(session, issue1, issue2, issue3);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    MockUserSession.set().setLogin("john");
    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam("resolved", "false")
      .setParam("assignees", "__me__")
      .setParam(WebService.Param.FACETS, "assignees,assigned_to_me")
      .execute()
      .assertJson(this.getClass(), "filter_by_assigned_to_me.json");
  }

  @Test
  public void filter_by_assigned_to_me_unauthenticated() throws Exception {
    MockUserSession.set();

    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));
    RuleDto rule = newRule();
    IssueDto issue1 = IssueTesting.newDto(rule, file, project)
      .setStatus("OPEN")
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setAssignee("john");
    IssueDto issue2 = IssueTesting.newDto(rule, file, project)
      .setStatus("OPEN")
      .setKee("7b112bd4-b650-4037-80bc-82fd47d4eac2")
      .setAssignee("alice");
    IssueDto issue3 = IssueTesting.newDto(rule, file, project)
      .setStatus("OPEN")
      .setKee("82fd47d4-4037-b650-80bc-7b112bd4eac2");
    db.issueDao().insert(session, issue1, issue2, issue3);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam("resolved", "false")
      .setParam("assignees", "__me__")
      .execute()
      .assertJson(this.getClass(), "empty_result.json");
  }

  @Test
  public void assigned_to_me_facet_is_sticky_relative_to_assignees() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));
    RuleDto rule = newRule();
    IssueDto issue1 = IssueTesting.newDto(rule, file, project)
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2017-12-04"))
      .setDebt(10L)
      .setStatus("OPEN")
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setSeverity("MAJOR")
      .setAssignee("john-bob.polop");
    IssueDto issue2 = IssueTesting.newDto(rule, file, project)
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2017-12-04"))
      .setDebt(10L)
      .setStatus("OPEN")
      .setKee("7b112bd4-b650-4037-80bc-82fd47d4eac2")
      .setSeverity("MAJOR")
      .setAssignee("alice");
    IssueDto issue3 = IssueTesting.newDto(rule, file, project)
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2017-12-04"))
      .setDebt(10L)
      .setStatus("OPEN")
      .setKee("82fd47d4-4037-b650-80bc-7b112bd4eac2")
      .setSeverity("MAJOR");
    db.issueDao().insert(session, issue1, issue2, issue3);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    MockUserSession.set().setLogin("john-bob.polop");
    wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam("resolved", "false")
      .setParam("assignees", "alice")
      .setParam(WebService.Param.FACETS, "assignees,assigned_to_me")
      .execute()
      .assertJson(this.getClass(), "assigned_to_me_facet_sticky.json");
  }

  @Test
  public void hide_rules() throws Exception {
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));
    IssueDto issue = IssueTesting.newDto(newRule(), file, project)
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2017-12-04"))
      .setDebt(10L)
      .setStatus("OPEN")
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setSeverity("MAJOR");
    db.issueDao().insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION).setParam(IssueFilterParameters.HIDE_RULES, "true").execute();
    result.assertJson(this.getClass(), "hide_rules.json");
  }

  @Test
  public void sort_by_updated_at() throws Exception {
    RuleDto rule = newRule();
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));
    db.issueDao().insert(session, IssueTesting.newDto(rule, file, project)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac1")
      .setIssueUpdateDate(DateUtils.parseDateTime("2014-11-02T00:00:00+0100")));
    db.issueDao().insert(session, IssueTesting.newDto(rule, file, project)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setIssueUpdateDate(DateUtils.parseDateTime("2014-11-01T00:00:00+0100")));
    db.issueDao().insert(session, IssueTesting.newDto(rule, file, project)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac3")
      .setIssueUpdateDate(DateUtils.parseDateTime("2014-11-03T00:00:00+0100")));
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    WsTester.Result result = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION)
      .setParam("sort", IssueQuery.SORT_BY_UPDATE_DATE)
      .setParam("asc", "false")
      .execute();
    result.assertJson(this.getClass(), "sort_by_updated_at.json");
  }

  @Test
  public void paging() throws Exception {
    RuleDto rule = newRule();
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));
    for (int i = 0; i < 12; i++) {
      IssueDto issue = IssueTesting.newDto(rule, file, project);
      tester.get(IssueDao.class).insert(session, issue);
    }
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    WsTester.TestRequest request = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION);
    request.setParam(WebService.Param.PAGE, "2");
    request.setParam(WebService.Param.PAGE_SIZE, "9");

    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "paging.json");
  }

  @Test
  public void paging_with_page_size_to_minus_one() throws Exception {
    RuleDto rule = newRule();
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));
    for (int i = 0; i < 12; i++) {
      IssueDto issue = IssueTesting.newDto(rule, file, project);
      tester.get(IssueDao.class).insert(session, issue);
    }
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    WsTester.TestRequest request = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION);
    request.setParam(WebService.Param.PAGE, "1");
    request.setParam(WebService.Param.PAGE_SIZE, "-1");

    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "paging_with_page_size_to_minus_one.json");
  }

  @Test
  public void deprecated_paging() throws Exception {
    RuleDto rule = newRule();
    ComponentDto project = insertComponent(ComponentTesting.newProjectDto("ABCD").setKey("MyProject"));
    setDefaultProjectPermission(project);
    ComponentDto file = insertComponent(ComponentTesting.newFileDto(project, "BCDE").setKey("MyComponent"));
    for (int i = 0; i < 12; i++) {
      IssueDto issue = IssueTesting.newDto(rule, file, project);
      tester.get(IssueDao.class).insert(session, issue);
    }
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    WsTester.TestRequest request = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION);
    request.setParam(IssueFilterParameters.PAGE_INDEX, "2");
    request.setParam(IssueFilterParameters.PAGE_SIZE, "9");

    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "deprecated_paging.json");
  }

  @Test
  public void default_page_size_is_100() throws Exception {
    WsTester.TestRequest request = wsTester.newGetRequest(IssuesWs.API_ENDPOINT, SearchAction.SEARCH_ACTION);

    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "default_page_size_is_100.json");
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
