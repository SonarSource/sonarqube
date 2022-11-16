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
package org.sonar.auth.ldap;

import javax.servlet.http.HttpServletRequest;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.auth.ldap.server.LdapServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LdapRealmTest {

  @ClassRule
  public static LdapServer server = new LdapServer("/users.example.org.ldif");

  @Test
  public void normal() {
    MapSettings settings = new MapSettings()
      .setProperty("ldap.url", server.getUrl())
      .setProperty("ldap.user.baseDn", "cn=users");
    LdapRealm realm = new LdapRealm(new LdapSettingsManager(settings.asConfig()));
    realm.init();
    assertThat(realm.doGetAuthenticator()).isInstanceOf(DefaultLdapAuthenticator.class);
    assertThat(realm.getUsersProvider()).isInstanceOf(LdapUsersProvider.class).isInstanceOf(DefaultLdapUsersProvider.class);
    assertThat(realm.getGroupsProvider()).isNull();
  }

  @Test
  public void noConnection() {
    MapSettings settings = new MapSettings()
      .setProperty("ldap.url", "ldap://no-such-host")
      .setProperty("ldap.group.baseDn", "cn=groups,dc=example,dc=org")
      .setProperty("ldap.user.baseDn", "cn=users,dc=example,dc=org");
    LdapRealm realm = new LdapRealm(new LdapSettingsManager(settings.asConfig()));
    assertThatThrownBy(realm::init).isInstanceOf(LdapException.class).hasMessage("Unable to open LDAP connection");

    assertThat(realm.doGetAuthenticator()).isInstanceOf(DefaultLdapAuthenticator.class);

    LdapUsersProvider usersProvider = realm.getUsersProvider();
    assertThat(usersProvider).isInstanceOf(LdapUsersProvider.class).isInstanceOf(DefaultLdapUsersProvider.class);

    LdapGroupsProvider groupsProvider = realm.getGroupsProvider();
    assertThat(groupsProvider).isInstanceOf(LdapGroupsProvider.class).isInstanceOf(DefaultLdapGroupsProvider.class);

    LdapUsersProvider.Context userContext = new DefaultLdapUsersProvider.Context("<default>", "tester", Mockito.mock(HttpServletRequest.class));
    assertThatThrownBy(() -> usersProvider.doGetUserDetails(userContext))
      .isInstanceOf(LdapException.class)
      .hasMessage("Unable to retrieve details for user tester and server key <default>: No user mapping found.");

    LdapGroupsProvider.Context groupsContext = new DefaultLdapGroupsProvider.Context("default", "tester", Mockito.mock(HttpServletRequest.class));
    assertThatThrownBy(() -> groupsProvider.doGetGroups(groupsContext))
      .isInstanceOf(LdapException.class)
      .hasMessage("Unable to retrieve groups for user tester in server with key <default>");

  }

}
