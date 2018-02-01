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
package org.sonarqube.qa.util.pageobjects;

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
import org.sonarqube.qa.util.SelenideConfig;
import org.sonarqube.qa.util.pageobjects.issues.IssuesPage;
import org.sonarqube.qa.util.pageobjects.measures.MeasuresPage;
import org.sonarqube.qa.util.pageobjects.organization.MembersPage;
import org.sonarqube.qa.util.pageobjects.projects.ProjectsPage;
import org.sonarqube.qa.util.pageobjects.settings.SettingsPage;

import static java.lang.String.format;

public class Navigation {

  public Navigation() {
    Selenide.$("#content").shouldBe(Condition.exist);
  }

  public static Navigation create(Orchestrator orchestrator) {
    return create(orchestrator, "/");
  }

  public static Navigation create(Orchestrator orchestrator, String path) {
    WebDriver driver = SelenideConfig.configure(orchestrator);
    driver.manage().deleteAllCookies();
    clearStorage(d -> d.getLocalStorage().clear());
    clearStorage(d -> d.getSessionStorage().clear());
    clearStorage(d -> Selenide.clearBrowserLocalStorage());
    return Selenide.open(path, Navigation.class);
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

  public ProjectsPage openProjectsWithQuery(String query) {
    return open("/projects?" + query, ProjectsPage.class);
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
    String url = "/dashboard?id=" + escape(projectKey);
    return open(url, ProjectDashboardPage.class);
  }

  public ProjectLinksPage openProjectLinks(String projectKey) {
    String url = "/project/links?id=" + escape(projectKey);
    return open(url, ProjectLinksPage.class);
  }

  public QualityGatePage openQualityGates() {
    String url = "/quality_gates";
    return open(url, QualityGatePage.class);
  }

  public QualityGatePage openQualityGates(String organization) {
    String url = "/organizations/" + escape(organization) + "/quality_gates";
    return open(url, QualityGatePage.class);
  }

  public ProjectQualityGatePage openProjectQualityGate(String projectKey) {
    String url = "/project/quality_gate?id=" + escape(projectKey);
    return open(url, ProjectQualityGatePage.class);
  }

  public ProjectKeyPage openProjectKey(String projectKey) {
    String url = "/project/key?id=" + escape(projectKey);
    return open(url, ProjectKeyPage.class);
  }

  public ProjectActivityPage openProjectActivity(String projectKey) {
    String url = "/project/activity?id=" + escape(projectKey);
    return open(url, ProjectActivityPage.class);
  }

  public MeasuresPage openProjectMeasures(String projectKey) {
    String url = "/component_measures?id=" + escape(projectKey);
    return open(url, MeasuresPage.class);
  }

  public ProjectCodePage openCode(String projectKey) {
    String url = "/code?id=" + escape(projectKey);
    return open(url, ProjectCodePage.class);
  }

  public ProjectCodePage openCode(String projectKey, String selected) {
    String url = "/code?id=" + escape(projectKey) + "&selected=" + escape(selected);
    return open(url, ProjectCodePage.class);
  }

  public MembersPage openOrganizationMembers(String orgKey) {
    String url = "/organizations/" + orgKey + "/members";
    return open(url, MembersPage.class);
  }

  public QualityProfilePage openQualityProfile(String language, String name, String organization) {
    String profileUrl = "/quality_profiles/show?language=" + escape(language) + "&name=" + escape(name);
    return open("/organizations/" + organization + profileUrl, QualityProfilePage.class);
  }

  public BackgroundTasksPage openBackgroundTasksPage() {
    return open("/background_tasks", BackgroundTasksPage.class);
  }

  public SettingsPage openSettings(@Nullable String projectKey) {
    String url = projectKey != null ? ("/project/settings?id=" + escape(projectKey)) : "/settings";
    return open(url, SettingsPage.class);
  }

  public EncryptionPage openEncryption() {
    return open("/settings/encryption", EncryptionPage.class);
  }

  public SystemInfoPage openSystemInfo() {
    return open("/admin/system", SystemInfoPage.class);
  }

  public MarketplacePage openMarketplace() {
    return open("/admin/marketplace", MarketplacePage.class);
  }

  public NotificationsPage openNotifications() {
    return open("/account/notifications", NotificationsPage.class);
  }

  public UsersManagementPage openUsersManagement() {
    return open("/admin/users", UsersManagementPage.class);
  }

  public ProjectPermissionsPage openProjectPermissions(String projectKey) {
    String url = "/project_roles?id=" + escape(projectKey);
    return open(url, ProjectPermissionsPage.class);
  }

  public ProjectsManagementPage openProjectsManagement() {
    return open("/projects_admin", ProjectsManagementPage.class);
  }

  /**
   * Should be replaced by an intermediary OrganizationPage
   */
  public ProjectsManagementPage openProjectsManagement(String orgKey) {
    return open("/organizations/" + orgKey + "/projects_management", ProjectsManagementPage.class);
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
    loggedInDropdown().should(Condition.visible);
    return this;
  }

  public Navigation shouldNotBeLoggedIn() {
    logInLink().should(Condition.visible);
    return this;
  }

  public LoginPage logIn() {
    logInLink().click();
    return Selenide.page(LoginPage.class);
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
    return Selenide.$(By.linkText("Quality Profiles"));
  }

  public SelenideElement getRightBar() {
    return Selenide.$("#global-navigation .navbar-right");
  }

  public SelenideElement getFooter() {
    return Selenide.$("#footer");
  }

  public SelenideElement getErrorMessage() {
    return Selenide.$("#error");
  }

  public EmailAlreadyExistsPage asEmailAlreadyExistsPage() {
    return new EmailAlreadyExistsPage();
  }

  private static SelenideElement logInLink() {
    return Selenide.$(By.linkText("Log in"));
  }

  private static SelenideElement loggedInDropdown() {
    return Selenide.$(".js-user-authenticated");
  }

  /**
   * Safe encoding for  URL parameters
   *
   * @param parameter the parameter to escape value
   * @return the escaped value of parameter
   */
  private static String escape(String parameter) {
    try {
      return URLEncoder.encode(parameter, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException(format("Unable to escape [%s]", parameter));
    }
  }

  public Navigation shouldBeRedirectedToLogin() {
    Selenide.$("#login_form").should(Condition.visible);
    return this;
  }

}
