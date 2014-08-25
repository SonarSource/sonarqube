/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.search;

import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.server.tester.ServerTester;

import java.util.Date;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class SearchHealthMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  @Test
  public void get_search_health(){
    SearchHealth health = tester.get(SearchHealth.class);
    Date now = new Date();

    ClusterHealth clusterHealth = health.getClusterHealth();
    assertThat(clusterHealth.isClusterAvailable()).isTrue();
    assertThat(clusterHealth.getNumberOfNodes()).isEqualTo(1);

    NodeHealth nodeHealth = health.getNodesHealth().values().iterator().next();
    assertThat(nodeHealth.isMaster()).isTrue();
    assertThat(nodeHealth.getJvmHeapUsedPercent()).contains("%");
    assertThat(nodeHealth.getFsUsedPercent()).contains("%");
    assertThat(nodeHealth.getJvmThreads()).isGreaterThanOrEqualTo(0L);
    assertThat(nodeHealth.getProcessCpuPercent()).contains("%");
    assertThat(nodeHealth.getOpenFiles()).isGreaterThanOrEqualTo(0L);
    assertThat(nodeHealth.getJvmUpSince().before(now)).isTrue();

    Map<String, IndexHealth> indexHealth = health.getIndexHealth();
    assertThat(indexHealth).isNotEmpty();
    for(IndexHealth index: indexHealth.values()) {
      assertThat(index.getDocumentCount()).isGreaterThanOrEqualTo(0L);
      assertThat(index.getLastSynchronization().before(now)).isTrue();
      assertThat(index.isOptimized()).isIn(true, false);
    }
  }

}
