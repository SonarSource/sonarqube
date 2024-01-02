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
package org.sonar.server.user.ws;

import java.io.IOException;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.user.SessionTokenDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.CredentialsLocalAuthentication;
import org.sonar.server.authentication.JwtHttpHandler;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.ServletFilterHandler;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_PASSWORD;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_PREVIOUS_PASSWORD;

public class ChangePasswordActionTest {

  private static final String OLD_PASSWORD = "1234";
  private static final String NEW_PASSWORD = "12345";

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.createCustom(UserIndexDefinition.createForTest());
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone().logIn();

  private final ArgumentCaptor<UserDto> userDtoCaptor = ArgumentCaptor.forClass(UserDto.class);

  private final HttpServletRequest request = mock(HttpServletRequest.class);
  private final HttpServletResponse response = mock(HttpServletResponse.class);
  private final FilterChain chain = mock(FilterChain.class);

  private final MapSettings settings = new MapSettings().setProperty("sonar.internal.pbkdf2.iterations", "1");
  private final CredentialsLocalAuthentication localAuthentication = new CredentialsLocalAuthentication(db.getDbClient(), settings.asConfig());

  private final UserUpdater userUpdater = new UserUpdater(mock(NewUserNotifier.class), db.getDbClient(),
    new UserIndexer(db.getDbClient(), es.client()), new DefaultGroupFinder(db.getDbClient()),
    new MapSettings().asConfig(), new NoOpAuditPersister(), localAuthentication);

  private final JwtHttpHandler jwtHttpHandler = mock(JwtHttpHandler.class);

  private final ChangePasswordAction changePasswordAction = new ChangePasswordAction(db.getDbClient(), userUpdater, userSessionRule, localAuthentication, jwtHttpHandler);

  @Before
  public void setUp() {
    db.users().insertDefaultGroup();
  }

  @Test
  public void a_user_can_update_his_password() throws ServletException, IOException {
    UserTestData user = createLocalUser(OLD_PASSWORD);
    String oldCryptedPassword = findEncryptedPassword(user.getLogin());
    userSessionRule.logIn(user.getUserDto());

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
  public void system_administrator_can_update_password_of_user() throws ServletException, IOException {
    UserTestData admin = createLocalUser();
    userSessionRule.logIn(admin.getUserDto()).setSystemAdministrator();
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
  public void fail_to_update_someone_else_password_if_not_admin() throws ServletException, IOException {
    UserTestData user = createLocalUser();
    userSessionRule.logIn(user.getLogin());
    UserTestData anotherLocalUser = createLocalUser();

    assertThatThrownBy(() -> executeTest(anotherLocalUser.getLogin(), "I dunno", NEW_PASSWORD))
      .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(jwtHttpHandler);
    assertThat(findSessionTokenDto(db.getSession(), user.getSessionTokenUuid())).isPresent();
    verifyNoInteractions(jwtHttpHandler);
  }

  @Test
  public void fail_to_update_someone_else_password_if_not_admin_and_user_doesnt_exist() throws ServletException, IOException {
    UserTestData user = createLocalUser();
    userSessionRule.logIn(user.getLogin());

    assertThatThrownBy(() -> executeTest("unknown", "I dunno", NEW_PASSWORD))
      .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(jwtHttpHandler);

    assertThat(findSessionTokenDto(db.getSession(), user.getSessionTokenUuid())).isPresent();
    verifyNoInteractions(jwtHttpHandler);
  }

  @Test
  public void fail_to_update_unknown_user() {
    UserTestData admin = createLocalUser();
    userSessionRule.logIn(admin.getUserDto()).setSystemAdministrator();

    assertThatThrownBy(() -> executeTest("polop", null, "polop"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("User with login 'polop' has not been found");
    assertThat(findSessionTokenDto(db.getSession(), admin.getSessionTokenUuid())).isPresent();
    verifyNoInteractions(jwtHttpHandler);
  }

  @Test
  public void fail_on_disabled_user() {
    UserDto user = db.users().insertUser(u -> u.setActive(false));
    userSessionRule.logIn(user);

    assertThatThrownBy(() -> executeTest(user.getLogin(), null, "polop"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("User with login '%s' has not been found", user.getLogin()));
    verifyNoInteractions(jwtHttpHandler);
  }

  @Test
  public void fail_to_update_password_on_self_without_login() {
    when(request.getParameter(PARAM_PASSWORD)).thenReturn("new password");
    when(request.getParameter(PARAM_PREVIOUS_PASSWORD)).thenReturn(NEW_PASSWORD);

    assertThatThrownBy(() -> executeTest(null, OLD_PASSWORD, NEW_PASSWORD))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'login' parameter is missing");
    verifyNoInteractions(jwtHttpHandler);
  }

  @Test
  public void fail_to_update_password_on_self_without_old_password() {
    UserTestData user = createLocalUser();
    userSessionRule.logIn(user.getUserDto());

    assertThatThrownBy(() -> executeTest(user.getLogin(), null, NEW_PASSWORD))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'previousPassword' parameter is missing");
    assertThat(findSessionTokenDto(db.getSession(), user.getSessionTokenUuid())).isPresent();
    verifyNoInteractions(jwtHttpHandler);
  }

  @Test
  public void fail_to_update_password_on_self_without_new_password() {
    UserTestData user = createLocalUser();
    userSessionRule.logIn(user.getUserDto());

    assertThatThrownBy(() -> executeTest(user.getLogin(), OLD_PASSWORD, null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'password' parameter is missing");
    assertThat(findSessionTokenDto(db.getSession(), user.getSessionTokenUuid())).isPresent();
    verifyNoInteractions(jwtHttpHandler);
  }

  @Test
  public void fail_to_update_password_on_self_with_bad_old_password() {
    UserTestData user = createLocalUser();
    userSessionRule.logIn(user.getUserDto());

    assertThatThrownBy(() -> executeTest(user.getLogin(), "I dunno", NEW_PASSWORD))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Incorrect password");
    assertThat(findSessionTokenDto(db.getSession(), user.getSessionTokenUuid())).isPresent();
    verifyNoInteractions(jwtHttpHandler);
  }

  @Test
  public void fail_to_update_password_on_external_auth() throws ServletException, IOException {
    UserDto admin = db.users().insertUser();
    userSessionRule.logIn(admin).setSystemAdministrator();
    UserDto user = db.users().insertUser(u -> u.setLocal(false));

    executeTest(user.getLogin(), "I dunno", NEW_PASSWORD);
    verify(response).setStatus(HTTP_BAD_REQUEST);
  }

  @Test
  public void fail_to_update_to_same_password() {
    UserTestData user = createLocalUser(OLD_PASSWORD);
    userSessionRule.logIn(user.getUserDto());

    assertThatThrownBy(() -> executeTest(user.getLogin(), OLD_PASSWORD, OLD_PASSWORD))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Password must be different from old password");
    assertThat(findSessionTokenDto(db.getSession(), user.getSessionTokenUuid())).isPresent();
    verifyNoInteractions(jwtHttpHandler);
  }

  @Test
  public void verify_definition() {
    String controllerKey = "foo";
    WebService.Context context = new WebService.Context();
    WebService.NewController newController = context.createController(controllerKey);

    changePasswordAction.define(newController);
    newController.done();

    WebService.Action changePassword = context.controller(controllerKey).action("change_password");
    assertThat(changePassword).isNotNull();
    assertThat(changePassword.handler()).isInstanceOf(ServletFilterHandler.class);
    assertThat(changePassword.isPost()).isTrue();
    assertThat(changePassword.params())
      .extracting(WebService.Param::key)
      .containsExactlyInAnyOrder(PARAM_LOGIN, PARAM_PASSWORD, PARAM_PREVIOUS_PASSWORD);
  }

  private void executeTest(@Nullable String login, @Nullable String oldPassword, @Nullable String newPassword) throws IOException, ServletException {
    when(request.getParameter(PARAM_LOGIN)).thenReturn(login);
    when(request.getParameter(PARAM_PREVIOUS_PASSWORD)).thenReturn(oldPassword);
    when(request.getParameter(PARAM_PASSWORD)).thenReturn(newPassword);
    changePasswordAction.doFilter(request, response, chain);
  }

  private UserTestData createLocalUser(String password) {
    UserTestData userTestData = createLocalUser();
    localAuthentication.storeHashPassword(userTestData.getUserDto(), password);
    db.getDbClient().userDao().update(db.getSession(), userTestData.getUserDto());
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

  private static class UserTestData {
    private final UserDto userDto;
    private final SessionTokenDto sessionTokenDto;

    private UserTestData(UserDto userDto, SessionTokenDto sessionTokenDto) {
      this.userDto = userDto;
      this.sessionTokenDto = sessionTokenDto;
    }

    UserDto getUserDto() {
      return userDto;
    }

    String getLogin() {
      return userDto.getLogin();
    }

    String getSessionTokenUuid() {
      return sessionTokenDto.getUuid();
    }
  }

}
