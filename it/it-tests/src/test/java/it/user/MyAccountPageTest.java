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
import com.sonar.orchestrator.selenium.Selenese;
import it.Category4Suite;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import util.QaOnly;
import util.selenium.SeleneseTest;

import static util.ItUtils.newAdminWsClient;

@Category(QaOnly.class)
public class MyAccountPageTest {

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;
  private static WsClient adminWsClient;

  @BeforeClass
  public static void setUp() {
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
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("should_display_user_details",
      "/user/MyAccountPageTest/should_display_user_details.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void should_change_password() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("should_change_password",
      "/user/MyAccountPageTest/should_change_password.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void should_display_issues() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("should_display_issues",
      "/user/MyAccountPageTest/should_display_issues.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
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

}
