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
package it.authorisation;

import com.google.common.base.Optional;
import com.sonar.orchestrator.Orchestrator;
import it.Category1Suite;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsResponse;
import util.user.UserRule;
import util.user.Users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.setServerProperty;

public class BaseIdentityProviderTest {

  @ClassRule
  public static Orchestrator ORCHESTRATOR = Category1Suite.ORCHESTRATOR;

  @Rule
  public UserRule userRule = UserRule.from(ORCHESTRATOR);

  static String FAKE_PROVIDER_KEY = "fake-base-id-provider";

  static String USER_LOGIN = "john";
  static String USER_PROVIDER_ID = "fake-john";
  static String USER_NAME = "John";
  static String USER_EMAIL = "john@email.com";

  static String USER_NAME_UPDATED = "John Doe";
  static String USER_EMAIL_UPDATED = "john.doe@email.com";

  static WsClient adminWsClient;

  @BeforeClass
  public static void setUp() {
    ORCHESTRATOR.resetData();
    adminWsClient = newAdminWsClient(ORCHESTRATOR);
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-base-id-provider.enabled", "true");
  }

  @After
  public void removeUsers() throws Exception {
    userRule.deactivateUsers(userRule.getUsersByEmails(USER_EMAIL, USER_EMAIL_UPDATED));
  }

  @Test
  public void create_new_user_when_authenticate() throws Exception {
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME, USER_EMAIL);

    userRule.verifyUserDoesNotExist(USER_LOGIN);

    // First connection, user is created
    authenticateWithFakeAuthProvider();

    userRule.verifyUserExists(USER_LOGIN, USER_NAME, USER_EMAIL);
  }

  @Test
  public void update_existing_user_when_authenticate() throws Exception {
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME, USER_EMAIL);

    // First connection, user is created
    authenticateWithFakeAuthProvider();

    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME_UPDATED, USER_EMAIL_UPDATED);

    // Second connection, user should be updated
    authenticateWithFakeAuthProvider();

    userRule.verifyUserExists(USER_LOGIN, USER_NAME_UPDATED, USER_EMAIL_UPDATED);
  }

  @Test
  @Ignore("Waiting for SONAR-7233 to be implemented")
  public void reactivate_disabled_user() throws Exception {
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME, USER_EMAIL);

    userRule.verifyUserDoesNotExist(USER_LOGIN);

    // First connection, user is created
    authenticateWithFakeAuthProvider();

    Optional<Users.User> user = userRule.getUserByLogin(USER_EMAIL);
    assertThat(user).isPresent();

    // Disable user
    userRule.deactivateUsers(user.get());

    // Second connection, user is reactivated
    authenticateWithFakeAuthProvider();
    userRule.verifyUserExists(USER_LOGIN, USER_NAME, USER_EMAIL);
  }

  private void setUserCreatedByAuthPlugin(String login, String providerId, String name, String email){
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-base-id-provider.user", login + "," + providerId + "," + name + "," + email);
  }

  private void authenticateWithFakeAuthProvider() {
    WsResponse response = adminWsClient.wsConnector().call(
      new GetRequest(("/sessions/init/" + FAKE_PROVIDER_KEY)));
    assertThat(response.code()).isEqualTo(200);
  }

}
