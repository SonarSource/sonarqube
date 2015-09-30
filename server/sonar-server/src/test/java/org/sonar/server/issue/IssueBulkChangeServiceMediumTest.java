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

import com.google.common.base.Joiner;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.issue.DefaultTransitions;
import org.sonar.api.issue.Issue;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDao;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.issue.IssueDao;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.db.user.UserDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotTesting;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.permission.PermissionUpdater;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class IssueBulkChangeServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withStartupTasks().withEsIndexes();

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  DbClient db;
  DbSession session;
  IssueBulkChangeService service;
  RuleDto rule;
  ComponentDto project;
  ComponentDto file;

  UserSession userSession;

  @Before
  public void setUp() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    session = db.openSession(false);
    service = tester.get(IssueBulkChangeService.class);

    rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(session, rule);

    project = ComponentTesting.newProjectDto().setKey("MyProject");
    tester.get(ComponentDao.class).insert(session, project);
    SnapshotDto projectSnapshot = SnapshotTesting.newSnapshotForProject(project);
    tester.get(SnapshotDao.class).insert(session, projectSnapshot);

    file = ComponentTesting.newFileDto(project).setKey("MyComponent");
    tester.get(ComponentDao.class).insert(session, file);
    tester.get(SnapshotDao.class).insert(session, SnapshotTesting.createForComponent(file, projectSnapshot));

    // project can be seen by anyone
    session.commit();
    userSessionRule.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    tester.get(PermissionUpdater.class).addPermission(new PermissionChange().setComponentKey(project.getKey()).setGroupName(DefaultGroups.ANYONE).setPermission(UserRole.USER));

    userSession = userSessionRule.login("john")
      .addProjectPermissions(UserRole.USER, project.key());
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void bulk_change() {
    UserDto user = new UserDto().setLogin("fred").setName("Fred");
    db.userDao().insert(session, user);

    IssueDto issue1 = IssueTesting.newDto(rule, file, project).setAssignee("fabrice");
    IssueDto issue2 = IssueTesting.newDto(rule, file, project).setAssignee("simon");
    tester.get(IssueDao.class).insert(session, issue1, issue2);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    Map<String, Object> properties = newHashMap();
    properties.put("issues", issue1.getKey() + "," + issue2.getKey());
    properties.put("actions", "assign");
    properties.put("assign.assignee", user.getLogin());

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties, true);
    IssueBulkChangeResult result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.issuesChanged()).hasSize(2);
    assertThat(result.issuesNotChanged()).isEmpty();
  }

  @Test
  public void bulk_change_on_500_issues() {
    List<String> issueKeys = newArrayList();
    for (int i = 0; i < 500; i++) {
      IssueDto issue = IssueTesting.newDto(rule, file, project).setStatus(Issue.STATUS_OPEN);
      tester.get(IssueDao.class).insert(session, issue);
      issueKeys.add(issue.getKey());
    }
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    Map<String, Object> properties = newHashMap();
    properties.put("issues", Joiner.on(",").join(issueKeys));
    properties.put("actions", "do_transition");
    properties.put("do_transition.transition", DefaultTransitions.CONFIRM);

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties, false);
    IssueBulkChangeResult result = service.execute(issueBulkChangeQuery, userSession);
    assertThat(result.issuesChanged()).hasSize(500);
    assertThat(result.issuesNotChanged()).isEmpty();
  }

  @Test
  public void fail_if_bulk_change_on_more_than_500_issues() {
    List<String> issueKeys = newArrayList();
    for (int i = 0; i < 510; i++) {
      issueKeys.add("issue-" + i);
    }

    Map<String, Object> properties = newHashMap();
    properties.put("issues", Joiner.on(",").join(issueKeys));
    properties.put("actions", "do_transition");
    properties.put("do_transition.transition", DefaultTransitions.CONFIRM);

    try {
      IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(properties, true);
      service.execute(issueBulkChangeQuery, userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Number of issue keys must be less than 500 (got 510)");
    }
  }
}
