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
package org.sonarqube.tests.user;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.Navigation;
import org.sonarqube.ws.Users.CreateWsResponse.User;
import org.sonarqube.ws.client.PostRequest;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static util.ItUtils.projectDir;
import static util.selenium.Selenese.runSelenese;

public class MyAccountPageTest {

  @ClassRule
  public static Orchestrator orchestrator = UserSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  private User administrator;

  @Before
  public void initUser() {
    administrator = tester.users().generateAdministrator();
    createUser("account-user", "User With Account", "user@example.com");
  }

  @Test
  public void should_display_user_details() {
    Navigation nav = tester.openBrowser();
    nav.openLogin().submitCredentials("account-user", "password").shouldBeLoggedIn();
    nav.open("/account");
    $("#name").shouldHave(text("User With Account"));
    $("#login").shouldHave(text("account-user"));
    $("#email").shouldHave(text("user@example.com"));
    $("#groups").shouldHave(text("sonar-users"));
    $("#scm-accounts").shouldHave(text("user@example.com"));
    $("#avatar").shouldBe(visible);
  }

  @Test
  public void should_change_password() {
    Navigation nav = tester.openBrowser();
    nav.openLogin().submitCredentials("account-user", "password").shouldBeLoggedIn();
    nav.open("/account/security");
    $("#old_password").val("password");
    $("#password").val("new_password");
    $("#password_confirmation").val("new_password");
    $("#change-password").click();
    $(".alert-success").shouldBe(visible);
    nav.logOut().logIn().submitCredentials("account-user", "new_password").shouldBeLoggedIn();
  }

  @Test
  public void should_display_projects() {
    // first, try on empty instance
    runSelenese(orchestrator, "/user/MyAccountPageTest/should_display_no_projects.html");

    // then, analyze a project
    analyzeProject("sample");
    grantAdminPermission("account-user", "sample");

    runSelenese(orchestrator, "/user/MyAccountPageTest/should_display_projects.html");
  }

  @Test
  public void notifications() {
    Navigation nav = tester.openBrowser();
    nav.logIn().submitCredentials(administrator.getLogin()).openNotifications()
      .addGlobalNotification("ChangesOnMyIssue")
      .addGlobalNotification("NewIssues")
      .removeGlobalNotification("ChangesOnMyIssue");

    nav.openNotifications()
      .shouldHaveGlobalNotification("NewIssues")
      .shouldNotHaveGlobalNotification("ChangesOnMyIssue");
  }

  private void createUser(String login, String name, String email) {
    tester.wsClient().wsConnector().call(
      new PostRequest("api/users/create")
        .setParam("login", login)
        .setParam("name", name)
        .setParam("email", email)
        .setParam("password", "password"));
  }

  private static void analyzeProject(String projectKey) {
    SonarScanner build = SonarScanner.create(projectDir("qualitygate/xoo-sample"))
      .setProjectKey(projectKey)
      .setProperty("sonar.projectDescription", "Description of a project")
      .setProperty("sonar.links.homepage", "http://example.com");
    orchestrator.executeBuild(build);
  }

  private void grantAdminPermission(String login, String projectKey) {
    tester.wsClient().wsConnector().call(
      new PostRequest("api/permissions/add_user")
        .setParam("login", login)
        .setParam("projectKey", projectKey)
        .setParam("permission", "admin"));
  }
}
