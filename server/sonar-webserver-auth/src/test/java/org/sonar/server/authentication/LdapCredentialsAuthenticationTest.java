/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.auth.ldap.LdapAuthenticator;
import org.sonar.auth.ldap.LdapGroupsProvider;
import org.sonar.auth.ldap.LdapRealm;
import org.sonar.auth.ldap.LdapUserDetails;
import org.sonar.auth.ldap.LdapUsersProvider;
import org.sonar.process.ProcessProperties;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationEvent.Source;
import org.sonar.server.authentication.event.AuthenticationException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method.BASIC;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method.BASIC_TOKEN;

@RunWith(MockitoJUnitRunner.Silent.class)
public class LdapCredentialsAuthenticationTest {

  private static final String LOGIN = "LOGIN";
  private static final String PASSWORD = "PASSWORD";

  private static final String REALM_NAME = "ldap";

  private MapSettings settings = new MapSettings();
  private TestUserRegistrar userRegistrar = new TestUserRegistrar();

  @Mock
  private AuthenticationEvent authenticationEvent;

  @Mock
  private HttpServletRequest request = mock(HttpServletRequest.class);

  @Mock
  private LdapAuthenticator ldapAuthenticator;
  @Mock
  private LdapGroupsProvider ldapGroupsProvider;
  @Mock
  private LdapUsersProvider ldapUsersProvider;
  @Mock
  private LdapRealm ldapRealm;

  private LdapCredentialsAuthentication underTest;

  @Before
  public void setUp() throws Exception {
    settings.setProperty(ProcessProperties.Property.SONAR_SECURITY_REALM.getKey(), "LDAP");
    when(ldapRealm.doGetAuthenticator()).thenReturn(ldapAuthenticator);
    when(ldapRealm.getUsersProvider()).thenReturn(ldapUsersProvider);
    when(ldapRealm.getGroupsProvider()).thenReturn(ldapGroupsProvider);
    underTest = new LdapCredentialsAuthentication(settings.asConfig(), userRegistrar, authenticationEvent, ldapRealm);
  }

  @Test
  public void authenticate_with_null_group_provider() {
    reset(ldapRealm);
    when(ldapRealm.doGetAuthenticator()).thenReturn(ldapAuthenticator);
    when(ldapRealm.getUsersProvider()).thenReturn(ldapUsersProvider);
    when(ldapRealm.getGroupsProvider()).thenReturn(null);
    underTest = new LdapCredentialsAuthentication(settings.asConfig(), userRegistrar, authenticationEvent, ldapRealm);

    when(ldapAuthenticator.doAuthenticate(any(LdapAuthenticator.Context.class))).thenReturn(true);
    LdapUserDetails userDetails = new LdapUserDetails();
    userDetails.setName("name");
    userDetails.setEmail("email");
    when(ldapUsersProvider.doGetUserDetails(any(LdapUsersProvider.Context.class))).thenReturn(userDetails);

    underTest.authenticate(new Credentials(LOGIN, PASSWORD), request, BASIC);

    assertThat(userRegistrar.isAuthenticated()).isTrue();
    assertThat(userRegistrar.getAuthenticatorParameters().getUserIdentity().getProviderLogin()).isEqualTo(LOGIN);
    assertThat(userRegistrar.getAuthenticatorParameters().getUserIdentity().getProviderId()).isNull();
    assertThat(userRegistrar.getAuthenticatorParameters().getUserIdentity().getName()).isEqualTo("name");
    assertThat(userRegistrar.getAuthenticatorParameters().getUserIdentity().getEmail()).isEqualTo("email");
    assertThat(userRegistrar.getAuthenticatorParameters().getUserIdentity().shouldSyncGroups()).isFalse();
    verify(authenticationEvent).loginSuccess(request, LOGIN, Source.realm(BASIC, REALM_NAME));
    verify(ldapRealm).init();
  }

  @Test
  public void authenticate_with_sonarqube_identity_provider() {
    when(ldapAuthenticator.doAuthenticate(any(LdapAuthenticator.Context.class))).thenReturn(true);
    LdapUserDetails userDetails = new LdapUserDetails();
    userDetails.setName("name");
    userDetails.setEmail("email");
    when(ldapUsersProvider.doGetUserDetails(any(LdapUsersProvider.Context.class))).thenReturn(userDetails);

    underTest.authenticate(new Credentials(LOGIN, PASSWORD), request, BASIC);

    assertThat(userRegistrar.isAuthenticated()).isTrue();
    assertThat(userRegistrar.getAuthenticatorParameters().getProvider().getKey()).isEqualTo("sonarqube");
    assertThat(userRegistrar.getAuthenticatorParameters().getProvider().getName()).isEqualTo("sonarqube");
    assertThat(userRegistrar.getAuthenticatorParameters().getProvider().getDisplay()).isNull();
    assertThat(userRegistrar.getAuthenticatorParameters().getProvider().isEnabled()).isTrue();
    verify(authenticationEvent).loginSuccess(request, LOGIN, Source.realm(BASIC, REALM_NAME));
    verify(ldapRealm).init();
  }

  @Test
  public void login_is_used_when_no_name_provided() {
    when(ldapAuthenticator.doAuthenticate(any(LdapAuthenticator.Context.class))).thenReturn(true);
    LdapUserDetails userDetails = new LdapUserDetails();
    userDetails.setEmail("email");
    when(ldapUsersProvider.doGetUserDetails(any(LdapUsersProvider.Context.class))).thenReturn(userDetails);

    underTest.authenticate(new Credentials(LOGIN, PASSWORD), request, BASIC);

    assertThat(userRegistrar.getAuthenticatorParameters().getProvider().getName()).isEqualTo("sonarqube");
    verify(authenticationEvent).loginSuccess(request, LOGIN, Source.realm(BASIC, REALM_NAME));
  }

  @Test
  public void authenticate_with_group_sync() {
    when(ldapGroupsProvider.doGetGroups(any(LdapGroupsProvider.Context.class))).thenReturn(asList("group1", "group2"));

    executeAuthenticate();

    assertThat(userRegistrar.isAuthenticated()).isTrue();
    assertThat(userRegistrar.getAuthenticatorParameters().getUserIdentity().shouldSyncGroups()).isTrue();
    verify(authenticationEvent).loginSuccess(request, LOGIN, Source.realm(BASIC, REALM_NAME));
  }

  @Test
  public void use_login_if_user_details_contains_no_name() {
    when(ldapAuthenticator.doAuthenticate(any(LdapAuthenticator.Context.class))).thenReturn(true);
    LdapUserDetails userDetails = new LdapUserDetails();
    userDetails.setName(null);
    when(ldapUsersProvider.doGetUserDetails(any(LdapUsersProvider.Context.class))).thenReturn(userDetails);

    underTest.authenticate(new Credentials(LOGIN, PASSWORD), request, BASIC);

    assertThat(userRegistrar.isAuthenticated()).isTrue();
    assertThat(userRegistrar.getAuthenticatorParameters().getUserIdentity().getName()).isEqualTo(LOGIN);
    verify(authenticationEvent).loginSuccess(request, LOGIN, Source.realm(BASIC, REALM_NAME));
  }

  @Test
  public void use_downcase_login() {
    settings.setProperty("sonar.authenticator.downcase", true);

    executeAuthenticate("LOGIN");

    assertThat(userRegistrar.isAuthenticated()).isTrue();
    assertThat(userRegistrar.getAuthenticatorParameters().getUserIdentity().getProviderLogin()).isEqualTo("login");
    verify(authenticationEvent).loginSuccess(request, "login", Source.realm(BASIC, REALM_NAME));
  }

  @Test
  public void does_not_user_downcase_login() {
    settings.setProperty("sonar.authenticator.downcase", false);

    executeAuthenticate("LoGiN");

    assertThat(userRegistrar.isAuthenticated()).isTrue();
    assertThat(userRegistrar.getAuthenticatorParameters().getUserIdentity().getProviderLogin()).isEqualTo("LoGiN");
    verify(authenticationEvent).loginSuccess(request, "LoGiN", Source.realm(BASIC, REALM_NAME));
  }

  @Test
  public void fail_to_authenticate_when_user_details_are_null() {
    when(ldapAuthenticator.doAuthenticate(any(LdapAuthenticator.Context.class))).thenReturn(true);

    when(ldapUsersProvider.doGetUserDetails(any(LdapUsersProvider.Context.class))).thenReturn(null);

    Credentials credentials = new Credentials(LOGIN, PASSWORD);
    assertThatThrownBy(() -> underTest.authenticate(credentials, request, BASIC))
      .hasMessage("No user details")
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.realm(BASIC, REALM_NAME))
      .hasFieldOrPropertyWithValue("login", LOGIN);

    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void fail_to_authenticate_when_external_authentication_fails() {
    when(ldapUsersProvider.doGetUserDetails(any(LdapUsersProvider.Context.class))).thenReturn(new LdapUserDetails());

    when(ldapAuthenticator.doAuthenticate(any(LdapAuthenticator.Context.class))).thenReturn(false);

    Credentials credentials = new Credentials(LOGIN, PASSWORD);
    assertThatThrownBy(() -> underTest.authenticate(credentials, request, BASIC))
      .hasMessage("Realm returned authenticate=false")
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.realm(BASIC, REALM_NAME))
      .hasFieldOrPropertyWithValue("login", LOGIN);

    verifyNoInteractions(authenticationEvent);

  }

  @Test
  public void fail_to_authenticate_when_any_exception_is_thrown() {
    String expectedMessage = "emulating exception in doAuthenticate";
    doThrow(new IllegalArgumentException(expectedMessage)).when(ldapAuthenticator).doAuthenticate(any(LdapAuthenticator.Context.class));

    when(ldapUsersProvider.doGetUserDetails(any(LdapUsersProvider.Context.class))).thenReturn(new LdapUserDetails());

    Credentials credentials = new Credentials(LOGIN, PASSWORD);
    assertThatThrownBy(() -> underTest.authenticate(credentials, request, BASIC_TOKEN))
      .hasMessage(expectedMessage)
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.realm(BASIC_TOKEN, REALM_NAME))
      .hasFieldOrPropertyWithValue("login", LOGIN);

    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void return_empty_user_when_ldap_not_activated() {
    reset(ldapRealm);
    settings.clear();
    underTest = new LdapCredentialsAuthentication(settings.asConfig(), userRegistrar, authenticationEvent, ldapRealm);

    assertThat(underTest.authenticate(new Credentials(LOGIN, PASSWORD), request, BASIC)).isEmpty();
    verifyNoInteractions(authenticationEvent);
    verifyNoInteractions(ldapRealm);
  }

  private void executeAuthenticate() {
    executeAuthenticate(LOGIN);
  }

  private void executeAuthenticate(String login) {
    when(ldapAuthenticator.doAuthenticate(any(LdapAuthenticator.Context.class))).thenReturn(true);
    LdapUserDetails userDetails = new LdapUserDetails();
    userDetails.setName("name");
    when(ldapUsersProvider.doGetUserDetails(any(LdapUsersProvider.Context.class))).thenReturn(userDetails);
    underTest.authenticate(new Credentials(login, PASSWORD), request, BASIC);
  }

}
