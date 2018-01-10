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
package org.sonarqube.tests.project;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.UnsupportedEncodingException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.Navigation;
import org.sonarqube.qa.util.pageobjects.settings.SettingsPage;

import static com.codeborne.selenide.Selenide.$;
import static util.ItUtils.projectDir;

public class ProjectSettingsTest {

  @ClassRule
  public static Orchestrator orchestrator = ProjectSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  private String adminUser;

  @Before
  public void setUp() {
    adminUser = tester.users().generateAdministratorOnDefaultOrganization().getLogin();
  }

  @Test
  public void display_project_settings() {
    analyzeSample();

    SettingsPage page = tester.openBrowser()
      .logIn()
      .submitCredentials(adminUser)
      .openSettings("sample")
      .assertMenuContains("Analysis Scope")
      .assertMenuContains("Category 1")
      .assertMenuContains("project-only")
      .assertMenuContains("Xoo")
      .assertSettingDisplayed("sonar.dbcleaner.hoursBeforeKeepingOnlyOneSnapshotByDay");

    page.openCategory("project-only")
      .assertSettingDisplayed("prop_only_on_project");

    page.openCategory("General")
      .assertStringSettingValue("sonar.dbcleaner.daysBeforeDeletingClosedIssues", "30")
      .assertStringSettingValue("sonar.leak.period", "previous_version")
      .assertBooleanSettingValue("sonar.dbcleaner.cleanDirectory", true)
      .setStringValue("sonar.dbcleaner.hoursBeforeKeepingOnlyOneSnapshotByDay", "48")
      .assertStringSettingValue("sonar.dbcleaner.hoursBeforeKeepingOnlyOneSnapshotByDay", "48");
  }

  /**
   * Values set on project level must not appear on global level
   */
  @Test
  public void display_correct_global_setting() {
    analyzeSample();
    Navigation nav = tester.openBrowser();
    SettingsPage page = nav.logIn()
      .submitCredentials(adminUser)
      .openSettings("sample")
      .openCategory("Analysis Scope")
      .assertSettingDisplayed("sonar.coverage.exclusions")
      .setStringValue("sonar.coverage.exclusions", "foo")
      .assertStringSettingValue("sonar.coverage.exclusions", "foo");
    nav.logOut();

    // login as root
    tester.wsClient().users().skipOnboardingTutorial();
    nav.logIn().submitCredentials("admin", "admin");
    $(".global-navbar-menu ").$(By.linkText("Administration")).click();
    page
      .openCategory("Analysis Scope")
      .assertSettingDisplayed("sonar.coverage.exclusions")
      .assertStringSettingValue("sonar.coverage.exclusions", "");
  }

  @Test
  public void display_module_settings() {
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-multi-modules-sample")));

    tester.openBrowser().logIn().submitCredentials(adminUser)
      .openSettings("com.sonarsource.it.samples:multi-modules-sample:module_a")
      .assertMenuContains("Analysis Scope")
      .assertSettingDisplayed("sonar.coverage.exclusions");
  }

  private void analyzeSample() {
    SonarScanner scan = SonarScanner.create(projectDir("shared/xoo-sample"))
      .setProperty("sonar.cpd.exclusions", "**/*");
    orchestrator.executeBuild(scan);
  }
}
