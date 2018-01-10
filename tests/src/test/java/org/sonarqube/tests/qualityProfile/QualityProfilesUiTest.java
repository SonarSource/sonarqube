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
package org.sonarqube.tests.qualityProfile;

import com.codeborne.selenide.Condition;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.Navigation;
import org.sonarqube.tests.Category4Suite;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.qualityprofiles.AddProjectRequest;
import org.sonarqube.ws.client.qualityprofiles.ChangeParentRequest;
import org.sonarqube.ws.client.qualityprofiles.CreateRequest;
import util.selenium.Selenese;
import util.user.UserRule;

import static com.codeborne.selenide.Selenide.$;
import static util.ItUtils.projectDir;

public class QualityProfilesUiTest {

  private static final String ADMIN_USER_LOGIN = "admin-user";

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Rule
  public UserRule userRule = UserRule.from(orchestrator);

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Before
  public void initAdminUser() {
    userRule.createAdminUser(ADMIN_USER_LOGIN, ADMIN_USER_LOGIN);
  }

  @After
  public void deleteAdminUser() {
    userRule.resetUsers();
  }

  @Before
  public void createSampleProfile() {
    createProfile("xoo", "sample");
    inheritProfile("xoo", "sample", "Basic");
    analyzeProject("shared/xoo-sample");
    addProfileToProject("xoo", "sample", "sample");
  }

  @After
  public void tearDown() {
    setDefault("xoo", "Basic");
    deleteProfile("xoo", "sample");
    deleteProfile("xoo", "new name");
  }

  @Test
  public void testHomePage() {
    Selenese.runSelenese(orchestrator,
      "/qualityProfile/QualityProfilesUiTest/should_display_list.html",
      "/qualityProfile/QualityProfilesUiTest/should_open_from_list.html",
      "/qualityProfile/QualityProfilesUiTest/should_filter_by_language.html");
  }

  @Test
  public void testProfilePage() {
    Selenese.runSelenese(orchestrator,
      "/qualityProfile/QualityProfilesUiTest/should_display_profile_rules.html",
      "/qualityProfile/QualityProfilesUiTest/should_display_profile_inheritance.html",
      "/qualityProfile/QualityProfilesUiTest/should_display_profile_projects.html",
      "/qualityProfile/QualityProfilesUiTest/should_display_profile_exporters.html");
  }

  @Test
  public void testNotFound() {
    Navigation nav = tester.openBrowser();

    nav.open("/profiles/show?key=unknown");
    $(".quality-profile-not-found").should(Condition.visible);

    nav.open("/profiles/show?language=xoo&name=unknown");
    $(".quality-profile-not-found").should(Condition.visible);
  }

  @Test
  public void testProfileChangelog() {
    Selenese.runSelenese(orchestrator,
      "/qualityProfile/QualityProfilesUiTest/should_display_changelog.html");
  }

  @Test
  public void testComparison() {
    Navigation nav = tester.openBrowser();
    nav.open("/profiles/show?language=xoo&name=sample");
    $(".quality-profile-header .dropdown-toggle").click();
    $("#quality-profile-compare").click();
    $(".js-profile-comparison .Select-control").click();
  }

  @Test
  public void testCreation() {
    Selenese.runSelenese(orchestrator, "/qualityProfile/QualityProfilesUiTest/should_create.html");
  }

  @Test
  public void testDeletion() {
    Selenese.runSelenese(orchestrator, "/qualityProfile/QualityProfilesUiTest/should_delete.html");
  }

  @Test
  public void testCopying() {
    Selenese.runSelenese(orchestrator, "/qualityProfile/QualityProfilesUiTest/should_copy.html");
  }

  @Test
  public void testRenaming() {
    Selenese.runSelenese(orchestrator, "/qualityProfile/QualityProfilesUiTest/should_rename.html");
  }

  @Test
  public void testSettingDefault() {
    Selenese.runSelenese(orchestrator, "/qualityProfile/QualityProfilesUiTest/should_set_default.html");
  }

  @Test
  public void testRestore() {
    deleteProfile("xoo", "empty");

    Selenese.runSelenese(orchestrator, "/qualityProfile/QualityProfilesUiTest/should_restore.html");
  }

  private void createProfile(String language, String name) {
    tester.wsClient().qualityprofiles().create(new CreateRequest()
      .setLanguage(language)
      .setName(name));
  }

  private void inheritProfile(String language, String name, String parentName) {
    tester.wsClient().qualityprofiles().changeParent(new ChangeParentRequest()
      .setLanguage(language)
      .setQualityProfile(name)
      .setParentQualityProfile(parentName));
  }

  private static void analyzeProject(String path) {
    orchestrator.executeBuild(SonarScanner.create(projectDir(path)));
  }

  private void addProfileToProject(String language, String profileName, String projectKey) {
    tester.wsClient().qualityprofiles().addProject(new AddProjectRequest()
      .setLanguage(language)
      .setQualityProfile(profileName)
      .setProject(projectKey));
  }

  private void deleteProfile(String language, String name) {
    tester.wsClient().wsConnector().call(
      new PostRequest("api/qualityprofiles/delete")
        .setParam("language", language)
        .setParam("profileName", name));
  }

  private void setDefault(String language, String name) {
    tester.wsClient().wsConnector().call(
      new PostRequest("api/qualityprofiles/set_default")
        .setParam("language", language)
        .setParam("profileName", name));
  }
}
