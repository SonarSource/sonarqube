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

package org.sonar.server.computation;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.user.UserDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.index.IssueAuthorizationDoc;
import org.sonar.server.issue.index.IssueAuthorizationIndex;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class SynchronizeProjectPermissionsStepMediumTest {
  private static final String DEFAULT_PROJECT_KEY = "123456789-987654321";

  @ClassRule
  public static ServerTester tester = new ServerTester();

  private SynchronizeProjectPermissionsStep sut;

  private AnalysisReportQueue queue;
  private GetAndSetProjectStep getAndSetProjectStep;
  private DbClient db;
  private DbSession session;
  private MockUserSession userSession;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    session = db.openSession(false);
    queue = tester.get(AnalysisReportQueue.class);
    getAndSetProjectStep = tester.get(GetAndSetProjectStep.class);

    sut = tester.get(SynchronizeProjectPermissionsStep.class);

    UserDto connectedUser = new UserDto().setLogin("gandalf").setName("Gandalf");
    db.userDao().insert(session, connectedUser);

    userSession = MockUserSession.set()
      .setLogin(connectedUser.getLogin())
      .setUserId(connectedUser.getId().intValue())
      .setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    session.commit();
  }

  @After
  public void after() {
    MyBatis.closeQuietly(session);
  }

  @Test
  public void add_project_issue_permission_in_index() throws Exception {
    ComponentDto project = insertPermissionsForProject(DEFAULT_PROJECT_KEY);

    queue.add(DEFAULT_PROJECT_KEY, 123L);
    List<AnalysisReportDto> reports = queue.findByProjectKey(DEFAULT_PROJECT_KEY);
    getAndSetProjectStep.execute(session, reports.get(0));

    sut.execute(session, reports.get(0));

    IssueAuthorizationDoc issueAuthorizationIndex = tester.get(IssueAuthorizationIndex.class).getNullableByKey(project.uuid());
    assertThat(issueAuthorizationIndex).isNotNull();
  }

  private ComponentDto insertPermissionsForProject(String projectKey) {
    ComponentDto project = ComponentTesting.newProjectDto().setKey(projectKey);
    db.componentDao().insert(session, project);

    tester.get(PermissionFacade.class).insertGroupPermission(project.getId(), DefaultGroups.ANYONE, UserRole.USER, session);
    userSession.addProjectPermissions(UserRole.USER, project.key());

    session.commit();

    return project;
  }
}
