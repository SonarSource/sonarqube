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
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.index.IssueAuthorizationIndex;
import org.sonar.server.tester.ServerTester;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

public class ComponentCleanerServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db;
  DbSession session;

  ComponentCleanerService service;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();

    db = tester.get(DbClient.class);
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
    tester.get(PermissionFacade.class).insertGroupPermission(project.getId(), DefaultGroups.ANYONE, UserRole.USER, session);
    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));

    session.commit();

    assertThat(tester.get(IssueAuthorizationIndex.class).getNullableByKey(project.uuid())).isNotNull();

    service.delete(project.getKey());

    assertThat(tester.get(IssueAuthorizationIndex.class).getNullableByKey(project.uuid())).isNull();
  }

  @Test
  public void not_fail_when_deleting_a_view() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto()
      .setQualifier(Qualifiers.VIEW)
      .setUuid(null)
      .setProjectUuid(null);
    db.componentDao().insert(session, project);

    // view can be seen by anyone
    tester.get(PermissionFacade.class).insertGroupPermission(project.getId(), DefaultGroups.ANYONE, UserRole.USER, session);
    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));

    session.commit();

    service.delete(project.getKey());
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_to_delete_not_project() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);
    db.componentDao().insert(session, project, file);
    session.commit();

    service.delete(file.getKey());
  }
}
