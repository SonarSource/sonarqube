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
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.auth.ldap.server.LdapServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DefaultLdapUsersProviderIT {
  /**
   * A reference to the original ldif file
   */
  public static final String USERS_EXAMPLE_ORG_LDIF = "/users.example.org.ldif";
  /**
   * A reference to an additional ldif file.
   */
  public static final String USERS_INFOSUPPORT_COM_LDIF = "/users.infosupport.com.ldif";

  @ClassRule
  public static LdapServer exampleServer = new LdapServer(USERS_EXAMPLE_ORG_LDIF);
  @ClassRule
  public static LdapServer infosupportServer = new LdapServer(USERS_INFOSUPPORT_COM_LDIF, "infosupport.com", "dc=infosupport,dc=com");

  @Test
  public void test_user_from_first_server() {
    MapSettings settings = LdapSettingsFactory.generateSimpleAnonymousAccessSettings(exampleServer, infosupportServer);
    LdapSettingsManager settingsManager = new LdapSettingsManager(settings.asConfig());
    DefaultLdapUsersProvider usersProvider = new DefaultLdapUsersProvider(settingsManager.getContextFactories(), settingsManager.getUserMappings());

    LdapUserDetails details = usersProvider.doGetUserDetails(createContext("example", "godin"));
    assertThat(details.getName()).isEqualTo("Evgeny Mandrikov");
    assertThat(details.getEmail()).isEqualTo("godin@example.org");
  }

  @Test
  public void test_user_from_second_server() {
    MapSettings settings = LdapSettingsFactory.generateSimpleAnonymousAccessSettings(exampleServer, infosupportServer);
    LdapSettingsManager settingsManager = new LdapSettingsManager(settings.asConfig());
    DefaultLdapUsersProvider usersProvider = new DefaultLdapUsersProvider(settingsManager.getContextFactories(), settingsManager.getUserMappings());

    LdapUserDetails details = usersProvider.doGetUserDetails(createContext("infosupport", "robby"));
    assertThat(details.getName()).isEqualTo("Robby Developer");
    assertThat(details.getEmail()).isEqualTo("rd@infosupport.com");

  }

  @Test
  public void test_user_on_multiple_servers() {
    MapSettings settings = LdapSettingsFactory.generateSimpleAnonymousAccessSettings(exampleServer, infosupportServer);
    LdapSettingsManager settingsManager = new LdapSettingsManager(settings.asConfig());
    DefaultLdapUsersProvider usersProvider = new DefaultLdapUsersProvider(settingsManager.getContextFactories(), settingsManager.getUserMappings());

    LdapUserDetails detailsExample = usersProvider.doGetUserDetails(createContext("example", "tester"));
    assertThat(detailsExample.getName()).isEqualTo("Tester Testerovich");
    assertThat(detailsExample.getEmail()).isEqualTo("tester@example.org");

    LdapUserDetails detailsInfoSupport = usersProvider.doGetUserDetails(createContext("infosupport", "tester"));
    assertThat(detailsInfoSupport.getName()).isEqualTo("Tester Testerovich Testerov");
    assertThat(detailsInfoSupport.getEmail()).isEqualTo("tester@example2.org");
  }

  @Test
  public void test_user_doesnt_exist() {
    MapSettings settings = LdapSettingsFactory.generateSimpleAnonymousAccessSettings(exampleServer, infosupportServer);
    LdapSettingsManager settingsManager = new LdapSettingsManager(settings.asConfig());
    DefaultLdapUsersProvider usersProvider = new DefaultLdapUsersProvider(settingsManager.getContextFactories(), settingsManager.getUserMappings());

    LdapUserDetails details = usersProvider.doGetUserDetails(createContext("example", "notfound"));
    assertThat(details).isNull();
  }

  private static LdapUsersProvider.Context createContext(String serverKey, String username) {
    return new LdapUsersProvider.Context(serverKey, username, mock(HttpRequest.class));
  }
}
