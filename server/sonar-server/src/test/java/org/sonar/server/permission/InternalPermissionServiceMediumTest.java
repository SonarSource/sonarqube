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
import org.sonar.server.db.DbClient;
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
  UserDto user;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    session = db.openSession(false);
    index = tester.get(IssueAuthorizationIndex.class);
    service = tester.get(InternalPermissionService.class);

    user = new UserDto().setLogin("john").setName("John");
    db.userDao().insert(session, user);

    project = new ComponentDto().setKey("Sample").setProjectId(1L);
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

    assertThat(tester.get(RoleDao.class).selectUserPermissions(session, user.getLogin(), project.getId())).isEmpty();
    assertThat(index.getByKey(project.getKey())).isNull();

    service.addPermission(params(user.getLogin(), null, project.key(), UserRole.USER));
    session.commit();

    assertThat(tester.get(RoleDao.class).selectUserPermissions(session, user.getLogin(), project.getId())).hasSize(1);
    assertThat(index.getByKey(project.getKey())).isNotNull();
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
