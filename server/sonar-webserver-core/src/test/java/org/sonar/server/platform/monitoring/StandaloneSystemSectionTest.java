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
package org.sonar.server.platform.monitoring;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.CoreProperties;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.platform.Server;
import org.sonar.api.security.SecurityRealm;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.authentication.IdentityProviderRepositoryRule;
import org.sonar.server.authentication.TestIdentityProvider;
import org.sonar.server.log.ServerLogging;
import org.sonar.server.platform.DockerSupport;
import org.sonar.server.platform.OfficialDistribution;
import org.sonar.server.platform.StatisticsSupport;
import org.sonar.server.user.SecurityRealmFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.SonarEdition.COMMUNITY;
import static org.sonar.api.SonarEdition.DATACENTER;
import static org.sonar.api.SonarEdition.DEVELOPER;
import static org.sonar.api.SonarEdition.ENTERPRISE;
import static org.sonar.process.systeminfo.SystemInfoUtils.attribute;
import static org.sonar.server.platform.monitoring.SystemInfoTesting.assertThatAttributeIs;

@RunWith(DataProviderRunner.class)
public class StandaloneSystemSectionTest {

  @Rule
  public IdentityProviderRepositoryRule identityProviderRepository = new IdentityProviderRepositoryRule();

  private MapSettings settings = new MapSettings();
  private Server server = mock(Server.class);
  private ServerLogging serverLogging = mock(ServerLogging.class);
  private SecurityRealmFactory securityRealmFactory = mock(SecurityRealmFactory.class);
  private OfficialDistribution officialDistribution = mock(OfficialDistribution.class);
  private DockerSupport dockerSupport = mock(DockerSupport.class);
  private StatisticsSupport statisticsSupport = mock(StatisticsSupport.class);

  private SonarRuntime sonarRuntime = mock(SonarRuntime.class);

  private StandaloneSystemSection underTest = new StandaloneSystemSection(settings.asConfig(), securityRealmFactory, identityProviderRepository, server,
    serverLogging, officialDistribution, dockerSupport, statisticsSupport, sonarRuntime);

  @Before
  public void setUp() {
    when(serverLogging.getRootLoggerLevel()).thenReturn(LoggerLevel.DEBUG);
    when(sonarRuntime.getEdition()).thenReturn(COMMUNITY);
  }

  @Test
  public void name_is_not_empty() {
    assertThat(underTest.name()).isNotEmpty();
  }

  @Test
  public void test_getServerId() {
    when(server.getId()).thenReturn("ABC");
    assertThat(underTest.getServerId()).isEqualTo("ABC");
  }

  @Test
  public void official_distribution() {
    when(officialDistribution.check()).thenReturn(true);

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "Official Distribution", true);
  }

  @Test
  public void not_an_official_distribution() {
    when(officialDistribution.check()).thenReturn(false);

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "Official Distribution", false);
  }

  @Test
  public void get_realm() {
    SecurityRealm realm = mock(SecurityRealm.class);
    when(realm.getName()).thenReturn("LDAP");
    when(securityRealmFactory.getRealm()).thenReturn(realm);

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "External User Authentication", "LDAP");
  }

  @Test
  public void no_realm() {
    when(securityRealmFactory.getRealm()).thenReturn(null);

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThat(attribute(protobuf, "External User Authentication")).isNull();
  }

  @Test
  public void get_enabled_identity_providers() {
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
  public void get_enabled_identity_providers_allowing_users_to_signup() {
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
  public void return_nb_of_processors() {
    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThat(attribute(protobuf, "Processors").getLongValue()).isPositive();
  }

  @Test
  public void get_force_authentication_defaults_to_true() {
    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "Force authentication", true);
  }

  @Test
  public void get_force_authentication() {
    settings.setProperty(CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY, false);
    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "Force authentication", false);
  }

  @Test
  public void return_Lines_of_Codes_from_StatisticsSupport(){
    when(statisticsSupport.getLinesOfCode()).thenReturn(17752L);
    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf,"Lines of Code", 17752L);
  }

  @Test
  @UseDataProvider("trueOrFalse")
  public void return_docker_flag_from_DockerSupport(boolean flag) {
    when(dockerSupport.isRunningInDocker()).thenReturn(flag);
    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThat(attribute(protobuf, "Docker").getBooleanValue()).isEqualTo(flag);
  }

  @Test
  @UseDataProvider("editions")
  public void get_edition(SonarEdition sonarEdition, String editionLabel) {
    when(sonarRuntime.getEdition()).thenReturn(sonarEdition);
    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "Edition", editionLabel);
  }

  @DataProvider
  public static Object[][] trueOrFalse() {
    return new Object[][] {
      {true},
      {false}
    };
  }

  @DataProvider
  public static Object[][] editions() {
    return new Object[][] {
      {COMMUNITY, COMMUNITY.getLabel()},
      {DEVELOPER, DEVELOPER.getLabel()},
      {ENTERPRISE, ENTERPRISE.getLabel()},
      {DATACENTER, DATACENTER.getLabel()},
    };
  }
}
