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

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;

/**
 * The LdapSettingsManager will parse the settings.
 * This class is also responsible to cope with multiple ldap servers.
 */
@ServerSide
public class LdapSettingsManager {

  public static final String DEFAULT_LDAP_SERVER_KEY = "default";
  private static final Logger LOG = LoggerFactory.getLogger(LdapSettingsManager.class);

  private static final String LDAP_SERVERS_PROPERTY = "ldap.servers";
  private static final String LDAP_PROPERTY_PREFIX = "ldap";
  protected static final String MANDATORY_LDAP_PROPERTY_ERROR = "The property '%s' property is empty while it is mandatory.";
  private final Configuration config;
  private Map<String, LdapUserMapping> userMappings = null;
  private Map<String, LdapGroupMapping> groupMappings = null;
  private Map<String, LdapContextFactory> contextFactories;

  /**
   * Create an instance of the settings manager.
   *
   * @param config The config to use.
   */
  public LdapSettingsManager(Configuration config) {
    this.config = config;
  }

  /**
   * Get all the @link{LdapUserMapping}s available in the settings.
   *
   * @return A @link{Map} with all the @link{LdapUserMapping} objects.
   *         The key is the server key used in the settings (ldap for old single server notation).
   */
  public Map<String, LdapUserMapping> getUserMappings() {
    if (userMappings == null) {
      createUserMappings();
    }
    return userMappings;
  }

  private void createUserMappings() {
    userMappings = new LinkedHashMap<>();
    String[] serverKeys = config.getStringArray(LDAP_SERVERS_PROPERTY);
    if (serverKeys.length > 0) {
      createUserMappingsForMultipleLdapConfig(serverKeys);
    } else {
      createUserMappingsForSingleLdapConfig();
    }
  }

  private void createUserMappingsForMultipleLdapConfig(String[] serverKeys) {
    for (String serverKey : serverKeys) {
      LdapUserMapping userMapping = new LdapUserMapping(config, LDAP_PROPERTY_PREFIX + "." + serverKey);
      if (StringUtils.isNotBlank(userMapping.getBaseDn())) {
        LOG.info("User mapping for server {}: {}", serverKey, userMapping);
        userMappings.put(serverKey, userMapping);
      } else {
        LOG.info("Users will not be synchronized for server {}, because property 'ldap.{}.user.baseDn' is empty.", serverKey, serverKey);
      }
    }
  }

  private void createUserMappingsForSingleLdapConfig() {
    LdapUserMapping userMapping = new LdapUserMapping(config, LDAP_PROPERTY_PREFIX);
    if (StringUtils.isNotBlank(userMapping.getBaseDn())) {
      LOG.info("User mapping: {}", userMapping);
      userMappings.put(DEFAULT_LDAP_SERVER_KEY, userMapping);
    } else {
      LOG.info("Users will not be synchronized, because property 'ldap.user.baseDn' is empty.");
    }
  }

  /**
   * Get all the @link{LdapGroupMapping}s available in the settings.
   *
   * @return A @link{Map} with all the @link{LdapGroupMapping} objects.
   *         The key is the server key used in the settings (ldap for old single server notation).
   */
  public Map<String, LdapGroupMapping> getGroupMappings() {
    if (groupMappings == null) {
      createGroupMappings();
    }
    return groupMappings;
  }

  private void createGroupMappings() {
    groupMappings = new LinkedHashMap<>();
    String[] serverKeys = config.getStringArray(LDAP_SERVERS_PROPERTY);
    if (serverKeys.length > 0) {
      createGroupMappingsForMultipleLdapConfig(serverKeys);
    } else {
      createGroupMappingsForSingleLdapConfig();
    }
  }

  private void createGroupMappingsForMultipleLdapConfig(String[] serverKeys) {
    for (String serverKey : serverKeys) {
      LdapGroupMapping groupMapping = new LdapGroupMapping(config, LDAP_PROPERTY_PREFIX + "." + serverKey);
      if (StringUtils.isNotBlank(groupMapping.getBaseDn())) {
        LOG.info("Group mapping for server {}: {}", serverKey, groupMapping);
        groupMappings.put(serverKey, groupMapping);
      } else {
        LOG.info("Groups will not be synchronized for server {}, because property 'ldap.{}.group.baseDn' is empty.", serverKey, serverKey);
      }
    }
  }

  private void createGroupMappingsForSingleLdapConfig() {
    LdapGroupMapping groupMapping = new LdapGroupMapping(config, LDAP_PROPERTY_PREFIX);
    if (StringUtils.isNotBlank(groupMapping.getBaseDn())) {
      LOG.info("Group mapping: {}", groupMapping);
      groupMappings.put(DEFAULT_LDAP_SERVER_KEY, groupMapping);
    } else {
      LOG.info("Groups will not be synchronized, because property 'ldap.group.baseDn' is empty.");
    }
  }

  /**
   * Get all the @link{LdapContextFactory}s available in the settings.
   *
   * @return A @link{Map} with all the @link{LdapContextFactory} objects.
   *        The key is the server key used in the settings (ldap for old single server notation).
   */
  public Map<String, LdapContextFactory> getContextFactories() {
    if (contextFactories == null) {
      contextFactories = new LinkedHashMap<>();
      String[] serverKeys = config.getStringArray(LDAP_SERVERS_PROPERTY);
      if (serverKeys.length > 0) {
        initMultiLdapConfiguration(serverKeys);
      } else {
        initSimpleLdapConfiguration();
      }
    }
    return contextFactories;
  }

  private void initSimpleLdapConfiguration() {
    LdapContextFactory contextFactory = initLdapContextFactory(LDAP_PROPERTY_PREFIX);
    contextFactories.put(DEFAULT_LDAP_SERVER_KEY, contextFactory);
  }

  private void initMultiLdapConfiguration(String[] serverKeys) {
    if (config.hasKey("ldap.url") || config.hasKey("ldap.realm")) {
      throw new LdapException("When defining multiple LDAP servers with the property '" + LDAP_SERVERS_PROPERTY + "', "
        + "all LDAP properties must be linked to one of those servers. Please remove properties like 'ldap.url', 'ldap.realm', ...");
    }
    for (String serverKey : serverKeys) {
      LdapContextFactory contextFactory = initLdapContextFactory(LDAP_PROPERTY_PREFIX + "." + serverKey);
      contextFactories.put(serverKey, contextFactory);
    }
  }

  private LdapContextFactory initLdapContextFactory(String prefix) {
    String ldapUrlKey = prefix + ".url";
    String ldapUrl = config.get(ldapUrlKey).orElse(null);
    if (StringUtils.isBlank(ldapUrl)) {
      throw new LdapException(String.format(MANDATORY_LDAP_PROPERTY_ERROR, ldapUrlKey));
    }
    return new LdapContextFactory(config, prefix, ldapUrl);
  }

}
