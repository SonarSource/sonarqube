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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemListener;
import com.hazelcast.core.ReplicatedMap;
import java.net.InetAddress;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.slf4j.LoggerFactory;
import org.sonar.application.AppStateListener;
import org.sonar.application.config.TestAppSettings;
import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessProperties;
import org.sonar.process.cluster.ClusterObjectKeys;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.sonar.application.cluster.HazelcastTestHelper.closeAllHazelcastClients;
import static org.sonar.application.cluster.HazelcastTestHelper.createHazelcastClient;
import static org.sonar.application.cluster.HazelcastTestHelper.newClusterSettings;
import static org.sonar.process.ProcessProperties.CLUSTER_NAME;
import static org.sonar.process.cluster.ClusterObjectKeys.LEADER;
import static org.sonar.process.cluster.ClusterObjectKeys.OPERATIONAL_PROCESSES;
import static org.sonar.process.cluster.ClusterObjectKeys.SONARQUBE_VERSION;

public class HazelcastClusterTest {
  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60));

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @AfterClass
  public static void closeHazelcastClients() {
    closeAllHazelcastClients();
  }

  @Test
  public void test_two_tryToLockWebLeader_must_return_true() {
    ClusterProperties clusterProperties = new ClusterProperties(newClusterSettings());
    try (HazelcastCluster hzCluster = HazelcastCluster.create(clusterProperties)) {
      assertThat(hzCluster.tryToLockWebLeader()).isEqualTo(true);
      assertThat(hzCluster.tryToLockWebLeader()).isEqualTo(false);
    }
  }

  @Test
  public void when_another_process_locked_webleader_tryToLockWebLeader_must_return_false() {
    ClusterProperties clusterProperties = new ClusterProperties(newClusterSettings());
    try (HazelcastCluster hzCluster = HazelcastCluster.create(clusterProperties)) {
      HazelcastInstance hzInstance = createHazelcastClient(hzCluster);
      hzInstance.getAtomicReference(LEADER).set("aaaa");
      assertThat(hzCluster.tryToLockWebLeader()).isEqualTo(false);
    }
  }

  @Test
  public void when_no_leader_getLeaderHostName_must_return_NO_LEADER() {
    ClusterProperties clusterProperties = new ClusterProperties(newClusterSettings());
    try (HazelcastCluster hzCluster = HazelcastCluster.create(clusterProperties)) {
      assertThat(hzCluster.getLeaderHostName()).isEmpty();
    }
  }

  @Test
  public void when_no_leader_getLeaderHostName_must_return_the_hostname() {
    ClusterProperties clusterProperties = new ClusterProperties(newClusterSettings());
    try (HazelcastCluster hzCluster = HazelcastCluster.create(clusterProperties)) {
      assertThat(hzCluster.tryToLockWebLeader()).isTrue();
      assertThat(hzCluster.getLeaderHostName().get()).isEqualTo(NetworkUtils.getHostName());
    }
  }

  @Test
  public void members_must_be_empty_when_there_is_no_other_node() {
    ClusterProperties clusterProperties = new ClusterProperties(newClusterSettings());
    try (HazelcastCluster hzCluster = HazelcastCluster.create(clusterProperties)) {
      assertThat(hzCluster.getMembers()).isEmpty();
    }
  }

  @Test
  public void set_operational_is_writing_to_cluster() {
    ClusterProperties clusterProperties = new ClusterProperties(newClusterSettings());
    try (HazelcastCluster hzCluster = HazelcastCluster.create(clusterProperties)) {

      hzCluster.setOperational(ProcessId.ELASTICSEARCH);

      assertThat(hzCluster.isOperational(ProcessId.ELASTICSEARCH)).isTrue();
      assertThat(hzCluster.isOperational(ProcessId.WEB_SERVER)).isFalse();
      assertThat(hzCluster.isOperational(ProcessId.COMPUTE_ENGINE)).isFalse();

      // Connect via Hazelcast client to test values
      HazelcastInstance hzInstance = createHazelcastClient(hzCluster);
      ReplicatedMap<ClusterProcess, Boolean> operationalProcesses = hzInstance.getReplicatedMap(OPERATIONAL_PROCESSES);
      assertThat(operationalProcesses)
        .containsExactly(new AbstractMap.SimpleEntry<>(new ClusterProcess(hzCluster.getLocalUUID(), ProcessId.ELASTICSEARCH), Boolean.TRUE));
    }
  }

  @Test
  public void cluster_name_comes_from_configuration() {
    TestAppSettings testAppSettings = newClusterSettings();
    testAppSettings.set(CLUSTER_NAME, "a_cluster_");
    ClusterProperties clusterProperties = new ClusterProperties(testAppSettings);
    try (HazelcastCluster hzCluster = HazelcastCluster.create(clusterProperties)) {
      assertThat(hzCluster.getName()).isEqualTo("a_cluster_");
    }
  }

  @Test
  public void cluster_must_keep_a_list_of_clients() throws InterruptedException {
    TestAppSettings testAppSettings = newClusterSettings();
    testAppSettings.set(CLUSTER_NAME, "a_cluster_");
    ClusterProperties clusterProperties = new ClusterProperties(testAppSettings);
    try (HazelcastCluster hzCluster = HazelcastCluster.create(clusterProperties)) {
      assertThat(hzCluster.hzInstance.getSet(ClusterObjectKeys.CLIENT_UUIDS)).isEmpty();
      HazelcastInstance hzClient = HazelcastTestHelper.createHazelcastClient(hzCluster);
      assertThat(hzCluster.hzInstance.getSet(ClusterObjectKeys.CLIENT_UUIDS)).containsExactly(hzClient.getLocalEndpoint().getUuid());

      CountDownLatch latch = new CountDownLatch(1);
      hzCluster.hzInstance.getSet(ClusterObjectKeys.CLIENT_UUIDS).addItemListener(new ItemListener<Object>() {
        @Override
        public void itemAdded(ItemEvent<Object> item) {
        }

        @Override
        public void itemRemoved(ItemEvent<Object> item) {
          latch.countDown();
        }
      }, false);

      hzClient.shutdown();
      if (latch.await(5, TimeUnit.SECONDS)) {
        assertThat(hzCluster.hzInstance.getSet(ClusterObjectKeys.CLIENT_UUIDS).size()).isEqualTo(0);
      } else {
        fail("The client UUID have not been removed from the Set within 5 seconds' time lapse");
      }
    }
  }

  @Test
  public void localUUID_must_not_be_empty() {
    ClusterProperties clusterProperties = new ClusterProperties(newClusterSettings());
    try (HazelcastCluster hzCluster = HazelcastCluster.create(clusterProperties)) {
      assertThat(hzCluster.getLocalUUID()).isNotEmpty();
    }
  }

  @Test
  public void when_a_process_is_set_operational_listener_must_be_triggered() {
    ClusterProperties clusterProperties = new ClusterProperties(newClusterSettings());
    try (HazelcastCluster hzCluster = HazelcastCluster.create(clusterProperties)) {
      AppStateListener listener = mock(AppStateListener.class);
      hzCluster.addListener(listener);

      // ElasticSearch is not operational
      assertThat(hzCluster.isOperational(ProcessId.ELASTICSEARCH)).isFalse();

      // Simulate a node that set ElasticSearch operational
      HazelcastInstance hzInstance = createHazelcastClient(hzCluster);
      ReplicatedMap<ClusterProcess, Boolean> operationalProcesses = hzInstance.getReplicatedMap(OPERATIONAL_PROCESSES);
      operationalProcesses.put(new ClusterProcess(UUID.randomUUID().toString(), ProcessId.ELASTICSEARCH), Boolean.TRUE);
      verify(listener, timeout(20_000)).onAppStateOperational(ProcessId.ELASTICSEARCH);
      verifyNoMoreInteractions(listener);

      // ElasticSearch is operational
      assertThat(hzCluster.isOperational(ProcessId.ELASTICSEARCH)).isTrue();
    }
  }


  @Test
  public void registerSonarQubeVersion_publishes_version_on_first_call() {
    ClusterProperties clusterProperties = new ClusterProperties(newClusterSettings());
    try (HazelcastCluster hzCluster = HazelcastCluster.create(clusterProperties)) {
      hzCluster.registerSonarQubeVersion("1.0.0.0");

      HazelcastInstance hzInstance = createHazelcastClient(hzCluster);
      assertThat(hzInstance.getAtomicReference(SONARQUBE_VERSION).get()).isEqualTo("1.0.0.0");
    }
  }

  @Test
  public void registerSonarQubeVersion_throws_ISE_if_initial_version_is_different() throws Exception {
    ClusterProperties clusterProperties = new ClusterProperties(newClusterSettings());
    try (HazelcastCluster hzCluster = HazelcastCluster.create(clusterProperties)) {
      // Register first version
      hzCluster.registerSonarQubeVersion("1.0.0");

      expectedException.expect(IllegalStateException.class);
      expectedException.expectMessage("The local version 2.0.0 is not the same as the cluster 1.0.0");

      // Registering a second different version must trigger an exception
      hzCluster.registerSonarQubeVersion("2.0.0");
    }
  }

  @Test
  public void simulate_network_cluster() throws InterruptedException {
    TestAppSettings settings = newClusterSettings();
    settings.set(ProcessProperties.CLUSTER_NETWORK_INTERFACES, InetAddress.getLoopbackAddress().getHostAddress());
    AppStateListener listener = mock(AppStateListener.class);

    try (AppStateClusterImpl appStateCluster = new AppStateClusterImpl(settings)) {
      appStateCluster.addListener(listener);

      HazelcastInstance hzInstance = createHazelcastClient(appStateCluster.getHazelcastCluster());
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
  public void hazelcast_must_log_through_sl4fj() {
    MemoryAppender<ILoggingEvent> memoryAppender = new MemoryAppender<>();
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    lc.reset();
    memoryAppender.setContext(lc);
    memoryAppender.start();
    lc.getLogger("com.hazelcast").addAppender(memoryAppender);

    try (AppStateClusterImpl appStateCluster = new AppStateClusterImpl(newClusterSettings())) {
    }

    assertThat(memoryAppender.events).isNotEmpty();
    memoryAppender.events.stream().forEach(
      e -> assertThat(e.getLoggerName()).startsWith("com.hazelcast")
    );
  }

  private class MemoryAppender<E> extends AppenderBase<E> {
    private final List<E> events = new ArrayList();

    @Override
    protected void append(E eventObject) {
      events.add(eventObject);
    }
  }


  @Test
  public void configuration_tweaks_of_hazelcast_must_be_present() {
    try (HazelcastCluster hzCluster = HazelcastCluster.create(new ClusterProperties(newClusterSettings()))) {
      assertThat(hzCluster.hzInstance.getConfig().getProperty("hazelcast.tcp.join.port.try.count")).isEqualTo("10");
      assertThat(hzCluster.hzInstance.getConfig().getProperty("hazelcast.phone.home.enabled")).isEqualTo("false");
      assertThat(hzCluster.hzInstance.getConfig().getProperty("hazelcast.logging.type")).isEqualTo("slf4j");
      assertThat(hzCluster.hzInstance.getConfig().getProperty("hazelcast.socket.bind.any")).isEqualTo("false");
    }
  }
}
