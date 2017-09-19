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
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.authentication.IdentityProviderRepositoryRule;
import org.sonar.server.authentication.TestIdentityProvider;
import org.sonar.server.health.Health;
import org.sonar.server.health.TestStandaloneHealthChecker;
import org.sonar.server.platform.ServerId;
import org.sonar.server.platform.ServerIdLoader;
import org.sonar.server.platform.ServerLogging;
import org.sonar.server.user.SecurityRealmFactory;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.process.systeminfo.SystemInfoUtils.attribute;
import static org.sonar.server.platform.monitoring.SystemInfoTesting.assertThatAttributeIs;

public class SystemSectionTest {

  private static final String SERVER_ID_PROPERTY = "Server ID";
  private static final String SERVER_ID_VALIDATED_PROPERTY = "Server ID validated";

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public IdentityProviderRepositoryRule identityProviderRepository = new IdentityProviderRepositoryRule();

  private MapSettings settings = new MapSettings();
  private Server server = mock(Server.class);
  private ServerIdLoader serverIdLoader = mock(ServerIdLoader.class);
  private ServerLogging serverLogging = mock(ServerLogging.class);
  private SecurityRealmFactory securityRealmFactory = mock(SecurityRealmFactory.class);
  private TestStandaloneHealthChecker healthChecker = new TestStandaloneHealthChecker();

  private SystemSection underTest = new SystemSection(settings.asConfig(), securityRealmFactory, identityProviderRepository, server,
    serverLogging, serverIdLoader, healthChecker);

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

    when(serverIdLoader.getRaw()).thenReturn(Optional.empty());
    assertThat(underTest.getServerId()).isNull();
  }

  @Test
  public void attributes_contain_information_about_valid_server_id() {
    when(serverIdLoader.get()).thenReturn(Optional.of(new ServerId("ABC", true)));

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, SERVER_ID_PROPERTY, "ABC");
    assertThatAttributeIs(protobuf, SERVER_ID_VALIDATED_PROPERTY, true);
  }

  @Test
  public void attributes_contain_information_about_non_valid_server_id() {
    when(serverIdLoader.get()).thenReturn(Optional.of(new ServerId("ABC", false)));

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, SERVER_ID_PROPERTY, "ABC");
    assertThatAttributeIs(protobuf, SERVER_ID_VALIDATED_PROPERTY, false);
  }

  @Test
  public void attributes_do_not_contain_information_about_server_id_if_absent() {
    when(serverIdLoader.get()).thenReturn(Optional.empty());

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThat(attribute(protobuf, SERVER_ID_PROPERTY)).isNull();
    assertThat(attribute(protobuf, SERVER_ID_VALIDATED_PROPERTY)).isNull();
  }

  @Test
  public void official_distribution() throws Exception {
    File rootDir = temp.newFolder();
    FileUtils.write(new File(rootDir, SystemSection.BRANDING_FILE_PATH), "1.2");

    when(server.getRootDir()).thenReturn(rootDir);

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "Official Distribution", true);
  }

  @Test
  public void not_an_official_distribution() throws Exception {
    File rootDir = temp.newFolder();
    // branding file is missing
    when(server.getRootDir()).thenReturn(rootDir);

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "Official Distribution", false);
  }

  @Test
  public void get_log_level() throws Exception {
    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "Logs Level", "DEBUG");
  }

  @Test
  public void get_realm() throws Exception {
    SecurityRealm realm = mock(SecurityRealm.class);
    when(realm.getName()).thenReturn("LDAP");
    when(securityRealmFactory.getRealm()).thenReturn(realm);

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "External User Authentication", "LDAP");
  }

  @Test
  public void no_realm() throws Exception {
    when(securityRealmFactory.getRealm()).thenReturn(null);

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThat(attribute(protobuf, "External User Authentication")).isNull();
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

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "Accepted external identity providers", "Bitbucket, GitHub");
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

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "External identity providers whose users are allowed to sign themselves up", "GitHub");
  }

  @Test
  public void return_health() {
    healthChecker.setHealth(Health.newHealthCheckBuilder()
      .setStatus(Health.Status.YELLOW)
      .addCause("foo")
      .addCause("bar")
      .build());

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "Health", "YELLOW");
    SystemInfoTesting.assertThatAttributeHasOnlyValues(protobuf, "Health Causes", asList("foo", "bar"));
  }
}
