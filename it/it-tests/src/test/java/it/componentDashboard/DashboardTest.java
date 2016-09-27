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
package it.componentDashboard;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category4Suite;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import static util.ItUtils.runProjectAnalysis;

public class DashboardTest {

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @BeforeClass
  public static void scanProject() {
    orchestrator.resetData();
    runProjectAnalysis(orchestrator, "shared/xoo-sample");
  }

  /**
   * SONAR-1929
   */
  @Test
  public void dashboard_extension() {
    seleniumSuite("dashboard_extension",
      // SSF-19
      "/componentDashboard/DashboardTest/dashboard_extension/xss.html");
  }

  @Test
  @Ignore
  public void global_dashboard() {
    seleniumSuite("global_dashboard",
      // SONAR-3462
      "/componentDashboard/DashboardTest/global_dashboard/edit-global-dashboards.html",

      // SONAR-4630
      "/componentDashboard/DashboardTest/global_dashboard/create-global-dashboards-error.html",

      // SONAR-3462
      "/componentDashboard/DashboardTest/global_dashboard/order-global-dashboard.html",

      // SONAR-1927 SONAR-3467
      "/componentDashboard/DashboardTest/global_dashboard/manage-global-dashboard.html",

      // SONAR-2073 SONAR-3459
      "/componentDashboard/DashboardTest/global_dashboard/filter-widget-admin.html",

      // SONAR-2073
      "/componentDashboard/DashboardTest/global_dashboard/filter-widget-anonymous.html",

      // SONAR-3461
      "/componentDashboard/DashboardTest/global_dashboard/default-dashboards.html",

      // SONAR-3457 SONAR-3563
      "/componentDashboard/DashboardTest/global_dashboard/project-widget.html");

    // Remove permission from anonymous user
    removeGroupPermission("anyone", "sample", "user");
    addUserPermission("admin", "sample", "user");
    seleniumSuite("global_dashboard_project_permissions",
      // SONAR-6004
      "/componentDashboard/DashboardTest/global_dashboard/global-dashboard-applies-project-permission.html");

    // Put back permissions
    addGroupPermission("anyone", "sample", "user");
    removeUserPermission("admin", "sample", "user");
  }

  @Test
  public void default_widgets() {
    seleniumSuite("default_widgets",
      "/componentDashboard/DashboardTest/default_widgets/welcome_widget.html",

      // SONAR-4448
      "/componentDashboard/DashboardTest/default_widgets/documentation_and_comments_widget.html");
  }

  private void seleniumSuite(String suiteName, String... tests) {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath(suiteName, tests).build();
    orchestrator.executeSelenese(selenese);
  }

  private static void addUserPermission(String login, String projectKey, String permission) {
    orchestrator.getServer().adminWsClient().post("api/permissions/add_user",
      "login", login,
      "projectKey", projectKey,
      "permission", permission);
  }

  private static void removeUserPermission(String login, String projectKey, String permission) {
    orchestrator.getServer().adminWsClient().post("api/permissions/remove_user",
      "login", login,
      "projectKey", projectKey,
      "permission", permission);
  }

  private static void addGroupPermission(String groupName, String projectKey, String permission) {
    orchestrator.getServer().adminWsClient().post("api/permissions/add_group",
      "groupName", groupName,
      "projectKey", projectKey,
      "permission", permission);
  }

  private static void removeGroupPermission(String groupName, String projectKey, String permission) {
    orchestrator.getServer().adminWsClient().post("api/permissions/remove_group",
      "groupName", groupName,
      "projectKey", projectKey,
      "permission", permission);
  }
}
