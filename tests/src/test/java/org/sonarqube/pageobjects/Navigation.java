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
package org.sonarqube.pageobjects;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.sonar.orchestrator.Orchestrator;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.html5.WebStorage;
import org.sonarqube.pageobjects.issues.IssuesPage;
import org.sonarqube.pageobjects.measures.MeasuresPage;
import org.sonarqube.pageobjects.organization.MembersPage;
import org.sonarqube.pageobjects.projects.ProjectsPage;
import org.sonarqube.pageobjects.settings.SettingsPage;
import org.sonarqube.tests.Tester;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.clearBrowserLocalStorage;
import static com.codeborne.selenide.Selenide.page;

public class Navigation {

  public Navigation() {
    $("#content").shouldBe(Condition.exist);
  }

  /**
   * @deprecated use {@link Tester#openBrowser()}
   */
  @Deprecated
  public static Navigation create(Orchestrator orchestrator) {
    WebDriver driver = SelenideConfig.configure(orchestrator);
    driver.manage().deleteAllCookies();
    clearStorage(d -> d.getLocalStorage().clear());
    clearStorage(d -> d.getSessionStorage().clear());
    clearStorage(d -> clearBrowserLocalStorage());
    return Selenide.open("/projects", Navigation.class);
  }

  private static void clearStorage(Consumer<WebStorage> cleaner) {
    try {
      cleaner.accept((WebStorage) WebDriverRunner.getWebDriver());
    } catch (Exception e) {
      // ignore, it may occur when the first test opens browser. No pages are loaded
      // and local/session storages are not available yet.
      // Example with Chrome: "Failed to read the 'localStorage' property from 'Window': Storage is disabled inside 'data:' URLs."
    }
  }

  public Navigation openHome() {
    return open("/", Navigation.class);
  }

  public ProjectsPage openProjects() {
    return open("/projects", ProjectsPage.class);
  }

  public ProjectsPage openProjects(String organization) {
    return open("/organizations/" + organization + "/projects", ProjectsPage.class);
  }

  public IssuesPage openIssues() {
    return open("/issues", IssuesPage.class);
  }

  public IssuesPage openIssues(String organization) {
    return open("/organizations/" + organization + "/issues", IssuesPage.class);
  }

  public IssuesPage openComponentIssues(String component) {
    return open("/component_issues?id=" + component, IssuesPage.class);
  }

  public ProjectDashboardPage openProjectDashboard(String projectKey) {
    // TODO encode projectKey
    String url = "/dashboard?id=" + projectKey;
    return open(url, ProjectDashboardPage.class);
  }

  public ProjectLinksPage openProjectLinks(String projectKey) {
    // TODO encode projectKey
    String url = "/project/links?id=" + projectKey;
    return open(url, ProjectLinksPage.class);
  }

  public QualityGatePage openQualityGates() {
    String url = "/quality_gates";
    return open(url, QualityGatePage.class);
  }

  public QualityGatePage openQualityGates(String organization) {
    String url = "/organizations/" + organization + "/quality_gates";
    return open(url, QualityGatePage.class);
  }

  public ProjectQualityGatePage openProjectQualityGate(String projectKey) {
    // TODO encode projectKey
    String url = "/project/quality_gate?id=" + projectKey;
    return open(url, ProjectQualityGatePage.class);
  }

  public ProjectKeyPage openProjectKey(String projectKey) {
    // TODO encode projectKey
    String url = "/project/key?id=" + projectKey;
    return open(url, ProjectKeyPage.class);
  }

  public ProjectActivityPage openProjectActivity(String projectKey) {
    // TODO encode projectKey
    String url = "/project/activity?id=" + projectKey;
    return open(url, ProjectActivityPage.class);
  }

  public MeasuresPage openProjectMeasures(String projectKey) {
    // TODO encode projectKey
    String url = "/component_measures?id=" + projectKey;
    return open(url, MeasuresPage.class);
  }

  public MembersPage openOrganizationMembers(String orgKey) {
    String url = "/organizations/" + orgKey + "/members";
    return open(url, MembersPage.class);
  }

  public QualityProfilePage openQualityProfile(String language, String name, String organization) {
    String profileUrl = "/quality_profiles/show?language=" + language + "&name=" + name;
    return open("/organizations/" + organization + profileUrl , QualityProfilePage.class);
  }

  public BackgroundTasksPage openBackgroundTasksPage() {
    return open("/background_tasks", BackgroundTasksPage.class);
  }

  public SettingsPage openSettings(@Nullable String projectKey) throws UnsupportedEncodingException {
    String url = projectKey != null ? "/project/settings?id=" + URLEncoder.encode(projectKey, "UTF-8") : "/settings";
    return open(url, SettingsPage.class);
  }

  public EncryptionPage openEncryption() {
    return open("/settings/encryption", EncryptionPage.class);
  }

  public SystemInfoPage openSystemInfo() {
    return open("/admin/system", SystemInfoPage.class);
  }

  public MarketplacePage openMarketplace() { return open("/admin/marketplace", MarketplacePage.class);}

  public NotificationsPage openNotifications() {
    return open("/account/notifications", NotificationsPage.class);
  }

  public ProjectPermissionsPage openProjectPermissions(String projectKey) {
    String url = "/project_roles?id=" + projectKey;
    return open(url, ProjectPermissionsPage.class);
  }

  public ProjectsManagementPage openProjectsManagement() {
    return open("/projects_admin", ProjectsManagementPage.class);
  }

  public LoginPage openLogin() {
    return open("/sessions/login", LoginPage.class);
  }

  public void open(String relativeUrl) {
    Selenide.open(relativeUrl);
  }

  public <P> P open(String relativeUrl, Class<P> pageObjectClassClass) {
    return Selenide.open(relativeUrl, pageObjectClassClass);
  }

  public Navigation shouldBeLoggedIn() {
    loggedInDropdown().should(visible);
    return this;
  }

  public Navigation shouldNotBeLoggedIn() {
    logInLink().should(visible);
    return this;
  }

  public LoginPage logIn() {
    logInLink().click();
    return page(LoginPage.class);
  }

  public Navigation logOut() {
    SelenideElement dropdown = loggedInDropdown();
    // click must be on the <a> but not on the dropdown <li>
    // for compatibility with phantomjs
    dropdown.find(".dropdown-toggle").click();
    dropdown.find(By.linkText("Log out")).click();
    return this;
  }

  public RulesPage openRules() {
    return open("/coding_rules", RulesPage.class);
  }

  public SelenideElement clickOnQualityProfiles() {
    return $(By.linkText("Quality Profiles"));
  }

  public SelenideElement getRightBar() {
    return $("#global-navigation .navbar-right");
  }

  public SelenideElement getFooter() {
    return $("#footer");
  }

  public SelenideElement getErrorMessage() {
    return $("#error");
  }

  private SelenideElement logInLink() {
    return $(By.linkText("Log in"));
  }

  private SelenideElement loggedInDropdown() {
    return $(".js-user-authenticated");
  }

  public Navigation shouldBeRedirectedToLogin() {
    $("#login_form").should(visible);
    return this;
  }

}
