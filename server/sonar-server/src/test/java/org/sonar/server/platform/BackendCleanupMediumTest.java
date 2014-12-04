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

package org.sonar.server.platform;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.SnapshotTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.permission.InternalPermissionService;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import static org.fest.assertions.Assertions.assertThat;

public class BackendCleanupMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db;
  DbSession session;
  RuleDto rule;
  ComponentDto project;
  ComponentDto file;
  IssueDto issue;

  BackendCleanup backendCleanup;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    session = db.openSession(false);
    backendCleanup = tester.get(BackendCleanup.class);

    rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(session, rule);

    project = ComponentTesting.newProjectDto().setKey("MyProject");
    tester.get(ComponentDao.class).insert(session, project);
    db.snapshotDao().insert(session, SnapshotTesting.createForProject(project));

    file = ComponentTesting.newFileDto(project).setKey("MyComponent");
    tester.get(ComponentDao.class).insert(session, file);
    db.snapshotDao().insert(session, SnapshotTesting.createForComponent(file, project));
    session.commit();
    // project can be seen by anyone
    MockUserSession.set().setLogin("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    tester.get(InternalPermissionService.class).addPermission(new PermissionChange().setComponentKey(project.getKey()).setGroup(DefaultGroups.ANYONE).setPermission(UserRole.USER));

    issue = IssueTesting.newDto(rule, file, project);
    tester.get(IssueDao.class).insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void clear_db() throws Exception {
    backendCleanup.clearDb();
    session.commit();

    // Everything should be removed from db
    assertThat(tester.get(ComponentDao.class).getNullableByKey(session, project.key())).isNull();
    assertThat(tester.get(ComponentDao.class).getNullableByKey(session, file.key())).isNull();
    assertThat(tester.get(IssueDao.class).selectNullableByKey(session, file.key())).isNull();
    assertThat(tester.get(RuleDao.class).getNullableByKey(session, rule.getKey())).isNull();

    // Nothing should be removed from indexes
    assertThat(tester.get(IssueIndex.class).getNullableByKey(issue.getKey())).isNotNull();
    assertThat(tester.get(RuleIndex.class).getNullableByKey(rule.getKey())).isNotNull();
  }

  @Test
  public void clear_indexes() throws Exception {
    backendCleanup.clearIndexes();
    session.commit();

    // Nothing should be removed from db
    assertThat(tester.get(ComponentDao.class).getNullableByKey(session, project.key())).isNotNull();
    assertThat(tester.get(ComponentDao.class).getNullableByKey(session, file.key())).isNotNull();
    assertThat(tester.get(IssueDao.class).selectNullableByKey(session, issue.getKey())).isNotNull();
    assertThat(tester.get(RuleDao.class).getNullableByKey(session, rule.getKey())).isNotNull();

    // Everything should be removed from indexes
    assertThat(tester.get(IssueIndex.class).getNullableByKey(issue.getKey())).isNull();
    assertThat(tester.get(RuleIndex.class).getNullableByKey(rule.getKey())).isNull();
  }

  @Test
  public void clear_all() throws Exception {
    backendCleanup.clearAll();
    session.commit();

    // Everything should be removed from db
    assertThat(tester.get(ComponentDao.class).getNullableByKey(session, project.key())).isNull();
    assertThat(tester.get(ComponentDao.class).getNullableByKey(session, file.key())).isNull();
    assertThat(tester.get(IssueDao.class).selectNullableByKey(session, file.key())).isNull();
    assertThat(tester.get(RuleDao.class).getNullableByKey(session, rule.getKey())).isNull();

    // Everything should be removed from indexes
    assertThat(tester.get(IssueIndex.class).getNullableByKey(issue.getKey())).isNull();
    assertThat(tester.get(RuleIndex.class).getNullableByKey(rule.getKey())).isNull();
  }

  @Test
  public void reset_data() throws Exception {
    backendCleanup.resetData();
    session.commit();

    // Every projects and issues are removed (from db and indexes)
    assertThat(tester.get(ComponentDao.class).getNullableByKey(session, project.key())).isNull();
    assertThat(tester.get(ComponentDao.class).getNullableByKey(session, file.key())).isNull();
    assertThat(tester.get(IssueDao.class).selectNullableByKey(session, issue.getKey())).isNull();
    assertThat(tester.get(IssueIndex.class).getNullableByKey(issue.getKey())).isNull();

    // Every rules should not be removed (from db and indexes)
    assertThat(tester.get(RuleDao.class).getNullableByKey(session, rule.getKey())).isNotNull();
    assertThat(tester.get(RuleIndex.class).getNullableByKey(rule.getKey())).isNotNull();
  }
}
