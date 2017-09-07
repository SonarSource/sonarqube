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
package org.sonarqube.tests.cluster;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.db.DefaultDatabase;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonarqube.ws.WsSystem;

import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonarqube.tests.cluster.NodeConfig.newApplicationConfig;
import static org.sonarqube.tests.cluster.NodeConfig.newSearchConfig;

public class ClusterTest {

  @Rule
  public TestRule safeguard = new DisableOnDebug(Timeout.seconds(300));

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @BeforeClass
  public static void initDbSchema() throws Exception {
    Orchestrator orchestrator = Orchestrator.builderEnv()
      // enforce (re-)creation of database schema
      .setOrchestratorProperty("orchestrator.keepDatabase", "false")
      .build();
    DefaultDatabase db = new DefaultDatabase(orchestrator.getConfiguration());
    checkState(!db.getClient().getDialect().equals("h2"), "H2 is not supported in cluster mode");
    db.start();
    db.stop();
  }

  @Test
  public void test_high_availability_topology() throws Exception {
    try (Cluster cluster = newCluster(3, 2)) {
      cluster.getNodes().forEach(Node::start);

      cluster.getAppNode(0).waitForHealthGreen();
      cluster.getAppNodes().forEach(node -> assertThat(node.getStatus()).hasValue(WsSystem.Status.UP));

      cluster.getNodes().forEach(node -> {
        node.assertThatProcessesAreUp();
        assertThat(node.anyLogsContain(" ERROR ")).isFalse();
        assertThat(node.anyLogsContain("MessageException")).isFalse();
      });

      verifyGreenHealthOfNodes(cluster);

      // verify that there's a single web startup leader
      Node startupLeader = cluster.getAppNodes()
        .filter(Node::isStartupLeader)
        .reduce(singleElement())
        .get();
      assertThat(startupLeader.hasStartupLeaderOperations()).isTrue();
      assertThat(startupLeader.hasCreatedSearchIndices()).isTrue();

      // verify that the second app node is a startup follower
      Node startupFollower = cluster.getAppNodes()
        .filter(Node::isStartupFollower)
        .reduce(singleElement())
        .get();
      assertThat(startupFollower.hasStartupLeaderOperations()).isFalse();
      assertThat(startupFollower.hasCreatedSearchIndices()).isFalse();

      cluster.getAppNodes().forEach(app -> {
        // compute engine is being started when app node is already in status UP
        app.waitForCeLogsContain("Compute Engine is operational");
        assertThat(app.anyLogsContain("Process[ce] is up")).isTrue();
      });
    }
  }

  private void verifyGreenHealthOfNodes(Cluster cluster) {
    WsSystem.HealthResponse health = cluster.getAppNode(0).getHealth().get();
    cluster.getNodes().forEach(node -> {
      WsSystem.Node healthNode = health.getNodesList().stream()
        .filter(n -> n.getPort() == node.getConfig().getHzPort())
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Node with port " + node.getConfig().getHzPort() + " not found in api/system/health"));
      // TODO assertions to be improved
      assertThat(healthNode.getStartedAt()).isGreaterThan(0);
      assertThat(healthNode.getHost()).isNotEmpty();
      assertThat(healthNode.getCausesCount()).isEqualTo(0);
      assertThat(healthNode.getHealth()).isEqualTo(WsSystem.Health.GREEN);
    });
  }

  @Test
  public void minimal_cluster_is_2_search_and_1_application_nodes() throws Exception {
    try (Cluster cluster = newCluster(2, 1)) {
      cluster.getNodes().forEach(Node::start);

      Node app = cluster.getAppNode(0);
      app.waitForStatusUp();
      app.waitForCeLogsContain("Compute Engine is operational");

      app.waitForHealth(WsSystem.Health.YELLOW);
      WsSystem.HealthResponse health = app.getHealth().orElseThrow(() -> new IllegalStateException("Health is not available"));
      assertThat(health.getCausesList()).extracting(WsSystem.Cause::getMessage)
        .contains("There should be at least three search nodes")
        .contains("There should be at least two application nodes");

      assertThat(app.isStartupLeader()).isTrue();
      assertThat(app.hasStartupLeaderOperations()).isTrue();

      cluster.getNodes().forEach(node -> {
        assertThat(node.anyLogsContain(" ERROR ")).isFalse();
        node.assertThatProcessesAreUp();
      });
    }
  }

  @Test
  public void configuration_of_connection_to_other_nodes_can_be_non_exhaustive() throws Exception {
    try (Cluster cluster = new Cluster(null)) {
      NodeConfig searchConfig1 = newSearchConfig();
      NodeConfig searchConfig2 = newSearchConfig();
      NodeConfig appConfig = newApplicationConfig();

      // HZ bus : app -> search 2 -> search1, which is not recommended at all !!!
      searchConfig2.addConnectionToBus(searchConfig1);
      appConfig.addConnectionToBus(searchConfig2);

      // search1 is not configured to connect search2
      // app is not configured to connect to search 1
      // --> not recommended at all !!!
      searchConfig2.addConnectionToSearch(searchConfig1);
      appConfig.addConnectionToSearch(searchConfig2);

      cluster.startNode(searchConfig1, nothing());
      cluster.startNode(searchConfig2, nothing());
      Node app = cluster.startNode(appConfig, nothing());

      app.waitForStatusUp();
      assertThat(app.isStartupLeader()).isTrue();
      assertThat(app.hasStartupLeaderOperations()).isTrue();

      // no errors
      cluster.getNodes().forEach(node -> assertThat(node.anyLogsContain(" ERROR ")).isFalse());
    }
  }

  @Test
  public void node_fails_to_join_cluster_if_different_cluster_name() throws Exception {
    try (Cluster cluster = new Cluster("foo")) {
      NodeConfig searchConfig1 = newSearchConfig();
      NodeConfig searchConfig2 = newSearchConfig();
      NodeConfig.interconnectBus(searchConfig1, searchConfig2);
      NodeConfig.interconnectSearch(searchConfig1, searchConfig2);
      cluster.startNode(searchConfig1, nothing());
      cluster.startNode(searchConfig2, nothing());

      NodeConfig searchConfig3 = newSearchConfig()
        .addConnectionToSearch(searchConfig1)
        .addConnectionToBus(searchConfig1, searchConfig2);
      Node search3 = cluster.addNode(searchConfig3, b -> b
        .setServerProperty("sonar.cluster.name", "bar")
        .setStartupLogWatcher(logLine -> logLine.contains("SonarQube is up")));
      try {
        search3.start();
        fail();
      } catch (IllegalStateException e) {
        assertThat(e).hasMessage("Server startup failure");
        // TODO how to force process to write into sonar.log, even if sonar.log.console=true ?
        // assertThat(search3.anyLogsContain("This node has a cluster name [bar], which does not match [foo] from the cluster")).isTrue();
      }
    }
  }

  @Test
  public void restarting_all_application_nodes_elects_a_new_startup_leader() throws Exception {
    // no need for 3 search nodes, 2 is enough for the test
    try (Cluster cluster = newCluster(2, 2)) {
      cluster.getNodes().forEach(Node::start);
      cluster.getAppNodes().forEach(Node::waitForStatusUp);

      // stop application nodes only
      cluster.getAppNodes().forEach(app -> {
        app.stop();
        app.cleanUpLogs();
        // logs are empty, no more possible to know if node was startup leader/follower
        assertThat(app.isStartupLeader()).isFalse();
        assertThat(app.isStartupFollower()).isFalse();
      });

      // restart application nodes
      cluster.getAppNodes().forEach(Node::start);
      cluster.getAppNodes().forEach(Node::waitForStatusUp);

      // one app node is elected as startup leader. It does some initialization stuff,
      // like registration of rules. Search indices already exist and are up-to-date.
      Node startupLeader = cluster.getAppNodes()
        .filter(Node::isStartupLeader)
        .reduce(singleElement())
        .get();
      assertThat(startupLeader.hasStartupLeaderOperations()).isTrue();
      assertThat(startupLeader.hasCreatedSearchIndices()).isFalse();

      Node startupFollower = cluster.getAppNodes()
        .filter(Node::isStartupFollower)
        .reduce(singleElement())
        .get();
      assertThat(startupFollower.hasStartupLeaderOperations()).isFalse();
      assertThat(startupFollower.hasCreatedSearchIndices()).isFalse();
      assertThat(startupFollower).isNotSameAs(startupLeader);
    }
  }

  @Test
  public void health_becomes_RED_when_all_search_nodes_go_down() throws Exception {
    try (Cluster cluster = newCluster(2, 1)) {
      cluster.getNodes().forEach(Node::start);

      Node app = cluster.getAppNode(0);
      app.waitForHealth(WsSystem.Health.YELLOW);

      cluster.getSearchNodes().forEach(Node::stop);

      app.waitForHealth(WsSystem.Health.RED);
      assertThat(app.getHealth().get().getCausesList()).extracting(WsSystem.Cause::getMessage)
        .contains("Elasticsearch status is RED (can't reach it)");
    }
  }

  @Test
  public void health_ws_is_available_when_server_is_starting() throws Exception {
    File startupLock = temp.newFile();
    FileUtils.touch(startupLock);

    try (Cluster cluster = newCluster(2, 0)) {
      // add an application node that pauses during startup
      NodeConfig appConfig = NodeConfig.newApplicationConfig()
        .addConnectionToBus(cluster.getSearchNode(0).getConfig())
        .addConnectionToSearch(cluster.getSearchNode(0).getConfig());
      Node appNode = cluster.addNode(appConfig, b -> b.setServerProperty("sonar.web.startupLock.path", startupLock.getAbsolutePath()));

      cluster.getNodes().forEach(Node::start);

      appNode.waitFor(node -> WsSystem.Status.STARTING == node.getStatus().orElse(null));

      // WS answers whereas server is still not started
      assertThat(appNode.getHealth().get().getHealth()).isEqualTo(WsSystem.Health.RED);

      // just to be sure, verify that server is still being started
      assertThat(appNode.getStatus()).hasValue(WsSystem.Status.STARTING);

      startupLock.delete();
    }
  }

  /**
   * Used to have non-blocking {@link Node#start()}. Orchestrator considers
   * node to be up as soon as the first log is generated.
   */
  private static Consumer<OrchestratorBuilder> nothing() {
    return b -> {
    };
  }

  /**
   * Configure a cluster with recommended configuration (each node has references
   * to other nodes)
   */
  private static Cluster newCluster(int nbOfSearchNodes, int nbOfAppNodes) {
    Cluster cluster = new Cluster(null);

    List<NodeConfig> configs = new ArrayList<>();
    IntStream.range(0, nbOfSearchNodes).forEach(i -> configs.add(newSearchConfig()));
    IntStream.range(0, nbOfAppNodes).forEach(i -> configs.add(newApplicationConfig()));
    NodeConfig[] configsArray = configs.toArray(new NodeConfig[configs.size()]);

    // a node is connected to all nodes, including itself (see sonar.cluster.hosts)
    NodeConfig.interconnectBus(configsArray);

    // search nodes are interconnected, and app nodes connect to all search nodes
    NodeConfig.interconnectSearch(configsArray);

    configs.forEach(c -> cluster.addNode(c, nothing()));
    return cluster;
  }

  private static BinaryOperator<Node> singleElement() {
    return (a, b) -> {
      throw new IllegalStateException("More than one element");
    };
  }
}
