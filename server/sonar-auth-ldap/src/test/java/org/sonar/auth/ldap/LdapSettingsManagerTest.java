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
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LdapSettingsManagerTest {
  @Test
  public void shouldFailWhenNoLdapUrl() {
    MapSettings settings = generateMultipleLdapSettingsWithUserAndGroupMapping();
    settings.removeProperty("ldap.example.url");
    LdapSettingsManager settingsManager = new LdapSettingsManager(settings.asConfig());

    assertThatThrownBy(settingsManager::getContextFactories)
      .isInstanceOf(LdapException.class)
      .hasMessage("The property 'ldap.example.url' property is empty while it is mandatory.");
  }

  @Test
  public void shouldFailWhenMixingSingleAndMultipleConfiguration() {
    MapSettings settings = generateMultipleLdapSettingsWithUserAndGroupMapping();
    settings.setProperty("ldap.url", "ldap://foo");
    LdapSettingsManager settingsManager = new LdapSettingsManager(settings.asConfig());

    assertThatThrownBy(settingsManager::getContextFactories)
      .isInstanceOf(LdapException.class)
      .hasMessage("When defining multiple LDAP servers with the property 'ldap.servers', all LDAP properties must be linked to one of those servers. Please remove properties like 'ldap.url', 'ldap.realm', ...");
  }

  @Test
  public void testContextFactoriesWithSingleLdap() {
    LdapSettingsManager settingsManager = new LdapSettingsManager(
      generateSingleLdapSettingsWithUserAndGroupMapping().asConfig());
    assertThat(settingsManager.getContextFactories()).hasSize(1);
  }

  /**
   * Test there are 2 @link{org.sonar.plugins.ldap.LdapContextFactory}s found.
   *
   */
  @Test
  public void testContextFactoriesWithMultipleLdap() {
    LdapSettingsManager settingsManager = new LdapSettingsManager(
      generateMultipleLdapSettingsWithUserAndGroupMapping().asConfig());
    assertThat(settingsManager.getContextFactories()).hasSize(2);
    // We do it twice to make sure the settings keep the same.
    assertThat(settingsManager.getContextFactories()).hasSize(2);
  }

  @Test
  public void getUserMappings_shouldCreateUserMappings_whenMultipleLdapConfig() {
    Configuration configuration = generateMultipleLdapSettingsWithUserAndGroupMapping().asConfig();
    LdapSettingsManager settingsManager = new LdapSettingsManager(configuration);

    Map<String, LdapUserMapping> result = settingsManager.getUserMappings();

    assertThat(result).hasSize(2).containsOnlyKeys("example", "infosupport");
    assertThat(result.get("example")).usingRecursiveComparison().isEqualTo(new LdapUserMapping(configuration, "ldap.example"));
    assertThat(result.get("infosupport")).usingRecursiveComparison().isEqualTo(new LdapUserMapping(configuration, "ldap.infosupport"));
  }

  @Test
  public void getGroupMappings_shouldCreateGroupMappings_whenMultipleLdapConfig() {
    Configuration configuration = generateMultipleLdapSettingsWithUserAndGroupMapping().asConfig();
    LdapSettingsManager settingsManager = new LdapSettingsManager(configuration);

    Map<String, LdapGroupMapping> result = settingsManager.getGroupMappings();

    assertThat(result).hasSize(2).containsOnlyKeys("example", "infosupport");
    assertThat(result.get("example")).usingRecursiveComparison().isEqualTo(new LdapGroupMapping(configuration, "ldap.example"));
    assertThat(result.get("infosupport")).usingRecursiveComparison().isEqualTo(new LdapGroupMapping(configuration, "ldap.infosupport"));
  }

  /**
   * Test what happens when no configuration is set.
   */
  @Test
  public void testEmptySettings() {
    LdapSettingsManager settingsManager = new LdapSettingsManager(
      new MapSettings().asConfig());

    assertThatThrownBy(settingsManager::getContextFactories)
      .isInstanceOf(LdapException.class)
      .hasMessage("The property 'ldap.url' property is empty while it is mandatory.");
  }

  @Test
  public void getUserMappings_shouldCreateUserMappings_whenSingleLdapConfig() {
    Configuration configuration = generateSingleLdapSettingsWithUserAndGroupMapping().asConfig();
    LdapSettingsManager settingsManager = new LdapSettingsManager(configuration);

    Map<String, LdapUserMapping> result = settingsManager.getUserMappings();

    assertThat(result).hasSize(1).containsOnlyKeys("default");
    assertThat(result.get("default")).usingRecursiveComparison().isEqualTo(new LdapUserMapping(configuration, "ldap"));
  }

  @Test
  public void getGroupMappings_shouldCreateGroupMappings_whenSingleLdapConfig() {
    Configuration configuration = generateSingleLdapSettingsWithUserAndGroupMapping().asConfig();
    LdapSettingsManager settingsManager = new LdapSettingsManager(configuration);

    Map<String, LdapGroupMapping> result = settingsManager.getGroupMappings();

    assertThat(result).hasSize(1).containsOnlyKeys("default");
    assertThat(result.get("default")).usingRecursiveComparison().isEqualTo(new LdapGroupMapping(configuration, "ldap"));
  }

  private MapSettings generateMultipleLdapSettingsWithUserAndGroupMapping() {
    MapSettings settings = new MapSettings();

    settings.setProperty("ldap.servers", "example,infosupport");

    settings.setProperty("ldap.example.url", "/users.example.org.ldif")
      .setProperty("ldap.example.user.baseDn", "ou=users,dc=example,dc=org")
      .setProperty("ldap.example.group.baseDn", "ou=groups,dc=example,dc=org")
      .setProperty("ldap.example.group.request",
        "(&(objectClass=posixGroup)(memberUid={uid}))");

    settings.setProperty("ldap.infosupport.url", "/users.infosupport.com.ldif")
      .setProperty("ldap.infosupport.user.baseDn",
        "ou=users,dc=infosupport,dc=com")
      .setProperty("ldap.infosupport.group.baseDn",
        "ou=groups,dc=infosupport,dc=com")
      .setProperty("ldap.infosupport.group.request",
        "(&(objectClass=posixGroup)(memberUid={uid}))");

    return settings;
  }

  private MapSettings generateSingleLdapSettingsWithUserAndGroupMapping() {
    MapSettings settings = new MapSettings();

    settings.setProperty("ldap.url", "/users.example.org.ldif")
      .setProperty("ldap.user.baseDn", "ou=users,dc=example,dc=org")
      .setProperty("ldap.group.baseDn", "ou=groups,dc=example,dc=org")
      .setProperty("ldap.group.request",
        "(&(objectClass=posixGroup)(memberUid={uid}))");

    return settings;
  }

}
