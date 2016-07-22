/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package it.authorisation;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category1Suite;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.client.permission.AddUserWsRequest;
import org.sonarqube.ws.client.permission.PermissionsService;
import util.user.UserRule;

import static util.ItUtils.newAdminWsClient;

public class DashboardSharingPermissionTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @ClassRule
  public static UserRule userRule = UserRule.from(orchestrator);

  static String DASHBOARD_USER = "dashboard_user";
  static String CAN_SHARE_DASHBOARDS = "can_share_dashboards";
  static String CANNOT_SHARE_DASHBOARDS = "cannot_share_dashboards";

  static PermissionsService permissionsWsClient;

  @BeforeClass
  public static void setUpUsers() {
    orchestrator.resetData();

    permissionsWsClient = newAdminWsClient(orchestrator).permissions();

    userRule.createUser(DASHBOARD_USER, "password");
    userRule.createUser(CAN_SHARE_DASHBOARDS, "password");
    userRule.createUser(CANNOT_SHARE_DASHBOARDS, "password");

    permissionsWsClient.addUser(new AddUserWsRequest()
      .setLogin(CAN_SHARE_DASHBOARDS)
      .setPermission("shareDashboard")
      );
  }

  @AfterClass
  public static void clearUsers() throws Exception {
    userRule.resetUsers();
  }

  @Before
  public void resetData() throws Exception {
    orchestrator.resetData();
  }

  /**
   * SONAR-4136
   */
  @Test
  public void share_global_dashboard() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("global-dashboard-sharing-permission",
      "/authorisation/DashboardSharingPermissionTest/global-dashboard-sharing-allowed.html",
      "/authorisation/DashboardSharingPermissionTest/global-dashboard-sharing-denied.html")
      .build();
    orchestrator.executeSelenese(selenese);
  }
}
