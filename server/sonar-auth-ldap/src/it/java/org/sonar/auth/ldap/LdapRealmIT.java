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
package org.sonar.auth.ldap;

import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.auth.ldap.server.LdapServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.process.ProcessProperties.Property.SONAR_AUTHENTICATOR_IGNORE_STARTUP_FAILURE;
import static org.sonar.process.ProcessProperties.Property.SONAR_SECURITY_REALM;

public class LdapRealmIT {

  @ClassRule
  public static LdapServer server = new LdapServer("/users.example.org.ldif");

  @Test
  public void normal() {
    MapSettings settings = new MapSettings()
      .setProperty("ldap.url", server.getUrl())
      .setProperty("ldap.user.baseDn", "cn=users")
      .setProperty(SONAR_SECURITY_REALM.getKey(), LdapRealm.LDAP_SECURITY_REALM);

    LdapRealm realm = new LdapRealm(new LdapSettingsManager(settings.asConfig()), settings.asConfig());
    assertThat(realm.getAuthenticator()).isInstanceOf(DefaultLdapAuthenticator.class);
    assertThat(realm.getUsersProvider()).isInstanceOf(LdapUsersProvider.class).isInstanceOf(DefaultLdapUsersProvider.class);
    assertThat(realm.getGroupsProvider()).isNull();
  }

  @Test
  public void noConnection() {
    MapSettings settings = new MapSettings()
      .setProperty("ldap.url", "ldap://no-such-host")
      .setProperty("ldap.group.baseDn", "cn=groups,dc=example,dc=org")
      .setProperty("ldap.user.baseDn", "cn=users,dc=example,dc=org")
      .setProperty(SONAR_SECURITY_REALM.getKey(), LdapRealm.LDAP_SECURITY_REALM);
    Configuration config = settings.asConfig();
    LdapSettingsManager settingsManager = new LdapSettingsManager(config);
    assertThatThrownBy(() -> new LdapRealm(settingsManager, config)).isInstanceOf(LdapException.class)
      .hasMessage("LDAP realm failed to start: Unable to open LDAP connection");
  }

  @Test
  public void noConnection_ignore_ignoreStartupFailure_is_false() {
    MapSettings settings = new MapSettings()
      .setProperty("ldap.url", "ldap://no-such-host")
      .setProperty("ldap.group.baseDn", "cn=groups,dc=example,dc=org")
      .setProperty("ldap.user.baseDn", "cn=users,dc=example,dc=org")
      .setProperty(SONAR_SECURITY_REALM.getKey(), LdapRealm.LDAP_SECURITY_REALM)
      .setProperty(SONAR_AUTHENTICATOR_IGNORE_STARTUP_FAILURE.getKey(), false);
    ;
    Configuration config = settings.asConfig();
    LdapSettingsManager settingsManager = new LdapSettingsManager(config);
    assertThatThrownBy(() -> new LdapRealm(settingsManager, config)).isInstanceOf(LdapException.class)
      .hasMessage("LDAP realm failed to start: Unable to open LDAP connection");
  }

  @Test
  public void noConnection_ignore_ignoreStartupFailure_is_true() {
    MapSettings settings = new MapSettings()
      .setProperty("ldap.url", "ldap://no-such-host")
      .setProperty("ldap.group.baseDn", "cn=groups,dc=example,dc=org")
      .setProperty("ldap.user.baseDn", "cn=users,dc=example,dc=org")
      .setProperty(SONAR_SECURITY_REALM.getKey(), LdapRealm.LDAP_SECURITY_REALM)
      .setProperty(SONAR_AUTHENTICATOR_IGNORE_STARTUP_FAILURE.getKey(), true);

    LdapRealm realm = new LdapRealm(new LdapSettingsManager(settings.asConfig()), settings.asConfig());
    verifyRealm(realm);
  }

  @Test
  public void should_not_activate_ldap_if_realm_is_not_set() {
    MapSettings settings = new MapSettings();

    LdapRealm realm = new LdapRealm(new LdapSettingsManager(settings.asConfig()), settings.asConfig());
    verifyDeactivatedRealm(realm);
  }

  @Test
  public void should_not_activate_ldap_if_realm_is_not_ldap() {
    MapSettings settings = new MapSettings()
      .setProperty(SONAR_SECURITY_REALM.getKey(), "not_ldap");

    LdapRealm realm = new LdapRealm(new LdapSettingsManager(settings.asConfig()), settings.asConfig());
    verifyDeactivatedRealm(realm);
  }

  private static void verifyRealm(LdapRealm realm) {
    assertThat(realm.getAuthenticator()).isInstanceOf(DefaultLdapAuthenticator.class);

    LdapUsersProvider usersProvider = realm.getUsersProvider();
    assertThat(usersProvider).isInstanceOf(LdapUsersProvider.class).isInstanceOf(DefaultLdapUsersProvider.class);

    LdapGroupsProvider groupsProvider = realm.getGroupsProvider();
    assertThat(groupsProvider).isInstanceOf(LdapGroupsProvider.class).isInstanceOf(DefaultLdapGroupsProvider.class);

    LdapUsersProvider.Context userContext = new DefaultLdapUsersProvider.Context("<default>", "tester", Mockito.mock(HttpRequest.class));
    assertThatThrownBy(() -> usersProvider.doGetUserDetails(userContext))
      .isInstanceOf(LdapException.class)
      .hasMessage("Unable to retrieve details for user tester and server key <default>: No user mapping found.");

    LdapGroupsProvider.Context groupsContext = new DefaultLdapGroupsProvider.Context("default", "tester", Mockito.mock(HttpRequest.class));
    assertThatThrownBy(() -> groupsProvider.doGetGroups(groupsContext))
      .isInstanceOf(LdapException.class)
      .hasMessage("Unable to retrieve groups for user tester in server with key <default>");

    assertThat(realm.isLdapAuthActivated()).isTrue();
  }

  private static void verifyDeactivatedRealm(LdapRealm realm) {
    assertThat(realm.getAuthenticator()).isNull();
    assertThat(realm.getUsersProvider()).isNull();
    assertThat(realm.getGroupsProvider()).isNull();
    assertThat(realm.isLdapAuthActivated()).isFalse();

  }

}
