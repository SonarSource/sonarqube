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
package org.sonar.server.authentication;

import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.security.Authenticator;
import org.sonar.api.security.ExternalGroupsProvider;
import org.sonar.api.security.ExternalUsersProvider;
import org.sonar.api.security.SecurityRealm;
import org.sonar.api.security.UserDetails;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationEvent.Source;
import org.sonar.server.user.SecurityRealmFactory;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.authentication.UserIdentityAuthenticator.ExistingEmailStrategy.FORBID;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method.BASIC;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method.BASIC_TOKEN;
import static org.sonar.server.authentication.event.AuthenticationExceptionMatcher.authenticationException;

public class RealmAuthenticatorTest {

  private static final String LOGIN = "LOGIN";
  private static final String PASSWORD = "PASSWORD";

  private static final UserDto USER = newUserDto();
  private static final String REALM_NAME = "realm name";

  @Rule
  public ExpectedException expectedException = none();

  private ArgumentCaptor<UserIdentity> userIdentityArgumentCaptor = ArgumentCaptor.forClass(UserIdentity.class);
  private ArgumentCaptor<IdentityProvider> identityProviderArgumentCaptor = ArgumentCaptor.forClass(IdentityProvider.class);
  private ArgumentCaptor<AuthenticationEvent.Source> sourceCaptor = ArgumentCaptor.forClass(Source.class);

  private MapSettings settings = new MapSettings();

  private SecurityRealmFactory securityRealmFactory = mock(SecurityRealmFactory.class);
  private SecurityRealm realm = mock(SecurityRealm.class);
  private Authenticator authenticator = mock(Authenticator.class);
  private ExternalUsersProvider externalUsersProvider = mock(ExternalUsersProvider.class);
  private ExternalGroupsProvider externalGroupsProvider = mock(ExternalGroupsProvider.class);

  private UserIdentityAuthenticator userIdentityAuthenticator = mock(UserIdentityAuthenticator.class);
  private AuthenticationEvent authenticationEvent = mock(AuthenticationEvent.class);

  private HttpServletRequest request = mock(HttpServletRequest.class);

  private RealmAuthenticator underTest = new RealmAuthenticator(settings.asConfig(), securityRealmFactory, userIdentityAuthenticator, authenticationEvent);

  @Before
  public void setUp() throws Exception {
    when(realm.getName()).thenReturn(REALM_NAME);
  }

  @Test
  public void authenticate() {
    executeStartWithoutGroupSync();
    when(authenticator.doAuthenticate(any(Authenticator.Context.class))).thenReturn(true);
    UserDetails userDetails = new UserDetails();
    userDetails.setName("name");
    userDetails.setEmail("email");
    when(externalUsersProvider.doGetUserDetails(any(ExternalUsersProvider.Context.class))).thenReturn(userDetails);
    when(userIdentityAuthenticator.authenticate(any(UserIdentity.class), any(IdentityProvider.class), any(Source.class), eq(FORBID))).thenReturn(USER);

    underTest.authenticate(LOGIN, PASSWORD, request, BASIC);

    verify(userIdentityAuthenticator).authenticate(userIdentityArgumentCaptor.capture(), identityProviderArgumentCaptor.capture(), sourceCaptor.capture(), eq(FORBID));
    UserIdentity userIdentity = userIdentityArgumentCaptor.getValue();
    assertThat(userIdentity.getLogin()).isEqualTo(LOGIN);
    assertThat(userIdentity.getProviderLogin()).isEqualTo(LOGIN);
    assertThat(userIdentity.getName()).isEqualTo("name");
    assertThat(userIdentity.getEmail()).isEqualTo("email");
    assertThat(userIdentity.shouldSyncGroups()).isFalse();
    verify(authenticationEvent).loginSuccess(request, LOGIN, Source.realm(BASIC, REALM_NAME));
  }

  @Test
  public void authenticate_with_sonarqube_identity_provider() {
    executeStartWithoutGroupSync();
    when(authenticator.doAuthenticate(any(Authenticator.Context.class))).thenReturn(true);
    UserDetails userDetails = new UserDetails();
    userDetails.setName("name");
    userDetails.setEmail("email");
    when(externalUsersProvider.doGetUserDetails(any(ExternalUsersProvider.Context.class))).thenReturn(userDetails);
    when(userIdentityAuthenticator.authenticate(any(UserIdentity.class), any(IdentityProvider.class), any(Source.class), eq(FORBID))).thenReturn(USER);

    underTest.authenticate(LOGIN, PASSWORD, request, BASIC);

    verify(userIdentityAuthenticator).authenticate(userIdentityArgumentCaptor.capture(), identityProviderArgumentCaptor.capture(), sourceCaptor.capture(), eq(FORBID));

    assertThat(identityProviderArgumentCaptor.getValue().getKey()).isEqualTo("sonarqube");
    assertThat(identityProviderArgumentCaptor.getValue().getName()).isEqualTo("sonarqube");
    assertThat(identityProviderArgumentCaptor.getValue().getDisplay()).isNull();
    assertThat(identityProviderArgumentCaptor.getValue().isEnabled()).isTrue();
    verify(authenticationEvent).loginSuccess(request, LOGIN, Source.realm(BASIC, REALM_NAME));
  }

  @Test
  public void login_is_used_when_no_name_provided() {
    executeStartWithoutGroupSync();
    when(authenticator.doAuthenticate(any(Authenticator.Context.class))).thenReturn(true);
    UserDetails userDetails = new UserDetails();
    userDetails.setEmail("email");
    when(externalUsersProvider.doGetUserDetails(any(ExternalUsersProvider.Context.class))).thenReturn(userDetails);
    when(userIdentityAuthenticator.authenticate(any(UserIdentity.class), any(IdentityProvider.class), any(Source.class), eq(FORBID))).thenReturn(USER);

    underTest.authenticate(LOGIN, PASSWORD, request, BASIC);

    verify(userIdentityAuthenticator).authenticate(userIdentityArgumentCaptor.capture(), identityProviderArgumentCaptor.capture(), sourceCaptor.capture(), eq(FORBID));
    assertThat(identityProviderArgumentCaptor.getValue().getName()).isEqualTo("sonarqube");
    verify(authenticationEvent).loginSuccess(request, LOGIN, Source.realm(BASIC, REALM_NAME));
  }

  @Test
  public void authenticate_with_group_sync() {
    when(externalGroupsProvider.doGetGroups(any(ExternalGroupsProvider.Context.class))).thenReturn(asList("group1", "group2"));
    when(userIdentityAuthenticator.authenticate(any(UserIdentity.class), any(IdentityProvider.class), any(Source.class), eq(FORBID))).thenReturn(USER);
    executeStartWithGroupSync();
    executeAuthenticate();

    verify(userIdentityAuthenticator).authenticate(userIdentityArgumentCaptor.capture(), identityProviderArgumentCaptor.capture(), sourceCaptor.capture(), eq(FORBID));

    UserIdentity userIdentity = userIdentityArgumentCaptor.getValue();
    assertThat(userIdentity.shouldSyncGroups()).isTrue();
    assertThat(userIdentity.getGroups()).containsOnly("group1", "group2");
    verify(authenticationEvent).loginSuccess(request, LOGIN, Source.realm(BASIC, REALM_NAME));
  }

  @Test
  public void use_login_if_user_details_contains_no_name() {
    executeStartWithoutGroupSync();
    when(authenticator.doAuthenticate(any(Authenticator.Context.class))).thenReturn(true);
    UserDetails userDetails = new UserDetails();
    userDetails.setName(null);
    when(externalUsersProvider.doGetUserDetails(any(ExternalUsersProvider.Context.class))).thenReturn(userDetails);
    when(userIdentityAuthenticator.authenticate(any(UserIdentity.class), any(IdentityProvider.class), any(Source.class), eq(FORBID))).thenReturn(USER);

    underTest.authenticate(LOGIN, PASSWORD, request, BASIC);

    verify(userIdentityAuthenticator).authenticate(userIdentityArgumentCaptor.capture(), identityProviderArgumentCaptor.capture(), sourceCaptor.capture(), eq(FORBID));
    assertThat(userIdentityArgumentCaptor.getValue().getName()).isEqualTo(LOGIN);
    verify(authenticationEvent).loginSuccess(request, LOGIN, Source.realm(BASIC, REALM_NAME));
  }

  @Test
  public void use_downcase_login() {
    settings.setProperty("sonar.authenticator.downcase", true);
    when(userIdentityAuthenticator.authenticate(any(UserIdentity.class), any(IdentityProvider.class), any(Source.class), eq(FORBID))).thenReturn(USER);
    executeStartWithoutGroupSync();
    executeAuthenticate("LOGIN");

    verify(userIdentityAuthenticator).authenticate(userIdentityArgumentCaptor.capture(), identityProviderArgumentCaptor.capture(), sourceCaptor.capture(), eq(FORBID));
    UserIdentity userIdentity = userIdentityArgumentCaptor.getValue();
    assertThat(userIdentity.getLogin()).isEqualTo("login");
    assertThat(userIdentity.getProviderLogin()).isEqualTo("login");
    verify(authenticationEvent).loginSuccess(request, "login", Source.realm(BASIC, REALM_NAME));
  }

  @Test
  public void does_not_user_downcase_login() {
    settings.setProperty("sonar.authenticator.downcase", false);
    when(userIdentityAuthenticator.authenticate(any(UserIdentity.class), any(IdentityProvider.class), any(Source.class), eq(FORBID))).thenReturn(USER);
    executeStartWithoutGroupSync();
    executeAuthenticate("LoGiN");

    verify(userIdentityAuthenticator).authenticate(userIdentityArgumentCaptor.capture(), identityProviderArgumentCaptor.capture(), sourceCaptor.capture(), eq(FORBID));
    UserIdentity userIdentity = userIdentityArgumentCaptor.getValue();
    assertThat(userIdentity.getLogin()).isEqualTo("LoGiN");
    assertThat(userIdentity.getProviderLogin()).isEqualTo("LoGiN");
    verify(authenticationEvent).loginSuccess(request, "LoGiN", Source.realm(BASIC, REALM_NAME));
  }

  @Test
  public void fail_to_authenticate_when_user_details_are_null() {
    executeStartWithoutGroupSync();
    when(authenticator.doAuthenticate(any(Authenticator.Context.class))).thenReturn(true);

    when(externalUsersProvider.doGetUserDetails(any(ExternalUsersProvider.Context.class))).thenReturn(null);

    expectedException.expect(authenticationException().from(Source.realm(BASIC, REALM_NAME)).withLogin(LOGIN).andNoPublicMessage());
    expectedException.expectMessage("No user details");
    try {
      underTest.authenticate(LOGIN, PASSWORD, request, BASIC);
    } finally {
      verifyZeroInteractions(authenticationEvent);
    }
  }

  @Test
  public void fail_to_authenticate_when_external_authentication_fails() {
    executeStartWithoutGroupSync();
    when(externalUsersProvider.doGetUserDetails(any(ExternalUsersProvider.Context.class))).thenReturn(new UserDetails());

    when(authenticator.doAuthenticate(any(Authenticator.Context.class))).thenReturn(false);

    expectedException.expect(authenticationException().from(Source.realm(BASIC, REALM_NAME)).withLogin(LOGIN).andNoPublicMessage());
    expectedException.expectMessage("Realm returned authenticate=false");
    try {
      underTest.authenticate(LOGIN, PASSWORD, request, BASIC);
    } finally {
      verifyZeroInteractions(authenticationEvent);
    }
  }

  @Test
  public void fail_to_authenticate_when_any_exception_is_thrown() {
    executeStartWithoutGroupSync();
    String expectedMessage = "emulating exception in doAuthenticate";
    doThrow(new IllegalArgumentException(expectedMessage)).when(authenticator).doAuthenticate(any(Authenticator.Context.class));

    when(externalUsersProvider.doGetUserDetails(any(ExternalUsersProvider.Context.class))).thenReturn(new UserDetails());

    expectedException.expect(authenticationException().from(Source.realm(BASIC_TOKEN, REALM_NAME)).withLogin(LOGIN).andNoPublicMessage());
    expectedException.expectMessage(expectedMessage);
    try {
      underTest.authenticate(LOGIN, PASSWORD, request, BASIC_TOKEN);
    } finally {
      verifyZeroInteractions(authenticationEvent);
    }
  }

  @Test
  public void return_empty_user_when_no_realm() {
    assertThat(underTest.authenticate(LOGIN, PASSWORD, request, BASIC)).isEmpty();
    verifyNoMoreInteractions(authenticationEvent);
  }

  @Test
  public void fail_to_start_when_no_authenticator() {
    when(realm.doGetAuthenticator()).thenReturn(null);
    when(securityRealmFactory.getRealm()).thenReturn(realm);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("No authenticator available");
    underTest.start();
  }

  @Test
  public void fail_to_start_when_no_user_provider() {
    when(realm.doGetAuthenticator()).thenReturn(authenticator);
    when(realm.getUsersProvider()).thenReturn(null);
    when(securityRealmFactory.getRealm()).thenReturn(realm);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("No users provider available");
    underTest.start();
  }

  private void executeStartWithoutGroupSync() {
    when(realm.doGetAuthenticator()).thenReturn(authenticator);
    when(realm.getUsersProvider()).thenReturn(externalUsersProvider);
    when(securityRealmFactory.getRealm()).thenReturn(realm);
    underTest.start();
  }

  private void executeStartWithGroupSync() {
    when(realm.doGetAuthenticator()).thenReturn(authenticator);
    when(realm.getUsersProvider()).thenReturn(externalUsersProvider);
    when(realm.getGroupsProvider()).thenReturn(externalGroupsProvider);
    when(securityRealmFactory.getRealm()).thenReturn(realm);
    underTest.start();
  }

  private void executeAuthenticate() {
    executeAuthenticate(LOGIN);
  }

  private void executeAuthenticate(String login) {
    when(authenticator.doAuthenticate(any(Authenticator.Context.class))).thenReturn(true);
    UserDetails userDetails = new UserDetails();
    userDetails.setName("name");
    when(externalUsersProvider.doGetUserDetails(any(ExternalUsersProvider.Context.class))).thenReturn(userDetails);
    underTest.authenticate(login, PASSWORD, request, BASIC);
  }

}
