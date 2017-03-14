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
package org.sonar.application.cluster;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ReplicatedMap;
import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.mockito.Matchers;
import org.sonar.application.AppStateListener;
import org.sonar.application.config.TestAppSettings;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessProperties;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.sonar.application.cluster.AppStateClusterImpl.OPERATIONAL_PROCESSES;
import static org.sonar.application.cluster.AppStateClusterImpl.SONARQUBE_VERSION;
import static org.sonar.application.cluster.HazelcastTestHelper.createHazelcastClient;
import static org.sonar.application.config.SonarQubeVersionHelper.getSonarqubeVersion;

public class AppStateClusterImplTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public TestRule safeGuard = new DisableOnDebug(Timeout.seconds(20));

  @Test
  public void instantiation_throws_ISE_if_cluster_mode_is_disabled() throws Exception {
    TestAppSettings settings = new TestAppSettings();
    settings.set(ProcessProperties.CLUSTER_ENABLED, "false");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Cluster is not enabled on this instance");

    new AppStateClusterImpl(settings);
  }

  @Test
  public void tryToLockWebLeader_returns_true_only_for_the_first_call() throws Exception {
    TestAppSettings settings = newClusterSettings();

    try (AppStateClusterImpl underTest = new AppStateClusterImpl(settings)) {
      assertThat(underTest.tryToLockWebLeader()).isEqualTo(true);
      assertThat(underTest.tryToLockWebLeader()).isEqualTo(false);
    }
  }

  @Test
  public void test_listeners() throws InterruptedException {
    AppStateListener listener = mock(AppStateListener.class);
    try (AppStateClusterImpl underTest = new AppStateClusterImpl(newClusterSettings())) {
      underTest.addListener(listener);

      underTest.setOperational(ProcessId.ELASTICSEARCH);
      verify(listener, timeout(20_000)).onAppStateOperational(ProcessId.ELASTICSEARCH);

      assertThat(underTest.isOperational(ProcessId.ELASTICSEARCH, true)).isEqualTo(true);
      assertThat(underTest.isOperational(ProcessId.APP, true)).isEqualTo(false);
      assertThat(underTest.isOperational(ProcessId.WEB_SERVER, true)).isEqualTo(false);
      assertThat(underTest.isOperational(ProcessId.COMPUTE_ENGINE, true)).isEqualTo(false);
    }
  }

  @Test
  public void simulate_network_cluster() throws InterruptedException {
    TestAppSettings settings = newClusterSettings();
    settings.set(ProcessProperties.CLUSTER_NETWORK_INTERFACES, InetAddress.getLoopbackAddress().getHostAddress());
    AppStateListener listener = mock(AppStateListener.class);

    try (AppStateClusterImpl appStateCluster = new AppStateClusterImpl(settings)) {
      appStateCluster.addListener(listener);

      HazelcastInstance hzInstance = createHazelcastClient(appStateCluster);
      String uuid = UUID.randomUUID().toString();
      ReplicatedMap<ClusterProcess, Boolean> replicatedMap = hzInstance.getReplicatedMap(OPERATIONAL_PROCESSES);
      // process is not up yet --> no events are sent to listeners
      replicatedMap.put(
        new ClusterProcess(uuid, ProcessId.ELASTICSEARCH),
        Boolean.FALSE);

      // process is up yet --> notify listeners
      replicatedMap.replace(
        new ClusterProcess(uuid, ProcessId.ELASTICSEARCH),
        Boolean.TRUE);

      // should be called only once
      verify(listener, timeout(20_000)).onAppStateOperational(ProcessId.ELASTICSEARCH);
      verifyNoMoreInteractions(listener);

      hzInstance.shutdown();
    }
  }

  @Test
  public void appstateclusterimpl_must_set_sonarqube_version() {
    TestAppSettings settings = newClusterSettings();

    try (AppStateClusterImpl appStateCluster = new AppStateClusterImpl(settings)) {
      HazelcastInstance hzInstance = createHazelcastClient(appStateCluster);
      assertThat(hzInstance.getAtomicReference(SONARQUBE_VERSION).get())
        .isNotNull()
        .isInstanceOf(String.class)
        .isEqualTo(getSonarqubeVersion());
    }
  }

 @Test
  public void incorrect_sonarqube_version_must_trigger_an_exception() throws IOException, InterruptedException, IllegalAccessException, NoSuchFieldException {
    // Now launch an instance that try to be part of the hzInstance cluster
    TestAppSettings settings = new TestAppSettings();
    settings.set(ProcessProperties.CLUSTER_ENABLED, "true");
    settings.set(ProcessProperties.CLUSTER_NAME, "sonarqube");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(Matchers.matches("The local version .* is not the same as the cluster 1\\.0\\.0"));

    try (AppStateClusterImpl appStateCluster = new AppStateClusterImpl(settings)) {
      appStateCluster.registerSonarQubeVersion("1.0.0");
    }
  }

  private static TestAppSettings newClusterSettings() {
    TestAppSettings settings = new TestAppSettings();
    settings.set(ProcessProperties.CLUSTER_ENABLED, "true");
    settings.set(ProcessProperties.CLUSTER_NAME, "sonarqube");
    return settings;
  }
}
