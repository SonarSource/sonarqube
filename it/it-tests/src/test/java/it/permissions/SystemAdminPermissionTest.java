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
package it.permissions;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category4Suite;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import util.ItUtils;

public class SystemAdminPermissionTest {

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @BeforeClass
  public static void setUpUsers() {
    orchestrator.resetData();

    WsClient client = ItUtils.newAdminWsClient(orchestrator);

    createUser(client, "can_share", "password");
    addPermission("can_share", "shareDashboard");

    createUser(client, "cannot_share", "password");
  }

  @AfterClass
  public static void reset() {
    WsClient client = ItUtils.newAdminWsClient(orchestrator);
    deactivateUser(client, "can_share");
    deactivateUser(client, "cannot_share");
  }

  /**
   * SONAR-4398
   */
  @Test
  public void should_change_ownership_of_shared_measure_filter() throws Exception {
    // change-own-measure-filter-owner.html
    // 1 - as admin, create measure filter, shared with every one
    // 2 - as admin, edit filter and set owner to can_share
    seleniumSuite("change-measure-filter-ownership",
      "/permissions/SystemAdminPermissionTest/change-own-measure-filter-owner.html",
      "/permissions/SystemAdminPermissionTest/change-other-measure-filter-owner.html",
      "/permissions/SystemAdminPermissionTest/change-system-measure-filter-owner.html");
  }

  /**
   * SONAR-4136
   */
  @Test
  public void should_change_ownership_of_shared_global_dashboard() throws Exception {
    seleniumSuite("change-global-dashboard-ownership",
      "/permissions/SystemAdminPermissionTest/change-shared-global-dashboard-owner.html",
      "/permissions/SystemAdminPermissionTest/change-shared-global-dashboard-owner-failure.html");
  }

  /**
   * SONAR-4136
   */
  @Test
  public void should_change_ownership_of_shared_project_dashboard() throws Exception {
    orchestrator.executeBuild(SonarScanner.create(ItUtils.projectDir("shared/xoo-sample")));

    seleniumSuite("change-project-dashboard-ownership",
      "/permissions/SystemAdminPermissionTest/change-shared-project-dashboard-owner.html",
      "/permissions/SystemAdminPermissionTest/change-shared-project-dashboard-owner-failure.html");
  }

  private void seleniumSuite(String suiteName, String... tests) {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath(suiteName, tests).build();
    orchestrator.executeSelenese(selenese);
  }

  private static void addPermission(String login, String permission) {
    orchestrator.getServer().adminWsClient().post("api/permissions/add_user",
      "login", login,
      "permission", permission);
  }

  private static void createUser(WsClient client, String login, String password) {
    client.wsConnector().call(
      new PostRequest("api/users/create")
        .setParam("login", login)
        .setParam("name", login)
        .setParam("password", password)
      );
  }

  private static void deactivateUser(WsClient client, String login) {
    client.wsConnector().call(
      new PostRequest("/api/users/deactivate")
        .setParam("login", login)
      );
  }
}
