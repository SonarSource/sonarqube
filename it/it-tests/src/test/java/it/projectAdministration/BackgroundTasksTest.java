/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package it.projectAdministration;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import it.Category1Suite;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import pageobjects.BackgroundTaskItem;
import pageobjects.BackgroundTasksPage;
import pageobjects.Navigation;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static util.ItUtils.projectDir;
import static util.selenium.Selenese.runSelenese;

public class BackgroundTasksTest {

  @ClassRule
  public static Orchestrator ORCHESTRATOR = Category1Suite.ORCHESTRATOR;

  @Rule
  public Navigation nav = Navigation.get(ORCHESTRATOR);

  @BeforeClass
  public static void beforeClass() {
    executeBuild("test-project", "Test Project");
    executeBuild("test-project-2", "Another Test Project");
  }

  @Test
  public void should_not_display_failing_and_search_and_filter_elements_on_project_level_page() throws Exception {
    runSelenese(ORCHESTRATOR,
      "/projectAdministration/BackgroundTasksTest/should_not_display_failing_and_search_and_filter_elements_on_project_level_page.html");
  }

  @Test
  public void display_scanner_context() {
    nav.logIn().submitCredentials("admin", "admin");
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
    executeBuild("test-project", "Test Project", "2010-01-01");

    nav.logIn().submitCredentials("admin", "admin");
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
