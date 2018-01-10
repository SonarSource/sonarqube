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
package org.sonarqube.tests.projectAdministration;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category1Suite;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.pageobjects.Navigation;
import org.sonarqube.qa.util.pageobjects.ProjectPermissionsPage;
import util.user.UserRule;

import static util.ItUtils.projectDir;
import static util.selenium.Selenese.runSelenese;

public class ProjectPermissionsTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Rule
  public UserRule userRule = UserRule.from(orchestrator);

  private Navigation nav = Navigation.create(orchestrator);
  private String adminUser;

  @BeforeClass
  public static void beforeClass() {
    executeBuild("project-permissions-project", "Test Project");
    executeBuild("project-permissions-project-2", "Another Test Project");
  }

  @Before
  public void before() {
    adminUser = userRule.createAdminUser();
  }

  @Test
  public void test_project_permissions_page_shows_only_single_project() {
    runSelenese(orchestrator, "/projectAdministration/ProjectPermissionsTest/test_project_permissions_page_shows_only_single_project.html");
  }

  @Test
  public void change_project_visibility() {
    ProjectPermissionsPage page = nav.logIn().submitCredentials(adminUser).openProjectPermissions("project-permissions-project");
    page
      .shouldBePublic()
      .turnToPrivate()
      .turnToPublic();
  }

  private static void executeBuild(String projectKey, String projectName) {
    orchestrator.executeBuild(
      SonarScanner.create(projectDir("shared/xoo-sample"))
        .setProjectKey(projectKey)
        .setProjectName(projectName)
    );
  }
}
