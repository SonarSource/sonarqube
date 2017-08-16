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

import com.sonar.orchestrator.db.Database;
import com.sonar.orchestrator.db.DatabaseClient;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.fail;
import static org.sonarqube.tests.cluster.Cluster.NodeType.APPLICATION;
import static org.sonarqube.tests.cluster.Cluster.NodeType.SEARCH;

public class DataCenterEditionTest {

  @Rule
  public TestRule timeout = new DisableOnDebug(Timeout.builder()
    .withLookingForStuckThread(true)
    .withTimeout(5, TimeUnit.MINUTES)
    .build());

  @Test
  public void launch() throws ExecutionException, InterruptedException {
    DataCenterEdition dce = new DataCenterEdition();
    Cluster cluster = dce.getCluster();
    dce.start();
    assertThat(cluster.getNodes())
      .extracting(Cluster.Node::getType, n -> isPortBound(false, n.getEsPort()), n -> isPortBound(true, n.getWebPort()))
      .containsExactlyInAnyOrder(
        tuple(SEARCH, true, false),
        tuple(SEARCH, true, false),
        tuple(SEARCH, true, false),
        tuple(APPLICATION, false, true),
        tuple(APPLICATION, false, true)
      );
    dce.stop();
  }

  @Test
  public void upgrade_application_nodes_without_stopping_search_nodes_must_work() throws ExecutionException, InterruptedException, SQLException {
    DataCenterEdition dce = new DataCenterEdition();
    Cluster cluster = dce.getCluster();
    dce.start();

    // Stop all Application nodes
    cluster.stopAll(n -> n.getType() == APPLICATION);

    // Drop the schema
    Database database = cluster.getNodes().get(0).getOrchestrator().getDatabase();
    dropAndCreate(database.getClient());
    assertDatabaseDropped(database);

    // Start all Application nodes
    cluster.startAll(n -> n.getType() == APPLICATION);

    // We are expecting a new leader to be elected which will recreate the database
    assertDatabaseInitialized(database);

    dce.stop();
  }

  private void assertDatabaseInitialized(Database database) {
    assertThat(countRowsOfMigration(database)).isGreaterThan(0);
  }

  private int countRowsOfMigration(Database database) {
    return database.countSql("select count(*) from schema_migrations");
  }

  private void assertDatabaseDropped(Database database) {
    try {
      countRowsOfMigration(database);
      fail("Table 'schema_migrations' has not been dropped");
    } catch (Exception e) {
      // we expect the table to not exist
    }
  }

  private static boolean isPortBound(boolean loopback, @Nullable Integer port) {
    if (port == null) {
      return false;
    }
    InetAddress inetAddress = loopback ? InetAddress.getLoopbackAddress() : Cluster.getNonloopbackIPv4Address();
    try (ServerSocket socket = new ServerSocket(port, 50, inetAddress)) {
      throw new IllegalStateException("A port was set explicitly, but was not bound (port="+port+")");
    } catch (IOException e) {
      return true;
    }
  }

  private static void dropAndCreate(DatabaseClient databaseClient) throws SQLException {
    try (Connection connection = databaseClient.openRootConnection()) {
      executeDdl(connection, databaseClient.getDropDdl());
      executeDdl(connection, databaseClient.getCreateDdl());
    }
  }

  private static void executeDdl(Connection connection, String... ddls) throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      for (String ddl : ddls) {
        stmt.executeUpdate(ddl);
      }
    }
  }
}
