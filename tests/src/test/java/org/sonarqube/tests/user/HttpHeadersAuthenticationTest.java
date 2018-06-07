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
import java.net.URLEncoder;
import java.util.List;
import javax.annotation.Nullable;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.UserGroups.Group;
import org.sonarqube.ws.Users.CreateWsResponse.User;
import org.sonarqube.ws.Users.SearchWsResponse;
import org.sonarqube.ws.client.users.SearchRequest;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static util.ItUtils.call;
import static util.ItUtils.newOrchestratorBuilder;

/**
 * Test authentication using HTTP headers.
 * <p>
 * It starts its own server as it's using a different authentication system
 */
public class HttpHeadersAuthenticationTest {

  private static final String LOGIN_HEADER = "H-Login";
  private static final String NAME_HEADER = "H-Name";
  private static final String EMAIL_HEADER = "H-Email";
  private static final String GROUPS_HEADER = "H-Groups";

  @ClassRule
  public static final Orchestrator orchestrator = newOrchestratorBuilder(
    builder -> builder
      .setServerProperty("sonar.web.sso.enable", "true")
      .setServerProperty("sonar.web.sso.loginHeader", LOGIN_HEADER)
      .setServerProperty("sonar.web.sso.nameHeader", NAME_HEADER)
      .setServerProperty("sonar.web.sso.emailHeader", EMAIL_HEADER)
      .setServerProperty("sonar.web.sso.groupsHeader", GROUPS_HEADER));

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Test
  public void authenticate() {
    String login = tester.users().generateLogin();

    doCall(login, "Tester", "tester@email.com", null);

    verifyUser(login, "Tester", "tester@email.com");
  }

  @Test
  public void authenticate_with_only_login() {
    String login = tester.users().generateLogin();

    doCall(login, null, null, null);

    assertThat(tester.wsClient().users().search(new SearchRequest().setQ(login)).getUsersList())
      .extracting(SearchWsResponse.User::getLogin, SearchWsResponse.User::getName, SearchWsResponse.User::hasEmail)
      .containsExactlyInAnyOrder(tuple(login, login, false));
  }

  @Test
  public void update_user_when_headers_are_updated() {
    String login = tester.users().generateLogin();
    doCall(login, "Tester", "tester@email.com", null);
    verifyUser(login, "Tester", "tester@email.com");

    // As we don't keep the JWT token is the test, the user is updated
    doCall(login, "new name", "new email", null);
    verifyUser(login, "new name", "new email");
  }

  @Test
  public void authenticate_with_groups() {
    String login = tester.users().generateLogin();
    Group group = tester.groups().generate();

    doCall(login, null, null, group.getName());

    assertThat(tester.wsClient().users().search(new SearchRequest().setQ(login)).getUsersList())
      .extracting(SearchWsResponse.User::getLogin, u -> u.getGroups().getGroupsList())
      .containsExactlyInAnyOrder(tuple(login, asList(group.getName(), "sonar-users")));
  }

  @Test
  public void synchronize_groups_when_authenticating_existing_user() {
    User user = tester.users().generate();
    Group group1 = tester.groups().generate();
    Group group2 = tester.groups().generate();
    Group group3 = tester.groups().generate();
    tester.groups().addMemberToGroups(null, user.getLogin(), group1.getName(), group2.getName());

    doCall(user.getLogin(), null, null, group2.getName() + "," + group3.getName());

    assertThat(tester.wsClient().users().search(new SearchRequest().setQ(user.getLogin())).getUsersList())
      .extracting(SearchWsResponse.User::getLogin, u -> u.getGroups().getGroupsList())
      .containsExactlyInAnyOrder(tuple(user.getLogin(), asList(group2.getName(), group3.getName(), "sonar-users")));
  }

  @Test
  public void authentication_with_local_user_is_possible_when_no_header() {
    User user = tester.users().generate();

    // Check any ws, no error should be thrown
    tester.as(user.getLogin(), user.getLogin()).wsClient().system().ping();
  }

  @Test
  public void display_message_in_ui_but_not_in_log_when_unauthorized_exception() throws Exception {
    String login = "invalid login $";
    Response response = doCall(login, null, null, null);

    assertThat(response.code()).isEqualTo(200);
    assertThat(response.request().url().toString()).contains("sessions/unauthorized");

    List<String> logsLines = FileUtils.readLines(orchestrator.getServer().getWebLogs(), UTF_8);
    assertThat(logsLines).doesNotContain("org.sonar.server.exceptions.BadRequestException: Use only letters, numbers, and .-_@ please.");
    assertThat(tester.wsClient().users().search(new SearchRequest().setQ(login)).getUsersList()).isEmpty();
  }

  @Test
  public void fail_when_email_already_exists() throws Exception {
    String login = tester.users().generateLogin();
    User existingUser = tester.users().generate();

    Response response = doCall(login, "Tester", existingUser.getEmail(), null);

    String expectedError = format("You can't sign up because email '%s' is already used by an existing user. This means that you probably already registered with another account",
      existingUser.getEmail());
    assertThat(response.code()).isEqualTo(200);
    assertThat(response.request().url().toString()).contains(URLEncoder.encode(expectedError, UTF_8.name()));
    assertThat(FileUtils.readLines(orchestrator.getServer().getWebLogs(), UTF_8)).doesNotContain(expectedError);
  }

  @Test
  public void fail_to_authenticate_user_when_email_already_exists_on_several_users() throws Exception {
    String login = tester.users().generateLogin();
    tester.users().generate(u -> u.setEmail("john@email.com"));
    tester.users().generate(u -> u.setEmail("john@email.com"));

    Response response = doCall(login, "Tester", "john@email.com", null);

    String expectedError = "You can't sign up because email 'john@email.com' is already used by an existing user. This means that you probably already registered with another account";
    assertThat(response.code()).isEqualTo(200);
    assertThat(response.request().url().toString()).contains(URLEncoder.encode(expectedError, UTF_8.name()));
    assertThat(FileUtils.readLines(orchestrator.getServer().getWebLogs(), UTF_8)).doesNotContain(expectedError);
  }

  private static Response doCall(String login, @Nullable String name, @Nullable String email, @Nullable String groups) {
    return call(orchestrator.getServer().getUrl(),
      LOGIN_HEADER, login,
      NAME_HEADER, name,
      EMAIL_HEADER, email,
      GROUPS_HEADER, groups);
  }

  private void verifyUser(String login, String name, String email) {
    assertThat(tester.wsClient().users().search(new SearchRequest().setQ(login)).getUsersList())
      .extracting(SearchWsResponse.User::getLogin, SearchWsResponse.User::getName, SearchWsResponse.User::getEmail,
        SearchWsResponse.User::getExternalIdentity, SearchWsResponse.User::getExternalProvider, SearchWsResponse.User::getLocal)
      .containsExactlyInAnyOrder(tuple(login, name, email, login, "sonarqube", false));
  }

}
