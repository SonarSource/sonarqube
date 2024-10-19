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
package org.sonar.server.authentication;

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.server.http.HttpRequest;
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
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.auth.ldap.LdapAuthenticationResult.failed;
import static org.sonar.auth.ldap.LdapAuthenticationResult.success;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method.BASIC;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method.SONARQUBE_TOKEN;

@RunWith(MockitoJUnitRunner.Silent.class)
public class LdapCredentialsAuthenticationTest {

  private static final String LOGIN = "LOGIN";
  private static final String PASSWORD = "PASSWORD";

  private static final String LDAP_SECURITY_REALM_NAME = "ldap";
  private static final String SERVER_KEY = "superServerKey";
  private static final String EXPECTED_EXTERNAL_PROVIDER_ID = "LDAP_superServerKey";

  private static final LdapUserDetails LDAP_USER_DETAILS;

  static {
    LDAP_USER_DETAILS = new LdapUserDetails();
    LDAP_USER_DETAILS.setName("name");
  }

  private static final LdapUserDetails LDAP_USER_DETAILS_WITH_EMAIL;

  static {
    LDAP_USER_DETAILS_WITH_EMAIL = new LdapUserDetails();
    LDAP_USER_DETAILS_WITH_EMAIL.setName("name");
    LDAP_USER_DETAILS_WITH_EMAIL.setEmail("email");
  }

  private final MapSettings settings = new MapSettings();
  private final TestUserRegistrar userRegistrar = new TestUserRegistrar();

  @Mock
  private AuthenticationEvent authenticationEvent;

  @Mock
  private HttpRequest request = mock(HttpRequest.class);

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
    settings.setProperty(ProcessProperties.Property.SONAR_AUTHENTICATOR_IGNORE_STARTUP_FAILURE.getKey(), "true");
    when(ldapRealm.getAuthenticator()).thenReturn(ldapAuthenticator);
    when(ldapRealm.getUsersProvider()).thenReturn(ldapUsersProvider);
    when(ldapRealm.getGroupsProvider()).thenReturn(ldapGroupsProvider);
    when(ldapRealm.isLdapAuthActivated()).thenReturn(true);
    underTest = new LdapCredentialsAuthentication(settings.asConfig(), userRegistrar, authenticationEvent, ldapRealm);
  }

  @Test
  public void authenticate_with_null_group_provider() {
    reset(ldapRealm);
    when(ldapRealm.getAuthenticator()).thenReturn(ldapAuthenticator);
    when(ldapRealm.getUsersProvider()).thenReturn(ldapUsersProvider);
    when(ldapRealm.getGroupsProvider()).thenReturn(null);
    when(ldapRealm.isLdapAuthActivated()).thenReturn(true);
    underTest = new LdapCredentialsAuthentication(settings.asConfig(), userRegistrar, authenticationEvent, ldapRealm);

    LdapAuthenticator.Context authenticationContext = new LdapAuthenticator.Context(LOGIN, PASSWORD, request);
    when(ldapAuthenticator.doAuthenticate(refEq(authenticationContext))).thenReturn(success(SERVER_KEY));

    LdapUsersProvider.Context expectedUserContext = new LdapUsersProvider.Context(SERVER_KEY, LOGIN, request);
    when(ldapUsersProvider.doGetUserDetails(refEq(expectedUserContext))).thenReturn(LDAP_USER_DETAILS_WITH_EMAIL);

    underTest.authenticate(new Credentials(LOGIN, PASSWORD), request, BASIC);

    UserIdentity identity = userRegistrar.getAuthenticatorParameters().getUserIdentity();
    assertThat(userRegistrar.isAuthenticated()).isTrue();
    assertThat(identity.getProviderLogin()).isEqualTo(LOGIN);
    assertThat(identity.getProviderId()).isNull();
    assertThat(identity.getName()).isEqualTo("name");
    assertThat(identity.getEmail()).isEqualTo("email");
    assertThat(identity.shouldSyncGroups()).isFalse();

    verify(authenticationEvent).loginSuccess(request, LOGIN, Source.realm(BASIC, LDAP_SECURITY_REALM_NAME));
  }

  @Test
  public void authenticate_with_ldap() {
    executeAuthenticate(LDAP_USER_DETAILS_WITH_EMAIL);

    IdentityProvider provider = userRegistrar.getAuthenticatorParameters().getProvider();
    assertThat(userRegistrar.isAuthenticated()).isTrue();
    assertThat(provider.getKey()).isEqualTo(EXPECTED_EXTERNAL_PROVIDER_ID);
    assertThat(provider.getName()).isEqualTo(EXPECTED_EXTERNAL_PROVIDER_ID);
    assertThat(provider.getDisplay()).isNull();
    assertThat(provider.isEnabled()).isTrue();
    verify(authenticationEvent).loginSuccess(request, LOGIN, Source.realm(BASIC, LDAP_SECURITY_REALM_NAME));
  }

  @Test
  public void login_is_used_when_no_name_provided() {
    LdapUserDetails userDetails = new LdapUserDetails();
    userDetails.setEmail("email");

    executeAuthenticate(userDetails);

    assertThat(userRegistrar.getAuthenticatorParameters().getProvider().getName()).isEqualTo(EXPECTED_EXTERNAL_PROVIDER_ID);
    verify(authenticationEvent).loginSuccess(request, LOGIN, Source.realm(BASIC, LDAP_SECURITY_REALM_NAME));
  }

  @Test
  public void authenticate_with_group_sync() {

    LdapGroupsProvider.Context expectedGroupContext = new LdapGroupsProvider.Context(SERVER_KEY, LOGIN, request);
    when(ldapGroupsProvider.doGetGroups(refEq(expectedGroupContext))).thenReturn(asList("group1", "group2"));

    executeAuthenticate(LDAP_USER_DETAILS);

    assertThat(userRegistrar.isAuthenticated()).isTrue();
    assertThat(userRegistrar.getAuthenticatorParameters().getUserIdentity().shouldSyncGroups()).isTrue();
    verify(authenticationEvent).loginSuccess(request, LOGIN, Source.realm(BASIC, LDAP_SECURITY_REALM_NAME));
  }

  @Test
  public void use_login_if_user_details_contains_no_name() {

    LdapUserDetails userDetails = new LdapUserDetails();
    userDetails.setName(null);

    executeAuthenticate(userDetails);

    assertThat(userRegistrar.isAuthenticated()).isTrue();
    assertThat(userRegistrar.getAuthenticatorParameters().getUserIdentity().getName()).isEqualTo(LOGIN);
    verify(authenticationEvent).loginSuccess(request, LOGIN, Source.realm(BASIC, LDAP_SECURITY_REALM_NAME));
  }

  @Test
  public void use_downcase_login() {
    settings.setProperty("sonar.authenticator.downcase", true);

    mockLdapAuthentication(LOGIN.toLowerCase());
    mockLdapUserDetailsRetrieval(LOGIN.toLowerCase(), LDAP_USER_DETAILS);

    underTest.authenticate(new Credentials(LOGIN.toLowerCase(), PASSWORD), request, BASIC);

    assertThat(userRegistrar.isAuthenticated()).isTrue();
    assertThat(userRegistrar.getAuthenticatorParameters().getUserIdentity().getProviderLogin()).isEqualTo("login");
    verify(authenticationEvent).loginSuccess(request, "login", Source.realm(BASIC, LDAP_SECURITY_REALM_NAME));
  }

  @Test
  public void does_not_user_downcase_login() {
    settings.setProperty("sonar.authenticator.downcase", false);

    mockLdapAuthentication("LoGiN");
    mockLdapUserDetailsRetrieval("LoGiN", LDAP_USER_DETAILS);

    underTest.authenticate(new Credentials("LoGiN", PASSWORD), request, BASIC);

    assertThat(userRegistrar.isAuthenticated()).isTrue();
    assertThat(userRegistrar.getAuthenticatorParameters().getUserIdentity().getProviderLogin()).isEqualTo("LoGiN");
    verify(authenticationEvent).loginSuccess(request, "LoGiN", Source.realm(BASIC, LDAP_SECURITY_REALM_NAME));
  }

  @Test
  public void fail_to_authenticate_when_user_details_are_null() {

    assertThatThrownBy(() -> executeAuthenticate(null))
      .hasMessage("No user details")
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.realm(BASIC, LDAP_SECURITY_REALM_NAME))
      .hasFieldOrPropertyWithValue("login", LOGIN);

    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void fail_to_authenticate_when_external_authentication_fails() {

    LdapAuthenticator.Context authenticationContext = new LdapAuthenticator.Context(LOGIN, PASSWORD, request);
    when(ldapAuthenticator.doAuthenticate(refEq(authenticationContext))).thenReturn(failed());

    Credentials credentials = new Credentials(LOGIN, PASSWORD);
    assertThatThrownBy(() -> underTest.authenticate(credentials, request, BASIC))
      .hasMessage("Realm returned authenticate=false")
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.realm(BASIC, LDAP_SECURITY_REALM_NAME))
      .hasFieldOrPropertyWithValue("login", LOGIN);

    verifyNoInteractions(ldapUsersProvider);
    verifyNoInteractions(authenticationEvent);

  }

  @Test
  public void fail_to_authenticate_when_any_exception_is_thrown() {
    String expectedMessage = "emulating exception in doAuthenticate";
    doThrow(new IllegalArgumentException(expectedMessage)).when(ldapAuthenticator).doAuthenticate(any(LdapAuthenticator.Context.class));

    Credentials credentials = new Credentials(LOGIN, PASSWORD);
    assertThatThrownBy(() -> underTest.authenticate(credentials, request, SONARQUBE_TOKEN))
      .hasMessage(expectedMessage)
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.realm(SONARQUBE_TOKEN, LDAP_SECURITY_REALM_NAME))
      .hasFieldOrPropertyWithValue("login", LOGIN);

    verifyNoInteractions(ldapUsersProvider);
    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void return_empty_user_when_ldap_not_activated() {
    reset(ldapRealm);
    when(ldapRealm.isLdapAuthActivated()).thenReturn(false);
    underTest = new LdapCredentialsAuthentication(settings.asConfig(), userRegistrar, authenticationEvent, ldapRealm);

    assertThat(underTest.authenticate(new Credentials(LOGIN, PASSWORD), request, BASIC)).isEmpty();
    verifyNoInteractions(authenticationEvent);
  }

  private void executeAuthenticate(@Nullable LdapUserDetails userDetails) {
    mockLdapAuthentication(LOGIN);
    mockLdapUserDetailsRetrieval(LOGIN, userDetails);
    underTest.authenticate(new Credentials(LOGIN, PASSWORD), request, BASIC);
  }

  private void mockLdapAuthentication(String login) {
    LdapAuthenticator.Context authenticationContext = new LdapAuthenticator.Context(login, PASSWORD, request);
    when(ldapAuthenticator.doAuthenticate(refEq(authenticationContext))).thenReturn(success(SERVER_KEY));
  }

  private void mockLdapUserDetailsRetrieval(String login, @Nullable LdapUserDetails userDetails) {
    LdapUsersProvider.Context expectedUserContext = new LdapUsersProvider.Context(SERVER_KEY, login, request);
    when(ldapUsersProvider.doGetUserDetails(refEq(expectedUserContext))).thenReturn(userDetails);
  }

}
