/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.SonarRuntime;
import org.sonar.api.platform.Server;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.platform.DockerSupport;
import org.sonar.server.platform.StatisticsSupport;
import org.sonar.server.platform.monitoring.CommonSystemInformation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.SonarEdition.COMMUNITY;
import static org.sonar.server.platform.monitoring.SystemInfoTesting.assertThatAttributeIs;

@RunWith(DataProviderRunner.class)
public class GlobalSystemSectionTest {

  private final Server server = mock(Server.class);
  private final DockerSupport dockerSupport = mock(DockerSupport.class);
  private final StatisticsSupport statisticsSupport = mock(StatisticsSupport.class);
  private final SonarRuntime sonarRuntime = mock(SonarRuntime.class);
  private final CommonSystemInformation commonSystemInformation = mock(CommonSystemInformation.class);

  private final GlobalSystemSection underTest = new GlobalSystemSection(server, dockerSupport, statisticsSupport, sonarRuntime, commonSystemInformation);

  @Before
  public void setUp() {
    when(sonarRuntime.getEdition()).thenReturn(COMMUNITY);
  }

  @Test
  public void name_is_not_empty() {
    assertThat(underTest.toProtobuf().getName()).isEqualTo("System");
  }

  @Test
  public void toProtobuf_whenExternalUserAuthentication_shouldWriteIt() {
    when(commonSystemInformation.getExternalUserAuthentication()).thenReturn("LDAP");
    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "External User Authentication", "LDAP");
  }

  @Test
  public void toProtobuf_whenNoExternalUserAuthentication_shouldWriteNothing() {
    when(commonSystemInformation.getExternalUserAuthentication()).thenReturn("");

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "External User Authentication", "");
  }

  @Test
  public void toProtobuf_whenEnabledIdentityProviders_shouldWriteThem() {
    when(commonSystemInformation.getEnabledIdentityProviders()).thenReturn(List.of("Bitbucket, GitHub"));

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "Accepted external identity providers", "Bitbucket, GitHub");
  }

  @Test
  public void toProtobuf_whenAllowsToSignUpEnabledIdentityProviders_shouldWriteThem() {
    when(commonSystemInformation.getAllowsToSignUpEnabledIdentityProviders()).thenReturn(List.of("GitHub"));

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "External identity providers whose users are allowed to sign themselves up", "GitHub");
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
  public void get_docker_flag(boolean flag) {
    when(dockerSupport.isRunningInDocker()).thenReturn(flag);

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "Docker", flag);
  }

  @Test
  public void get_edition() {
    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "Edition", COMMUNITY.getLabel());
  }

  @DataProvider
  public static Object[][] trueOrFalse() {
    return new Object[][] {
      {true},
      {false},
    };
  }
}
