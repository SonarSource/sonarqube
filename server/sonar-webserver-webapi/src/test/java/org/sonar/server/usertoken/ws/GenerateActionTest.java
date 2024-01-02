/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.usertoken.ws;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.TokenType;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usertoken.TokenGenerator;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.UserTokens.GenerateWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.SonarEdition.COMMUNITY;
import static org.sonar.api.SonarEdition.DATACENTER;
import static org.sonar.api.SonarEdition.DEVELOPER;
import static org.sonar.api.SonarEdition.ENTERPRISE;
import static org.sonar.api.utils.DateUtils.DATETIME_FORMAT;
import static org.sonar.api.utils.DateUtils.DATE_FORMAT;
import static org.sonar.core.config.MaxTokenLifetimeOption.NO_EXPIRATION;
import static org.sonar.core.config.MaxTokenLifetimeOption.THIRTY_DAYS;
import static org.sonar.core.config.TokenExpirationConstants.MAX_ALLOWED_TOKEN_LIFETIME;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonar.db.user.TokenType.GLOBAL_ANALYSIS_TOKEN;
import static org.sonar.db.user.TokenType.PROJECT_ANALYSIS_TOKEN;
import static org.sonar.db.user.TokenType.USER_TOKEN;
import static org.sonar.server.usertoken.ws.UserTokenSupport.PARAM_EXPIRATION_DATE;
import static org.sonar.server.usertoken.ws.UserTokenSupport.PARAM_LOGIN;
import static org.sonar.server.usertoken.ws.UserTokenSupport.PARAM_NAME;
import static org.sonar.server.usertoken.ws.UserTokenSupport.PARAM_PROJECT_KEY;
import static org.sonar.server.usertoken.ws.UserTokenSupport.PARAM_TYPE;
import static org.sonar.test.JsonAssert.assertJson;

public class GenerateActionTest {

  private static final String TOKEN_NAME = "Third Party Application";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final SonarRuntime runtime = mock(SonarRuntime.class);
  private final TokenGenerator tokenGenerator = mock(TokenGenerator.class);
  private final MapSettings mapSettings = new MapSettings();
  private final Configuration configuration = mapSettings.asConfig();
  private final GenerateActionValidation validation = new GenerateActionValidation(configuration, runtime);

  private final WsActionTester ws = new WsActionTester(
    new GenerateAction(db.getDbClient(), System2.INSTANCE, tokenGenerator, new UserTokenSupport(db.getDbClient(), userSession), validation));

  @Before
  public void setUp() {
    when(tokenGenerator.generate(USER_TOKEN)).thenReturn("123456789");
    when(tokenGenerator.generate(GLOBAL_ANALYSIS_TOKEN)).thenReturn("sqa_123456789");
    when(tokenGenerator.generate(PROJECT_ANALYSIS_TOKEN)).thenReturn("sqp_123456789");
    when(tokenGenerator.hash(anyString())).thenReturn("987654321");
    when(runtime.getEdition()).thenReturn(ENTERPRISE); // by default, a Sonar version that supports the max allowed lifetime token property
  }

  @Test
  public void generate_action() {
    WebService.Action action = ws.getDef();

    assertThat(action.key()).isEqualTo("generate");
    assertThat(action.since()).isEqualTo("5.3");
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.isPost()).isTrue();
    assertThat(action.param(PARAM_LOGIN).isRequired()).isFalse();
    assertThat(action.param(PARAM_NAME).isRequired()).isTrue();
    assertThat(action.param(PARAM_TYPE).isRequired()).isFalse();
    assertThat(action.param(PARAM_TYPE).since()).isEqualTo("9.5");
    assertThat(action.param(PARAM_PROJECT_KEY).isRequired()).isFalse();
    assertThat(action.param(PARAM_PROJECT_KEY).since()).isEqualTo("9.5");
    assertThat(action.param(PARAM_EXPIRATION_DATE).isRequired()).isFalse();
    assertThat(action.param(PARAM_EXPIRATION_DATE).since()).isEqualTo("9.6");
  }

  @Test
  public void json_example() {
    UserDto user1 = db.users().insertUser(u -> u.setLogin("grace.hopper"));
    logInAsSystemAdministrator();

    String response = ws.newRequest()
      .setMediaType(MediaTypes.JSON)
      .setParam(PARAM_LOGIN, user1.getLogin())
      .setParam(PARAM_NAME, TOKEN_NAME)
      .execute().getInput();

    assertJson(response).ignoreFields("createdAt").ignoreFields("expirationDate").isSimilarTo(getClass().getResource("generate-example.json"));
  }

  @Test
  public void a_user_can_generate_token_for_himself() {
    UserDto user = userLogin();

    GenerateWsResponse response = newRequest(null, TOKEN_NAME);

    assertThat(response.getLogin()).isEqualTo(user.getLogin());
    assertThat(response.getCreatedAt()).isNotEmpty();
  }

  @Test
  public void a_user_can_generate_globalAnalysisToken_with_the_global_scan_permission() {
    UserDto user = userLogin();
    userSession.addPermission(SCAN);

    GenerateWsResponse response = newRequest(null, TOKEN_NAME, GLOBAL_ANALYSIS_TOKEN, null);

    assertThat(response.getLogin()).isEqualTo(user.getLogin());
    assertThat(response.getToken()).startsWith("sqa_");
    assertThat(response.getCreatedAt()).isNotEmpty();
  }

  @Test
  public void a_user_can_generate_projectAnalysisToken_with_the_project_global_scan_permission() {
    UserDto user = userLogin();
    ComponentDto project = db.components().insertPublicProject();
    userSession.addPermission(SCAN);

    GenerateWsResponse response = newRequest(null, TOKEN_NAME, PROJECT_ANALYSIS_TOKEN, project.getKey());

    assertThat(response.getLogin()).isEqualTo(user.getLogin());
    assertThat(response.getToken()).startsWith("sqp_");
    assertThat(response.getProjectKey()).isEqualTo(project.getKey());
    assertThat(response.getCreatedAt()).isNotEmpty();
  }

  @Test
  public void a_user_can_generate_projectAnalysisToken_with_the_project_scan_permission() {
    UserDto user = userLogin();
    ComponentDto project = db.components().insertPublicProject();
    userSession.addProjectPermission(SCAN.toString(), project);

    GenerateWsResponse response = newRequest(null, TOKEN_NAME, PROJECT_ANALYSIS_TOKEN, project.getKey());

    assertThat(response.getLogin()).isEqualTo(user.getLogin());
    assertThat(response.getToken()).startsWith("sqp_");
    assertThat(response.getProjectKey()).isEqualTo(project.getKey());
    assertThat(response.getCreatedAt()).isNotEmpty();
  }

  @Test
  public void a_user_can_generate_projectAnalysisToken_with_the_project_scan_permission_passing_login() {
    UserDto user = userLogin();
    ComponentDto project = db.components().insertPublicProject();
    userSession.addProjectPermission(SCAN.toString(), project);

    GenerateWsResponse responseWithLogin = newRequest(user.getLogin(), TOKEN_NAME, PROJECT_ANALYSIS_TOKEN, project.getKey());

    assertThat(responseWithLogin.getLogin()).isEqualTo(user.getLogin());
    assertThat(responseWithLogin.getToken()).startsWith("sqp_");
    assertThat(responseWithLogin.getProjectKey()).isEqualTo(project.getKey());
    assertThat(responseWithLogin.getCreatedAt()).isNotEmpty();
  }

  @Test
  public void a_user_can_generate_token_for_himself_with_expiration_date() {
    UserDto user = userLogin();

    // A date 10 days in the future with format yyyy-MM-dd
    String expirationDateValue = LocalDate.now().plusDays(10).format(DateTimeFormatter.ofPattern(DATE_FORMAT));

    GenerateWsResponse response = newRequest(null, TOKEN_NAME, expirationDateValue);

    assertThat(response.getLogin()).isEqualTo(user.getLogin());
    assertThat(response.getCreatedAt()).isNotEmpty();
    assertThat(response.getExpirationDate()).isEqualTo(getFormattedDate(expirationDateValue));
  }

  @Test
  public void an_administrator_can_generate_token_for_users_with_expiration_date() {
    UserDto user = userLogin();
    logInAsSystemAdministrator();

    // A date 10 days in the future with format yyyy-MM-dd
    String expirationDateValue = LocalDate.now().plusDays(10).format(DateTimeFormatter.ofPattern(DATE_FORMAT));

    GenerateWsResponse response = newRequest(user.getLogin(), TOKEN_NAME, expirationDateValue);

    assertThat(response.getLogin()).isEqualTo(user.getLogin());
    assertThat(response.getCreatedAt()).isNotEmpty();
    assertThat(response.getExpirationDate()).isEqualTo(getFormattedDate(expirationDateValue));
  }

  @Test
  public void fail_if_login_does_not_exist() {
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> newRequest("unknown-login", "any-name"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("User with login 'unknown-login' doesn't exist");
  }

  @Test
  public void fail_if_name_is_blank() {
    UserDto user = userLogin();
    logInAsSystemAdministrator();

    String login = user.getLogin();

    assertThatThrownBy(() -> newRequest(login, "   "))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'name' parameter is missing");
  }

  @Test
  public void fail_if_globalAnalysisToken_created_for_other_user() {
    String login = userLogin().getLogin();
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> newRequest(login, "token 1", GLOBAL_ANALYSIS_TOKEN, null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("A Global Analysis Token cannot be generated for another user.");
  }

  @Test
  public void fail_if_projectAnalysisToken_created_for_other_user() {
    String login = userLogin().getLogin();
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> newRequest(login, "token 1", PROJECT_ANALYSIS_TOKEN, "project 1"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("A Project Analysis Token cannot be generated for another user.");
  }

  @Test
  public void fail_if_globalAnalysisToken_created_without_global_permission() {
    userLogin();

    assertThatThrownBy(() -> {
      newRequest(null, "token 1", GLOBAL_ANALYSIS_TOKEN, null);
    })
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fail_if_projectAnalysisToken_created_without_project_permission() {
    userLogin();
    String projectKey = db.components().insertPublicProject().getKey();

    assertThatThrownBy(() -> newRequest(null, "token 1", PROJECT_ANALYSIS_TOKEN, projectKey))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fail_if_projectAnalysisToken_created_for_blank_projectKey() {
    userLogin();

    assertThatThrownBy(() -> {
      newRequest(null, "token 1", PROJECT_ANALYSIS_TOKEN, null);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("A projectKey is needed when creating Project Analysis Token");
  }

  @Test
  public void fail_if_projectAnalysisToken_created_for_non_existing_project() {
    userLogin();
    userSession.addPermission(SCAN);

    assertThatThrownBy(() -> {
      newRequest(null, "token 1", PROJECT_ANALYSIS_TOKEN, "nonExistingProjectKey");
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Project key 'nonExistingProjectKey' not found");
  }

  @Test
  public void fail_if_token_with_same_login_and_name_exists() {
    UserDto user = db.users().insertUser();
    String login = user.getLogin();
    logInAsSystemAdministrator();
    db.users().insertToken(user, t -> t.setName(TOKEN_NAME));

    assertThatThrownBy(() -> {
      newRequest(login, TOKEN_NAME);
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage(String.format("A user token for login '%s' and name 'Third Party Application' already exists", user.getLogin()));
  }

  @Test
  public void fail_if_expirationDate_format_is_wrong() {
    UserDto user = db.users().insertUser();
    String login = user.getLogin();
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> {
      newRequest(login, TOKEN_NAME, "21/06/2022");
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Supplied date format for parameter expirationDate is wrong. Please supply date in the ISO 8601 date format (YYYY-MM-DD)");
  }

  @Test
  public void fail_if_expirationDate_is_not_in_future() {
    UserDto user = db.users().insertUser();
    String login = user.getLogin();
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> {
      newRequest(login, TOKEN_NAME, "2022-06-29");
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(String.format("The minimum value for parameter %s is %s.", PARAM_EXPIRATION_DATE, LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE)));
  }

  @Test
  public void success_if_expirationDate_is_equal_to_the_max_allowed_token_lifetime() {
    String login = userLogin().getLogin();

    mapSettings.setProperty(MAX_ALLOWED_TOKEN_LIFETIME, THIRTY_DAYS.getName());
    String expirationDateString = LocalDate.now().plusDays(30).format(DateTimeFormatter.ofPattern(DATE_FORMAT));

    GenerateWsResponse response = newRequest(login, TOKEN_NAME, expirationDateString);
    assertThat(response.getLogin()).isEqualTo(login);
    assertThat(response.getCreatedAt()).isNotEmpty();
    assertThat(response.getExpirationDate()).isEqualTo(getFormattedDate(expirationDateString));
  }

  @Test
  public void success_if_expirationDate_is_before_the_max_allowed_token_lifetime() {
    String login = userLogin().getLogin();

    mapSettings.setProperty(MAX_ALLOWED_TOKEN_LIFETIME, THIRTY_DAYS.getName());
    String expirationDateString = LocalDate.now().plusDays(29).format(DateTimeFormatter.ofPattern(DATE_FORMAT));

    GenerateWsResponse response = newRequest(login, TOKEN_NAME, expirationDateString);
    assertThat(response.getLogin()).isEqualTo(login);
    assertThat(response.getCreatedAt()).isNotEmpty();
    assertThat(response.getExpirationDate()).isEqualTo(getFormattedDate(expirationDateString));
  }

  @Test
  public void success_if_no_expiration_date_is_allowed_with_expiration_date() {
    String login = userLogin().getLogin();

    mapSettings.setProperty(MAX_ALLOWED_TOKEN_LIFETIME, NO_EXPIRATION.getName());
    String expirationDateString = LocalDate.now().plusDays(30).format(DateTimeFormatter.ofPattern(DATE_FORMAT));

    GenerateWsResponse response = newRequest(login, TOKEN_NAME, expirationDateString);
    assertThat(response.getLogin()).isEqualTo(login);
    assertThat(response.getCreatedAt()).isNotEmpty();
    assertThat(response.getExpirationDate()).isEqualTo(getFormattedDate(expirationDateString));
  }

  @Test
  public void success_if_no_expiration_date_is_allowed_without_expiration_date() {
    String login = userLogin().getLogin();

    mapSettings.setProperty(MAX_ALLOWED_TOKEN_LIFETIME, NO_EXPIRATION.getName());

    GenerateWsResponse response = newRequest(login, TOKEN_NAME);
    assertThat(response.getLogin()).isEqualTo(login);
    assertThat(response.getCreatedAt()).isNotEmpty();
  }

  @Test
  public void fail_if_expirationDate_is_after_the_max_allowed_token_lifetime() {
    String login = userLogin().getLogin();

    mapSettings.setProperty(MAX_ALLOWED_TOKEN_LIFETIME, THIRTY_DAYS.getName());

    String expirationDateString = LocalDate.now().plusDays(31).format(DateTimeFormatter.ofPattern(DATE_FORMAT));

    // with expiration date
    assertThatThrownBy(() -> {
      newRequest(login, TOKEN_NAME, expirationDateString);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Tokens expiring after %s are not allowed. Please use a valid expiration date.",
        LocalDate.now().plusDays(THIRTY_DAYS.getDays().get()).format(DateTimeFormatter.ISO_DATE));

    // without expiration date
    when(tokenGenerator.hash(anyString())).thenReturn("random");
    assertThatThrownBy(() -> {
      newRequest(login, TOKEN_NAME);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Tokens expiring after %s are not allowed. Please use an expiration date.",
        LocalDate.now().plusDays(THIRTY_DAYS.getDays().get()).format(DateTimeFormatter.ISO_DATE));
  }

  @Test
  public void max_allowed_token_lifetime_not_enforced_for_unsupported_versions() {
    String login = userLogin().getLogin();

    mapSettings.setProperty(MAX_ALLOWED_TOKEN_LIFETIME, THIRTY_DAYS.getName());

    List.of(DEVELOPER, COMMUNITY).forEach(edition -> {
      when(runtime.getEdition()).thenReturn(edition);
      when(tokenGenerator.hash(anyString())).thenReturn("987654321" + edition);

      GenerateWsResponse response = newRequest(login, TOKEN_NAME + edition);
      assertThat(response.getLogin()).isEqualTo(login);
      assertThat(response.getCreatedAt()).isNotEmpty();
    });
  }

  @Test
  public void max_allowed_token_lifetime_enforced_for_supported_versions() {
    String login = userLogin().getLogin();

    mapSettings.setProperty(MAX_ALLOWED_TOKEN_LIFETIME, THIRTY_DAYS.getName());

    List.of(ENTERPRISE, DATACENTER).forEach(edition -> {
      when(runtime.getEdition()).thenReturn(edition);
      when(tokenGenerator.hash(anyString())).thenReturn("987654321" + edition);

      assertThatThrownBy(() -> {
        newRequest(login, TOKEN_NAME);
      })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Tokens expiring after %s are not allowed. Please use an expiration date.",
          LocalDate.now().plusDays(THIRTY_DAYS.getDays().get()).format(DateTimeFormatter.ISO_DATE));
    });
  }

  @Test
  public void fail_if_token_hash_already_exists_in_db() {
    UserDto user = db.users().insertUser();
    String login = user.getLogin();
    logInAsSystemAdministrator();
    when(tokenGenerator.hash(anyString())).thenReturn("987654321");
    db.users().insertToken(user, t -> t.setTokenHash("987654321"));

    assertThatThrownBy(() -> {
      newRequest(login, TOKEN_NAME);
    })
      .isInstanceOf(ServerException.class)
      .hasMessage("Error while generating token. Please try again.");
  }

  @Test
  public void throw_ForbiddenException_if_non_administrator_creates_token_for_someone_else() {
    String login = db.users().insertUser().getLogin();
    userSession.logIn().setNonSystemAdministrator();

    assertThatThrownBy(() -> {
      newRequest(login, TOKEN_NAME);
    })
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    String login = db.users().insertUser().getLogin();
    userSession.anonymous();

    assertThatThrownBy(() -> {
      newRequest(login, TOKEN_NAME);
    })
      .isInstanceOf(UnauthorizedException.class);
  }

  private GenerateWsResponse newRequest(@Nullable String login, String name) {
    TestRequest testRequest = ws.newRequest()
      .setParam(PARAM_NAME, name);
    if (login != null) {
      testRequest.setParam(PARAM_LOGIN, login);
    }

    return testRequest.executeProtobuf(GenerateWsResponse.class);
  }

  private GenerateWsResponse newRequest(@Nullable String login, String name, String expirationDate) {
    TestRequest testRequest = ws.newRequest()
      .setParam(PARAM_NAME, name)
      .setParam(PARAM_EXPIRATION_DATE, expirationDate);
    if (login != null) {
      testRequest.setParam(PARAM_LOGIN, login);
    }

    return testRequest.executeProtobuf(GenerateWsResponse.class);
  }

  private GenerateWsResponse newRequest(@Nullable String login, String name, TokenType tokenType, @Nullable String projectKey) {
    TestRequest testRequest = ws.newRequest()
      .setParam(PARAM_NAME, name)
      .setParam(PARAM_TYPE, tokenType.toString());
    if (login != null) {
      testRequest.setParam(PARAM_LOGIN, login);
    }
    if (projectKey != null) {
      testRequest.setParam(PARAM_PROJECT_KEY, projectKey);
    }

    return testRequest.executeProtobuf(GenerateWsResponse.class);
  }

  private UserDto userLogin() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    return user;
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }

  private String getFormattedDate(String expirationDateValue) {
    return DateTimeFormatter
      .ofPattern(DATETIME_FORMAT)
      .format(LocalDate.parse(expirationDateValue, DateTimeFormatter.ofPattern(DATE_FORMAT)).atStartOfDay(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault()));
  }
}
