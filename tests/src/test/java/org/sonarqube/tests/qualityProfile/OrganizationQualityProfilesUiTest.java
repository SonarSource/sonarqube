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
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.pageobjects.Navigation;
import org.sonarqube.pageobjects.QualityProfilePage;
import org.sonarqube.pageobjects.RulesPage;
import org.sonarqube.tests.Category6Suite;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.QualityProfiles;
import org.sonarqube.ws.WsUsers.CreateWsResponse.User;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.qualityprofile.AddProjectRequest;
import org.sonarqube.ws.client.qualityprofile.ChangeParentRequest;

import static com.codeborne.selenide.Selenide.$;
import static util.ItUtils.projectDir;

public class OrganizationQualityProfilesUiTest {

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  private Organization organization;
  private User user;

  @Before
  public void setUp() {
    // key and name are overridden for HTML Selenese tests
    organization = tester.organizations().generate(o -> o.setKey("test-org").setName("test-org"));
    user = tester.users().generateAdministrator(organization, u -> u.setLogin("admin2").setPassword("admin2"));
    createProfile("xoo", "sample");
    inheritProfile("xoo", "sample", "Basic");
    analyzeProject("shared/xoo-sample");
    addProfileToProject("xoo", "sample", "sample");
  }

  @Test
  public void testNoGlobalPage() {
    Navigation nav = tester.openBrowser();
    nav.open("/profiles");
    $(".page-wrapper-simple").should(Condition.visible);
  }

  @Test
  public void testHomePage() {
    tester.runHtmlTests(
      "/organization/OrganizationQualityProfilesUiTest/should_display_list.html",
      "/organization/OrganizationQualityProfilesUiTest/should_open_from_list.html",
      "/organization/OrganizationQualityProfilesUiTest/should_filter_by_language.html");
  }

  @Test
  public void testProfilePage() {
    tester.runHtmlTests(
      "/organization/OrganizationQualityProfilesUiTest/should_display_profile_rules.html",
      "/organization/OrganizationQualityProfilesUiTest/should_display_profile_inheritance.html",
      "/organization/OrganizationQualityProfilesUiTest/should_display_profile_exporters.html");

    tester.openBrowser().logIn().submitCredentials(user.getLogin())
      .openQualityProfile("xoo", "sample", organization.getKey())
      .shouldHaveAssociatedProject("Sample")
      .shouldAllowToChangeProjects();
  }

  @Test
  public void testNotFound() {
    Navigation nav = tester.openBrowser();
    nav.open("/organizations/" + organization.getKey() + "/quality_profiles/show?key=unknown");
    $(".quality-profile-not-found").should(Condition.visible);

    nav.open("/organizations/" + organization.getKey() + "/quality_profiles/show?language=xoo&name=unknown");
    $(".quality-profile-not-found").should(Condition.visible);
  }

  @Test
  public void testProfileChangelog() {
    tester.runHtmlTests(
      "/organization/OrganizationQualityProfilesUiTest/should_display_changelog.html");
  }

  @Ignore("to be replaced by selenide test in order to inject profile key")
  @Test
  public void testComparison() {
    tester.runHtmlTests("/organization/OrganizationQualityProfilesUiTest/should_compare.html");
  }

  @Test
  public void testCreation() {
    tester.runHtmlTests("/organization/OrganizationQualityProfilesUiTest/should_create.html");
  }

  @Test
  public void testDeletion() {
    tester.runHtmlTests("/organization/OrganizationQualityProfilesUiTest/should_delete.html");
  }

  @Test
  public void testCopying() {
    tester.runHtmlTests("/organization/OrganizationQualityProfilesUiTest/should_copy.html");
  }

  @Test
  public void testRenaming() {
    tester.runHtmlTests("/organization/OrganizationQualityProfilesUiTest/should_rename.html");
  }

  @Test
  public void testSettingDefault() {
    tester.runHtmlTests("/organization/OrganizationQualityProfilesUiTest/should_set_default.html");
  }

  @Test
  public void testRestoration() {
    deleteProfile("xoo", "empty");

    tester.runHtmlTests("/organization/OrganizationQualityProfilesUiTest/should_restore.html");
  }

  @Test
  public void testSonarWayComparison() {
    QualityProfiles.CreateWsResponse.QualityProfile xooProfile = tester.qProfiles().createXooProfile(organization);
    tester.qProfiles().activateRule(xooProfile, "xoo:OneBugIssuePerLine");
    tester.qProfiles().activateRule(xooProfile, "xoo:OneIssuePerLine");
    Navigation nav = tester.openBrowser();
    QualityProfilePage qpPage = nav.openQualityProfile(xooProfile.getLanguage(), xooProfile.getName(), organization.getKey());
    qpPage.shouldHaveMissingSonarWayRules(2);
    RulesPage rPage = qpPage.showMissingSonarWayRules();
    rPage.shouldHaveTotalRules(2);
    rPage.getSelectedFacetItems("qprofile")
      .shouldHaveSize(2)
      .findBy(Condition.cssClass("compare")).has(Condition.text("Sonar way"));
  }

  private void createProfile(String language, String name) {
    tester.wsClient().wsConnector().call(
      new PostRequest("api/qualityprofiles/create")
        .setParam("language", language)
        .setParam("name", name)
        .setParam("organization", organization.getKey()));
  }

  private void inheritProfile(String language, String name, String parentName) {
    tester.wsClient().qualityProfiles().changeParent(ChangeParentRequest.builder()
      .setLanguage(language)
      .setProfileName(name)
      .setParentName(parentName)
      .setOrganization(organization.getKey())
      .build());
  }

  private void analyzeProject(String path) {
    orchestrator.executeBuild(SonarScanner.create(projectDir(path)).setProperties(
      "sonar.organization", organization.getKey(),
      "sonar.login", "admin",
      "sonar.password", "admin"));
  }

  private void addProfileToProject(String language, String profileName, String projectKey) {
    tester.wsClient().qualityProfiles().addProject(AddProjectRequest.builder()
      .setLanguage(language)
      .setQualityProfile(profileName)
      .setProjectKey(projectKey)
      .setOrganization(organization.getKey())
      .build());
  }

  private void deleteProfile(String language, String name) {
    tester.wsClient().wsConnector().call(
      new PostRequest("api/qualityprofiles/delete")
        .setParam("language", language)
        .setParam("profileName", name)
        .setParam("organization", organization.getKey()));
  }

}
