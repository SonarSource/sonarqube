/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static org.sonar.auth.ldap.LdapAutodiscovery.LdapSrvRecord;

/**
 * The LdapSettingsManager will parse the settings.
 * This class is also responsible to cope with multiple ldap servers.
 */
@ServerSide
public class LdapSettingsManager {

  private static final Logger LOG = Loggers.get(LdapSettingsManager.class);

  private static final String LDAP_SERVERS_PROPERTY = "ldap.servers";
  private static final String LDAP_PROPERTY_PREFIX = "ldap";
  private static final String DEFAULT_LDAP_SERVER_KEY = "<default>";
  private final Configuration config;
  private final LdapAutodiscovery ldapAutodiscovery;
  private Map<String, LdapUserMapping> userMappings = null;
  private Map<String, LdapGroupMapping> groupMappings = null;
  private Map<String, LdapContextFactory> contextFactories;

  /**
   * Create an instance of the settings manager.
   *
   * @param config The config to use.
   */
  public LdapSettingsManager(Configuration config, LdapAutodiscovery ldapAutodiscovery) {
    this.config = config;
    this.ldapAutodiscovery = ldapAutodiscovery;
  }

  /**
   * Get all the @link{LdapUserMapping}s available in the settings.
   *
   * @return A @link{Map} with all the @link{LdapUserMapping} objects.
   *         The key is the server key used in the settings (ldap for old single server notation).
   */
  public Map<String, LdapUserMapping> getUserMappings() {
    if (userMappings == null) {
      // Use linked hash map to preserve order
      userMappings = new LinkedHashMap<>();
      String[] serverKeys = config.getStringArray(LDAP_SERVERS_PROPERTY);
      if (serverKeys.length > 0) {
        for (String serverKey : serverKeys) {
          LdapUserMapping userMapping = new LdapUserMapping(config, LDAP_PROPERTY_PREFIX + "." + serverKey);
          if (StringUtils.isNotBlank(userMapping.getBaseDn())) {
            LOG.info("User mapping for server {}: {}", serverKey, userMapping);
            userMappings.put(serverKey, userMapping);
          } else {
            LOG.info("Users will not be synchronized for server {}, because property 'ldap.{}.user.baseDn' is empty.", serverKey, serverKey);
          }
        }
      } else {
        // Backward compatibility with single server configuration
        LdapUserMapping userMapping = new LdapUserMapping(config, LDAP_PROPERTY_PREFIX);
        if (StringUtils.isNotBlank(userMapping.getBaseDn())) {
          LOG.info("User mapping: {}", userMapping);
          userMappings.put(DEFAULT_LDAP_SERVER_KEY, userMapping);
        } else {
          LOG.info("Users will not be synchronized, because property 'ldap.user.baseDn' is empty.");
        }
      }
    }
    return userMappings;
  }

  /**
   * Get all the @link{LdapGroupMapping}s available in the settings.
   *
   * @return A @link{Map} with all the @link{LdapGroupMapping} objects.
   *         The key is the server key used in the settings (ldap for old single server notation).
   */
  public Map<String, LdapGroupMapping> getGroupMappings() {
    if (groupMappings == null) {
      // Use linked hash map to preserve order
      groupMappings = new LinkedHashMap<>();
      String[] serverKeys = config.getStringArray(LDAP_SERVERS_PROPERTY);
      if (serverKeys.length > 0) {
        for (String serverKey : serverKeys) {
          LdapGroupMapping groupMapping = new LdapGroupMapping(config, LDAP_PROPERTY_PREFIX + "." + serverKey);
          if (StringUtils.isNotBlank(groupMapping.getBaseDn())) {
            LOG.info("Group mapping for server {}: {}", serverKey, groupMapping);
            groupMappings.put(serverKey, groupMapping);
          } else {
            LOG.info("Groups will not be synchronized for server {}, because property 'ldap.{}.group.baseDn' is empty.", serverKey, serverKey);
          }
        }
      } else {
        // Backward compatibility with single server configuration
        LdapGroupMapping groupMapping = new LdapGroupMapping(config, LDAP_PROPERTY_PREFIX);
        if (StringUtils.isNotBlank(groupMapping.getBaseDn())) {
          LOG.info("Group mapping: {}", groupMapping);
          groupMappings.put(DEFAULT_LDAP_SERVER_KEY, groupMapping);
        } else {
          LOG.info("Groups will not be synchronized, because property 'ldap.group.baseDn' is empty.");
        }
      }
    }
    return groupMappings;
  }

  /**
   * Get all the @link{LdapContextFactory}s available in the settings.
   *
   * @return A @link{Map} with all the @link{LdapContextFactory} objects.
   *        The key is the server key used in the settings (ldap for old single server notation).
   */
  public Map<String, LdapContextFactory> getContextFactories() {
    if (contextFactories == null) {
      // Use linked hash map to preserve order
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
    String realm = config.get(LDAP_PROPERTY_PREFIX + ".realm").orElse(null);
    String ldapUrlKey = LDAP_PROPERTY_PREFIX + ".url";
    String ldapUrl = config.get(ldapUrlKey).orElse(null);
    if (ldapUrl == null && realm != null) {
      LOG.warn("Auto-discovery feature is deprecated, please use '{}' to specify LDAP url", ldapUrlKey);
      List<LdapSrvRecord> ldapServers = ldapAutodiscovery.getLdapServers(realm);
      if (ldapServers.isEmpty()) {
        throw new LdapException(String.format("The property '%s' is empty and SonarQube is not able to auto-discover any LDAP server.", ldapUrlKey));
      }
      int index = 1;
      for (LdapSrvRecord ldapSrvRecord : ldapServers) {
        if (StringUtils.isNotBlank(ldapSrvRecord.getServerUrl())) {
          LOG.info("Detected server: {}", ldapSrvRecord.getServerUrl());
          LdapContextFactory contextFactory = new LdapContextFactory(config, LDAP_PROPERTY_PREFIX, ldapSrvRecord.getServerUrl());
          contextFactories.put(DEFAULT_LDAP_SERVER_KEY + index, contextFactory);
          index++;
        }
      }
    } else {
      if (StringUtils.isBlank(ldapUrl)) {
        throw new LdapException(String.format("The property '%s' is empty and no realm configured to try auto-discovery.", ldapUrlKey));
      }
      LdapContextFactory contextFactory = new LdapContextFactory(config, LDAP_PROPERTY_PREFIX, ldapUrl);
      contextFactories.put(DEFAULT_LDAP_SERVER_KEY, contextFactory);
    }
  }

  private void initMultiLdapConfiguration(String[] serverKeys) {
    if (config.hasKey("ldap.url") || config.hasKey("ldap.realm")) {
      throw new LdapException("When defining multiple LDAP servers with the property '" + LDAP_SERVERS_PROPERTY + "', "
        + "all LDAP properties must be linked to one of those servers. Please remove properties like 'ldap.url', 'ldap.realm', ...");
    }
    for (String serverKey : serverKeys) {
      String prefix = LDAP_PROPERTY_PREFIX + "." + serverKey;
      String ldapUrlKey = prefix + ".url";
      String ldapUrl = config.get(ldapUrlKey).orElse(null);
      if (StringUtils.isBlank(ldapUrl)) {
        throw new LdapException(String.format("The property '%s' property is empty while it is mandatory.", ldapUrlKey));
      }
      LdapContextFactory contextFactory = new LdapContextFactory(config, prefix, ldapUrl);
      contextFactories.put(serverKey, contextFactory);
    }
  }
}
