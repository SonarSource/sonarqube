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
package it.user;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import it.Category4Suite;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import pageobjects.Navigation;

import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.projectDir;
import static util.selenium.Selenese.runSelenese;

public class MyAccountPageTest {

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;
  private static WsClient adminWsClient;

  @Rule
  public Navigation nav = Navigation.get(orchestrator);

  @BeforeClass
  public static void setUp() {
    orchestrator.resetData();
    adminWsClient = newAdminWsClient(orchestrator);
  }

  @Before
  public void initUser() {
    createUser("account-user", "User With Account", "user@example.com");
  }

  @After
  public void deleteTestUser() {
    deactivateUser("account-user");
  }

  @Test
  public void should_display_user_details() throws Exception {
    runSelenese(orchestrator, "/user/MyAccountPageTest/should_display_user_details.html");
  }

  @Test
  public void should_change_password() throws Exception {
    runSelenese(orchestrator, "/user/MyAccountPageTest/should_change_password.html");
  }

  @Test
  public void should_display_projects() throws Exception {
    // first, try on empty instance
    runSelenese(orchestrator, "/user/MyAccountPageTest/should_display_no_projects.html");

    // then, analyze a project
    analyzeProject("sample");
    grantAdminPermission("account-user", "sample");

    runSelenese(orchestrator, "/user/MyAccountPageTest/should_display_projects.html");
  }

  @Test
  public void notifications() {
    nav.logIn().asAdmin().openNotifications()
      .addGlobalNotification("ChangesOnMyIssue")
      .addGlobalNotification("NewIssues")
      .removeGlobalNotification("ChangesOnMyIssue");

    nav.openNotifications()
      .shouldHaveGlobalNotification("NewIssues")
      .shouldNotHaveGlobalNotification("ChangesOnMyIssue");
  }

  private static void createUser(String login, String name, String email) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/users/create")
        .setParam("login", login)
        .setParam("name", name)
        .setParam("email", email)
        .setParam("password", "password"));
  }

  private static void deactivateUser(String login) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/users/deactivate")
        .setParam("login", login));
  }

  private static void analyzeProject(String projectKey) {
    SonarScanner build = SonarScanner.create(projectDir("qualitygate/xoo-sample"))
      .setProjectKey(projectKey)
      .setProperty("sonar.projectDescription", "Description of a project")
      .setProperty("sonar.links.homepage", "http://example.com");
    orchestrator.executeBuild(build);
  }

  private static void grantAdminPermission(String login, String projectKey) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/permissions/add_user")
        .setParam("login", login)
        .setParam("projectKey", projectKey)
        .setParam("permission", "admin"));
  }
}
