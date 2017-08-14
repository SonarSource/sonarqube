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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonarqube.tests.cluster.Cluster.NodeType.APPLICATION;
import static org.sonarqube.tests.cluster.Cluster.NodeType.SEARCH;

public class DataCenterEditionTest {

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
}
