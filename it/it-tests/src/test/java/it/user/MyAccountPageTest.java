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
import it.Category1Suite;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.user.UserParameters;
import util.selenium.SeleneseTest;

public class MyAccountPageTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @BeforeClass
  public static void initUser() {
    createUser("account-user", "User With Account", "user@example.com");
  }

  @AfterClass
  public static void deleteTestUser() {
    deactivateUser("account-user");
  }

  @Test
  public void should_display_user_details() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("should_display_user_details",
      "/user/MyAccountPageTest/should_display_user_details.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  private static void createUser(String login, String name, String email) {
    SonarClient client = orchestrator.getServer().adminWsClient();
    UserParameters userCreationParameters = UserParameters.create()
      .login(login)
      .name(name)
      .email(email)
      .password("password")
      .passwordConfirmation("password");
    client.userClient().create(userCreationParameters);
  }

  private static void deactivateUser(String user) {
    orchestrator.getServer().adminWsClient().userClient().deactivate(user);
  }

}
