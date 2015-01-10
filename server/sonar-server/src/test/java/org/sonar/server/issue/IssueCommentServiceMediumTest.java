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
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssueComment;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.SnapshotTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.component.db.SnapshotDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.permission.InternalPermissionService;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.IndexClient;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueCommentServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db;
  IndexClient indexClient;
  DbSession session;
  IssueCommentService service;

  RuleDto rule;
  ComponentDto project;
  ComponentDto file;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    indexClient = tester.get(IndexClient.class);
    session = db.openSession(false);
    service = tester.get(IssueCommentService.class);

    rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(session, rule);

    project = ComponentTesting.newProjectDto();
    tester.get(ComponentDao.class).insert(session, project);
    SnapshotDto projectSnapshot = SnapshotTesting.createForProject(project);
    tester.get(SnapshotDao.class).insert(session, projectSnapshot);

    file = ComponentTesting.newFileDto(project);
    tester.get(ComponentDao.class).insert(session, file);
    tester.get(SnapshotDao.class).insert(session, SnapshotTesting.createForComponent(file, projectSnapshot));

    // project can be seen by anyone
    session.commit();
    MockUserSession.set().setLogin("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    tester.get(InternalPermissionService.class).addPermission(new PermissionChange().setComponentKey(project.getKey()).setGroup(DefaultGroups.ANYONE).setPermission(UserRole.USER));

    MockUserSession.set().setLogin("gandalf");

    session.commit();
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void add_comment() throws Exception {
    IssueDto issue = IssueTesting.newDto(rule, file, project);
    tester.get(IssueDao.class).insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    service.addComment(issue.getKey(), "my comment", MockUserSession.get());

    List<DefaultIssueComment> comments = service.findComments(issue.getKey());
    assertThat(comments).hasSize(1);
    assertThat(comments.get(0).markdownText()).isEqualTo("my comment");
  }

  @Test
  public void add_comment_on_removed_issue() throws Exception {
    RuleDto removedRule = RuleTesting.newDto(RuleKey.of("removed", "rule")).setStatus(RuleStatus.REMOVED);
    tester.get(RuleDao.class).insert(session, removedRule);

    IssueDto issue = IssueTesting.newDto(removedRule, file, project).setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_REMOVED);
    tester.get(IssueDao.class).insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    service.addComment(issue.getKey(), "my comment", MockUserSession.get());

    List<DefaultIssueComment> comments = service.findComments(issue.getKey());
    assertThat(comments).hasSize(1);
    assertThat(comments.get(0).markdownText()).isEqualTo("my comment");
  }

}
