/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.process.cluster.hz;

import com.hazelcast.config.Config;
import com.hazelcast.config.InterfacesConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessProperties;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;


@Timeout(60)
class HazelcastMemberBuilderTest {

  @CsvSource({
    "::1,[::1]:9003",
    "[::1],[::1]:9003",
    "[::1]:5000,[::1]:5000",
    "127.0.0.1,127.0.0.1:9003",
    "127.0.0.1:5000,127.0.0.1:5000"
  })
  @ParameterizedTest
  void add_default_port_to_member(String memberParam, String memberAddress) {
    try (MockedStatic<Hazelcast> mockedHazelcast = mockStatic(Hazelcast.class)) {
      ArgumentCaptor<Config> configCaptor = forClass(Config.class);
      HazelcastInstance hazelcastInstance = mock(HazelcastInstance.class);
      mockedHazelcast.when(() -> Hazelcast.newHazelcastInstance(configCaptor.capture()))
        .thenReturn(hazelcastInstance);

      int defaultPort = Integer.parseInt(ProcessProperties.Property.CLUSTER_NODE_HZ_PORT.getDefaultValue());
      new HazelcastMemberBuilder(JoinConfigurationType.TCP_IP)
        .setMembers(memberParam)
        .setProcessId(ProcessId.COMPUTE_ENGINE)
        .setNodeName("bar")
        .setPort(defaultPort)
        .setNetworkInterface("127.0.0.1")
        .build();

      Config config = configCaptor.getValue();

      assertThat(config.getNetworkConfig())
        .extracting(NetworkConfig::getPort)
        .isEqualTo(defaultPort);

      assertThat(config.getNetworkConfig())
        .extracting(NetworkConfig::getInterfaces)
        .extracting(InterfacesConfig::getInterfaces, as(InstanceOfAssertFactories.COLLECTION))
        .hasSize(1).element(0)
        .isEqualTo("127.0.0.1");

      assertThat(config.getNetworkConfig())
        .extracting(NetworkConfig::getJoin)
        .extracting(JoinConfig::getTcpIpConfig)
        .extracting(TcpIpConfig::getMembers)
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .hasSize(1).element(0)
        .isEqualTo(memberAddress);
    }
  }

  @Test
  void kubernetes_join_uses_configured_port() {
    try (MockedStatic<Hazelcast> mockedHazelcast = mockStatic(Hazelcast.class)) {
      ArgumentCaptor<Config> configCaptor = forClass(Config.class);
      HazelcastInstance hazelcastInstance = mock(HazelcastInstance.class);
      mockedHazelcast.when(() -> Hazelcast.newHazelcastInstance(configCaptor.capture()))
        .thenReturn(hazelcastInstance);

      int configuredPort = 19003;
      new HazelcastMemberBuilder(JoinConfigurationType.KUBERNETES)
        .setMembers("sonarqube-dce-app.default.svc.cluster.local")
        .setProcessId(ProcessId.APP)
        .setNodeName("app-1")
        .setPort(configuredPort)
        .setNetworkInterface("127.0.0.1")
        .build();

      Config config = configCaptor.getValue();

      assertThat(config.getNetworkConfig())
        .extracting(NetworkConfig::getPort)
        .isEqualTo(configuredPort);

      assertThat(config.getNetworkConfig())
        .extracting(NetworkConfig::getJoin)
        .extracting(JoinConfig::getKubernetesConfig)
        .satisfies(kubernetesConfig -> {
          assertThat(kubernetesConfig.isEnabled()).isTrue();
          assertThat(kubernetesConfig.getProperty("service-dns")).isEqualTo("sonarqube-dce-app.default.svc.cluster.local");
          assertThat(kubernetesConfig.getProperty("service-port")).isEqualTo(String.valueOf(configuredPort));
        });
    }
  }

  @Test
  void kubernetes_join_discovers_peers_on_application_port() {
    try (MockedStatic<Hazelcast> mockedHazelcast = mockStatic(Hazelcast.class)) {
      ArgumentCaptor<Config> configCaptor = forClass(Config.class);
      HazelcastInstance hazelcastInstance = mock(HazelcastInstance.class);
      mockedHazelcast.when(() -> Hazelcast.newHazelcastInstance(configCaptor.capture()))
        .thenReturn(hazelcastInstance);

      int bindPort = 4023;
      int applicationPort = 19003;
      new HazelcastMemberBuilder(JoinConfigurationType.KUBERNETES)
        .setMembers("sonarqube-dce-app.default.svc.cluster.local")
        .setProcessId(ProcessId.WEB_SERVER)
        .setNodeName("web-1")
        .setPort(bindPort)
        .setDiscoveryPort(applicationPort)
        .setNetworkInterface("127.0.0.1")
        .build();

      Config config = configCaptor.getValue();

      assertThat(config.getNetworkConfig())
        .extracting(NetworkConfig::getPort)
        .isEqualTo(bindPort);
      assertThat(config.getNetworkConfig())
        .extracting(NetworkConfig::getJoin)
        .extracting(JoinConfig::getKubernetesConfig)
        .extracting(kubernetesConfig -> kubernetesConfig.getProperty("service-port"))
        .isEqualTo(String.valueOf(applicationPort));
    }
  }

  @Test
  void tcp_ip_join_uses_discovery_port_when_member_has_no_port() {
    try (MockedStatic<Hazelcast> mockedHazelcast = mockStatic(Hazelcast.class)) {
      ArgumentCaptor<Config> configCaptor = forClass(Config.class);
      HazelcastInstance hazelcastInstance = mock(HazelcastInstance.class);
      mockedHazelcast.when(() -> Hazelcast.newHazelcastInstance(configCaptor.capture()))
        .thenReturn(hazelcastInstance);

      new HazelcastMemberBuilder(JoinConfigurationType.TCP_IP)
        .setMembers("127.0.0.1")
        .setProcessId(ProcessId.WEB_SERVER)
        .setNodeName("web-1")
        .setPort(4023)
        .setDiscoveryPort(19003)
        .setNetworkInterface("127.0.0.1")
        .build();

      Config config = configCaptor.getValue();

      assertThat(config.getNetworkConfig())
        .extracting(NetworkConfig::getPort)
        .isEqualTo(4023);
      assertThat(config.getNetworkConfig())
        .extracting(NetworkConfig::getJoin)
        .extracting(JoinConfig::getTcpIpConfig)
        .extracting(TcpIpConfig::getMembers)
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .containsExactly("127.0.0.1:19003");
    }
  }

  @ParameterizedTest
  @EnumSource(value = ProcessId.class, names = {"WEB_SERVER", "COMPUTE_ENGINE"})
  void kubernetes_join_requires_discovery_port_for_child_process(ProcessId processId) {
    HazelcastMemberBuilder builder = new HazelcastMemberBuilder(JoinConfigurationType.KUBERNETES)
      .setMembers("sonarqube-dce-app.default.svc.cluster.local")
      .setProcessId(processId)
      .setNodeName("node-1")
      .setPort(4023)
      .setNetworkInterface("127.0.0.1");

    assertThatThrownBy(builder::build)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Discovery port is required for " + processId.getHumanReadableName() + " members");
  }
}
