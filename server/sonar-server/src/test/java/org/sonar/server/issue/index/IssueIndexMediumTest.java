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
package org.sonar.server.issue.index;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.UserDto;
import org.sonar.server.component.persistence.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import java.util.Date;
import java.util.UUID;

import static org.fest.assertions.Assertions.assertThat;

public class IssueIndexMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db;
  DbSession session;
  IssueIndex index;

  RuleDto rule;
  ComponentDto project;
  ComponentDto resource;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    session = db.openSession(false);
    index = tester.get(IssueIndex.class);

    rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(session, rule);

    project = new ComponentDto()
      .setId(1L)
      .setKey("MyProject")
      .setProjectId(1L);
    tester.get(ComponentDao.class).insert(session, project);

    resource = new ComponentDto()
      .setProjectId(1L)
      .setKey("MyComponent")
      .setId(2L);
    tester.get(ComponentDao.class).insert(session, resource);

    // project can be seen by anyone
    tester.get(PermissionFacade.class).insertGroupPermission(project.getId(), DefaultGroups.ANYONE, UserRole.USER, session);
    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));

    MockUserSession.set();

    session.commit();
    session.clearCache();
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void filter_by_actionPlan() throws Exception {
    String plan1 = "plan1";
    String plan2 = "plan2";
    IssueDto issue1 = createIssue()
      .setActionPlanKey(plan1);
    IssueDto issue2 = createIssue()
      .setActionPlanKey(plan2);
    db.issueDao().insert(session, issue1, issue2);
    session.commit();

    IssueQuery.Builder query = IssueQuery.builder();
    query.actionPlans(ImmutableList.of(plan1));
    Result<Issue> result = index.search(query.build(), new QueryContext());

    assertThat(result.getHits()).hasSize(1);

    query = IssueQuery.builder();
    query.actionPlans(ImmutableList.of(plan2));
    result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(1);

    query = IssueQuery.builder();
    query.actionPlans(ImmutableList.of(plan2, plan1));
    result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(2);
  }

  @Test
  public void is_assigned_filter() throws Exception {
    String assignee = "steph";
    IssueDto issue1 = createIssue()
      .setAssignee(assignee);
    IssueDto issue2 = createIssue();
    db.issueDao().insert(session, issue1, issue2);
    session.commit();

    IssueQuery.Builder query = IssueQuery.builder();
    Result<Issue> result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(2);

    query = IssueQuery.builder();
    query.assigned(true);
    result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(1);
  }

  @Test
  public void filter_assignee() throws Exception {
    String assignee1 = "steph";
    String assignee2 = "simon";
    IssueDto issue1 = createIssue()
      .setAssignee(assignee1);
    IssueDto issue2 = createIssue()
      .setAssignee(assignee2);
    IssueDto issue3 = createIssue();
    db.issueDao().insert(session, issue1, issue2, issue3);
    session.commit();

    IssueQuery.Builder query = IssueQuery.builder();
    Result<Issue> result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(3);

    query = IssueQuery.builder();
    query.assignees(ImmutableList.of(assignee1));
    result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(1);

    query = IssueQuery.builder();
    query.assignees(ImmutableList.of(assignee1, assignee2));
    result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(2);
  }

  @Test
  public void authorized_issues_on_groups() throws Exception {
    ComponentDto project1 = new ComponentDto()
      .setId(10L)
      .setKey("project1");
    ComponentDto project2 = new ComponentDto()
      .setId(11L)
      .setKey("project2");
    tester.get(ComponentDao.class).insert(session, project1, project2);

    IssueDto issue1 = createIssue().setRootComponent(project1);
    IssueDto issue2 = createIssue().setRootComponent(project2);
    db.issueDao().insert(session, issue1, issue2);

    // project1 can be seen by sonar-users
    GroupDto groupDto = new GroupDto().setName("sonar-users");
    db.groupDao().insert(session, groupDto);
    tester.get(PermissionFacade.class).insertGroupPermission(project1.getId(), groupDto.getName(), UserRole.USER, session);

    // project2 can be seen by sonar-admins
    groupDto = new GroupDto().setName("sonar-admins");
    db.groupDao().insert(session, groupDto);
    tester.get(PermissionFacade.class).insertGroupPermission(project2.getId(), groupDto.getName(), UserRole.USER, session);

    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));

    session.commit();
    session.clearCache();

    IssueQuery.Builder query = IssueQuery.builder();

    MockUserSession.set().setUserGroups("sonar-users");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(1);

    MockUserSession.set().setUserGroups("sonar-admins");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(1);

    MockUserSession.set().setUserGroups("sonar-users", "sonar-admins");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(2);

    MockUserSession.set().setUserGroups("another group");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(0);
  }

  @Test
  public void authorized_issues_on_user() throws Exception {
    ComponentDto project1 = new ComponentDto()
      .setId(10L)
      .setKey("project1");
    ComponentDto project2 = new ComponentDto()
      .setId(11L)
      .setKey("project2");
    tester.get(ComponentDao.class).insert(session, project1, project2);

    IssueDto issue1 = createIssue().setRootComponent(project1);
    IssueDto issue2 = createIssue().setRootComponent(project2);
    db.issueDao().insert(session, issue1, issue2);

    // project1 can be seen by john
    UserDto john = new UserDto().setLogin("john").setName("john").setActive(true);
    db.userDao().insert(session, john);
    tester.get(PermissionFacade.class).insertUserPermission(project1.getId(), john.getId(), UserRole.USER, session);

    // project2 can be seen by max
    UserDto max = new UserDto().setLogin("max").setName("max").setActive(true);
    db.userDao().insert(session, max);
    tester.get(PermissionFacade.class).insertUserPermission(project2.getId(), max.getId(), UserRole.USER, session);

    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));

    session.commit();
    session.clearCache();

    IssueQuery.Builder query = IssueQuery.builder();

    MockUserSession.set().setLogin("john");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(1);

    MockUserSession.set().setLogin("max");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(1);

    MockUserSession.set().setLogin("another guy");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(0);
  }

  @Test
  public void authorized_issues_on_user_with_group() throws Exception {
    ComponentDto project1 = new ComponentDto()
      .setId(10L)
      .setKey("project1");
    ComponentDto project2 = new ComponentDto()
      .setId(11L)
      .setKey("project2");
    tester.get(ComponentDao.class).insert(session, project1, project2);

    IssueDto issue1 = createIssue().setRootComponent(project1);
    IssueDto issue2 = createIssue().setRootComponent(project2);
    db.issueDao().insert(session, issue1, issue2);

    // project1 can be seen by john
    UserDto john = new UserDto().setLogin("john").setName("john").setActive(true);
    db.userDao().insert(session, john);
    tester.get(PermissionFacade.class).insertUserPermission(project1.getId(), john.getId(), UserRole.USER, session);

    // project1 can be seen by sonar-users
    GroupDto groupDto = new GroupDto().setName("sonar-users");
    db.groupDao().insert(session, groupDto);
    tester.get(PermissionFacade.class).insertGroupPermission(project1.getId(), "sonar-users", UserRole.USER, session);

    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));

    session.commit();
    session.clearCache();

    IssueQuery.Builder query = IssueQuery.builder();

    MockUserSession.set().setLogin("john").setUserGroups("sonar-users");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(1);
  }

  private IssueDto createIssue() {
    return new IssueDto()
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2014-12-04"))
      .setRule(rule)
      .setDebt(10L)
      .setRootComponent(project)
      .setComponent(resource)
      .setStatus("OPEN").setResolution("OPEN")
      .setSeverity("MAJOR")
      .setKee(UUID.randomUUID().toString());
  }
}
