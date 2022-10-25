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
import static org.junit.Assert.fail;

public class LdapRealmTest {

  @ClassRule
  public static LdapServer server = new LdapServer("/users.example.org.ldif");

  @Test
  public void normal() {
    MapSettings settings = new MapSettings()
      .setProperty("ldap.url", server.getUrl());
    LdapRealm realm = new LdapRealm(new LdapSettingsManager(settings.asConfig(), new LdapAutodiscovery()));
    realm.init();
    assertThat(realm.doGetAuthenticator()).isInstanceOf(DefaultLdapAuthenticator.class);
    assertThat(realm.getUsersProvider()).isInstanceOf(LdapUsersProvider.class).isInstanceOf(DefaultLdapUsersProvider.class);
    assertThat(realm.getGroupsProvider()).isNull();
  }

  @Test
  public void noConnection() {
    MapSettings settings = new MapSettings()
      .setProperty("ldap.url", "ldap://no-such-host")
      .setProperty("ldap.group.baseDn", "cn=groups,dc=example,dc=org");
    LdapRealm realm = new LdapRealm(new LdapSettingsManager(settings.asConfig(), new LdapAutodiscovery()));
    try {
      realm.init();
      fail("Since there is no connection, the init method has to throw an exception.");
    } catch (LdapException e) {
      assertThat(e).hasMessage("Unable to open LDAP connection");
    }
    assertThat(realm.doGetAuthenticator()).isInstanceOf(DefaultLdapAuthenticator.class);
    assertThat(realm.getUsersProvider()).isInstanceOf(LdapUsersProvider.class).isInstanceOf(DefaultLdapUsersProvider.class);
    assertThat(realm.getGroupsProvider()).isInstanceOf(LdapGroupsProvider.class).isInstanceOf(DefaultLdapGroupsProvider.class);

    try {
      LdapUsersProvider.Context userContext = new DefaultLdapUsersProvider.Context("tester", Mockito.mock(HttpServletRequest.class));
      realm.getUsersProvider().doGetUserDetails(userContext);
      fail("Since there is no connection, the doGetUserDetails method has to throw an exception.");
    } catch (LdapException e) {
      assertThat(e.getMessage()).contains("Unable to retrieve details for user tester");
    }
    try {
      LdapGroupsProvider.Context groupsContext = new DefaultLdapGroupsProvider.Context("tester", Mockito.mock(HttpServletRequest.class));
      realm.getGroupsProvider().doGetGroups(groupsContext);
      fail("Since there is no connection, the doGetGroups method has to throw an exception.");
    } catch (LdapException e) {
      assertThat(e.getMessage()).contains("Unable to retrieve details for user tester");
    }
  }

}
