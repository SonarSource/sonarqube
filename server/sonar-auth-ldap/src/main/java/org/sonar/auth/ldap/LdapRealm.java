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

import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonar.auth.ldap.LdapSettingsManager.DEFAULT_LDAP_SERVER_KEY;
import static org.sonar.process.ProcessProperties.Property.SONAR_AUTHENTICATOR_IGNORE_STARTUP_FAILURE;
import static org.sonar.process.ProcessProperties.Property.SONAR_SECURITY_REALM;

/**
 * @author Evgeny Mandrikov
 */
@ServerSide
public class LdapRealm {

  public static final String LDAP_SECURITY_REALM = "LDAP";
  public static final String DEFAULT_LDAP_IDENTITY_PROVIDER_ID = LDAP_SECURITY_REALM + "_" + DEFAULT_LDAP_SERVER_KEY;
  private static final Logger LOG = LoggerFactory.getLogger(LdapRealm.class);

  private final boolean isLdapAuthActivated;
  private final LdapUsersProvider usersProvider;
  private final LdapGroupsProvider groupsProvider;
  private final LdapAuthenticator authenticator;

  public LdapRealm(LdapSettingsManager settingsManager, Configuration configuration) {
    String realmName = configuration.get(SONAR_SECURITY_REALM.getKey()).orElse(null);
    this.isLdapAuthActivated = LDAP_SECURITY_REALM.equals(realmName);
    boolean ignoreStartupFailure = configuration.getBoolean(SONAR_AUTHENTICATOR_IGNORE_STARTUP_FAILURE.getKey()).orElse(false);
    if (!isLdapAuthActivated) {
      this.usersProvider = null;
      this.groupsProvider = null;
      this.authenticator = null;
    } else {
      Map<String, LdapContextFactory> contextFactories = settingsManager.getContextFactories();
      Map<String, LdapUserMapping> userMappings = settingsManager.getUserMappings();
      this.usersProvider = new DefaultLdapUsersProvider(contextFactories, userMappings);
      this.authenticator = new DefaultLdapAuthenticator(contextFactories, userMappings);
      this.groupsProvider = createGroupsProvider(contextFactories, userMappings, settingsManager);
      testConnections(contextFactories, ignoreStartupFailure);
    }
  }

  private static LdapGroupsProvider createGroupsProvider(Map<String, LdapContextFactory> contextFactories, Map<String, LdapUserMapping> userMappings,
    LdapSettingsManager settingsManager) {
    Map<String, LdapGroupMapping> groupMappings = settingsManager.getGroupMappings();
    if (!groupMappings.isEmpty()) {
      return new DefaultLdapGroupsProvider(contextFactories, userMappings, groupMappings);
    } else {
      return null;
    }
  }

  private static void testConnections(Map<String, LdapContextFactory> contextFactories, boolean ignoreStartupFailure) {
    try {
      for (LdapContextFactory contextFactory : contextFactories.values()) {
        contextFactory.testConnection();
      }
    } catch (RuntimeException e) {
      if (ignoreStartupFailure) {
        LOG.error("IGNORED - LDAP realm failed to start: " + e.getMessage());
      } else {
        throw new LdapException("LDAP realm failed to start: " + e.getMessage(), e);
      }
    }
  }

  @CheckForNull
  public LdapAuthenticator getAuthenticator() {
    return authenticator;
  }

  @CheckForNull
  public LdapUsersProvider getUsersProvider() {
    return usersProvider;
  }

  @CheckForNull
  public LdapGroupsProvider getGroupsProvider() {
    return groupsProvider;
  }

  public boolean isLdapAuthActivated() {
    return isLdapAuthActivated;
  }
}
