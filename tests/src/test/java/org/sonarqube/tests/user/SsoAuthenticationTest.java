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
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import util.user.UserRule;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.call;

/**
 * Test SSO authentication (using HTTP headers).
 * <p>
 * It starts its own server as it's using a different authentication system
 */
public class SsoAuthenticationTest {

  private static final String LOGIN_HEADER = "H-Login";
  private static final String NAME_HEADER = "H-Name";
  private static final String EMAIL_HEADER = "H-Email";
  private static final String GROUPS_HEADER = "H-Groups";

  static final String USER_LOGIN = "tester";
  static final String USER_NAME = "Tester";
  static final String USER_EMAIL = "tester@email.com";

  static final String GROUP_1 = "group-1";
  static final String GROUP_2 = "group-2";
  static final String GROUP_3 = "group-3";

  @ClassRule
  public static final Orchestrator orchestrator = Orchestrator.builderEnv()
    .setServerProperty("sonar.web.sso.enable", "true")
    .setServerProperty("sonar.web.sso.loginHeader", LOGIN_HEADER)
    .setServerProperty("sonar.web.sso.nameHeader", NAME_HEADER)
    .setServerProperty("sonar.web.sso.emailHeader", EMAIL_HEADER)
    .setServerProperty("sonar.web.sso.groupsHeader", GROUPS_HEADER)
    .build();

  private static UserRule userRule = UserRule.from(orchestrator);

  @ClassRule
  public static RuleChain ruleChain = RuleChain.outerRule(orchestrator).around(userRule);

  @Before
  public void resetData() {
    userRule.resetUsers();
  }

  @Test
  public void authenticate() {
    doCall(USER_LOGIN, USER_NAME, USER_EMAIL, null);

    userRule.verifyUserExists(USER_LOGIN, USER_NAME, USER_EMAIL);
  }

  @Test
  public void authenticate_with_only_login() {
    doCall(USER_LOGIN, null, null, null);

    userRule.verifyUserExists(USER_LOGIN, USER_LOGIN, null);
  }

  @Test
  public void update_user_when_headers_are_updated() {
    doCall(USER_LOGIN, USER_NAME, USER_EMAIL, null);
    userRule.verifyUserExists(USER_LOGIN, USER_NAME, USER_EMAIL);

    // As we don't keep the JWT token is the test, the user is updated
    doCall(USER_LOGIN, "new name", "new email", null);
    userRule.verifyUserExists(USER_LOGIN, "new name", "new email");
  }

  @Test
  public void authenticate_with_groups() {
    doCall(USER_LOGIN, null, null, GROUP_1);

    userRule.verifyUserGroupMembership(USER_LOGIN, GROUP_1, "sonar-users");
  }

  @Test
  public void synchronize_groups_when_authenticating_existing_user() {
    userRule.createGroup(GROUP_1);
    userRule.createGroup(GROUP_2);
    userRule.createGroup(GROUP_3);
    userRule.createUser(USER_LOGIN, "password");
    userRule.associateGroupsToUser(USER_LOGIN, GROUP_1, GROUP_2);

    doCall(USER_LOGIN, null, null, GROUP_2 + "," + GROUP_3);

    userRule.verifyUserGroupMembership(USER_LOGIN, GROUP_2, GROUP_3, "sonar-users");
  }

  @Test
  public void authentication_with_local_user_is_possible_when_no_header() {
    userRule.createUser(USER_LOGIN, "password");

    checkLocalAuthentication(USER_LOGIN, "password");
  }

  @Test
  public void display_message_in_ui_but_not_in_log_when_unauthorized_exception() throws Exception {
    Response response = doCall("invalid login $", null, null, null);

    assertThat(response.code()).isEqualTo(200);
    assertThat(response.request().url().toString()).contains("sessions/unauthorized");

    List<String> logsLines = FileUtils.readLines(orchestrator.getServer().getWebLogs(), UTF_8);
    assertThat(logsLines).doesNotContain("org.sonar.server.exceptions.BadRequestException: Use only letters, numbers, and .-_@ please.");
    userRule.verifyUserDoesNotExist(USER_LOGIN);
  }

  @Test
  public void fail_when_email_already_exists() throws Exception {
    userRule.createUser("another", "Another", USER_EMAIL, "another");

    Response response = doCall(USER_LOGIN, USER_NAME, USER_EMAIL, null);

    String expectedError = "You can't sign up because email 'tester@email.com' is already used by an existing user. This means that you probably already registered with another account";
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

  private boolean checkLocalAuthentication(String login, String password) {
    String result = orchestrator.getServer().wsClient(login, password).get("/api/authentication/validate");
    return result.contains("{\"valid\":true}");
  }

}
