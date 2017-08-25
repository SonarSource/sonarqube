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

package org.sonar.cluster.localclient;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.google.common.collect.ImmutableSet;
import com.hazelcast.client.impl.HazelcastClientInstanceImpl;
import com.hazelcast.client.impl.HazelcastClientProxy;
import com.hazelcast.core.Client;
import com.hazelcast.core.ClientListener;
import com.hazelcast.core.HazelcastInstance;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.slf4j.LoggerFactory;
import org.sonar.NetworkUtils;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.cluster.internal.HazelcastTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.sonar.cluster.ClusterProperties.CLUSTER_ENABLED;
import static org.sonar.cluster.ClusterProperties.CLUSTER_LOCALENDPOINT;

public class HazelcastClientWrapperImplTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60));

  private static HazelcastInstance hzCluster;
  private static HazelcastClientWrapperImpl hzClient;

  @BeforeClass
  public static void setupHazelcastClusterAndHazelcastClient() {
    int port = NetworkUtils.getNextAvailablePort(InetAddress.getLoopbackAddress());
    hzCluster = HazelcastTestHelper.createHazelcastCluster(NetworkUtils.getHostname(), port);

    MapSettings settings = createClusterSettings("localhost:" + port);
    hzClient = new HazelcastClientWrapperImpl(settings.asConfig());
  }

  @AfterClass
  public static void stopHazelcastClusterAndHazelcastClient() {
    try {
      hzClient.stop();
    } catch (Exception e) {
      // Ignore it
    }
    try {
      hzCluster.shutdown();
    } catch (Exception e) {
      // Ignore it
    }
  }

  @Test
  public void start_throws_ISE_if_LOCALENDPOINT_is_incorrect() {
    MapSettings settings = createClusterSettings("\u4563\u1432\u1564");
    HazelcastClientWrapperImpl hzClient = new HazelcastClientWrapperImpl(settings.asConfig());

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Unable to connect to any address in the config! The following addresses were tried:");

    hzClient.start();
  }

  @Test
  public void constructor_throws_ISE_if_LOCALENDPOINT_is_empty() {
    MapSettings settings = createClusterSettings("");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("LocalEndPoint have not been set");

    new HazelcastClientWrapperImpl(settings.asConfig());
  }

  @Test
  public void constructor_throws_ISE_if_CLUSTER_ENABLED_is_false() {
    MapSettings settings = createClusterSettings("localhost:9003");
    settings.setProperty(CLUSTER_ENABLED, false);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Cluster is not enabled");

    new HazelcastClientWrapperImpl(settings.asConfig());
  }

  @Test
  public void constructor_throws_ISE_if_missing_CLUSTER_ENABLED() {
    MapSettings settings = createClusterSettings("localhost:9003");
    settings.removeProperty(CLUSTER_ENABLED);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Cluster is not enabled");

    new HazelcastClientWrapperImpl(settings.asConfig());
  }

  @Test
  public void constructor_throws_ISE_if_missing_CLUSTER_LOCALENDPOINT() {
    MapSettings settings = createClusterSettings("localhost:9003");
    settings.removeProperty(CLUSTER_LOCALENDPOINT);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("LocalEndPoint have not been set");

    new HazelcastClientWrapperImpl(settings.asConfig());
  }

  @Test
  public void client_must_connect_to_hazelcast() throws InterruptedException {
    int port = NetworkUtils.getNextAvailablePort(InetAddress.getLoopbackAddress());
    // Launch a fake Hazelcast instance
    HazelcastInstance hzInstance = HazelcastTestHelper.createHazelcastCluster(NetworkUtils.getHostname(), port);
    MapSettings settings = createClusterSettings("localhost:" + port);

    HazelcastClientWrapperImpl hazelcastClientWrapperImpl = new HazelcastClientWrapperImpl(settings.asConfig());
    ClientListenerImpl clientListener = new ClientListenerImpl();
    hzInstance.getClientService().addClientListener(clientListener);
    try {
      hazelcastClientWrapperImpl.start();
      clientListener.counter.await(5, TimeUnit.SECONDS);
      assertThat(hazelcastClientWrapperImpl.getConnectedClients()).hasSize(1);
      assertThat(hazelcastClientWrapperImpl.getClientUUID()).isNotEmpty();
    } finally {
      hazelcastClientWrapperImpl.stop();
    }
  }

  @Test
  public void client_must_be_able_to_set_ReplicatedMap_objects() throws InterruptedException {
    hzClient.start();
    try {

      Set<String> setTest = new HashSet<>();
      setTest.addAll(
        Arrays.asList(RandomStringUtils.randomAlphanumeric(10), RandomStringUtils.randomAlphanumeric(10)));
      Map<String, Set<String>> replicatedMap = hzClient.getReplicatedMap("TEST1");
      replicatedMap.put("KEY1", ImmutableSet.copyOf(setTest));
      Assertions.assertThat(hzCluster.getReplicatedMap("TEST1"))
        .containsOnlyKeys("KEY1");
      Assertions.assertThat(hzCluster.getReplicatedMap("TEST1").get("KEY1"))
        .isEqualTo(setTest);
    } finally {
      hzClient.stop();
    }
  }

  @Test
  public void client_must_be_able_to_retrieve_Set_objects() {
    hzClient.start();
    try {

      // Set
      Set<String> setTest = new HashSet<>();
      setTest.addAll(Arrays.asList("8", "9"));
      hzCluster.getSet("TEST1").addAll(setTest);
      assertThat(hzClient.getSet("TEST1")).containsAll(setTest);
    } finally {
      hzClient.stop();
    }
  }

  @Test
  public void client_must_be_able_to_retrieve_List_objects() {
    hzClient.start();
    try {

      // List
      List<String> listTest = Arrays.asList("1", "2");
      hzCluster.getList("TEST2").addAll(listTest);
      assertThat(hzClient.getList("TEST2")).containsAll(listTest);
    } finally {
      hzClient.stop();
    }
  }

  @Test
  public void client_must_be_able_to_retrieve_Map_objects() {
    hzClient.start();
    try {
      Map mapTest = new HashMap<>();
      mapTest.put("a", Arrays.asList("123", "456"));
      hzCluster.getMap("TEST3").putAll(mapTest);
      assertThat(hzClient.getMap("TEST3")).containsExactly(
        entry("a", Arrays.asList("123", "456")));
    } finally {
      hzClient.stop();
    }
  }

  @Test
  public void configuration_tweaks_of_hazelcast_must_be_present() {
    hzClient.start();
    try {
      HazelcastClientInstanceImpl realClient = ((HazelcastClientProxy) hzClient.hzInstance).client;
      Assertions.assertThat(realClient.getClientConfig().getProperty("hazelcast.tcp.join.port.try.count")).isEqualTo("10");
      Assertions.assertThat(realClient.getClientConfig().getProperty("hazelcast.phone.home.enabled")).isEqualTo("false");
      Assertions.assertThat(realClient.getClientConfig().getProperty("hazelcast.logging.type")).isEqualTo("slf4j");
    } finally {
      hzClient.stop();
    }
  }

  @Test
  public void hazelcast_client_must_log_through_sl4fj() {
    MemoryAppender<ILoggingEvent> memoryAppender = new MemoryAppender<>();
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    lc.reset();
    memoryAppender.setContext(lc);
    memoryAppender.start();
    lc.getLogger("com.hazelcast").addAppender(memoryAppender);

    hzClient.start();
    hzClient.stop();
    memoryAppender.stop();
    Assertions.assertThat(memoryAppender.events).isNotEmpty();
    memoryAppender.events.stream().forEach(
      e -> Assertions.assertThat(e.getLoggerName()).startsWith("com.hazelcast"));
  }

  private class ClientListenerImpl implements ClientListener {
    CountDownLatch counter = new CountDownLatch(1);

    @Override
    public void clientConnected(Client client) {
      counter.countDown();
    }

    @Override
    public void clientDisconnected(Client client) {

    }
  }

  private static MapSettings createClusterSettings(String localEndPoint) {
    return new MapSettings(new PropertyDefinitions())
      .setProperty(CLUSTER_LOCALENDPOINT, localEndPoint)
      .setProperty(CLUSTER_ENABLED, "true");
  }

  private class MemoryAppender<E> extends AppenderBase<E> {
    private final List<E> events = new ArrayList();

    @Override
    protected void append(E eventObject) {
      events.add(eventObject);
    }
  }
}
