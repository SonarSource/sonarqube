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
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.client.permission.AddUserWsRequest;
import org.sonarqube.ws.client.permission.PermissionsService;
import util.user.UserRule;

import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.runProjectAnalysis;

public class SystemAdminPermissionTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @ClassRule
  public static UserRule userRule = UserRule.from(orchestrator);

  @BeforeClass
  public static void setUpUsers() {
    orchestrator.resetData();

    PermissionsService permissionsWsClient = newAdminWsClient(orchestrator).permissions();

    userRule.createUser("can_share", "password");
    permissionsWsClient.addUser(new AddUserWsRequest().setLogin("can_share").setPermission("shareDashboard"));

    userRule.createUser("cannot_share", "password");
  }

  @AfterClass
  public static void reset() {
    userRule.resetUsers();
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
      "/authorisation/SystemAdminPermissionTest/change-own-measure-filter-owner.html",
      "/authorisation/SystemAdminPermissionTest/change-other-measure-filter-owner.html",
      "/authorisation/SystemAdminPermissionTest/change-system-measure-filter-owner.html");
  }

  /**
   * SONAR-4136
   */
  @Test
  public void should_change_ownership_of_shared_global_dashboard() throws Exception {
    seleniumSuite("change-global-dashboard-ownership",
      "/authorisation/SystemAdminPermissionTest/change-shared-global-dashboard-owner.html",
      "/authorisation/SystemAdminPermissionTest/change-shared-global-dashboard-owner-failure.html");
  }

  /**
   * SONAR-4136
   */
  @Test
  public void should_change_ownership_of_shared_project_dashboard() throws Exception {
    runProjectAnalysis(orchestrator, "shared/xoo-sample");

    seleniumSuite("change-project-dashboard-ownership",
      "/authorisation/SystemAdminPermissionTest/change-shared-project-dashboard-owner.html",
      "/authorisation/SystemAdminPermissionTest/change-shared-project-dashboard-owner-failure.html");
  }

  private void seleniumSuite(String suiteName, String... tests) {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath(suiteName, tests).build();
    orchestrator.executeSelenese(selenese);
  }

}
