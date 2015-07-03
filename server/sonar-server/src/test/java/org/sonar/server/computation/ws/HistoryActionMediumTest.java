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

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.compute.AnalysisReportDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.activity.Activity;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import java.util.Date;

/**
 * TODO replace this medium test by a small test
 */
public class HistoryActionMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withStartupTasks().withEsIndexes();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  HistoryAction sut;
  ActivityService activityService;

  @Before
  public void setUp() {
    tester.clearDbAndIndexes();
    sut = tester.get(HistoryAction.class);
    activityService = tester.get(ActivityService.class);
  }

  @Test
  public void search() throws Exception {
    Activity activity1 = new Activity();
    activity1.setType(Activity.Type.ANALYSIS_REPORT);
    activity1.setAction("LOG_ANALYSIS_REPORT");
    activity1.setData("projectKey", "P1");
    activity1.setData("projectName", "POne");
    activity1.setData("projectUuid", "U1");
    activity1.setData("status", AnalysisReportDto.Status.SUCCESS);
    activity1.setData("submittedAt", new Date());
    activityService.save(activity1);

    Activity activity2 = new Activity();
    activity2.setType(Activity.Type.ANALYSIS_REPORT);
    activity2.setAction("LOG_ANALYSIS_REPORT");
    activity2.setData("projectKey", "P2");
    activity2.setData("projectName", "PTwo");
    activity2.setData("projectUuid", "U2");
    activity2.setData("status", AnalysisReportDto.Status.FAILED);
    activity2.setData("submittedAt", new Date());
    activityService.save(activity2);

    userSessionRule.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    WsTester.TestRequest request = tester.wsTester().newGetRequest("api/computation", "history");
    request.execute().assertJson(getClass(), "list_history_reports.json");
  }

  @Test(expected = ForbiddenException.class)
  public void requires_admin_right() throws Exception {
    WsTester.TestRequest request = tester.wsTester().newGetRequest("api/computation", "history");
    request.execute();
  }
}
