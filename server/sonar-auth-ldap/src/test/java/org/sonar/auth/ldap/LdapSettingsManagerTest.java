/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.Arrays;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.auth.ldap.LdapAutodiscovery.LdapSrvRecord;

public class LdapSettingsManagerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldFailWhenNoLdapUrl() {
    Settings settings = generateMultipleLdapSettingsWithUserAndGroupMapping();
    settings.removeProperty("ldap.example.url");
    LdapSettingsManager settingsManager = new LdapSettingsManager(settings, new LdapAutodiscovery());

    thrown.expect(LdapException.class);
    thrown.expectMessage("The property 'ldap.example.url' property is empty while it is mandatory.");
    settingsManager.getContextFactories();
  }

  @Test
  public void shouldFailWhenMixingSingleAndMultipleConfiguration() {
    Settings settings = generateMultipleLdapSettingsWithUserAndGroupMapping();
    settings.setProperty("ldap.url", "ldap://foo");
    LdapSettingsManager settingsManager = new LdapSettingsManager(settings, new LdapAutodiscovery());

    thrown.expect(LdapException.class);
    thrown
      .expectMessage(
        "When defining multiple LDAP servers with the property 'ldap.servers', all LDAP properties must be linked to one of those servers. Please remove properties like 'ldap.url', 'ldap.realm', ...");
    settingsManager.getContextFactories();
  }

  @Test
  public void testContextFactoriesWithSingleLdap() {
    LdapSettingsManager settingsManager = new LdapSettingsManager(
      generateSingleLdapSettingsWithUserAndGroupMapping(), new LdapAutodiscovery());
    assertThat(settingsManager.getContextFactories().size()).isEqualTo(1);
  }

  /**
   * Test there are 2 @link{org.sonar.plugins.ldap.LdapContextFactory}s found.
   *
   */
  @Test
  public void testContextFactoriesWithMultipleLdap() {
    LdapSettingsManager settingsManager = new LdapSettingsManager(
      generateMultipleLdapSettingsWithUserAndGroupMapping(), new LdapAutodiscovery());
    assertThat(settingsManager.getContextFactories().size()).isEqualTo(2);
    // We do it twice to make sure the settings keep the same.
    assertThat(settingsManager.getContextFactories().size()).isEqualTo(2);
  }

  @Test
  public void testAutodiscover() {
    LdapAutodiscovery ldapAutodiscovery = mock(LdapAutodiscovery.class);
    LdapSrvRecord ldap1 = new LdapSrvRecord("ldap://localhost:189", 1, 1);
    LdapSrvRecord ldap2 = new LdapSrvRecord("ldap://localhost:1899", 1, 1);
    when(ldapAutodiscovery.getLdapServers("example.org")).thenReturn(Arrays.asList(ldap1, ldap2));
    LdapSettingsManager settingsManager = new LdapSettingsManager(
      generateAutodiscoverSettings(), ldapAutodiscovery);
    assertThat(settingsManager.getContextFactories().size()).isEqualTo(2);
  }

  @Test
  public void testAutodiscoverFailed() {
    LdapAutodiscovery ldapAutodiscovery = mock(LdapAutodiscovery.class);
    when(ldapAutodiscovery.getLdapServers("example.org")).thenReturn(Collections.emptyList());
    LdapSettingsManager settingsManager = new LdapSettingsManager(
      generateAutodiscoverSettings(), ldapAutodiscovery);

    thrown.expect(LdapException.class);
    thrown.expectMessage("The property 'ldap.url' is empty and SonarQube is not able to auto-discover any LDAP server.");

    settingsManager.getContextFactories();
  }

  /**
   * Test there are 2 @link{org.sonar.plugins.ldap.LdapUserMapping}s found.
   *
   */
  @Test
  public void testUserMappings() {
    LdapSettingsManager settingsManager = new LdapSettingsManager(
      generateMultipleLdapSettingsWithUserAndGroupMapping(), new LdapAutodiscovery());
    assertThat(settingsManager.getUserMappings().size()).isEqualTo(2);
    // We do it twice to make sure the settings keep the same.
    assertThat(settingsManager.getUserMappings().size()).isEqualTo(2);
  }

  /**
   * Test there are 2 @link{org.sonar.plugins.ldap.LdapGroupMapping}s found.
   *
   */
  @Test
  public void testGroupMappings() {
    LdapSettingsManager settingsManager = new LdapSettingsManager(
      generateMultipleLdapSettingsWithUserAndGroupMapping(), new LdapAutodiscovery());
    assertThat(settingsManager.getGroupMappings().size()).isEqualTo(2);
    // We do it twice to make sure the settings keep the same.
    assertThat(settingsManager.getGroupMappings().size()).isEqualTo(2);
  }

  /**
   * Test what happens when no configuration is set.
   * Normally there will be a contextFactory, but the autodiscovery doesn't work for the test server.
   */
  @Test
  public void testEmptySettings() {
    LdapSettingsManager settingsManager = new LdapSettingsManager(
      new MapSettings(), new LdapAutodiscovery());

    thrown.expect(LdapException.class);
    thrown.expectMessage("The property 'ldap.url' is empty and no realm configured to try auto-discovery.");
    settingsManager.getContextFactories();
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

  private MapSettings generateAutodiscoverSettings() {
    MapSettings settings = new MapSettings();

    settings.setProperty("ldap.realm", "example.org")
      .setProperty("ldap.user.baseDn", "ou=users,dc=example,dc=org")
      .setProperty("ldap.group.baseDn", "ou=groups,dc=example,dc=org")
      .setProperty("ldap.group.request",
        "(&(objectClass=posixGroup)(memberUid={uid}))");

    return settings;
  }

}
