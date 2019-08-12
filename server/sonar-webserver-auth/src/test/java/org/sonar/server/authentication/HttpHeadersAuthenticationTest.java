/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.utils.System2;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationEvent.Source;
import org.sonar.server.es.EsTester;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationUpdater;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.organization.TestOrganizationFlags;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.authentication.event.AuthenticationExceptionMatcher.authenticationException;

public class HttpHeadersAuthenticationTest {

  private MapSettings settings = new MapSettings();

  @Rule
  public ExpectedException expectedException = none();
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

  private System2 system2 = mock(System2.class);
  private OrganizationUpdater organizationUpdater = mock(OrganizationUpdater.class);
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private TestOrganizationFlags organizationFlags = TestOrganizationFlags.standalone();
  private CredentialsLocalAuthentication localAuthentication = new CredentialsLocalAuthentication(db.getDbClient());

  private UserIndexer userIndexer = new UserIndexer(db.getDbClient(), es.client());
  private UserRegistrarImpl userIdentityAuthenticator = new UserRegistrarImpl(
    db.getDbClient(),
    new UserUpdater(system2, mock(NewUserNotifier.class), db.getDbClient(), userIndexer, organizationFlags, defaultOrganizationProvider,
      new DefaultGroupFinder(db.getDbClient()), settings.asConfig(), localAuthentication),
    defaultOrganizationProvider, organizationFlags, new DefaultGroupFinder(db.getDbClient()), null);

  private HttpServletResponse response = mock(HttpServletResponse.class);
  private JwtHttpHandler jwtHttpHandler = mock(JwtHttpHandler.class);
  private AuthenticationEvent authenticationEvent = mock(AuthenticationEvent.class);

  private HttpHeadersAuthentication underTest = new HttpHeadersAuthentication(system2, settings.asConfig(), userIdentityAuthenticator, jwtHttpHandler, authenticationEvent);

  @Before
  public void setUp() throws Exception {
    when(system2.now()).thenReturn(NOW);
    group1 = db.users().insertGroup(db.getDefaultOrganization(), GROUP1);
    group2 = db.users().insertGroup(db.getDefaultOrganization(), GROUP2);
    sonarUsers = db.users().insertDefaultGroup(db.getDefaultOrganization(), "sonar-users");
  }

  @Test
  public void create_user_when_authenticating_new_user() {
    startWithSso();
    setNotUserInToken();
    HttpServletRequest request = createRequest(DEFAULT_LOGIN, DEFAULT_NAME, DEFAULT_EMAIL, GROUPS);

    underTest.authenticate(request, response);

    verifyUserInDb(DEFAULT_LOGIN, DEFAULT_NAME, DEFAULT_EMAIL, group1, group2, sonarUsers);
    verifyTokenIsUpdated(NOW);
    verify(authenticationEvent).loginSuccess(request, DEFAULT_LOGIN, Source.sso());
  }

  @Test
  public void use_login_when_name_is_not_provided() {
    startWithSso();
    setNotUserInToken();

    HttpServletRequest request = createRequest(DEFAULT_LOGIN, null, null, null);
    underTest.authenticate(request, response);

    verifyUserInDb(DEFAULT_LOGIN, DEFAULT_LOGIN, null, sonarUsers);
    verify(authenticationEvent).loginSuccess(request, DEFAULT_LOGIN, Source.sso());
  }

  @Test
  public void update_user_when_authenticating_exiting_user() {
    startWithSso();
    setNotUserInToken();
    insertUser(newUserDto().setLogin(DEFAULT_LOGIN).setName("old name").setEmail("old email"), group1);
    // Name, email and groups are different
    HttpServletRequest request = createRequest(DEFAULT_LOGIN, DEFAULT_NAME, DEFAULT_EMAIL, GROUP2);

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
    HttpServletRequest request = createRequest(DEFAULT_LOGIN, DEFAULT_NAME, DEFAULT_EMAIL, "");

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
    headerValuesByName.put("X-Forwarded-Groups", null);
    HttpServletRequest request = createRequest(headerValuesByName);

    underTest.authenticate(request, response);

    verityUserHasNoGroup(DEFAULT_LOGIN);
    verify(authenticationEvent).loginSuccess(request, DEFAULT_LOGIN, Source.sso());
  }

  @Test
  public void does_not_update_groups_when_no_group_headers() {
    startWithSso();
    setNotUserInToken();
    insertUser(DEFAULT_USER, group1, sonarUsers);
    HttpServletRequest request = createRequest(DEFAULT_LOGIN, DEFAULT_NAME, DEFAULT_EMAIL, null);

    underTest.authenticate(request, response);

    verityUserGroups(DEFAULT_LOGIN, group1, sonarUsers);
    verify(authenticationEvent).loginSuccess(request, DEFAULT_LOGIN, Source.sso());
  }

  @Test
  public void does_not_update_user_when_user_is_in_token_and_refresh_time_is_close() {
    startWithSso();
    UserDto user = insertUser(DEFAULT_USER, group1);
    setUserInToken(user, CLOSE_REFRESH_TIME);
    HttpServletRequest request = createRequest(DEFAULT_LOGIN, "new name", "new email", GROUP2);

    underTest.authenticate(request, response);

    // User is not updated
    verifyUserInDb(DEFAULT_LOGIN, DEFAULT_NAME, DEFAULT_EMAIL, group1);
    verifyTokenIsNotUpdated();
    verifyZeroInteractions(authenticationEvent);
  }

  @Test
  public void update_user_when_user_in_token_but_refresh_time_is_old() {
    startWithSso();
    UserDto user = insertUser(DEFAULT_USER, group1);
    // Refresh time was updated 6 minutes ago => more than 5 minutes
    setUserInToken(user, NOW - 6 * 60 * 1000L);
    HttpServletRequest request = createRequest(DEFAULT_LOGIN, "new name", "new email", GROUP2);

    underTest.authenticate(request, response);

    // User is updated
    verifyUserInDb(DEFAULT_LOGIN, "new name", "new email", group2);
    verifyTokenIsUpdated(NOW);
    verify(authenticationEvent).loginSuccess(request, DEFAULT_LOGIN, Source.sso());
  }

  @Test
  public void update_user_when_user_in_token_but_no_refresh_time() {
    startWithSso();
    UserDto user = insertUser(DEFAULT_USER, group1);
    setUserInToken(user, null);
    HttpServletRequest request = createRequest(DEFAULT_LOGIN, "new name", "new email", GROUP2);

    underTest.authenticate(request, response);

    // User is updated
    verifyUserInDb(DEFAULT_LOGIN, "new name", "new email", group2);
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
    HttpServletRequest request = createRequest(DEFAULT_LOGIN, "new name", "new email", GROUP2);

    underTest.authenticate(request, response);

    // User is not updated
    verifyUserInDb(DEFAULT_LOGIN, DEFAULT_NAME, DEFAULT_EMAIL, group1);
    verifyTokenIsNotUpdated();
    verifyZeroInteractions(authenticationEvent);
  }

  @Test
  public void update_user_when_login_from_token_is_different_than_login_from_request() {
    startWithSso();
    insertUser(DEFAULT_USER, group1);
    setUserInToken(DEFAULT_USER, CLOSE_REFRESH_TIME);
    HttpServletRequest request = createRequest("AnotherLogin", "Another name", "Another email", GROUP2);

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
    HttpServletRequest request = createRequest(ImmutableMap.of("head-login", DEFAULT_LOGIN, "head-name", DEFAULT_NAME, "head-email", DEFAULT_EMAIL, "head-groups", GROUPS));

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
    HttpServletRequest request = createRequest(ImmutableMap.of("login", DEFAULT_LOGIN, "name", DEFAULT_NAME, "email", DEFAULT_EMAIL, "groups", GROUPS));

    underTest.authenticate(request, response);

    verifyUserInDb(DEFAULT_LOGIN, DEFAULT_NAME, DEFAULT_EMAIL, group1, group2, sonarUsers);
    verify(authenticationEvent).loginSuccess(request, DEFAULT_LOGIN, Source.sso());
  }

  @Test
  public void trim_groups() {
    startWithSso();
    setNotUserInToken();
    HttpServletRequest request = createRequest(DEFAULT_LOGIN, null, null, "  dev ,    admin ");

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
    verifyZeroInteractions(authenticationEvent);
  }

  @Test
  public void does_not_authenticate_when_not_enabled() {
    startWithoutSso();

    underTest.authenticate(createRequest(DEFAULT_LOGIN, DEFAULT_NAME, DEFAULT_EMAIL, GROUPS), response);

    verifyUserNotAuthenticated();
    verifyZeroInteractions(jwtHttpHandler, authenticationEvent);
  }

  @Test
  public void throw_AuthenticationException_when_BadRequestException_is_generated() {
    startWithSso();
    setNotUserInToken();

    expectedException.expect(authenticationException().from(Source.sso()).withoutLogin().andNoPublicMessage());
    expectedException.expectMessage("Use only letters, numbers, and .-_@ please.");
    try {
      underTest.authenticate(createRequest("invalid login", DEFAULT_NAME, DEFAULT_EMAIL, GROUPS), response);
    } finally {
      verifyZeroInteractions(authenticationEvent);
    }
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
    when(jwtHttpHandler.getToken(any(HttpServletRequest.class), any(HttpServletResponse.class)))
      .thenReturn(Optional.of(new JwtHttpHandler.Token(
        user,
        lastRefreshTime == null ? Collections.emptyMap() : ImmutableMap.of("ssoLastRefreshTime", lastRefreshTime))));
  }

  private void setNotUserInToken() {
    when(jwtHttpHandler.getToken(any(HttpServletRequest.class), any(HttpServletResponse.class))).thenReturn(Optional.empty());
  }

  private UserDto insertUser(UserDto user, GroupDto... groups) {
    db.users().insertUser(user);
    stream(groups).forEach(group -> db.users().insertMember(group, user));
    db.commit();
    return user;
  }

  private static HttpServletRequest createRequest(Map<String, String> headerValuesByName) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    setHeaders(request, headerValuesByName);
    return request;
  }

  private static HttpServletRequest createRequest(String login, @Nullable String name, @Nullable String email, @Nullable String groups) {
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
    HttpServletRequest request = mock(HttpServletRequest.class);
    setHeaders(request, headerValuesByName);
    return request;
  }

  private static void setHeaders(HttpServletRequest request, Map<String, String> valuesByName) {
    valuesByName.entrySet().forEach(entry -> when(request.getHeader(entry.getKey())).thenReturn(entry.getValue()));
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
      assertThat(db.users().selectGroupIdsOfUser(userDto)).isEmpty();
    } else {
      assertThat(db.users().selectGroupIdsOfUser(userDto)).containsOnly(stream(expectedGroups).map(GroupDto::getId).collect(MoreCollectors.toList()).toArray(new Integer[] {}));
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
    verify(jwtHttpHandler).generateToken(any(UserDto.class), eq(ImmutableMap.of("ssoLastRefreshTime", refreshTime)), any(HttpServletRequest.class), any(HttpServletResponse.class));
  }

  private void verifyTokenIsNotUpdated() {
    verify(jwtHttpHandler, never()).generateToken(any(UserDto.class), anyMap(), any(HttpServletRequest.class), any(HttpServletResponse.class));
  }

}
