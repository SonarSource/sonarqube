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

import java.util.Map;
import org.sonar.api.server.ServerSide;

/**
 * @author Evgeny Mandrikov
 */
@ServerSide
public class LdapRealm {

  private LdapUsersProvider usersProvider;
  private LdapGroupsProvider groupsProvider;
  private LdapAuthenticator authenticator;
  private final LdapSettingsManager settingsManager;

  public LdapRealm(LdapSettingsManager settingsManager) {
    this.settingsManager = settingsManager;
  }

  /**
   * Initializes LDAP realm and tests connection.
   *
   * @throws LdapException if a NamingException was thrown during test
   */
  public void init() {
    Map<String, LdapContextFactory> contextFactories = settingsManager.getContextFactories();
    Map<String, LdapUserMapping> userMappings = settingsManager.getUserMappings();
    usersProvider = new DefaultLdapUsersProvider(contextFactories, userMappings);
    authenticator = new DefaultLdapAuthenticator(contextFactories, userMappings);
    Map<String, LdapGroupMapping> groupMappings = settingsManager.getGroupMappings();
    if (!groupMappings.isEmpty()) {
      groupsProvider = new DefaultLdapGroupsProvider(contextFactories, userMappings, groupMappings);
    }
    for (LdapContextFactory contextFactory : contextFactories.values()) {
      contextFactory.testConnection();
    }
  }

  public LdapAuthenticator doGetAuthenticator() {
    return authenticator;
  }

  public LdapUsersProvider getUsersProvider() {
    return usersProvider;
  }

  public LdapGroupsProvider getGroupsProvider() {
    return groupsProvider;
  }

}
