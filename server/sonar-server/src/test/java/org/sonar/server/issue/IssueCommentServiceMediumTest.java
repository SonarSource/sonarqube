/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.issue;

import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDao;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotTesting;
import org.sonar.db.issue.IssueDao;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.permission.PermissionUpdater;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueCommentServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withStartupTasks().withEsIndexes();

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  DbClient db;
  DbSession session;
  IssueCommentService service;

  RuleDto rule;
  ComponentDto project;
  ComponentDto file;

  @Before
  public void setUp() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    session = db.openSession(false);
    service = tester.get(IssueCommentService.class);

    rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(session, rule);

    project = ComponentTesting.newProjectDto();
    tester.get(ComponentDao.class).insert(session, project);
    SnapshotDto projectSnapshot = SnapshotTesting.newSnapshotForProject(project);
    tester.get(SnapshotDao.class).insert(session, projectSnapshot);

    file = ComponentTesting.newFileDto(project);
    tester.get(ComponentDao.class).insert(session, file);
    tester.get(SnapshotDao.class).insert(session, SnapshotTesting.createForComponent(file, projectSnapshot));

    // project can be seen by anyone
    session.commit();
    userSessionRule.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    tester.get(PermissionUpdater.class).addPermission(new PermissionChange().setComponentKey(project.getKey()).setGroupName(DefaultGroups.ANYONE).setPermission(UserRole.USER));

    userSessionRule.login("gandalf");

    session.commit();
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void add_comment() {
    IssueDto issue = IssueTesting.newDto(rule, file, project);
    tester.get(IssueDao.class).insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    service.addComment(issue.getKey(), "my comment");

    List<DefaultIssueComment> comments = service.findComments(issue.getKey());
    assertThat(comments).hasSize(1);
    assertThat(comments.get(0).markdownText()).isEqualTo("my comment");
  }

  @Test
  public void add_comment_on_removed_issue() {
    RuleDto removedRule = RuleTesting.newDto(RuleKey.of("removed", "rule")).setStatus(RuleStatus.REMOVED);
    tester.get(RuleDao.class).insert(session, removedRule);

    IssueDto issue = IssueTesting.newDto(removedRule, file, project).setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_REMOVED);
    tester.get(IssueDao.class).insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    service.addComment(issue.getKey(), "my comment");

    List<DefaultIssueComment> comments = service.findComments(issue.getKey());
    assertThat(comments).hasSize(1);
    assertThat(comments.get(0).markdownText()).isEqualTo("my comment");
  }

}
