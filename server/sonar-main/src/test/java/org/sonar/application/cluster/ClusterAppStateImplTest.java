/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.application.cluster;

import java.net.InetAddress;
import java.util.Optional;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonar.application.AppStateListener;
import org.sonar.application.config.TestAppSettings;
import org.sonar.application.es.EsConnector;
import org.sonar.process.MessageException;
import org.sonar.process.NetworkUtilsImpl;
import org.sonar.process.ProcessId;
import org.sonar.process.cluster.hz.HazelcastMember;
import org.sonar.process.cluster.hz.HazelcastMemberBuilder;
import org.sonar.process.cluster.hz.JoinConfigurationType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.process.cluster.hz.HazelcastObjects.CLUSTER_NAME;
import static org.sonar.process.cluster.hz.HazelcastObjects.SONARQUBE_VERSION;

public class ClusterAppStateImplTest {

  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60));

  @Test
  public void tryToLockWebLeader_returns_true_only_for_the_first_call() {
    try (ClusterAppStateImpl underTest = new ClusterAppStateImpl(new TestAppSettings(), newHzMember(),
      mock(EsConnector.class), mock(AppNodesClusterHostsConsistency.class))) {
      assertThat(underTest.tryToLockWebLeader()).isTrue();
      assertThat(underTest.tryToLockWebLeader()).isFalse();
    }
  }

  @Test
  public void test_listeners() {
    AppStateListener listener = mock(AppStateListener.class);
    try (ClusterAppStateImpl underTest = createClusterAppState()) {
      underTest.addListener(listener);

      underTest.setOperational(ProcessId.ELASTICSEARCH);
      verify(listener, timeout(20_000)).onAppStateOperational(ProcessId.ELASTICSEARCH);

      assertThat(underTest.isOperational(ProcessId.ELASTICSEARCH, true)).isTrue();
      assertThat(underTest.isOperational(ProcessId.APP, true)).isFalse();
      assertThat(underTest.isOperational(ProcessId.WEB_SERVER, true)).isFalse();
      assertThat(underTest.isOperational(ProcessId.COMPUTE_ENGINE, true)).isFalse();
    }
  }

  @Test
  public void tryToReleaseWebLeaderLock_shouldReleaseLock() {
    try (ClusterAppStateImpl underTest = createClusterAppState()) {
      underTest.tryToLockWebLeader();
      assertThat(underTest.getLeaderHostName()).isPresent();

      underTest.tryToReleaseWebLeaderLock();
      assertThat(underTest.getLeaderHostName()).isEmpty();
    }
  }

  @Test
  public void check_if_elasticsearch_is_operational_on_cluster() {
    AppStateListener listener = mock(AppStateListener.class);
    EsConnector esConnectorMock = mock(EsConnector.class);
    when(esConnectorMock.getClusterHealthStatus())
      .thenReturn(Optional.empty())
      .thenReturn(Optional.of(ClusterHealthStatus.RED))
      .thenReturn(Optional.of(ClusterHealthStatus.GREEN));
    try (ClusterAppStateImpl underTest = createClusterAppState(esConnectorMock)) {
      underTest.addListener(listener);

      underTest.isOperational(ProcessId.ELASTICSEARCH, false);

      //wait until undergoing thread marks ES as operational
      verify(listener, timeout(20_000)).onAppStateOperational(ProcessId.ELASTICSEARCH);
    }
  }

  @Test
  public void constructor_checks_appNodesClusterHostsConsistency() {
    AppNodesClusterHostsConsistency clusterHostsConsistency = mock(AppNodesClusterHostsConsistency.class);
    try (ClusterAppStateImpl underTest = new ClusterAppStateImpl(new TestAppSettings(), newHzMember(),
      mock(EsConnector.class), clusterHostsConsistency)) {
      verify(clusterHostsConsistency).check();
    }
  }

  @Test
  public void registerSonarQubeVersion_publishes_version_on_first_call() {

    try (ClusterAppStateImpl underTest = createClusterAppState()) {
      underTest.registerSonarQubeVersion("6.4.1.5");

      assertThat(underTest.getHazelcastMember().getAtomicReference(SONARQUBE_VERSION).get())
        .isEqualTo("6.4.1.5");
    }
  }

  @Test
  public void registerClusterName_publishes_clusterName_on_first_call() {
    try (ClusterAppStateImpl underTest = createClusterAppState()) {
      underTest.registerClusterName("foo");

      assertThat(underTest.getHazelcastMember().getAtomicReference(CLUSTER_NAME).get())
        .isEqualTo("foo");
    }
  }

  @Test
  public void reset_always_throws_ISE() {
    try (ClusterAppStateImpl underTest = createClusterAppState()) {
      assertThatThrownBy(underTest::reset)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("state reset is not supported in cluster mode");
    }
  }

  @Test
  public void registerSonarQubeVersion_throws_ISE_if_initial_version_is_different() {
    // Now launch an instance that try to be part of the hzInstance cluster
    try (ClusterAppStateImpl underTest = createClusterAppState()) {
      // Register first version
      underTest.getHazelcastMember().getAtomicReference(SONARQUBE_VERSION).set("6.6.0.1111");

      // Registering a second different version must trigger an exception
      assertThatThrownBy(() -> underTest.registerSonarQubeVersion("6.7.0.9999"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("The local version 6.7.0.9999 is not the same as the cluster 6.6.0.1111");
    }
  }

  @Test
  public void registerClusterName_throws_MessageException_if_clusterName_is_different() {
    try (ClusterAppStateImpl underTest = createClusterAppState()) {
      // Register first version
      underTest.getHazelcastMember().getAtomicReference(CLUSTER_NAME).set("goodClusterName");

      // Registering a second different cluster name must trigger an exception
      assertThatThrownBy(() -> underTest.registerClusterName("badClusterName"))
        .isInstanceOf(MessageException.class)
        .hasMessage("This node has a cluster name [badClusterName], which does not match [goodClusterName] from the cluster");
    }
  }

  @Test
  public void return_hostname_if_node_is_leader() {
    try (ClusterAppStateImpl underTest = createClusterAppState()) {
      underTest.tryToLockWebLeader();
      Optional<String> hostname = underTest.getLeaderHostName();
      assertThat(hostname).isNotEmpty();
    }
  }

  @Test
  public void return_null_if_node_is_not_leader() {
    try (ClusterAppStateImpl underTest = createClusterAppState()) {
      Optional<String> hostname = underTest.getLeaderHostName();
      assertThat(hostname).isEmpty();
    }
  }

  private ClusterAppStateImpl createClusterAppState() {
    return createClusterAppState(mock(EsConnector.class));
  }

  private ClusterAppStateImpl createClusterAppState(EsConnector esConnector) {
    return new ClusterAppStateImpl(new TestAppSettings(), newHzMember(), esConnector, mock(AppNodesClusterHostsConsistency.class));
  }

  private static HazelcastMember newHzMember() {
    // use loopback for support of offline builds
    InetAddress loopback = InetAddress.getLoopbackAddress();

    return new HazelcastMemberBuilder(JoinConfigurationType.TCP_IP)
      .setProcessId(ProcessId.COMPUTE_ENGINE)
      .setNodeName("bar")
      .setPort(NetworkUtilsImpl.INSTANCE.getNextLoopbackAvailablePort())
      .setMembers(loopback.getHostAddress())
      .setNetworkInterface(loopback.getHostAddress())
      .build();
  }
}
