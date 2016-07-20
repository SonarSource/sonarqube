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
package it.qualityProfile;

import com.codeborne.selenide.Condition;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category4Suite;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import pageobjects.Navigation;
import util.selenium.SeleneseTest;

import static com.codeborne.selenide.Selenide.$;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.projectDir;

public class QualityProfilesPageTest {

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;
  private static WsClient adminWsClient;

  @BeforeClass
  public static void setUp() {
    adminWsClient = newAdminWsClient(orchestrator);
    orchestrator.resetData();
  }

  @Before
  public void createSampleProfile() {
    createProfile("xoo", "sample");
    inheritProfile("xoo", "sample", "Basic");
    analyzeProject("shared/xoo-sample");
    addProfileToProject("xoo", "sample", "sample");
  }

  @After
  public void deleteSampleProfile() {
    setDefault("xoo", "Basic");
    deleteProfile("xoo", "sample");
    deleteProfile("xoo", "new name");
  }

  @Test
  public void testHomePage() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test_home_page",
      "/qualityProfile/QualityProfilesPageTest/should_display_list.html",
      "/qualityProfile/QualityProfilesPageTest/should_open_from_list.html",
      "/qualityProfile/QualityProfilesPageTest/should_filter_by_language.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void testProfilePage() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test_profile_page",
      "/qualityProfile/QualityProfilesPageTest/should_display_profile_rules.html",
      "/qualityProfile/QualityProfilesPageTest/should_display_profile_inheritance.html",
      "/qualityProfile/QualityProfilesPageTest/should_display_profile_projects.html",
      "/qualityProfile/QualityProfilesPageTest/should_display_profile_exporters.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void testNotFound() {
    Navigation nav = Navigation.get(orchestrator);
    nav.open("/profiles/show?key=unknown");
    $(".quality-profile-not-found").should(Condition.visible);
  }

  @Test
  public void testProfileChangelog() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test_profile_changelog",
      "/qualityProfile/QualityProfilesPageTest/should_display_changelog.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Ignore("find a way to know profile key inside selenium tests")
  @Test
  public void testComparison() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test_comparison",
      "/qualityProfile/QualityProfilesPageTest/should_compare.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void testCreation() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test_creation",
      "/qualityProfile/QualityProfilesPageTest/should_create.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void testDeletion() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test_deletion",
      "/qualityProfile/QualityProfilesPageTest/should_delete.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void testCopying() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test_copying",
      "/qualityProfile/QualityProfilesPageTest/should_copy.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void testRenaming() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test_renaming",
      "/qualityProfile/QualityProfilesPageTest/should_rename.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void testSettingDefault() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test_setting_default",
      "/qualityProfile/QualityProfilesPageTest/should_set_default.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void testRestoration() throws Exception {
    deleteProfile("xoo", "empty");

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test_restoration",
      "/qualityProfile/QualityProfilesPageTest/should_restore.html",
      "/qualityProfile/QualityProfilesPageTest/should_restore_built_in.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  private static void createProfile(String language, String name) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/qualityprofiles/create")
        .setParam("language", language)
        .setParam("name", name));
  }

  private static void inheritProfile(String language, String name, String parentName) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/qualityprofiles/change_parent")
        .setParam("language", language)
        .setParam("profileName", name)
        .setParam("parentName", parentName));
  }

  private static void analyzeProject(String path) {
    orchestrator.executeBuild(SonarScanner.create(projectDir(path)));
  }

  private static void addProfileToProject(String language, String profileName, String projectKey) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/qualityprofiles/add_project")
        .setParam("language", language)
        .setParam("profileName", profileName)
        .setParam("projectKey", projectKey));
  }

  private static void deleteProfile(String language, String name) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/qualityprofiles/delete")
        .setParam("language", language)
        .setParam("profileName", name));
  }

  private static void setDefault(String language, String name) {
     adminWsClient.wsConnector().call(
      new PostRequest("api/qualityprofiles/set_default")
        .setParam("language", language)
        .setParam("profileName", name));
  }
}
