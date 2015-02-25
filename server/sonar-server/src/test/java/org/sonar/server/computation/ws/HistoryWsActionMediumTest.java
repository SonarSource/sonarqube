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

package org.sonar.server.computation.ws;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.activity.Activity;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.user.UserDto;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.computation.ReportActivity;
import org.sonar.server.computation.ReportQueue;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
* TODO replace this medium test by a small test
*/
public class HistoryWsActionMediumTest {
  private static final String DEFAULT_PROJECT_KEY = "DefaultProjectKey";
  private static final String DEFAULT_PROJECT_NAME = "DefaultProjectName";
  private static final String DEFAULT_REPORT_DATA = "default-project";

  @ClassRule
  public static ServerTester tester = new ServerTester();

  private DbClient dbClient;
  private DbSession session;
  private WsTester wsTester;
  private ReportQueue queue;
  private MockUserSession userSession;
  private ActivityService activityService;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    dbClient = tester.get(DbClient.class);
    wsTester = tester.get(WsTester.class);
    session = dbClient.openSession(false);
    queue = tester.get(ReportQueue.class);
    activityService = tester.get(ActivityService.class);

    UserDto connectedUser = new UserDto().setLogin("gandalf").setName("Gandalf");
    dbClient.userDao().insert(session, connectedUser);

    userSession = MockUserSession.set()
      .setLogin(connectedUser.getLogin())
      .setUserId(connectedUser.getId().intValue())
      .setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
  }

  @After
  public void after() {
    MyBatis.closeQuietly(session);
  }

  @Test
  public void add_and_try_to_retrieve_activities() throws Exception {
    insertPermissionsForProject(DEFAULT_PROJECT_KEY);
    queue.add(DEFAULT_PROJECT_KEY, IOUtils.toInputStream(DEFAULT_REPORT_DATA));
    queue.add(DEFAULT_PROJECT_KEY, IOUtils.toInputStream(DEFAULT_REPORT_DATA));
    queue.add(DEFAULT_PROJECT_KEY, IOUtils.toInputStream(DEFAULT_REPORT_DATA));

    List<AnalysisReportDto> reports = queue.all();
    ComponentDto project = ComponentTesting.newProjectDto()
      .setName(DEFAULT_PROJECT_NAME)
      .setKey(DEFAULT_PROJECT_KEY);
    for (AnalysisReportDto report : reports) {
      report.succeed();
      activityService.write(session, Activity.Type.ANALYSIS_REPORT, new ReportActivity(report, project));
    }

    session.commit();
    userSession.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    WsTester.TestRequest request = wsTester.newGetRequest(ComputationWebService.API_ENDPOINT, "history");
    WsTester.Result result = request.execute();

    assertThat(result).isNotNull();
    result.assertJson(getClass(), "list_history_reports.json", false);
  }

  private ComponentDto insertPermissionsForProject(String projectKey) {
    ComponentDto project = new ComponentDto().setKey(projectKey).setId(1L);
    dbClient.componentDao().insert(session, project);

    tester.get(PermissionFacade.class).insertGroupPermission(project.getId(), DefaultGroups.ANYONE, UserRole.USER, session);
    userSession.addProjectPermissions(UserRole.ADMIN, project.key());
    userSession.addProjectPermissions(UserRole.USER, project.key());

    session.commit();

    return project;
  }

  @Test(expected = ForbiddenException.class)
  public void user_rights_is_not_enough_throw_ForbiddenException() throws Exception {
    insertPermissionsForProject(DEFAULT_PROJECT_KEY);
    queue.add(DEFAULT_PROJECT_KEY, IOUtils.toInputStream(DEFAULT_REPORT_DATA));

    AnalysisReportDto report = queue.all().get(0);
    report.succeed();
    // queue.remove(report);
    userSession.setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    WsTester.TestRequest sut = wsTester.newGetRequest(ComputationWebService.API_ENDPOINT, "history");
    sut.execute();
  }
}
