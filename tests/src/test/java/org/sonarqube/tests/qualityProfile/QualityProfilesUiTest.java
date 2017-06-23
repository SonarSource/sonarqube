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
package org.sonarqube.tests.qualityProfile;

import com.codeborne.selenide.Condition;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category4Suite;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.qualityprofile.AddProjectRequest;
import org.sonarqube.ws.client.qualityprofile.ChangeParentRequest;
import org.sonarqube.ws.client.qualityprofile.CreateRequest;
import org.sonarqube.pageobjects.Navigation;
import util.user.UserRule;

import static com.codeborne.selenide.Selenide.$;
import static util.ItUtils.projectDir;

public class QualityProfilesUiTest {

  private static final String ADMIN_USER_LOGIN = "admin-user";

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Rule
  public UserRule userRule = UserRule.from(orchestrator);

  private static WsClient adminWsClient;

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Before
  public void initAdminUser() throws Exception {
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
    tester.runHtmlTests(
      "/qualityProfile/QualityProfilesUiTest/should_display_list.html",
      "/qualityProfile/QualityProfilesUiTest/should_open_from_list.html",
      "/qualityProfile/QualityProfilesUiTest/should_filter_by_language.html");
  }

  @Test
  public void testProfilePage() {
    tester.runHtmlTests(
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
    tester.runHtmlTests(
      "/qualityProfile/QualityProfilesUiTest/should_display_changelog.html");
  }

  @Ignore("find a way to know profile key inside selenium tests")
  @Test
  public void testComparison() {
    tester.runHtmlTests("/qualityProfile/QualityProfilesUiTest/should_compare.html");
  }

  @Test
  public void testCreation() {
    tester.runHtmlTests("/qualityProfile/QualityProfilesUiTest/should_create.html");
  }

  @Test
  public void testDeletion() {
    tester.runHtmlTests("/qualityProfile/QualityProfilesUiTest/should_delete.html");
  }

  @Test
  public void testCopying() {
    tester.runHtmlTests("/qualityProfile/QualityProfilesUiTest/should_copy.html");
  }

  @Test
  public void testRenaming() {
    tester.runHtmlTests("/qualityProfile/QualityProfilesUiTest/should_rename.html");
  }

  @Test
  public void testSettingDefault() {
    tester.runHtmlTests("/qualityProfile/QualityProfilesUiTest/should_set_default.html");
  }

  @Test
  public void testRestore() {
    deleteProfile("xoo", "empty");

    tester.runHtmlTests("/qualityProfile/QualityProfilesUiTest/should_restore.html");
  }

  private void createProfile(String language, String name) {
    tester.wsClient().qualityProfiles().create(CreateRequest.builder()
      .setLanguage(language)
      .setProfileName(name)
      .build());
  }

  private void inheritProfile(String language, String name, String parentName) {
    tester.wsClient().qualityProfiles().changeParent(ChangeParentRequest.builder()
      .setLanguage(language)
      .setProfileName(name)
      .setParentName(parentName)
      .build());
  }

  private static void analyzeProject(String path) {
    orchestrator.executeBuild(SonarScanner.create(projectDir(path)));
  }

  private void addProfileToProject(String language, String profileName, String projectKey) {
    tester.wsClient().qualityProfiles().addProject(AddProjectRequest.builder()
      .setLanguage(language)
      .setProfileName(profileName)
      .setProjectKey(projectKey)
      .build());
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
