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
package pageobjects;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.sonar.orchestrator.Orchestrator;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.annotation.Nullable;
import org.junit.rules.ExternalResource;
import org.openqa.selenium.By;
import pageobjects.issues.IssuesPage;
import pageobjects.licenses.LicensesPage;
import pageobjects.organization.MembersPage;
import pageobjects.projects.ProjectsPage;
import pageobjects.settings.SettingsPage;

import static com.codeborne.selenide.Condition.hasText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.page;

public class Navigation extends ExternalResource {

  public static Navigation get(Orchestrator orchestrator) {
    String browser = orchestrator.getConfiguration().getString("orchestrator.browser", "firefox");
    SelenideConfig.INSTANCE
      .setBrowser(browser)
      .setBaseUrl(orchestrator.getServer().getUrl());
    return new Navigation();
  }

  @Override
  protected void before() throws Throwable {
    WebDriverRunner.getWebDriver().manage().deleteAllCookies();
    openHomepage();
  }

  public Navigation openHomepage() {
    return open("/", Navigation.class);
  }

  public ProjectsPage openProjects() {
    return open("/projects", ProjectsPage.class);
  }

  public IssuesPage openIssues() {
    return open("/issues", IssuesPage.class);
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

  public MembersPage openOrganizationMembers(String orgKey) {
    String url = "/organizations/" + orgKey + "/members";
    return open(url, MembersPage.class);
  }

  public BackgroundTasksPage openBackgroundTasksPage() {
    return open("/background_tasks", BackgroundTasksPage.class);
  }

  public SettingsPage openSettings(@Nullable String projectKey) throws UnsupportedEncodingException {
    String url = projectKey != null ? "/project/settings?id=" + URLEncoder.encode(projectKey, "UTF-8") : "/settings";
    return open(url, SettingsPage.class);
  }

  public LicensesPage openLicenses() {
    return open("/settings/licenses", LicensesPage.class);
  }

  public EncryptionPage openEncryption() {
    return open("/settings/encryption", EncryptionPage.class);
  }

  public ServerIdPage openServerId() {
    return open("/settings/server_id", ServerIdPage.class);
  }

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

  public void shouldBeLoggedIn() {
    loggedInDropdown().should(Condition.visible);
  }

  public void shouldNotBeLoggedIn() {
    logInLink().should(Condition.visible);
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

  public RulesPage clickOnRules() {
    $(By.linkText("Rules")).click();
    return page(RulesPage.class);
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

  public void shouldBeRedirectToLogin() {
    $("#content").should(hasText("Log In to SonarQube"));
  }

}
