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
package org.sonar.server.platform.monitoring.cluster;

import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.security.SecurityRealm;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.authentication.IdentityProviderRepositoryRule;
import org.sonar.server.authentication.TestIdentityProvider;
import org.sonar.server.platform.ServerId;
import org.sonar.server.platform.ServerIdLoader;
import org.sonar.server.user.SecurityRealmFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.process.systeminfo.SystemInfoUtils.attribute;
import static org.sonar.server.platform.monitoring.SystemInfoTesting.assertThatAttributeIs;


public class GlobalSystemSectionTest {

  private static final String SERVER_ID_PROPERTY = "Server ID";
  private static final String SERVER_ID_VALIDATED_PROPERTY = "Server ID validated";

  @Rule
  public IdentityProviderRepositoryRule identityProviderRepository = new IdentityProviderRepositoryRule();

  private MapSettings settings = new MapSettings();
  private ServerIdLoader serverIdLoader = mock(ServerIdLoader.class);
  private SecurityRealmFactory securityRealmFactory = mock(SecurityRealmFactory.class);

  private GlobalSystemSection underTest = new GlobalSystemSection(settings.asConfig(),
    serverIdLoader, securityRealmFactory, identityProviderRepository);

  @Before
  public void setUp() throws Exception {
    when(serverIdLoader.getRaw()).thenReturn(Optional.empty());
    when(serverIdLoader.get()).thenReturn(Optional.empty());
  }

  @Test
  public void name_is_not_empty() {
    assertThat(underTest.toProtobuf().getName()).isEqualTo("System");
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
}
