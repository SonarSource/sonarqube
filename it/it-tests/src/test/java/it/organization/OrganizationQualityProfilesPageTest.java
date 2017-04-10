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
package it.organization;

import com.codeborne.selenide.Condition;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import it.Category6Suite;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.organization.CreateWsRequest;
import pageobjects.Navigation;

import static com.codeborne.selenide.Selenide.$;
import static it.Category6Suite.enableOrganizationsSupport;
import static util.ItUtils.deleteOrganizationsIfExists;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.projectDir;
import static util.selenium.Selenese.runSelenese;

public class OrganizationQualityProfilesPageTest {

  private static WsClient adminWsClient;
  private static final String ORGANIZATION = "test-org";

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  @BeforeClass
  public static void setUp() {
    adminWsClient = newAdminWsClient(orchestrator);
    enableOrganizationsSupport();
    createOrganization();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    deleteOrganizationsIfExists(orchestrator, ORGANIZATION);
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
  public void testNoGlobalPage() {
    Navigation nav = Navigation.get(orchestrator);
    nav.open("/profiles");
    $(".page-wrapper-simple").should(Condition.visible);
  }

  @Test
  public void testHomePage() throws Exception {
    runSelenese(orchestrator,
      "/organization/OrganizationQualityProfilesPageTest/should_display_list.html",
      "/organization/OrganizationQualityProfilesPageTest/should_open_from_list.html",
      "/organization/OrganizationQualityProfilesPageTest/should_filter_by_language.html");
  }

  @Test
  public void testProfilePage() throws Exception {
    runSelenese(orchestrator,
      "/organization/OrganizationQualityProfilesPageTest/should_display_profile_rules.html",
      "/organization/OrganizationQualityProfilesPageTest/should_display_profile_inheritance.html",
      "/organization/OrganizationQualityProfilesPageTest/should_display_profile_projects.html",
      "/organization/OrganizationQualityProfilesPageTest/should_display_profile_exporters.html");
  }

  @Test
  public void testNotFound() {
    Navigation nav = Navigation.get(orchestrator);
    nav.open("/organizations/" + ORGANIZATION + "/quality_profiles/show?key=unknown");
    $(".quality-profile-not-found").should(Condition.visible);

    nav.open("/organizations/" + ORGANIZATION + "/quality_profiles/show?language=xoo&name=unknown");
    $(".quality-profile-not-found").should(Condition.visible);
  }

  @Test
  public void testProfileChangelog() throws Exception {
    runSelenese(orchestrator,
      "/organization/OrganizationQualityProfilesPageTest/should_display_changelog.html");
  }

  @Ignore("find a way to know profile key inside selenium tests")
  @Test
  public void testComparison() throws Exception {
    runSelenese(orchestrator, "/organization/OrganizationQualityProfilesPageTest/should_compare.html");
  }

  @Test
  public void testCreation() throws Exception {
    runSelenese(orchestrator, "/organization/OrganizationQualityProfilesPageTest/should_create.html");
  }

  @Test
  public void testDeletion() throws Exception {
    runSelenese(orchestrator, "/organization/OrganizationQualityProfilesPageTest/should_delete.html");
  }

  @Test
  public void testCopying() throws Exception {
    runSelenese(orchestrator, "/organization/OrganizationQualityProfilesPageTest/should_copy.html");
  }

  @Test
  public void testRenaming() throws Exception {
    runSelenese(orchestrator, "/organization/OrganizationQualityProfilesPageTest/should_rename.html");
  }

  @Test
  public void testSettingDefault() throws Exception {
    runSelenese(orchestrator, "/organization/OrganizationQualityProfilesPageTest/should_set_default.html");
  }

  @Test
  public void testRestoration() throws Exception {
    deleteProfile("xoo", "empty");

    runSelenese(orchestrator,
      "/organization/OrganizationQualityProfilesPageTest/should_restore.html",
      "/organization/OrganizationQualityProfilesPageTest/should_restore_built_in.html");
  }

  private static void createProfile(String language, String name) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/qualityprofiles/create")
        .setParam("language", language)
        .setParam("name", name)
        .setParam("organization", ORGANIZATION));
  }

  private static void inheritProfile(String language, String name, String parentName) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/qualityprofiles/change_parent")
        .setParam("language", language)
        .setParam("profileName", name)
        .setParam("parentName", parentName)
        .setParam("organization", ORGANIZATION));
  }

  private static void analyzeProject(String path) {
    orchestrator.executeBuild(SonarScanner.create(projectDir(path)).setProperties(
      "sonar.organization", ORGANIZATION,
      "sonar.login", "admin",
      "sonar.password", "admin"));
  }

  private static void addProfileToProject(String language, String profileName, String projectKey) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/qualityprofiles/add_project")
        .setParam("language", language)
        .setParam("profileName", profileName)
        .setParam("organization", ORGANIZATION)
        .setParam("projectKey", projectKey));
  }

  private static void deleteProfile(String language, String name) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/qualityprofiles/delete")
        .setParam("language", language)
        .setParam("profileName", name)
        .setParam("organization", ORGANIZATION));
  }

  private static void setDefault(String language, String name) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/qualityprofiles/set_default")
        .setParam("language", language)
        .setParam("profileName", name)
        .setParam("organization", ORGANIZATION));
  }

  private static void createOrganization() {
    adminWsClient.organizations().create(new CreateWsRequest.Builder().setKey(ORGANIZATION).setName(ORGANIZATION).build());
  }
}
