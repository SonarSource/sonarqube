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

package org.sonar.server.permission;

import com.google.common.collect.Maps;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.user.RoleDao;
import org.sonar.core.user.UserDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.index.IssueAuthorizationDoc;
import org.sonar.server.issue.index.IssueAuthorizationIndex;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import javax.annotation.Nullable;

import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

/**
 * New tests should be added in order to be able to remove InternalPermissionServiceTest
 */
public class InternalPermissionServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db;
  DbSession session;
  IssueAuthorizationIndex index;
  InternalPermissionService service;

  ComponentDto project;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    session = db.openSession(false);
    index = tester.get(IssueAuthorizationIndex.class);
    service = tester.get(InternalPermissionService.class);

    project = ComponentTesting.newProjectDto();
    db.componentDao().insert(session, project);
    session.commit();
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void add_component_user_permission() throws Exception {
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, project.key());

    UserDto user = new UserDto().setLogin("john").setName("John");
    db.userDao().insert(session, user);
    session.commit();

    assertThat(tester.get(RoleDao.class).selectUserPermissions(session, user.getLogin(), project.getId())).isEmpty();
    assertThat(index.getNullableByKey(project.uuid())).isNull();

    service.addPermission(params(user.getLogin(), null, project.key(), UserRole.USER));
    session.commit();

    // Check in db
    assertThat(tester.get(RoleDao.class).selectUserPermissions(session, user.getLogin(), project.getId())).hasSize(1);

    // Check in index
    IssueAuthorizationDoc issueAuthorizationDoc = index.getNullableByKey(project.uuid());
    assertThat(issueAuthorizationDoc).isNotNull();
    assertThat(issueAuthorizationDoc.project()).isEqualTo(project.uuid());
    assertThat(issueAuthorizationDoc.permission()).isEqualTo(UserRole.USER);
    assertThat(issueAuthorizationDoc.users()).containsExactly(user.getLogin());
    assertThat(issueAuthorizationDoc.groups()).isEmpty();
  }

  @Test
  public void remove_component_user_permission() throws Exception {
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, project.key());

    UserDto user1 = new UserDto().setLogin("user1").setName("User1");
    db.userDao().insert(session, user1);

    UserDto user2 = new UserDto().setLogin("user2").setName("User2");
    db.userDao().insert(session, user2);
    session.commit();

    service.addPermission(params(user1.getLogin(), null, project.key(), UserRole.USER));
    service.addPermission(params(user2.getLogin(), null, project.key(), UserRole.USER));
    service.removePermission(params(user1.getLogin(), null, project.key(), UserRole.USER));
    session.commit();

    // Check in db
    assertThat(tester.get(RoleDao.class).selectUserPermissions(session, user1.getLogin(), project.getId())).isEmpty();
    assertThat(tester.get(RoleDao.class).selectUserPermissions(session, user2.getLogin(), project.getId())).hasSize(1);

    // Check in index
    IssueAuthorizationDoc issueAuthorizationDoc = index.getNullableByKey(project.uuid());
    assertThat(issueAuthorizationDoc).isNotNull();
    assertThat(issueAuthorizationDoc.project()).isEqualTo(project.uuid());
    assertThat(issueAuthorizationDoc.permission()).isEqualTo(UserRole.USER);
    assertThat(issueAuthorizationDoc.users()).containsExactly(user2.getLogin());
    assertThat(issueAuthorizationDoc.groups()).isEmpty();
  }

  @Test
  public void remove_all_component_user_permissions() throws Exception {
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, project.key());

    UserDto user = new UserDto().setLogin("user1").setName("User1");
    db.userDao().insert(session, user);
    session.commit();

    service.addPermission(params(user.getLogin(), null, project.key(), UserRole.USER));
    service.removePermission(params(user.getLogin(), null, project.key(), UserRole.USER));
    session.commit();

    // Check in db
    assertThat(tester.get(RoleDao.class).selectUserPermissions(session, user.getLogin(), project.getId())).isEmpty();

    // Check in index
    IssueAuthorizationDoc issueAuthorizationDoc = index.getNullableByKey(project.uuid());
    assertThat(issueAuthorizationDoc).isNull();
  }

  private Map<String, Object> params(@Nullable String login, @Nullable String group, @Nullable String component, String permission) {
    Map<String, Object> params = Maps.newHashMap();
    params.put("user", login);
    params.put("group", group);
    params.put("component", component);
    params.put("permission", permission);
    return params;
  }

}
