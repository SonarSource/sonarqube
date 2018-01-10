/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonarqube.tests.ce;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category1Suite;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.pageobjects.BackgroundTaskItem;
import org.sonarqube.qa.util.pageobjects.BackgroundTasksPage;
import org.sonarqube.qa.util.pageobjects.Navigation;
import util.user.UserRule;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static util.ItUtils.projectDir;
import static util.selenium.Selenese.runSelenese;

public class BackgroundTasksTest {

  private static final String ADMIN_USER_LOGIN = "admin-user";

  @ClassRule
  public static Orchestrator ORCHESTRATOR = Category1Suite.ORCHESTRATOR;

  @Rule
  public UserRule userRule = UserRule.from(ORCHESTRATOR);

  private Navigation nav = Navigation.create(ORCHESTRATOR);

  @BeforeClass
  public static void beforeClass() {
    executeBuild("test-project", "Test Project");
    executeBuild("test-project-2", "Another Test Project");
  }

  @Before
  public void before() {
    userRule.createAdminUser(ADMIN_USER_LOGIN, ADMIN_USER_LOGIN);
  }

  @After
  public void deleteAdminUser() {
    userRule.resetUsers();
  }

  @Test
  public void should_not_display_failing_and_search_and_filter_elements_on_project_level_page() {
    runSelenese(ORCHESTRATOR, "/projectAdministration/BackgroundTasksTest/should_not_display_failing_and_search_and_filter_elements_on_project_level_page.html");
  }

  @Test
  public void display_scanner_context() {
    nav.logIn().submitCredentials(ADMIN_USER_LOGIN);
    BackgroundTasksPage page = nav.openBackgroundTasksPage();

    page.getTasks().shouldHave(sizeGreaterThan(0));
    BackgroundTaskItem task = page.getTasksAsItems().get(0);
    task.openActions()
      .openScannerContext()
      .assertScannerContextContains("SonarQube plugins:")
      .assertScannerContextContains("Global properties:");
  }

  @Test
  public void display_error_stacktrace() {
    Navigation nav = Navigation.create(ORCHESTRATOR);
    executeBuild("test-project", "Test Project", "2010-01-01");

    nav.logIn().submitCredentials(ADMIN_USER_LOGIN);
    BackgroundTasksPage page = nav.openBackgroundTasksPage();

    page.getTasks().shouldHave(sizeGreaterThan(0));
    BackgroundTaskItem task = page.getTasksAsItems().get(0);
    task.openActions()
      .openErrorStacktrace()
      .assertErrorStacktraceContains("Date of analysis cannot be older than the date of the last known analysis");
  }

  private static void executeBuild(String projectKey, String projectName) {
    ORCHESTRATOR.executeBuild(
      SonarScanner.create(projectDir("shared/xoo-sample"))
        .setProjectKey(projectKey)
        .setProjectName(projectName));
  }

  private static void executeBuild(String projectKey, String projectName, String date) {
    ORCHESTRATOR.executeBuild(
      SonarScanner.create(projectDir("shared/xoo-sample"))
        .setProjectKey(projectKey)
        .setProjectName(projectName)
        .setProperty("sonar.projectDate", date));
  }
}
