/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.platform.monitoring;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.platform.Server;
import org.sonar.api.security.SecurityRealm;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.authentication.IdentityProviderRepositoryRule;
import org.sonar.server.authentication.TestIdentityProvider;
import org.sonar.server.platform.ServerId;
import org.sonar.server.platform.ServerIdLoader;
import org.sonar.server.platform.ServerLogging;
import org.sonar.server.user.SecurityRealmFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SonarQubeMonitorTest {

  private static final String SERVER_ID_PROPERTY = "Server ID";
  private static final String SERVER_ID_VALIDATED_PROPERTY = "Server ID validated";

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public IdentityProviderRepositoryRule identityProviderRepository = new IdentityProviderRepositoryRule();

  MapSettings settings = new MapSettings();
  Server server = mock(Server.class);
  ServerIdLoader serverIdLoader = mock(ServerIdLoader.class);
  ServerLogging serverLogging = mock(ServerLogging.class);
  SecurityRealmFactory securityRealmFactory = mock(SecurityRealmFactory.class);

  SonarQubeMonitor underTest = new SonarQubeMonitor(settings.asConfig(), securityRealmFactory, identityProviderRepository, server,
    serverLogging, serverIdLoader);

  @Before
  public void setUp() throws Exception {
    when(serverLogging.getRootLoggerLevel()).thenReturn(LoggerLevel.DEBUG);
    when(serverIdLoader.getRaw()).thenReturn(Optional.empty());
    when(serverIdLoader.get()).thenReturn(Optional.empty());
  }

  @Test
  public void name_is_not_empty() {
    assertThat(underTest.name()).isNotEmpty();
  }

  @Test
  public void test_getServerId() {
    when(serverIdLoader.getRaw()).thenReturn(Optional.of("ABC"));
    assertThat(underTest.getServerId()).isEqualTo("ABC");

    when(serverIdLoader.getRaw()).thenReturn(Optional.<String>empty());
    assertThat(underTest.getServerId()).isNull();
  }

  @Test
  public void attributes_contain_information_about_valid_server_id() {
    when(serverIdLoader.get()).thenReturn(Optional.of(new ServerId("ABC", true)));

    Map<String, Object> attributes = underTest.attributes();
    assertThat(attributes).contains(entry(SERVER_ID_PROPERTY, "ABC"), entry(SERVER_ID_VALIDATED_PROPERTY, true));
  }

  @Test
  public void attributes_contain_information_about_non_valid_server_id() {
    when(serverIdLoader.get()).thenReturn(Optional.of(new ServerId("ABC", false)));

    Map<String, Object> attributes = underTest.attributes();
    assertThat(attributes).contains(entry(SERVER_ID_PROPERTY, "ABC"), entry(SERVER_ID_VALIDATED_PROPERTY, false));
  }

  @Test
  public void attributes_do_not_contain_information_about_server_id_if_absent() {
    when(serverIdLoader.get()).thenReturn(Optional.<ServerId>empty());

    Map<String, Object> attributes = underTest.attributes();
    assertThat(attributes).doesNotContainKeys(SERVER_ID_PROPERTY, SERVER_ID_VALIDATED_PROPERTY);
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
    assertThat(attributes).containsEntry("Accepted external identity providers", "Bitbucket, GitHub");
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
    assertThat(attributes).containsEntry("External identity providers whose users are allowed to sign themselves up", "GitHub");
  }
}
