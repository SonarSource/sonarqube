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

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.UserDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.platform.Platform;
import org.sonar.server.tester.ServerTester;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

public class IssueAuthorizationIndexMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester()
    .setProperty("sonar.issues.use_es_backend", "true");

  DbClient db;
  DbSession session;
  IssueAuthorizationIndex index;

  ComponentDto project;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    session = db.openSession(false);
    index = tester.get(IssueAuthorizationIndex.class);
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void synchronize() throws Exception {
    project = new ComponentDto()
      .setKey("Sample")
      .setAuthorizationUpdatedAt(DateUtils.parseDate("2014-09-11"));
    db.componentDao().insert(session, project);

    GroupDto sonarUsers = new GroupDto().setName("devs");
    db.groupDao().insert(session, sonarUsers);

    UserDto john = new UserDto().setLogin("john").setName("John").setActive(true);
    db.userDao().insert(session, john);

    tester.get(PermissionFacade.class).insertGroupPermission(project.getId(), "devs", UserRole.USER, session);
    tester.get(PermissionFacade.class).insertUserPermission(project.getId(), john.getId(), UserRole.USER, session);

    session.commit();

    assertThat(index.getNullableByKey(project.getKey())).isNull();
    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));
    session.commit();

    IssueAuthorizationDoc issueAuthorizationDoc = index.getByKey(project.getKey());
    assertThat(issueAuthorizationDoc).isNotNull();
    assertThat(issueAuthorizationDoc.project()).isEqualTo("Sample");
    assertThat(issueAuthorizationDoc.permission()).isEqualTo("user");
    assertThat(issueAuthorizationDoc.groups()).containsExactly("devs");
    assertThat(issueAuthorizationDoc.users()).containsExactly("john");
    assertThat(issueAuthorizationDoc.updatedAt()).isNotNull();

    tester.clearIndexes();
    tester.get(Platform.class).executeStartupTasks();
    assertThat(index.getNullableByKey(project.getKey())).isNotNull();
  }

  @Test
  public void delete_index() throws Exception {
    project = new ComponentDto()
      .setKey("Sample")
      .setAuthorizationUpdatedAt(DateUtils.parseDate("2014-09-11"));
    db.componentDao().insert(session, project);

    GroupDto sonarUsers = new GroupDto().setName("devs");
    db.groupDao().insert(session, sonarUsers);

    UserDto john = new UserDto().setLogin("john").setName("John").setActive(true);
    db.userDao().insert(session, john);

    tester.get(PermissionFacade.class).insertGroupPermission(project.getId(), "devs", UserRole.USER, session);
    tester.get(PermissionFacade.class).insertUserPermission(project.getId(), john.getId(), UserRole.USER, session);

    session.commit();

    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));
    session.commit();
    assertThat(index.getNullableByKey(project.getKey())).isNotNull();

    db.issueAuthorizationDao().deleteByKey(session, project.key());
    session.commit();

    assertThat(index.getNullableByKey(project.getKey())).isNull();
  }

}
