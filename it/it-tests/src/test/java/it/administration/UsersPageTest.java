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
package it.administration;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category1Suite;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import util.selenium.SeleneseTest;

import static util.ItUtils.newAdminWsClient;

public class UsersPageTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;
  private static WsClient adminWsClient;

  @BeforeClass
  public static void setUp() {
    adminWsClient = newAdminWsClient(orchestrator);
  }

  @Test
  public void generate_and_revoke_user_token() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("generate_and_revoke_user_token",
      "/administration/UsersUITest/generate_and_revoke_user_token.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void admin_should_change_its_own_password() throws Exception {
    createUser("users-page-user", "User");
    makeAdmin("users-page-user");

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("admin_should_change_its_own_password",
      "/administration/UsersUITest/admin_should_change_its_own_password.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);

    deactivateUser("users-page-user");
  }

  private static void createUser(String login, String name) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/users/create")
        .setParam("login", login)
        .setParam("name", name)
        .setParam("password", "password"));
  }

  private static void makeAdmin(String login) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/permissions/add_user")
        .setParam("login", login)
        .setParam("permission", "admin"));
  }

  private static void deactivateUser(String login) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/users/deactivate")
        .setParam("login", login));
  }
}
