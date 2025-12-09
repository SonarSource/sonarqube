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
package org.sonar.process.cluster.hz;

import com.hazelcast.config.Config;
import com.hazelcast.config.InterfacesConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessProperties;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
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
      mockedHazelcast.when(() -> Hazelcast.newHazelcastInstance(configCaptor.capture()))
        .thenReturn(mock(HazelcastInstance.class));

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
}
