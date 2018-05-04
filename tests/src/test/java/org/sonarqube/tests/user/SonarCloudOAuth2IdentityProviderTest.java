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
import java.net.HttpURLConnection;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.client.organizations.SearchRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarCloudOAuth2IdentityProviderTest {

  @ClassRule
  public static Orchestrator orchestrator = SonarCloudUserSuite.ORCHESTRATOR;

  private static String FAKE_PROVIDER_KEY = "fake-oauth2-id-provider";

  @Rule
  public Tester tester = new Tester(orchestrator);

  private MockWebServer fakeServerAuthProvider;

  @Before
  public void setUp() throws Exception {
    fakeServerAuthProvider = new MockWebServer();
    fakeServerAuthProvider.start();
    tester.settings().setGlobalSettings("sonar.auth.fake-oauth2-id-provider.enabled", "true");
    tester.settings().setGlobalSettings("sonar.auth.fake-oauth2-id-provider.url", fakeServerAuthProvider.url("").url().toString());
  }

  @After
  public void tearDown() throws Exception {
    tester.settings().resetSettings(
      "sonar.auth.fake-oauth2-id-provider.enabled",
      "sonar.auth.fake-oauth2-id-provider.url",
      "sonar.auth.fake-oauth2-id-provider.user");
    fakeServerAuthProvider.shutdown();
  }

  @Test
  public void user_is_redirect_to_warning_page_when_updating_login_and_personal_organization_key() {
    String oldLogin = tester.users().generateLogin();
    String newLogin = tester.users().generateLogin();
    String providerId = tester.users().generateProviderId();
    tester.settings().setGlobalSettings("sonar.organizations.createPersonalOrg", "true");

    // First authentication to create the user with its personal organization
    tester.settings().setGlobalSettings("sonar.auth.fake-oauth2-id-provider.user", oldLogin + "," + providerId + ",fake-" + oldLogin + ",John,john@email.com");
    simulateRedirectionToCallback();
    tester.openBrowser().openLogin().useOAuth2().shouldBeLoggedIn();
    assertThat(tester.organizations().service().search(new SearchRequest()).getOrganizationsList())
      .extracting(Organizations.Organization::getKey)
      .contains(oldLogin);

    // Second authentication, login is updated
    tester.settings().setGlobalSettings("sonar.auth.fake-oauth2-id-provider.user", newLogin + "," + providerId + ",fake-" + newLogin + ",John,john@email.com");
    simulateRedirectionToCallback();
    tester.openBrowser()
      .logIn()
      .useOAuth2()
      .asUpdateLoginPage()
      .shouldHaveProviderName("Fake oauth2 identity provider")
      .shouldHaveNewAccount("fake-" + newLogin)
      .shouldHaveOldLogin(oldLogin)
      .shouldHaveOldOrganizationKey(oldLogin)
      .clickContinue();

    assertThat(tester.users().service().search(new org.sonarqube.ws.client.users.SearchRequest()).getUsersList())
      .extracting(Users.SearchWsResponse.User::getLogin)
      .contains(newLogin)
      .doesNotContainSequence(oldLogin);
    assertThat(tester.organizations().service().search(new SearchRequest()).getOrganizationsList())
      .extracting(Organizations.Organization::getKey)
      .contains(newLogin)
      .doesNotContain(oldLogin);
  }

  private void simulateRedirectionToCallback() {
    fakeServerAuthProvider.enqueue(new MockResponse()
      .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP)
      .addHeader("Location: " + orchestrator.getServer().getUrl() + "/oauth2/callback/" + FAKE_PROVIDER_KEY)
      .setBody("Redirect to SonarQube"));
    fakeServerAuthProvider.enqueue(new MockResponse()
      .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP)
      .addHeader("Location: " + orchestrator.getServer().getUrl() + "/oauth2/callback/" + FAKE_PROVIDER_KEY)
      .setBody("Redirect to SonarQube"));
  }

}
