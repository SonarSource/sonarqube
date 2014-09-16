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
package org.sonar.server.issue;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.issue.DefaultTransitions;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rule.Severity;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.user.UserDto;
import org.sonar.server.component.SnapshotTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.component.db.SnapshotDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.IndexClient;
import org.sonar.server.search.QueryContext;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.UserSession;

import java.util.Date;
import java.util.UUID;

import static org.fest.assertions.Assertions.assertThat;

public class IssueServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db;
  IndexClient indexClient;
  DbSession session;
  IssueService service;
  UserSession userSession;

  RuleDto rule;
  ComponentDto project;
  ComponentDto resource;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    indexClient = tester.get(IndexClient.class);
    session = db.openSession(false);
    service = tester.get(IssueService.class);

    rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(session, rule);

    project = new ComponentDto()
      .setId(1L)
      .setProjectId(1L)
      .setKey("MyProject")
      .setLongName("My Project")
      .setQualifier(Qualifiers.PROJECT)
      .setScope(Scopes.PROJECT);
    tester.get(ComponentDao.class).insert(session, project);
    tester.get(SnapshotDao.class).insert(session, SnapshotTesting.createForComponent(project));

    resource = new ComponentDto()
      .setId(2L)
      .setProjectId(1L)
      .setKey("MyComponent")
      .setLongName("My Component");
    tester.get(ComponentDao.class).insert(session, resource);
    tester.get(SnapshotDao.class).insert(session, SnapshotTesting.createForComponent(resource));

    // project can be seen by anyone
    tester.get(PermissionFacade.class).insertGroupPermission(project.getId(), DefaultGroups.ANYONE, UserRole.USER, session);
    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));

    UserDto user = new UserDto().setLogin("gandalf").setName("Gandalf");
    db.userDao().insert(session, user);
    tester.get(PermissionFacade.class).insertUserPermission(project.getId(), user.getId(), UserRole.USER, session);

    userSession = MockUserSession.create().setLogin(user.getLogin()).setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN)
      .addProjectPermissions(UserRole.USER, project.key());

    session.commit();
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void can_facet() throws Exception {
    IssueDto issue1 = getIssue().setActionPlanKey("P1");
    IssueDto issue2 = getIssue().setActionPlanKey("P2").setResolution("NONE");
    tester.get(IssueDao.class).insert(session, issue1, issue2);
    session.commit();

    org.sonar.server.search.Result<Issue> result = service.search(IssueQuery.builder().build(), new QueryContext());
    assertThat(result.getHits()).hasSize(2);
    assertThat(result.getFacets()).isEmpty();

    result = service.search(IssueQuery.builder().build(), new QueryContext().setFacet(true));
    assertThat(result.getFacets().keySet()).hasSize(4);
    assertThat(result.getFacetKeys("actionPlan")).hasSize(2);
  }

  @Test
  public void do_transition() {
    IssueDto issue = getIssue();
    tester.get(IssueDao.class).insert(session, issue);
    session.commit();

    assertThat(db.issueDao().getByKey(session, issue.getKey())).isNotNull();

    IssueTesting.assertIsEquivalent(issue, indexClient.get(IssueIndex.class).getByKey(issue.getKey()));

    service.doTransition(issue.getKey(), DefaultTransitions.CONFIRM, userSession);
    issue = tester.get(IssueDao.class).getByKey(session, issue.getKey());
    IssueTesting.assertIsEquivalent(issue, indexClient.get(IssueIndex.class).getByKey(issue.getKey()));
  }

  private IssueDto getIssue() {
    return new IssueDto()
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2014-12-04"))
      .setRule(rule)
      .setDebt(10L)
      .setRootComponent(project)
      .setComponent(resource)
      .setStatus(Issue.STATUS_OPEN)
      .setResolution(null)
      .setSeverity(Severity.MAJOR)
      .setKee(UUID.randomUUID().toString());
  }
}
