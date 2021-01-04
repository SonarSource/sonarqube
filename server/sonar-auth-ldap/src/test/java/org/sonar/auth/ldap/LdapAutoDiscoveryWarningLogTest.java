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

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.auth.ldap.server.ApacheDS;
import org.sonar.auth.ldap.server.LdapServer;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LdapAutoDiscoveryWarningLogTest {

  @Rule
  public LogTester logTester = new LogTester();

  @ClassRule
  public static LdapServer server = new LdapServer("/users.example.org.ldif");

  @Test
  public void does_not_display_log_when_not_using_auto_discovery() {
    MapSettings settings = new MapSettings()
      .setProperty("ldap.url", server.getUrl());
    LdapRealm realm = new LdapRealm(new LdapSettingsManager(settings.asConfig(), new LdapAutodiscovery()));
    assertThat(realm.getName()).isEqualTo("LDAP");

    realm.init();

    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
  }

  @Test
  public void display_warning_log_when_using_auto_discovery_to_detect_server_url() {
    LdapAutodiscovery ldapAutodiscovery = mock(LdapAutodiscovery.class);
    when(ldapAutodiscovery.getLdapServers("example.org")).thenReturn(singletonList(new LdapAutodiscovery.LdapSrvRecord(server.getUrl(), 1, 1)));
    // ldap.url setting is not set
    LdapRealm realm = new LdapRealm(new LdapSettingsManager(new MapSettings().setProperty("ldap.realm", "example.org").asConfig(),
      ldapAutodiscovery));

    realm.init();

    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Auto-discovery feature is deprecated, please use 'ldap.url' to specify LDAP url");
  }

  @Test
  public void display_warning_log_when_using_auto_discovery_to_detect_user_baseDn_on_single_server() {
    // ldap.user.baseDn setting is not set
    MapSettings settings = new MapSettings().setProperty("ldap.url", server.getUrl()).setProperty("ldap.realm", "example.org");
    LdapRealm realm = new LdapRealm(new LdapSettingsManager(settings.asConfig(), new LdapAutodiscovery()));

    realm.init();

    assertThat(logTester.logs(LoggerLevel.WARN)).containsOnly("Auto-discovery feature is deprecated, please use 'ldap.user.baseDn' to specify user search dn");
  }

  @Test
  public void display_warning_log_when_using_auto_discovery_to_detect_user_baseDn_on_multiple_servers() throws Exception {
    ApacheDS server2 = ApacheDS.start("example.org", "dc=example,dc=org", "target/ldap-work2/");
    server2.importLdif(LdapAutoDiscoveryWarningLogTest.class.getResourceAsStream("/users.example.org.ldif"));
    MapSettings settings = new MapSettings()
      .setProperty("ldap.servers", "example,infosupport")
      // ldap.XXX.user.baseDn settings are not set on both servers
      .setProperty("ldap.example.url", server.getUrl())
      .setProperty("ldap.example.realm", "example.org")
      .setProperty("ldap.infosupport.url", server2.getUrl())
      .setProperty("ldap.infosupport.realm", "infosupport.org");
    LdapRealm realm = new LdapRealm(new LdapSettingsManager(settings.asConfig(), new LdapAutodiscovery()));

    realm.init();

    assertThat(logTester.logs(LoggerLevel.WARN)).containsOnly(
      "Auto-discovery feature is deprecated, please use 'ldap.example.user.baseDn' to specify user search dn",
      "Auto-discovery feature is deprecated, please use 'ldap.infosupport.user.baseDn' to specify user search dn");
  }

}
