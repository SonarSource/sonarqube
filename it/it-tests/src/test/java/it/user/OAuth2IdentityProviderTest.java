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
import it.Category4Suite;
import java.net.HttpURLConnection;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsResponse;
import util.user.UserRule;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.setServerProperty;

/**
 * There's only tests specific to OAuth2 in this class
 */
public class OAuth2IdentityProviderTest {

  @ClassRule
  public static Orchestrator ORCHESTRATOR = Category4Suite.ORCHESTRATOR;

  @Rule
  public UserRule userRule = UserRule.from(ORCHESTRATOR);

  static String FAKE_PROVIDER_KEY = "fake-oauth2-id-provider";

  static String USER_LOGIN = "john";
  static String USER_PROVIDER_ID = "fake-john";
  static String USER_NAME = "John";
  static String USER_EMAIL = "john@email.com";

  static WsClient adminWsClient;

  MockWebServer fakeServerAuthProvider;
  String fakeServerAuthProviderUrl;

  @BeforeClass
  public static void resetData() {
    ORCHESTRATOR.resetData();
    adminWsClient = newAdminWsClient(ORCHESTRATOR);
  }

  @After
  public void resetUsers() throws Exception {
    userRule.resetUsers();
  }

  @Before
  public void setUp() throws Exception {
    fakeServerAuthProvider = new MockWebServer();
    fakeServerAuthProvider.start();
    fakeServerAuthProviderUrl = fakeServerAuthProvider.url("").url().toString();
  }

  @After
  public void tearDown() throws Exception {
    fakeServerAuthProvider.shutdown();
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-oauth2-id-provider.enabled", null);
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-oauth2-id-provider.url", null);
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-oauth2-id-provider.user", null);
  }

  @Test
  public void create_new_user_when_authenticate() throws Exception {
    simulateRedirectionToCallback();

    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-oauth2-id-provider.enabled", "true");
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-oauth2-id-provider.url", fakeServerAuthProviderUrl);
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-oauth2-id-provider.user", USER_LOGIN + "," + USER_PROVIDER_ID + "," + USER_NAME + "," + USER_EMAIL);

    authenticateWithFakeAuthProvider();

    userRule.verifyUserExists(USER_LOGIN, USER_NAME, USER_EMAIL);
  }

  private void authenticateWithFakeAuthProvider() {
    WsResponse response = adminWsClient.wsConnector().call(
      new GetRequest(("/sessions/init/" + FAKE_PROVIDER_KEY)));
    assertThat(response.code()).isEqualTo(200);
  }

  private void simulateRedirectionToCallback() {
    fakeServerAuthProvider.enqueue(new MockResponse()
      .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP)
      .addHeader("Location: " + ORCHESTRATOR.getServer().getUrl() + "/oauth2/callback/" + FAKE_PROVIDER_KEY)
      .setBody("Redirect to SonarQube"));
  }

}
