/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.user.ws;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.FilterChain;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.user.SessionTokenDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.CredentialsLocalAuthentication;
import org.sonar.server.authentication.JwtHttpHandler;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.ServletFilterHandler;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_PASSWORD;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_PREVIOUS_PASSWORD;

class ChangePasswordActionIT {

  private static final String OLD_PASSWORD = "1234567890_aA";
  private static final String NEW_PASSWORD = "1234567890_bB";

  @RegisterExtension
  private final DbTester db = DbTester.create();

  @RegisterExtension
  private final UserSessionRule userSessionRule = UserSessionRule.standalone().logIn();

  private final ArgumentCaptor<UserDto> userDtoCaptor = ArgumentCaptor.forClass(UserDto.class);

  private final HttpRequest request = mock(HttpRequest.class);
  private final HttpResponse response = mock(HttpResponse.class);
  private final FilterChain chain = mock(FilterChain.class);

  private final MapSettings settings = new MapSettings().setProperty("sonar.internal.pbkdf2.iterations", "1");
  private final CredentialsLocalAuthentication localAuthentication = new CredentialsLocalAuthentication(db.getDbClient(), settings.asConfig());

  private final UserUpdater userUpdater = new UserUpdater(mock(NewUserNotifier.class), db.getDbClient(),
    new DefaultGroupFinder(db.getDbClient()),
    new MapSettings().asConfig(), new NoOpAuditPersister(), localAuthentication);

  private final JwtHttpHandler jwtHttpHandler = mock(JwtHttpHandler.class);

  private final ChangePasswordAction underTest = new ChangePasswordAction(db.getDbClient(), userUpdater, userSessionRule, localAuthentication, jwtHttpHandler);
  private ServletOutputStream responseOutputStream;

  @BeforeEach
  void setUp() throws IOException {
    db.users().insertDefaultGroup();
    responseOutputStream = new StringOutputStream();
    doReturn(responseOutputStream).when(response).getOutputStream();
  }

  @Test
  void a_user_can_update_his_password() {
    UserTestData user = createLocalUser(OLD_PASSWORD);
    String oldCryptedPassword = findEncryptedPassword(user.getLogin());
    userSessionRule.logIn(user.userDto());

    executeTest(user.getLogin(), OLD_PASSWORD, NEW_PASSWORD);

    String newCryptedPassword = findEncryptedPassword(user.getLogin());
    assertThat(newCryptedPassword).isNotEqualTo(oldCryptedPassword);
    verify(jwtHttpHandler).removeToken(request, response);
    verify(jwtHttpHandler).generateToken(userDtoCaptor.capture(), eq(request), eq(response));
    assertThat(findSessionTokenDto(db.getSession(), user.getSessionTokenUuid())).isEmpty();
    assertThat(userDtoCaptor.getValue().getLogin()).isEqualTo(user.getLogin());
    verify(response).setStatus(HTTP_NO_CONTENT);
  }

  @Test
  void system_administrator_can_update_password_of_user() {
    UserTestData admin = createLocalUser();
    userSessionRule.logIn(admin.userDto()).setSystemAdministrator();
    UserTestData user = createLocalUser();
    String originalPassword = findEncryptedPassword(user.getLogin());
    db.commit();

    executeTest(user.getLogin(), null, NEW_PASSWORD);

    String newPassword = findEncryptedPassword(user.getLogin());
    assertThat(newPassword).isNotEqualTo(originalPassword);
    assertThat(findSessionTokenDto(db.getSession(), user.getSessionTokenUuid())).isEmpty();
    assertThat(findSessionTokenDto(db.getSession(), admin.getSessionTokenUuid())).isPresent();
    verifyNoInteractions(jwtHttpHandler);
    verify(response).setStatus(HTTP_NO_CONTENT);
  }

  private String findEncryptedPassword(String login) {
    return db.getDbClient().userDao().selectByLogin(db.getSession(), login).getCryptedPassword();
  }

  private Optional<SessionTokenDto> findSessionTokenDto(DbSession dbSession, String tokenUuid) {
    return db.getDbClient().sessionTokensDao().selectByUuid(dbSession, tokenUuid);
  }

  @Test
  void fail_to_update_someone_else_password_if_not_admin() {
    UserTestData user = createLocalUser();
    userSessionRule.logIn(user.getLogin());
    UserTestData anotherLocalUser = createLocalUser();

    String anotherLocalUserLogin = anotherLocalUser.getLogin();
    assertThatThrownBy(() -> executeTest(anotherLocalUserLogin, "I dunno", NEW_PASSWORD))
      .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(jwtHttpHandler);
    assertThat(findSessionTokenDto(db.getSession(), user.getSessionTokenUuid())).isPresent();
    verifyNoInteractions(jwtHttpHandler);
  }

  @Test
  void fail_to_update_someone_else_password_if_not_admin_and_user_doesnt_exist() {
    UserTestData user = createLocalUser();
    userSessionRule.logIn(user.getLogin());

    assertThatThrownBy(() -> executeTest("unknown", "I dunno", NEW_PASSWORD))
      .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(jwtHttpHandler);

    assertThat(findSessionTokenDto(db.getSession(), user.getSessionTokenUuid())).isPresent();
    verifyNoInteractions(jwtHttpHandler);
  }

  @Test
  void fail_to_update_unknown_user() {
    UserTestData admin = createLocalUser();
    userSessionRule.logIn(admin.userDto()).setSystemAdministrator();

    assertThatThrownBy(() -> executeTest("polop", null, NEW_PASSWORD))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("User with login 'polop' has not been found");
    assertThat(findSessionTokenDto(db.getSession(), admin.getSessionTokenUuid())).isPresent();
    verifyNoInteractions(jwtHttpHandler);
  }

  @Test
  void fail_on_disabled_user() {
    UserDto user = db.users().insertUser(u -> u.setActive(false));
    userSessionRule.logIn(user);

    String userLogin = user.getLogin();
    assertThatThrownBy(() -> executeTest(userLogin, null, NEW_PASSWORD))
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("User with login '%s' has not been found", userLogin));
    verifyNoInteractions(jwtHttpHandler);
  }

  @Test
  void fail_to_update_password_on_self_without_login() {
    when(request.getParameter(PARAM_PASSWORD)).thenReturn("new password");
    when(request.getParameter(PARAM_PREVIOUS_PASSWORD)).thenReturn(NEW_PASSWORD);

    executeTest(null, OLD_PASSWORD, NEW_PASSWORD);
    verify(response).setStatus(HTTP_BAD_REQUEST);
    assertThat(responseOutputStream).hasToString("{\"result\":\"The 'login' parameter is missing\"}");
    verifyNoInteractions(jwtHttpHandler);
  }

  @Test
  void fail_to_update_password_on_self_without_old_password() {
    UserTestData user = createLocalUser();
    userSessionRule.logIn(user.userDto());

    executeTest(user.getLogin(), null, NEW_PASSWORD);
    verify(response).setStatus(HTTP_BAD_REQUEST);
    assertThat(responseOutputStream).hasToString("{\"result\":\"The 'previousPassword' parameter is missing\"}");
    assertThat(findSessionTokenDto(db.getSession(), user.getSessionTokenUuid())).isPresent();
    verifyNoInteractions(jwtHttpHandler);
  }

  @Test
  void fail_to_update_password_on_self_without_new_password() {
    UserTestData user = createLocalUser();
    userSessionRule.logIn(user.userDto());

    executeTest(user.getLogin(), OLD_PASSWORD, null);
    verify(response).setStatus(HTTP_BAD_REQUEST);
    assertThat(responseOutputStream).hasToString("{\"result\":\"The 'password' parameter is missing\"}");
    assertThat(findSessionTokenDto(db.getSession(), user.getSessionTokenUuid())).isPresent();
    assertThat(findSessionTokenDto(db.getSession(), user.getSessionTokenUuid())).isPresent();
    verifyNoInteractions(jwtHttpHandler);
  }

  @Test
  void fail_to_update_password_on_self_with_bad_old_password() {
    UserTestData user = createLocalUser();
    userSessionRule.logIn(user.userDto());

    executeTest(user.getLogin(), "I dunno", NEW_PASSWORD);
    verify(response).setStatus(HTTP_BAD_REQUEST);
    assertThat(responseOutputStream).hasToString("{\"result\":\"old_password_incorrect\"}");
    assertThat(findSessionTokenDto(db.getSession(), user.getSessionTokenUuid())).isPresent();
    verifyNoInteractions(jwtHttpHandler);
  }

  @Test
  void fail_to_update_password_on_external_auth() {
    UserDto admin = db.users().insertUser();
    userSessionRule.logIn(admin).setSystemAdministrator();
    UserDto user = db.users().insertUser(u -> u.setLocal(false));

    executeTest(user.getLogin(), "I dunno", NEW_PASSWORD);
    verify(response).setStatus(HTTP_BAD_REQUEST);
  }

  @Test
  void fail_to_update_to_same_password() {
    UserTestData user = createLocalUser(OLD_PASSWORD);
    userSessionRule.logIn(user.userDto());

    executeTest(user.getLogin(), OLD_PASSWORD, OLD_PASSWORD);
    verify(response).setStatus(HTTP_BAD_REQUEST);
    assertThat(responseOutputStream).hasToString("{\"result\":\"new_password_same_as_old\"}");
    assertThat(findSessionTokenDto(db.getSession(), user.getSessionTokenUuid())).isPresent();
    verifyNoInteractions(jwtHttpHandler);
  }

  static Stream<Arguments> invalidPasswords() {
    return Stream.of(
      Arguments.of("12345678901", "Password must be at least 12 characters long"),
      Arguments.of("123456789012", "Password must contain at least one uppercase character"),
      Arguments.of("12345678901A", "Password must contain at least one lowercase character"),
      Arguments.of("1234567890aA", "Password must contain at least one special character"),
      Arguments.of("abcdefghiaA%", "Password must contain at least one digit")
    );
  }

  @ParameterizedTest
  @MethodSource("invalidPasswords")
  void changePassword_whenPasswordDoesNotMatchSecurityRequirements_shouldThrowWithExpectedMessage(String newPassword, String expectedMessage) {
    UserTestData user = createLocalUser(OLD_PASSWORD);
    userSessionRule.logIn(user.userDto());

    executeTest(user.getLogin(), OLD_PASSWORD, newPassword);

    verify(response).setStatus(HTTP_BAD_REQUEST);
    assertThat(responseOutputStream).hasToString("{\"result\":\"" + expectedMessage + "\"}");
  }

  @Test
  void verify_definition() {
    String controllerKey = "foo";
    WebService.Context context = new WebService.Context();
    WebService.NewController newController = context.createController(controllerKey);

    underTest.define(newController);
    newController.done();

    WebService.Action changePassword = context.controller(controllerKey).action("change_password");
    assertThat(changePassword).isNotNull();
    assertThat(changePassword.handler()).isInstanceOf(ServletFilterHandler.class);
    assertThat(changePassword.isPost()).isTrue();
    assertThat(changePassword.params())
      .extracting(WebService.Param::key)
      .containsExactlyInAnyOrder(PARAM_LOGIN, PARAM_PASSWORD, PARAM_PREVIOUS_PASSWORD);
  }

  private void executeTest(@Nullable String login, @Nullable String oldPassword, @Nullable String newPassword) {
    when(request.getParameter(PARAM_LOGIN)).thenReturn(login);
    when(request.getParameter(PARAM_PREVIOUS_PASSWORD)).thenReturn(oldPassword);
    when(request.getParameter(PARAM_PASSWORD)).thenReturn(newPassword);
    underTest.doFilter(request, response, chain);
  }

  private UserTestData createLocalUser(String password) {
    UserTestData userTestData = createLocalUser();
    localAuthentication.storeHashPassword(userTestData.userDto(), password);
    db.getDbClient().userDao().update(db.getSession(), userTestData.userDto());
    db.commit();
    return userTestData;
  }

  private UserTestData createLocalUser() {
    UserDto userDto = db.users().insertUser(u -> u.setLocal(true));
    SessionTokenDto sessionTokenForUser = createSessionTokenForUser(userDto);
    db.commit();
    return new UserTestData(userDto, sessionTokenForUser);
  }

  private SessionTokenDto createSessionTokenForUser(UserDto user) {
    SessionTokenDto userTokenDto = new SessionTokenDto().setUserUuid(user.getUuid()).setExpirationDate(1000L);
    return db.getDbClient().sessionTokensDao().insert(db.getSession(), userTokenDto);
  }

  private record UserTestData(UserDto userDto, SessionTokenDto sessionTokenDto) {

    String getLogin() {
      return userDto.getLogin();
    }

    String getSessionTokenUuid() {
      return sessionTokenDto.getUuid();
    }
  }

  static class StringOutputStream extends ServletOutputStream {
    private final StringBuilder buf = new StringBuilder();

    StringOutputStream() {
    }

    @Override
    public boolean isReady() {
      return false;
    }

    @Override
    public void setWriteListener(WriteListener listener) {

    }

    public void write(byte[] b) {
      this.buf.append(new String(b));
    }

    public void write(byte[] b, int off, int len) {
      this.buf.append(new String(b, off, len));
    }

    public void write(int b) {
      byte[] bytes = new byte[] {(byte) b};
      this.buf.append(new String(bytes));
    }

    public String toString() {
      return this.buf.toString();
    }
  }

}
