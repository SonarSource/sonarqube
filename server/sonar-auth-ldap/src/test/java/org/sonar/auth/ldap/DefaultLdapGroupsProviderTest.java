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
package org.sonar.auth.ldap;

import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.auth.ldap.server.LdapServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DefaultLdapGroupsProviderTest {

  /**
   * A reference to the original ldif file
   */
  public static final String USERS_EXAMPLE_ORG_LDIF = "/users.example.org.ldif";
  /**
   * A reference to an aditional ldif file.
   */
  public static final String USERS_INFOSUPPORT_COM_LDIF = "/users.infosupport.com.ldif";

  @ClassRule
  public static LdapServer exampleServer = new LdapServer(USERS_EXAMPLE_ORG_LDIF);
  @ClassRule
  public static LdapServer infosupportServer = new LdapServer(USERS_INFOSUPPORT_COM_LDIF, "infosupport.com", "dc=infosupport,dc=com");

  @Test
  public void doGetGroups_when_single_server_without_key() {
    MapSettings settings = LdapSettingsFactory.generateSimpleAnonymousAccessSettings(exampleServer, null);

    LdapSettingsManager settingsManager = new LdapSettingsManager(settings.asConfig());
    DefaultLdapGroupsProvider groupsProvider = new DefaultLdapGroupsProvider(settingsManager.getContextFactories(), settingsManager.getUserMappings(),
      settingsManager.getGroupMappings());

    Collection<String> groups = getGroupsForContext(createContextForDefaultServer("tester"), groupsProvider);
    assertThat(groups).containsOnly("sonar-users");

    groups = getGroupsForContext(createContextForDefaultServer("godin"), groupsProvider);
    assertThat(groups).containsOnly("sonar-users", "sonar-developers");

    groups = getGroupsForContext(createContextForDefaultServer("unknown_user"), groupsProvider);
    assertThat(groups).isEmpty();
  }

  @Test
  public void doGetGroups_when_two_ldap_servers() {
    MapSettings settings = LdapSettingsFactory.generateSimpleAnonymousAccessSettings(exampleServer, infosupportServer);

    LdapSettingsManager settingsManager = new LdapSettingsManager(settings.asConfig());
    DefaultLdapGroupsProvider groupsProvider = new DefaultLdapGroupsProvider(settingsManager.getContextFactories(), settingsManager.getUserMappings(),
      settingsManager.getGroupMappings());

    Collection<String> groups = getGroupsForContext(createContextForExampleServer("tester"), groupsProvider);
    assertThat(groups).containsOnly("sonar-users");

    groups = getGroupsForContext(createContextForExampleServer("godin"), groupsProvider);
    assertThat(groups).containsOnly("sonar-users", "sonar-developers");

    groups = getGroupsForContext(createContextForExampleServer("unknown_user"), groupsProvider);
    assertThat(groups).isEmpty();

    groups = getGroupsForContext(createContextForInfoSupportServer("testerInfo"), groupsProvider);
    assertThat(groups).containsOnly("sonar-users");

    groups = getGroupsForContext(createContextForInfoSupportServer("robby"), groupsProvider);
    assertThat(groups).containsOnly("sonar-users", "sonar-developers");
  }

  @Test
  public void doGetGroups_when_two_ldap_servers_with_same_username_resolves_groups_from_right_server() {
    MapSettings settings = LdapSettingsFactory.generateSimpleAnonymousAccessSettings(exampleServer, infosupportServer);

    LdapSettingsManager settingsManager = new LdapSettingsManager(settings.asConfig());
    DefaultLdapGroupsProvider groupsProvider = new DefaultLdapGroupsProvider(settingsManager.getContextFactories(), settingsManager.getUserMappings(),
      settingsManager.getGroupMappings());

    Collection<String> groups = getGroupsForContext(createContextForExampleServer("duplicated"), groupsProvider);
    assertThat(groups).containsOnly("sonar-users");

    groups = getGroupsForContext(createContextForInfoSupportServer("duplicated"), groupsProvider);
    assertThat(groups).containsOnly("sonar-developers");
  }

  @Test
  public void posix() {
    MapSettings settings = LdapSettingsFactory.generateSimpleAnonymousAccessSettings(exampleServer, null);
    settings.setProperty("ldap.group.request", "(&(objectClass=posixGroup)(memberUid={uid}))");
    LdapSettingsManager settingsManager = new LdapSettingsManager(settings.asConfig());
    DefaultLdapGroupsProvider groupsProvider = new DefaultLdapGroupsProvider(settingsManager.getContextFactories(), settingsManager.getUserMappings(),
      settingsManager.getGroupMappings());

    Collection<String> groups = getGroupsForContext(createContextForDefaultServer("godin"), groupsProvider);
    assertThat(groups).containsOnly("linux-users");
  }

  @Test
  public void posixMultipleLdap() {
    MapSettings settings = LdapSettingsFactory.generateSimpleAnonymousAccessSettings(exampleServer, infosupportServer);
    settings.setProperty("ldap.example.group.request", "(&(objectClass=posixGroup)(memberUid={uid}))");
    settings.setProperty("ldap.infosupport.group.request", "(&(objectClass=posixGroup)(memberUid={uid}))");
    LdapSettingsManager settingsManager = new LdapSettingsManager(settings.asConfig());
    DefaultLdapGroupsProvider groupsProvider = new DefaultLdapGroupsProvider(settingsManager.getContextFactories(), settingsManager.getUserMappings(),
      settingsManager.getGroupMappings());

    Collection<String> groups = getGroupsForContext(createContextForExampleServer("godin"), groupsProvider);
    assertThat(groups).containsOnly("linux-users");

    groups = getGroupsForContext(createContextForInfoSupportServer("robby"), groupsProvider);
    assertThat(groups).containsOnly("linux-users");
  }

  private static Collection<String> getGroupsForContext(LdapGroupsProvider.Context context, DefaultLdapGroupsProvider groupsProvider) {
    return groupsProvider.doGetGroups(context);
  }

  @Test
  public void mixed() {
    MapSettings settings = LdapSettingsFactory.generateSimpleAnonymousAccessSettings(exampleServer, infosupportServer);
    settings.setProperty("ldap.example.group.request", "(&(|(objectClass=groupOfUniqueNames)(objectClass=posixGroup))(|(uniqueMember={dn})(memberUid={uid})))");
    LdapSettingsManager settingsManager = new LdapSettingsManager(settings.asConfig());
    DefaultLdapGroupsProvider groupsProvider = new DefaultLdapGroupsProvider(settingsManager.getContextFactories(), settingsManager.getUserMappings(),
      settingsManager.getGroupMappings());

    Collection<String> groups = getGroupsForContext(createContextForExampleServer("godin"), groupsProvider);
    assertThat(groups).containsOnly("sonar-users", "sonar-developers", "linux-users");
  }

  @Test
  public void mixedMultipleLdap() {
    MapSettings settings = LdapSettingsFactory.generateSimpleAnonymousAccessSettings(exampleServer, infosupportServer);
    settings.setProperty("ldap.example.group.request", "(&(|(objectClass=groupOfUniqueNames)(objectClass=posixGroup))(|(uniqueMember={dn})(memberUid={uid})))");
    settings.setProperty("ldap.infosupport.group.request", "(&(|(objectClass=groupOfUniqueNames)(objectClass=posixGroup))(|(uniqueMember={dn})(memberUid={uid})))");
    LdapSettingsManager settingsManager = new LdapSettingsManager(settings.asConfig());
    DefaultLdapGroupsProvider groupsProvider = new DefaultLdapGroupsProvider(settingsManager.getContextFactories(), settingsManager.getUserMappings(),
      settingsManager.getGroupMappings());

    Collection<String> groups = getGroupsForContext(createContextForExampleServer("godin"), groupsProvider);
    assertThat(groups).containsOnly("sonar-users", "sonar-developers", "linux-users");

    groups = getGroupsForContext(createContextForInfoSupportServer("robby"), groupsProvider);
    assertThat(groups).containsOnly("sonar-users", "sonar-developers", "linux-users");
  }

  private static LdapGroupsProvider.Context createContextForDefaultServer(String userName) {
    return createContext("default", userName);
  }

  private static LdapGroupsProvider.Context createContextForExampleServer(String userName) {
    return createContext("example", userName);
  }

  private static LdapGroupsProvider.Context createContextForInfoSupportServer(String userName) {
    return createContext("infosupport", userName);
  }

  private static LdapGroupsProvider.Context createContext(String serverName, String userName) {
    return new LdapGroupsProvider.Context(serverName, userName, mock(HttpServletRequest.class));
  }

}
