/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.log.ServerLogging;
import org.sonar.server.platform.ContainerSupport;
import org.sonar.server.platform.OfficialDistribution;
import org.sonar.server.platform.StatisticsSupport;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.SonarEdition.COMMUNITY;
import static org.sonar.api.SonarEdition.DATACENTER;
import static org.sonar.api.SonarEdition.DEVELOPER;
import static org.sonar.api.SonarEdition.ENTERPRISE;
import static org.sonar.process.systeminfo.SystemInfoUtils.attribute;
import static org.sonar.server.platform.monitoring.SystemInfoTesting.assertThatAttributeDoesNotExist;
import static org.sonar.server.platform.monitoring.SystemInfoTesting.assertThatAttributeIs;

@RunWith(DataProviderRunner.class)
public class StandaloneSystemSectionTest {

  private final MapSettings settings = new MapSettings();
  private final Configuration config = settings.asConfig();
  private final Server server = mock(Server.class);
  private final ServerLogging serverLogging = mock(ServerLogging.class);
  private final OfficialDistribution officialDistribution = mock(OfficialDistribution.class);
  private final ContainerSupport containerSupport = mock(ContainerSupport.class);
  private final StatisticsSupport statisticsSupport = mock(StatisticsSupport.class);
  private final SonarRuntime sonarRuntime = mock(SonarRuntime.class);
  private final CommonSystemInformation commonSystemInformation = mock(CommonSystemInformation.class);

  private final StandaloneSystemSection underTest = new StandaloneSystemSection(config, server, serverLogging,
    officialDistribution, containerSupport, statisticsSupport, sonarRuntime, commonSystemInformation);

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
  public void toProtobuf_whenNoExternalUserAuthentication_shouldWriteNothing() {
    when(commonSystemInformation.getExternalUserAuthentication()).thenReturn(null);

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeDoesNotExist(protobuf, "External User Authentication");
  }

  @Test
  public void toProtobuf_whenExternalUserAuthentication_shouldWriteIt() {
    when(commonSystemInformation.getExternalUserAuthentication()).thenReturn("LDAP");
    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "External User Authentication", "LDAP");
  }

  @Test
  public void toProtobuf_whenNoIdentityProviders_shouldWriteNothing() {
    when(commonSystemInformation.getEnabledIdentityProviders()).thenReturn(emptyList());

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeDoesNotExist(protobuf, "Accepted external identity providers");
  }

  @Test
  public void toProtobuf_whenEnabledIdentityProviders_shouldWriteThem() {
    when(commonSystemInformation.getEnabledIdentityProviders()).thenReturn(List.of("Bitbucket, GitHub"));

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "Accepted external identity providers", "Bitbucket, GitHub");
  }

  @Test
  public void toProtobuf_whenNoAllowsToSignUpEnabledIdentityProviders_shouldWriteNothing() {
    when(commonSystemInformation.getAllowsToSignUpEnabledIdentityProviders()).thenReturn(emptyList());

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeDoesNotExist(protobuf, "External identity providers whose users are allowed to sign themselves up");
  }

  @Test
  public void toProtobuf_whenAllowsToSignUpEnabledIdentityProviders_shouldWriteThem() {
    when(commonSystemInformation.getAllowsToSignUpEnabledIdentityProviders()).thenReturn(List.of("GitHub"));

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "External identity providers whose users are allowed to sign themselves up", "GitHub");
  }

  @Test
  public void return_nb_of_processors() {
    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThat(attribute(protobuf, "Processors").getLongValue()).isPositive();
  }

  @Test
  public void toProtobuf_whenForceAuthentication_returnIt() {
    when(commonSystemInformation.getForceAuthentication()).thenReturn(false);
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
  public void toProtobuf_whenRunningOrNotRunningInContainer_shouldReturnCorrectFlag(boolean flag) {
    when(containerSupport.isRunningInContainer()).thenReturn(flag);
    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThat(attribute(protobuf, "Container").getBooleanValue()).isEqualTo(flag);
  }

  @Test
  @UseDataProvider("editions")
  public void get_edition(SonarEdition sonarEdition, String editionLabel) {
    when(sonarRuntime.getEdition()).thenReturn(sonarEdition);
    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "Edition", editionLabel);
  }

  @Test
  public void toProtobuf_whenInstanceIsNotManaged_shouldWriteNothing() {
    when(commonSystemInformation.getManagedInstanceProviderName()).thenReturn(null);
    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();

    assertThatAttributeDoesNotExist(protobuf, "External Users and Groups Provisioning");
  }

  @Test
  public void toProtobuf_whenInstanceIsManaged_shouldWriteItsProviderName() {
    when(commonSystemInformation.getManagedInstanceProviderName()).thenReturn("Okta");

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "External Users and Groups Provisioning", "Okta");
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
