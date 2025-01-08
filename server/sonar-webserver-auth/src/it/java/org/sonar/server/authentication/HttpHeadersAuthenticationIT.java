/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.authentication;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationEvent.Source;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.es.EsTester;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.UserTesting.newUserDto;

public class HttpHeadersAuthenticationIT {

  private final MapSettings settings = new MapSettings().setProperty("sonar.internal.pbkdf2.iterations", "1");

  @Rule
  public DbTester db = DbTester.create(new AlwaysIncreasingSystem2());
  @Rule
  public EsTester es = EsTester.create();

  private static final String DEFAULT_LOGIN = "john";
  private static final String DEFAULT_NAME = "John";
  private static final String DEFAULT_EMAIL = "john@doo.com";
  private static final String GROUP1 = "dev";
  private static final String GROUP2 = "admin";
  private static final String GROUPS = GROUP1 + "," + GROUP2;

  private static final Long NOW = 1_000_000L;
  private static final Long CLOSE_REFRESH_TIME = NOW - 1_000L;

  private static final UserDto DEFAULT_USER = newUserDto()
    .setLogin(DEFAULT_LOGIN)
    .setName(DEFAULT_NAME)
    .setEmail(DEFAULT_EMAIL)
    .setExternalLogin(DEFAULT_LOGIN)
    .setExternalIdentityProvider("sonarqube");

  private GroupDto group1;
  private GroupDto group2;
  private GroupDto sonarUsers;

  private final System2 system2 = mock(System2.class);
  private final CredentialsLocalAuthentication localAuthentication = new CredentialsLocalAuthentication(db.getDbClient(), settings.asConfig());

  private final DefaultGroupFinder defaultGroupFinder = new DefaultGroupFinder(db.getDbClient());
  private final UserRegistrarImpl userIdentityAuthenticator = new UserRegistrarImpl(db.getDbClient(),
    new UserUpdater(mock(NewUserNotifier.class), db.getDbClient(), defaultGroupFinder, settings.asConfig(), mock(AuditPersister.class), localAuthentication),
    defaultGroupFinder, mock(ManagedInstanceService.class));
  private final HttpResponse response = mock(HttpResponse.class);
  private final JwtHttpHandler jwtHttpHandler = mock(JwtHttpHandler.class);
  private final AuthenticationEvent authenticationEvent = mock(AuthenticationEvent.class);
  private final HttpHeadersAuthentication underTest = new HttpHeadersAuthentication(system2, settings.asConfig(), userIdentityAuthenticator, jwtHttpHandler,
    authenticationEvent);

  @Before
  public void setUp() {
    when(system2.now()).thenReturn(NOW);
    group1 = db.users().insertGroup(GROUP1);
    group2 = db.users().insertGroup(GROUP2);
    sonarUsers = db.users().insertDefaultGroup();
  }

  @Test
  public void create_user_when_authenticating_new_user() {
    startWithSso();
    setNotUserInToken();
    HttpRequest request = createRequest(DEFAULT_LOGIN, DEFAULT_NAME, DEFAULT_EMAIL, GROUPS);

    underTest.authenticate(request, response);

    verifyUserInDb(DEFAULT_LOGIN, DEFAULT_NAME, DEFAULT_EMAIL, group1, group2, sonarUsers);
    verifyTokenIsUpdated(NOW);
    verify(authenticationEvent).loginSuccess(request, DEFAULT_LOGIN, Source.sso());
  }

  @Test
  public void use_login_when_name_is_not_provided() {
    startWithSso();
    setNotUserInToken();

    HttpRequest request = createRequest(DEFAULT_LOGIN, null, null, null);
    underTest.authenticate(request, response);

    verifyUserInDb(DEFAULT_LOGIN, DEFAULT_LOGIN, null, sonarUsers);
    verify(authenticationEvent).loginSuccess(request, DEFAULT_LOGIN, Source.sso());
  }

  @Test
  public void update_user_when_authenticating_exiting_user() {
    startWithSso();
    setNotUserInToken();
    insertUser(newUserDto().setLogin(DEFAULT_LOGIN).setExternalLogin(DEFAULT_LOGIN).setExternalIdentityProvider("sonarqube").setName("old name").setEmail(DEFAULT_USER.getEmail()), group1);
    // Name, email and groups are different
    HttpRequest request = createRequest(DEFAULT_LOGIN, DEFAULT_NAME, DEFAULT_EMAIL, GROUP2);

    underTest.authenticate(request, response);

    verifyUserInDb(DEFAULT_LOGIN, DEFAULT_NAME, DEFAULT_EMAIL, group2);
    verifyTokenIsUpdated(NOW);
    verify(authenticationEvent).loginSuccess(request, DEFAULT_LOGIN, Source.sso());
  }

  @Test
  public void remove_groups_when_group_headers_is_empty() {
    startWithSso();
    setNotUserInToken();
    insertUser(DEFAULT_USER, group1);
    HttpRequest request = createRequest(DEFAULT_LOGIN, DEFAULT_NAME, DEFAULT_EMAIL, "");

    underTest.authenticate(request, response);

    verityUserHasNoGroup(DEFAULT_LOGIN);
    verify(authenticationEvent).loginSuccess(request, DEFAULT_LOGIN, Source.sso());
  }

  @Test
  public void remove_groups_when_group_headers_is_null() {
    startWithSso();
    setNotUserInToken();
    insertUser(DEFAULT_USER, group1);
    Map<String, String> headerValuesByName = new HashMap<>();
    headerValuesByName.put("X-Forwarded-Login", DEFAULT_LOGIN);
    headerValuesByName.put("X-Forwarded-Email", DEFAULT_USER.getEmail());
    headerValuesByName.put("X-Forwarded-Groups", null);
    HttpRequest request = createRequest(headerValuesByName);

    underTest.authenticate(request, response);

    verityUserHasNoGroup(DEFAULT_LOGIN);
    verify(authenticationEvent).loginSuccess(request, DEFAULT_LOGIN, Source.sso());
  }

  @Test
  public void does_not_update_groups_when_no_group_headers() {
    startWithSso();
    setNotUserInToken();
    insertUser(DEFAULT_USER, group1, sonarUsers);
    HttpRequest request = createRequest(DEFAULT_LOGIN, DEFAULT_NAME, DEFAULT_EMAIL, null);

    underTest.authenticate(request, response);

    verityUserGroups(DEFAULT_LOGIN, group1, sonarUsers);
    verify(authenticationEvent).loginSuccess(request, DEFAULT_LOGIN, Source.sso());
  }

  @Test
  public void does_not_update_user_when_user_is_in_token_and_refresh_time_is_close() {
    startWithSso();
    UserDto user = insertUser(DEFAULT_USER, group1);
    setUserInToken(user, CLOSE_REFRESH_TIME);
    HttpRequest request = createRequest(DEFAULT_LOGIN, "new name", "new email", GROUP2);

    underTest.authenticate(request, response);

    // User is not updated
    verifyUserInDb(DEFAULT_LOGIN, DEFAULT_NAME, DEFAULT_EMAIL, group1);
    verifyTokenIsNotUpdated();
    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void update_user_when_user_in_token_but_refresh_time_is_old() {
    startWithSso();
    UserDto user = insertUser(DEFAULT_USER, group1);
    // Refresh time was updated 6 minutes ago => more than 5 minutes
    setUserInToken(user, NOW - 6 * 60 * 1000L);
    HttpRequest request = createRequest(DEFAULT_LOGIN, "new name", DEFAULT_USER.getEmail(), GROUP2);

    underTest.authenticate(request, response);

    // User is updated
    verifyUserInDb(DEFAULT_LOGIN, "new name", DEFAULT_USER.getEmail(), group2);
    verifyTokenIsUpdated(NOW);
    verify(authenticationEvent).loginSuccess(request, DEFAULT_LOGIN, Source.sso());
  }

  @Test
  public void update_user_when_user_in_token_but_no_refresh_time() {
    startWithSso();
    UserDto user = insertUser(DEFAULT_USER, group1);
    setUserInToken(user, null);
    HttpRequest request = createRequest(DEFAULT_LOGIN, "new name", DEFAULT_USER.getEmail(), GROUP2);

    underTest.authenticate(request, response);

    // User is updated
    verifyUserInDb(DEFAULT_LOGIN, "new name", DEFAULT_USER.getEmail(), group2);
    verifyTokenIsUpdated(NOW);
    verify(authenticationEvent).loginSuccess(request, DEFAULT_LOGIN, Source.sso());
  }

  @Test
  public void use_refresh_time_from_settings() {
    settings.setProperty("sonar.web.sso.refreshIntervalInMinutes", "10");
    startWithSso();
    UserDto user = insertUser(DEFAULT_USER, group1);
    // Refresh time was updated 6 minutes ago => less than 10 minutes ago so not updated
    setUserInToken(user, NOW - 6 * 60 * 1000L);
    HttpRequest request = createRequest(DEFAULT_LOGIN, "new name", "new email", GROUP2);

    underTest.authenticate(request, response);

    // User is not updated
    verifyUserInDb(DEFAULT_LOGIN, DEFAULT_NAME, DEFAULT_EMAIL, group1);
    verifyTokenIsNotUpdated();
    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void update_user_when_login_from_token_is_different_than_login_from_request() {
    startWithSso();
    insertUser(DEFAULT_USER, group1);
    setUserInToken(DEFAULT_USER, CLOSE_REFRESH_TIME);
    HttpRequest request = createRequest("AnotherLogin", "Another name", "Another email", GROUP2);

    underTest.authenticate(request, response);

    verifyUserInDb("AnotherLogin", "Another name", "Another email", group2, sonarUsers);
    verifyTokenIsUpdated(NOW);
    verify(authenticationEvent).loginSuccess(request, "AnotherLogin", Source.sso());
  }

  @Test
  public void use_headers_from_settings() {
    settings.setProperty("sonar.web.sso.loginHeader", "head-login");
    settings.setProperty("sonar.web.sso.nameHeader", "head-name");
    settings.setProperty("sonar.web.sso.emailHeader", "head-email");
    settings.setProperty("sonar.web.sso.groupsHeader", "head-groups");
    startWithSso();
    setNotUserInToken();
    HttpRequest request = createRequest(ImmutableMap.of("head-login", DEFAULT_LOGIN, "head-name", DEFAULT_NAME, "head-email", DEFAULT_EMAIL, "head-groups", GROUPS));

    underTest.authenticate(request, response);

    verifyUserInDb(DEFAULT_LOGIN, DEFAULT_NAME, DEFAULT_EMAIL, group1, group2, sonarUsers);
    verify(authenticationEvent).loginSuccess(request, DEFAULT_LOGIN, Source.sso());
  }

  @Test
  public void detect_group_header_even_with_wrong_case() {
    settings.setProperty("sonar.web.sso.loginHeader", "login");
    settings.setProperty("sonar.web.sso.nameHeader", "name");
    settings.setProperty("sonar.web.sso.emailHeader", "email");
    settings.setProperty("sonar.web.sso.groupsHeader", "Groups");
    startWithSso();
    setNotUserInToken();
    HttpRequest request = createRequest(ImmutableMap.of("login", DEFAULT_LOGIN, "name", DEFAULT_NAME, "email", DEFAULT_EMAIL, "groups", GROUPS));

    underTest.authenticate(request, response);

    verifyUserInDb(DEFAULT_LOGIN, DEFAULT_NAME, DEFAULT_EMAIL, group1, group2, sonarUsers);
    verify(authenticationEvent).loginSuccess(request, DEFAULT_LOGIN, Source.sso());
  }

  @Test
  public void trim_groups() {
    startWithSso();
    setNotUserInToken();
    HttpRequest request = createRequest(DEFAULT_LOGIN, null, null, "  dev ,    admin ");

    underTest.authenticate(request, response);

    verifyUserInDb(DEFAULT_LOGIN, DEFAULT_LOGIN, null, group1, group2, sonarUsers);
    verify(authenticationEvent).loginSuccess(request, DEFAULT_LOGIN, Source.sso());
  }

  @Test
  public void does_not_authenticate_when_no_header() {
    startWithSso();
    setNotUserInToken();

    underTest.authenticate(createRequest(Collections.emptyMap()), response);

    verifyUserNotAuthenticated();
    verifyTokenIsNotUpdated();
    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void does_not_authenticate_when_not_enabled() {
    startWithoutSso();

    underTest.authenticate(createRequest(DEFAULT_LOGIN, DEFAULT_NAME, DEFAULT_EMAIL, GROUPS), response);

    verifyUserNotAuthenticated();
    verifyNoInteractions(jwtHttpHandler, authenticationEvent);
  }

  @Test
  public void throw_AuthenticationException_when_BadRequestException_is_generated() {
    startWithSso();
    setNotUserInToken();

    assertThatThrownBy(() -> underTest.authenticate(createRequest("invalid login", DEFAULT_NAME, DEFAULT_EMAIL, GROUPS), response))
      .hasMessage("Login should contain only letters, numbers, and .-_@")
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.sso());

    verifyNoInteractions(authenticationEvent);
  }

  private void startWithSso() {
    settings.setProperty("sonar.web.sso.enable", true);
    underTest.start();
  }

  private void startWithoutSso() {
    settings.setProperty("sonar.web.sso.enable", false);
    underTest.start();
  }

  private void setUserInToken(UserDto user, @Nullable Long lastRefreshTime) {
    when(jwtHttpHandler.getToken(any(HttpRequest.class), any(HttpResponse.class)))
      .thenReturn(Optional.of(new JwtHttpHandler.Token(
        user,
        lastRefreshTime == null ? Collections.emptyMap() : ImmutableMap.of("ssoLastRefreshTime", lastRefreshTime))));
  }

  private void setNotUserInToken() {
    when(jwtHttpHandler.getToken(any(HttpRequest.class), any(HttpResponse.class))).thenReturn(Optional.empty());
  }

  private UserDto insertUser(UserDto user, GroupDto... groups) {
    db.users().insertUser(user);
    stream(groups).forEach(group -> db.users().insertMember(group, user));
    db.commit();
    return user;
  }

  private static HttpRequest createRequest(Map<String, String> headerValuesByName) {
    HttpRequest request = mock(HttpRequest.class);
    setHeaders(request, headerValuesByName);
    return request;
  }

  private static HttpRequest createRequest(String login, @Nullable String name, @Nullable String email, @Nullable String groups) {
    Map<String, String> headerValuesByName = new HashMap<>();
    headerValuesByName.put("X-Forwarded-Login", login);
    if (name != null) {
      headerValuesByName.put("X-Forwarded-Name", name);
    }
    if (email != null) {
      headerValuesByName.put("X-Forwarded-Email", email);
    }
    if (groups != null) {
      headerValuesByName.put("X-Forwarded-Groups", groups);
    }
    HttpRequest request = mock(HttpRequest.class);
    setHeaders(request, headerValuesByName);
    return request;
  }

  private static void setHeaders(HttpRequest request, Map<String, String> valuesByName) {
    valuesByName.forEach((key, value) -> when(request.getHeader(key)).thenReturn(value));
    when(request.getHeaderNames()).thenReturn(Collections.enumeration(valuesByName.keySet()));
  }

  private void verifyUserInDb(String expectedLogin, String expectedName, @Nullable String expectedEmail, GroupDto... expectedGroups) {
    UserDto userDto = db.users().selectUserByLogin(expectedLogin).get();
    assertThat(userDto.isActive()).isTrue();
    assertThat(userDto.getName()).isEqualTo(expectedName);
    assertThat(userDto.getEmail()).isEqualTo(expectedEmail);
    assertThat(userDto.getExternalLogin()).isEqualTo(expectedLogin);
    assertThat(userDto.getExternalIdentityProvider()).isEqualTo("sonarqube");
    verityUserGroups(expectedLogin, expectedGroups);
  }

  private void verityUserGroups(String login, GroupDto... expectedGroups) {
    UserDto userDto = db.users().selectUserByLogin(login).get();
    if (expectedGroups.length == 0) {
      assertThat(db.users().selectGroupUuidsOfUser(userDto)).isEmpty();
    } else {
      assertThat(db.users().selectGroupUuidsOfUser(userDto)).containsOnly(stream(expectedGroups).map(GroupDto::getUuid).toArray(String[]::new));
    }
  }

  private void verityUserHasNoGroup(String login) {
    verityUserGroups(login);
  }

  private void verifyUserNotAuthenticated() {
    assertThat(db.countRowsOfTable(db.getSession(), "users")).isZero();
    verifyTokenIsNotUpdated();
  }

  private void verifyTokenIsUpdated(long refreshTime) {
    verify(jwtHttpHandler).generateToken(any(UserDto.class), eq(ImmutableMap.of("ssoLastRefreshTime", refreshTime)), any(HttpRequest.class), any(HttpResponse.class));
  }

  private void verifyTokenIsNotUpdated() {
    verify(jwtHttpHandler, never()).generateToken(any(UserDto.class), anyMap(), any(HttpRequest.class), any(HttpResponse.class));
  }
}
