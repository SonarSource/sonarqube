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

package org.sonar.ce.cluster;

import com.hazelcast.core.HazelcastInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonar.api.config.Settings;
import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessProperties;
import org.sonar.server.computation.taskprocessor.CeWorker;
import org.sonar.server.computation.taskprocessor.CeWorkerFactory;

import static java.util.Collections.unmodifiableList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ClusterClientTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public TestRule safeGuard = new DisableOnDebug(Timeout.seconds(10));

  @Test
  public void missing_CLUSTER_ENABLED_throw_ISE() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Cluster is not enabled");

    Settings settings = createClusterSettings("sonarqube", "localhost:9003");
    settings.removeProperty(ProcessProperties.CLUSTER_ENABLED);
    new ClusterClient(new ClusterClientProperties(settings), new CeWorkerFactoryTest());
  }

  @Test
  public void missing_CLUSTER_NAME_throw_ISE() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("sonar.cluster.name is missing");

    Settings settings = createClusterSettings("sonarqube", "localhost:9003");
    settings.removeProperty(ProcessProperties.CLUSTER_NAME);
    new ClusterClient(new ClusterClientProperties(settings), new CeWorkerFactoryTest());
  }

  @Test
  public void missing_CLUSTER_LOCALENDPOINT_throw_ISE() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("LocalEndPoint have not been set");

    Settings settings = createClusterSettings("sonarqube", "localhost:9003");
    settings.removeProperty(ProcessProperties.CLUSTER_LOCALENDPOINT);
    new ClusterClient(new ClusterClientProperties(settings), new CeWorkerFactoryTest());
  }

  @Test
  public void client_must_connect_to_hazelcast() {
    int port = NetworkUtils.freePort();
    HazelcastInstance hzInstance = HazelcastTestHelper.createHazelcastCluster("client_must_connect_to_hazelcast", port);
    Settings settings = createClusterSettings("client_must_connect_to_hazelcast", "localhost:" + port);
    try (ClusterClient clusterClient = new ClusterClient(new ClusterClientProperties(settings), new CeWorkerFactoryTest())) {
      // No exception thrown
    }
  }

  @Test
  public void worker_uuids_must_be_synchronized_with_cluster() {
    int port = NetworkUtils.freePort();
    HazelcastInstance hzInstance = HazelcastTestHelper.createHazelcastCluster("client_must_connect_to_hazelcast", port);
    Settings settings = createClusterSettings("client_must_connect_to_hazelcast", "localhost:" + port);
    CeWorkerFactoryTest ceWorkerFactory = new CeWorkerFactoryTest();
    try (ClusterClient clusterClient = new ClusterClient(new ClusterClientProperties(settings), ceWorkerFactory)) {
      assertThat(clusterClient.getWorkerUUIDs()).isEmpty();
      // Add a two workers
      ceWorkerFactory.create();
      ceWorkerFactory.create();
      assertThat(clusterClient.getWorkerUUIDs()).isEqualTo(ceWorkerFactory.workerUUIDs);
    }
  }

  private static Settings createClusterSettings(String name, String localEndPoint) {
    Properties properties = new Properties();
    properties.setProperty(ProcessProperties.CLUSTER_NAME, name);
    properties.setProperty(ProcessProperties.CLUSTER_LOCALENDPOINT, localEndPoint);
    properties.setProperty(ProcessProperties.CLUSTER_ENABLED, "true");
    return new TestSettings(properties);
  }

  private class CeWorkerFactoryTest implements CeWorkerFactory {
    private final List<String> workerUUIDs = new ArrayList<>();
    private final List<Listener> listeners = new ArrayList<>();

    @Override
    public CeWorker create() {
      String uuid = UUID.randomUUID().toString();
      workerUUIDs.add(uuid);
      listeners.stream().forEach(l -> l.onChange());
      return mock(CeWorker.class);
    }

    @Override
    public List<String> getWorkerUUIDs() {
      return unmodifiableList(workerUUIDs);
    }

    @Override
    public void addListener(Listener listener) {
      listeners.add(listener);
    }
  }
}
