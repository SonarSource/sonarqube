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
package org.sonarqube.tests.settings;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.settings.SettingsPage;
import org.sonarqube.tests.component.ComponentSuite;
import org.sonarqube.ws.Users;

import static util.ItUtils.projectDir;

public class ValidatorsTest {

  private Users.CreateWsResponse.User adminUser;

  @ClassRule
  public static Orchestrator orchestrator = ComponentSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Before
  public void initUsers() {
    adminUser = tester.users().generateAdministrator(u -> u.setLogin("admin-user").setPassword("admin-user"));
    tester.users().generate(u -> u.setLogin("random-user").setPassword("random-user"));
  }

  @Test
  public void main_settings_simple_value() {
    SettingsPage page = tester.openBrowser().logIn().submitCredentials(adminUser.getLogin())
      .openSettings(null);
    tester.wsClient().users().skipOnboardingTutorial();

    String elementSelector = "sonar.dbcleaner.hoursBeforeKeepingOnlyOneSnapshotByDay";
    page.assertSettingValueIsNotedAsDefault(elementSelector)
      .changeSettingValue(elementSelector, "1")
      .assertSettingValueCanBeSaved(elementSelector)
      .assertSettingValueCanBeCanceled(elementSelector);

    page.sendDeleteKeyToSettingField(elementSelector)
      .assertSettingValueCannotBeSaved(elementSelector)
      .assertSettingValueCanBeReset(elementSelector)
      .assertSettingValueCanBeCanceled(elementSelector);
  }

  @Test
  public void main_settings_complex_value() {
    SettingsPage page = tester.openBrowser().logIn().submitCredentials(adminUser.getLogin())
      .openSettings(null);
    tester.wsClient().users().skipOnboardingTutorial();

    String elementSelector = "sonar.preview.excludePlugins";
    page.assertSettingValueIsNotedAsDefault(elementSelector)
      .removeFirstValue(elementSelector)
      .assertSettingValueCanBeSaved(elementSelector)
      .assertSettingValueCanBeCanceled(elementSelector);

    page.clickOnCancel(elementSelector)
      .assertSettingValueIsNotedAsDefault(elementSelector);
  }

  @Test
  public void project_settings_simple_value() {
    analyzeSample();

    SettingsPage page = tester.openBrowser().logIn().submitCredentials(adminUser.getLogin())
      .openSettings("sample");
    tester.wsClient().users().skipOnboardingTutorial();

    String elementSelector = "sonar.dbcleaner.hoursBeforeKeepingOnlyOneSnapshotByDay";
    page.assertSettingValueIsNotedAsDefault(elementSelector)
      .changeSettingValue(elementSelector, "1")
      .assertSettingValueCanBeSaved(elementSelector)
      .assertSettingValueCanBeCanceled(elementSelector);

    page.sendDeleteKeyToSettingField(elementSelector)
      .assertSettingValueCannotBeSaved(elementSelector)
      .assertSettingValueCanBeReset(elementSelector)
      .assertSettingValueCanBeCanceled(elementSelector);
  }

  @Test
  public void project_settings_complex_value() {
    analyzeSample();

    SettingsPage page = tester.openBrowser().logIn().submitCredentials(adminUser.getLogin())
      .openSettings("sample");
    tester.wsClient().users().skipOnboardingTutorial();

    String elementSelector = "sonar.issue.ignore.allfile";
    page.openCategory("Analysis Scope")
      .changeSettingValue(elementSelector, "foo")
      .changeSettingValue(elementSelector, 1, "bar")
      .assertSettingValueCanBeSaved(elementSelector)
      .assertSettingValueCanBeCanceled(elementSelector);

    page.clickOnCancel(elementSelector)
      .assertInputCount(elementSelector, 1);
  }

  private void analyzeSample() {
    SonarScanner scan = SonarScanner.create(projectDir("shared/xoo-sample"))
      .setProperty("sonar.cpd.exclusions", "**/*");
    orchestrator.executeBuild(scan);
  }

}
