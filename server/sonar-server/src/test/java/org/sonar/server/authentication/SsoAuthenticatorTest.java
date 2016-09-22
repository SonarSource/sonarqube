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

package org.sonar.server.authentication;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.UserTesting.newUserDto;

public class SsoAuthenticatorTest {

  @Rule
  public ExpectedException expectedException = none();

  private static final String LOGIN = "john";
  private static final String NAME = "John";
  private static final String EMAIL = "john@doo.com";
  private static final String GROUPS = "dev,admin";

  private static final UserDto USER = newUserDto()
    .setLogin(LOGIN)
    .setName(NAME)
    .setEmail(EMAIL);

  private Settings settings = new MapSettings();

  private UserIdentityAuthenticator userIdentityAuthenticator = mock(UserIdentityAuthenticator.class);

  private HttpServletRequest request = mock(HttpServletRequest.class);
  private HttpServletResponse response = mock(HttpServletResponse.class);
  private JwtHttpHandler jwtHttpHandler = mock(JwtHttpHandler.class);

  private ArgumentCaptor<UserIdentity> userIdentityCaptor = ArgumentCaptor.forClass(UserIdentity.class);
  private ArgumentCaptor<IdentityProvider> identityProviderCaptor = ArgumentCaptor.forClass(IdentityProvider.class);

  private SsoAuthenticator underTest = new SsoAuthenticator(settings, userIdentityAuthenticator, jwtHttpHandler);

  @Test
  public void authenticate() throws Exception {
    enableSsoAndSimulateNewUser();
    defineHeadersFromDefaultValues(LOGIN, NAME, EMAIL, GROUPS);

    underTest.authenticate(request, response);

    verifyUser(LOGIN, NAME, EMAIL, "dev", "admin");
    verify(jwtHttpHandler).validateToken(request, response);
  }

  @Test
  public void use_login_when_name_is_not_provided() throws Exception {
    enableSsoAndSimulateNewUser();
    defineHeadersFromDefaultValues(LOGIN, null, null, null);

    underTest.authenticate(request, response);

    verifyUser(LOGIN, LOGIN, null);
  }

  @Test
  public void authenticate_using_headers_defined_in_settings() throws Exception {
    enableSsoAndSimulateNewUser();
    settings.setProperty("sonar.sso.loginHeader", "head-login");
    settings.setProperty("sonar.sso.nameHeader", "head-name");
    settings.setProperty("sonar.sso.emailHeader", "head-email");
    settings.setProperty("sonar.sso.groupsHeader", "head-groups");
    defineHeaders(ImmutableMap.of("head-login", LOGIN, "head-name", NAME, "head-email", EMAIL, "head-groups", GROUPS));

    underTest.authenticate(request, response);

    verifyUser(LOGIN, NAME, EMAIL, "dev", "admin");
  }

  @Test
  public void trim_groups() throws Exception {
    enableSsoAndSimulateNewUser();
    defineHeadersFromDefaultValues(LOGIN, null, null, "  dev ,    admin ");

    underTest.authenticate(request, response);

    verifyUser(LOGIN, LOGIN, null, "dev", "admin");
  }

  @Test
  public void verify_identity_provider() throws Exception {
    enableSsoAndSimulateNewUser();
    defineHeadersFromDefaultValues(LOGIN, NAME, EMAIL, GROUPS);

    underTest.authenticate(request, response);

    verify(userIdentityAuthenticator).authenticate(any(UserIdentity.class), identityProviderCaptor.capture());
    assertThat(identityProviderCaptor.getValue().getKey()).isEqualTo("sonarqube");
    assertThat(identityProviderCaptor.getValue().getName()).isEqualTo("sonarqube");
    assertThat(identityProviderCaptor.getValue().getDisplay()).isNull();
    assertThat(identityProviderCaptor.getValue().isEnabled()).isTrue();
    assertThat(identityProviderCaptor.getValue().allowsUsersToSignUp()).isTrue();
  }

  @Test
  public void does_not_authenticate_when_no_header() throws Exception {
    enableSsoAndSimulateNewUser();

    underTest.authenticate(request, response);

    verifyZeroInteractions(userIdentityAuthenticator, jwtHttpHandler);
  }

  @Test
  public void does_not_authenticate_when_not_enabled() throws Exception {
    settings.setProperty("sonar.sso.enable", false);

    underTest.authenticate(request, response);

    verifyZeroInteractions(userIdentityAuthenticator, jwtHttpHandler);
  }

  private void enableSsoAndSimulateNewUser() {
    settings.setProperty("sonar.sso.enable", true);
    when(userIdentityAuthenticator.authenticate(any(UserIdentity.class), any(IdentityProvider.class))).thenReturn(USER);
    when(jwtHttpHandler.validateToken(request, response)).thenReturn(Optional.of(USER));
  }

  private void defineHeadersFromDefaultValues(String login, @Nullable String name, @Nullable String email, @Nullable String groups) {
    Map<String, String> valuesByName = new HashMap<>();
    valuesByName.put("X-Forwarded-Login", login);
    if (name != null) {
      valuesByName.put("X-Forwarded-Name", name);
    }
    if (email != null) {
      valuesByName.put("X-Forwarded-Email", email);
    }
    if (groups != null) {
      valuesByName.put("X-Forwarded-Groups", groups);
    }
    defineHeaders(valuesByName);
  }

  private void defineHeaders(Map<String, String> valuesByName) {
    valuesByName.entrySet().forEach(entry -> when(request.getHeader(entry.getKey())).thenReturn(entry.getValue()));
    when(request.getHeaderNames()).thenReturn(Collections.enumeration(valuesByName.keySet()));
  }

  private void verifyUser(String expectedLogin, String expectedName, @Nullable String expectedEmail, String... expectedGroups) {
    verify(userIdentityAuthenticator).authenticate(userIdentityCaptor.capture(), any(IdentityProvider.class));
    assertThat(userIdentityCaptor.getValue().getLogin()).isEqualTo(expectedLogin);
    assertThat(userIdentityCaptor.getValue().getName()).isEqualTo(expectedName);
    assertThat(userIdentityCaptor.getValue().getEmail()).isEqualTo(expectedEmail);
    assertThat(userIdentityCaptor.getValue().getGroups()).containsOnly(expectedGroups);
  }

}
