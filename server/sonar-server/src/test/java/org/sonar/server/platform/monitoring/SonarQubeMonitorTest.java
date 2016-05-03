/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.platform.monitoring;

import java.io.File;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;
import org.sonar.api.security.SecurityRealm;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.authentication.IdentityProviderRepositoryRule;
import org.sonar.server.authentication.TestIdentityProvider;
import org.sonar.server.platform.ServerLogging;
import org.sonar.server.user.SecurityRealmFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SonarQubeMonitorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public IdentityProviderRepositoryRule identityProviderRepository = new IdentityProviderRepositoryRule();

  Settings settings = new Settings();
  Server server = mock(Server.class);
  ServerLogging serverLogging = mock(ServerLogging.class);
  SecurityRealmFactory securityRealmFactory = mock(SecurityRealmFactory.class);

  SonarQubeMonitor underTest = new SonarQubeMonitor(settings, securityRealmFactory, identityProviderRepository, server, serverLogging);

  @Before
  public void setUp() throws Exception {
    when(serverLogging.getRootLoggerLevel()).thenReturn(LoggerLevel.DEBUG);
  }

  @Test
  public void name_is_not_empty() {
    assertThat(underTest.name()).isNotEmpty();
  }

  @Test
  public void getServerId() {
    when(server.getStartedAt()).thenReturn(DateUtils.parseDate("2015-01-01"));

    Map<String, Object> attributes = underTest.attributes();
    assertThat(attributes).containsKeys("Server ID", "Version");
  }

  @Test
  public void official_distribution() throws Exception {
    File rootDir = temp.newFolder();
    FileUtils.write(new File(rootDir, SonarQubeMonitor.BRANDING_FILE_PATH), "1.2");

    when(server.getRootDir()).thenReturn(rootDir);

    Map<String, Object> attributes = underTest.attributes();
    assertThat(attributes).containsEntry("Official Distribution", Boolean.TRUE);
  }

  @Test
  public void not_an_official_distribution() throws Exception {
    File rootDir = temp.newFolder();
    // branding file is missing
    when(server.getRootDir()).thenReturn(rootDir);

    Map<String, Object> attributes = underTest.attributes();
    assertThat(attributes).containsEntry("Official Distribution", Boolean.FALSE);
  }

  @Test
  public void get_log_level() throws Exception {
    Map<String, Object> attributes = underTest.attributes();
    assertThat(attributes).containsEntry("Logs Level", "DEBUG");
  }

  @Test
  public void get_realm() throws Exception {
    SecurityRealm realm = mock(SecurityRealm.class);
    when(realm.getName()).thenReturn("LDAP");
    when(securityRealmFactory.getRealm()).thenReturn(realm);

    Map<String, Object> attributes = underTest.attributes();
    assertThat(attributes).containsEntry("External User Authentication", "LDAP");
  }

  @Test
  public void no_realm() throws Exception {
    when(securityRealmFactory.getRealm()).thenReturn(null);

    Map<String, Object> attributes = underTest.attributes();
    assertThat(attributes).doesNotContainKey("External User Authentication");
  }

  @Test
  public void get_enabled_identity_providers() throws Exception {
    identityProviderRepository.addIdentityProvider(new TestIdentityProvider()
      .setKey("github")
      .setName("GitHub")
      .setEnabled(true));
    identityProviderRepository.addIdentityProvider(new TestIdentityProvider()
      .setKey("bitbucket")
      .setName("Bitbucket")
      .setEnabled(true));
    identityProviderRepository.addIdentityProvider(new TestIdentityProvider()
      .setKey("disabled")
      .setName("Disabled")
      .setEnabled(false));

    Map<String, Object> attributes = underTest.attributes();
    assertThat(attributes).containsEntry("Identity Providers", "Bitbucket, GitHub");
  }

  @Test
  public void get_enabled_identity_providers_allowing_users_to_signup() throws Exception {
    identityProviderRepository.addIdentityProvider(new TestIdentityProvider()
      .setKey("github")
      .setName("GitHub")
      .setEnabled(true)
      .setAllowsUsersToSignUp(true));
    identityProviderRepository.addIdentityProvider(new TestIdentityProvider()
      .setKey("bitbucket")
      .setName("Bitbucket")
      .setEnabled(true)
      .setAllowsUsersToSignUp(false));
    identityProviderRepository.addIdentityProvider(new TestIdentityProvider()
      .setKey("disabled")
      .setName("Disabled")
      .setEnabled(false)
      .setAllowsUsersToSignUp(true));

    Map<String, Object> attributes = underTest.attributes();
    assertThat(attributes).containsEntry("Identity Providers allowing users to signup", "GitHub");
  }
}
