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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.Settings;
import org.sonar.api.security.Authenticator;
import org.sonar.api.security.ExternalGroupsProvider;
import org.sonar.api.security.ExternalUsersProvider;
import org.sonar.api.security.SecurityRealm;
import org.sonar.api.security.UserDetails;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.user.SecurityRealmFactory;

public class RealmAuthenticatorTest {

  @Rule
  public ExpectedException expectedException = none();

  static final String LOGIN = "LOGIN";
  static final String PASSWORD = "PASSWORD";

  ArgumentCaptor<UserIdentity> userIdentityArgumentCaptor = ArgumentCaptor.forClass(UserIdentity.class);
  ArgumentCaptor<IdentityProvider> identityProviderArgumentCaptor = ArgumentCaptor.forClass(IdentityProvider.class);

  Settings settings = new Settings();

  SecurityRealmFactory securityRealmFactory = mock(SecurityRealmFactory.class);
  SecurityRealm realm = mock(SecurityRealm.class);
  Authenticator authenticator = mock(Authenticator.class);
  ExternalUsersProvider externalUsersProvider = mock(ExternalUsersProvider.class);
  ExternalGroupsProvider externalGroupsProvider = mock(ExternalGroupsProvider.class);

  UserIdentityAuthenticator userIdentityAuthenticator = mock(UserIdentityAuthenticator.class);

  HttpServletRequest request = mock(HttpServletRequest.class);
  HttpServletResponse response = mock(HttpServletResponse.class);

  RealmAuthenticator underTest = new RealmAuthenticator(settings, securityRealmFactory, userIdentityAuthenticator);

  @Test
  public void authenticate() throws Exception {
    executeStartWithoutGroupSync();
    when(authenticator.doAuthenticate(any(Authenticator.Context.class))).thenReturn(true);
    UserDetails userDetails = new UserDetails();
    userDetails.setName("name");
    userDetails.setEmail("email");
    when(externalUsersProvider.doGetUserDetails(any(ExternalUsersProvider.Context.class))).thenReturn(userDetails);

    underTest.authenticate(LOGIN, PASSWORD, request);

    verify(userIdentityAuthenticator).authenticate(userIdentityArgumentCaptor.capture(), identityProviderArgumentCaptor.capture());
    UserIdentity userIdentity = userIdentityArgumentCaptor.getValue();
    assertThat(userIdentity.getLogin()).isEqualTo(LOGIN);
    assertThat(userIdentity.getProviderLogin()).isEqualTo(LOGIN);
    assertThat(userIdentity.getName()).isEqualTo("name");
    assertThat(userIdentity.getEmail()).isEqualTo("email");
    assertThat(userIdentity.shouldSyncGroups()).isFalse();
  }

  @Test
  public void authenticate_with_sonarqube_identity_provider() throws Exception {
    executeStartWithoutGroupSync();
    when(authenticator.doAuthenticate(any(Authenticator.Context.class))).thenReturn(true);
    UserDetails userDetails = new UserDetails();
    userDetails.setName("name");
    userDetails.setEmail("email");
    when(externalUsersProvider.doGetUserDetails(any(ExternalUsersProvider.Context.class))).thenReturn(userDetails);

    underTest.authenticate(LOGIN, PASSWORD, request);

    verify(userIdentityAuthenticator).authenticate(userIdentityArgumentCaptor.capture(), identityProviderArgumentCaptor.capture());

    assertThat(identityProviderArgumentCaptor.getValue().getKey()).isEqualTo("sonarqube");
    assertThat(identityProviderArgumentCaptor.getValue().getName()).isEqualTo("sonarqube");
    assertThat(identityProviderArgumentCaptor.getValue().getDisplay()).isNull();
    assertThat(identityProviderArgumentCaptor.getValue().isEnabled()).isTrue();
  }

  @Test
  public void authenticate_with_group_sync() throws Exception {
    when(externalGroupsProvider.doGetGroups(any(ExternalGroupsProvider.Context.class))).thenReturn(asList("group1", "group2"));
    executeStartWithGroupSync();
    executeAuthenticate();

    verify(userIdentityAuthenticator).authenticate(userIdentityArgumentCaptor.capture(), identityProviderArgumentCaptor.capture());

    UserIdentity userIdentity = userIdentityArgumentCaptor.getValue();
    assertThat(userIdentity.shouldSyncGroups()).isTrue();
    assertThat(userIdentity.getGroups()).containsOnly("group1", "group2");
  }

  @Test
  public void use_login_if_user_details_contains_no_name() throws Exception {
    executeStartWithoutGroupSync();
    when(authenticator.doAuthenticate(any(Authenticator.Context.class))).thenReturn(true);
    UserDetails userDetails = new UserDetails();
    userDetails.setName(null);
    when(externalUsersProvider.doGetUserDetails(any(ExternalUsersProvider.Context.class))).thenReturn(userDetails);

    underTest.authenticate(LOGIN, PASSWORD, request);

    verify(userIdentityAuthenticator).authenticate(userIdentityArgumentCaptor.capture(), identityProviderArgumentCaptor.capture());
    assertThat(userIdentityArgumentCaptor.getValue().getName()).isEqualTo(LOGIN);
  }

  @Test
  public void allow_to_sign_up_property() throws Exception {
    settings.setProperty("sonar.authenticator.createUsers", true);
    executeStartWithoutGroupSync();
    executeAuthenticate();

    verify(userIdentityAuthenticator).authenticate(userIdentityArgumentCaptor.capture(), identityProviderArgumentCaptor.capture());
    assertThat(identityProviderArgumentCaptor.getValue().allowsUsersToSignUp()).isTrue();
  }

  @Test
  public void does_not_allow_to_sign_up_property() throws Exception {
    settings.setProperty("sonar.authenticator.createUsers", false);
    executeStartWithoutGroupSync();
    executeAuthenticate();

    verify(userIdentityAuthenticator).authenticate(userIdentityArgumentCaptor.capture(), identityProviderArgumentCaptor.capture());
    assertThat(identityProviderArgumentCaptor.getValue().allowsUsersToSignUp()).isFalse();
  }

  @Test
  public void use_downcase_login() throws Exception {
    settings.setProperty("sonar.authenticator.downcase", true);
    executeStartWithoutGroupSync();
    executeAuthenticate("LOGIN");

    verify(userIdentityAuthenticator).authenticate(userIdentityArgumentCaptor.capture(), identityProviderArgumentCaptor.capture());
    UserIdentity userIdentity = userIdentityArgumentCaptor.getValue();
    assertThat(userIdentity.getLogin()).isEqualTo("login");
    assertThat(userIdentity.getProviderLogin()).isEqualTo("login");
  }

  @Test
  public void does_not_user_downcase_login() throws Exception {
    settings.setProperty("sonar.authenticator.downcase", false);
    executeStartWithoutGroupSync();
    executeAuthenticate("LoGiN");

    verify(userIdentityAuthenticator).authenticate(userIdentityArgumentCaptor.capture(), identityProviderArgumentCaptor.capture());
    UserIdentity userIdentity = userIdentityArgumentCaptor.getValue();
    assertThat(userIdentity.getLogin()).isEqualTo("LoGiN");
    assertThat(userIdentity.getProviderLogin()).isEqualTo("LoGiN");
  }

  @Test
  public void fail_to_authenticate_when_name_is_blank() throws Exception {
    executeStartWithoutGroupSync();
    when(authenticator.doAuthenticate(any(Authenticator.Context.class))).thenReturn(true);
    UserDetails userDetails = new UserDetails();
    userDetails.setName("");
    userDetails.setEmail("email");
    when(externalUsersProvider.doGetUserDetails(any(ExternalUsersProvider.Context.class))).thenReturn(userDetails);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("User name must not be blank");
    underTest.authenticate(LOGIN, PASSWORD, request);
  }

  @Test
  public void fail_to_authenticate_when_user_details_are_null() throws Exception {
    executeStartWithoutGroupSync();
    when(authenticator.doAuthenticate(any(Authenticator.Context.class))).thenReturn(true);

    when(externalUsersProvider.doGetUserDetails(any(ExternalUsersProvider.Context.class))).thenReturn(null);

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("No user details");
    underTest.authenticate(LOGIN, PASSWORD, request);
  }

  @Test
  public void fail_to_authenticate_when_external_authentication_fails() throws Exception {
    executeStartWithoutGroupSync();
    when(externalUsersProvider.doGetUserDetails(any(ExternalUsersProvider.Context.class))).thenReturn(new UserDetails());

    when(authenticator.doAuthenticate(any(Authenticator.Context.class))).thenReturn(false);

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Fail to authenticate from external provider");
    underTest.authenticate(LOGIN, PASSWORD, request);
  }

  @Test
  public void fail_to_authenticate_when_not_started() throws Exception {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("No realm available");
    underTest.authenticate(LOGIN, PASSWORD, request);
  }

  @Test
  public void fail_to_start_when_no_authenticator() throws Exception {
    when(realm.doGetAuthenticator()).thenReturn(null);
    when(securityRealmFactory.getRealm()).thenReturn(realm);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("No authenticator available");
    underTest.start();
  }

  @Test
  public void fail_to_start_when_no_user_provider() throws Exception {
    when(realm.doGetAuthenticator()).thenReturn(authenticator);
    when(realm.getUsersProvider()).thenReturn(null);
    when(securityRealmFactory.getRealm()).thenReturn(realm);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("No users provider available");
    underTest.start();
  }

  @Test
  public void external_authentication_is_activated_when_realm_exists() throws Exception {
    executeStartWithoutGroupSync();

    assertThat(underTest.isExternalAuthenticationUsed()).isTrue();
  }

  @Test
  public void external_authentication_is_disabled_when_no_realm() throws Exception {
    when(securityRealmFactory.getRealm()).thenReturn(null);
    underTest.start();

    assertThat(underTest.isExternalAuthenticationUsed()).isFalse();
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
    underTest.authenticate(login, PASSWORD, request);
  }

}
