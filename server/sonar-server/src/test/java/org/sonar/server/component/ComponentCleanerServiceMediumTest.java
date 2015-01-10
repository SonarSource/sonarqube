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

package org.sonar.server.component;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsClient;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.permission.InternalPermissionService;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.IndexClient;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentCleanerServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db;
  IndexClient index;
  DbSession session;

  ComponentCleanerService service;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();

    db = tester.get(DbClient.class);
    index = tester.get(IndexClient.class);
    session = db.openSession(false);
    service = tester.get(ComponentCleanerService.class);
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void delete_project() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    db.componentDao().insert(session, project);
    session.commit();

    service.delete(project.getKey());

    assertThat(db.componentDao().getNullableByKey(session, project.key())).isNull();
  }

  @Test
  public void remove_issue_permission_index_when_deleting_a_project() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    db.componentDao().insert(session, project);

    // project can be seen by anyone
    session.commit();
    MockUserSession.set().setLogin("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    tester.get(InternalPermissionService.class).addPermission(new PermissionChange().setComponentKey(project.getKey()).setGroup(DefaultGroups.ANYONE).setPermission(UserRole.USER));

    assertThat(countIssueAuthorizationDocs()).isEqualTo(1);

    service.delete(project.getKey());

    assertThat(countIssueAuthorizationDocs()).isEqualTo(0);
  }

  @Test
  public void remove_issue_when_deleting_a_project() throws Exception {
    // ARRANGE
    ComponentDto project = ComponentTesting.newProjectDto();
    db.componentDao().insert(session, project);

    RuleDto rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(session, rule);

    ComponentDto file = ComponentTesting.newFileDto(project);
    tester.get(ComponentDao.class).insert(session, file);

    tester.get(IssueDao.class).insert(session, IssueTesting.newDto(rule, file, project));
    session.commit();
    tester.get(IssueIndexer.class).indexAll();

    assertThat(tester.get(IssueIndex.class).countAll()).isEqualTo(1);

    // ACT
    service.delete(project.getKey());

    assertThat(tester.get(IssueIndex.class).countAll()).isEqualTo(0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_to_delete_not_project() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);
    db.componentDao().insert(session, project, file);
    session.commit();

    service.delete(file.getKey());
  }

  private long countIssueAuthorizationDocs() {
    return tester.get(EsClient.class).prepareCount(IssueIndexDefinition.INDEX).setTypes(IssueIndexDefinition.TYPE_AUTHORIZATION).get().getCount();
  }
}
