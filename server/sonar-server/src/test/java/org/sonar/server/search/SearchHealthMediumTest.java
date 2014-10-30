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
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.tester.ServerTester;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class SearchHealthMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  @Test
  public void get_search_health() {
    DbSession dbSession = tester.get(DbClient.class).openSession(false);
    tester.get(RuleDao.class).insert(dbSession, RuleTesting.newDto(RuleKey.of("javascript", "S001")));
    dbSession.commit();
    dbSession.close();

    SearchHealth health = tester.get(SearchHealth.class);
    Date now = new Date();

    ClusterHealth clusterHealth = health.getClusterHealth();
    assertThat(clusterHealth.isClusterAvailable()).isTrue();
    assertThat(clusterHealth.getNumberOfNodes()).isEqualTo(1);

    NodeHealth nodeHealth = health.getNodesHealth().values().iterator().next();
    assertThat(nodeHealth.isMaster()).isTrue();
    assertThat(nodeHealth.getAddress()).contains(":");
    assertThat(nodeHealth.getJvmHeapUsedPercent()).contains("%");
    assertThat(nodeHealth.getFsUsedPercent()).contains("%");
    assertThat(nodeHealth.getJvmThreads()).isGreaterThanOrEqualTo(0L);
    assertThat(nodeHealth.getFieldCacheMemory()).isGreaterThanOrEqualTo(0L);
    assertThat(nodeHealth.getFilterCacheMemory()).isGreaterThanOrEqualTo(0L);
    assertThat(nodeHealth.getProcessCpuPercent()).contains("%");
    long openFiles = nodeHealth.getOpenFiles();
    if (!tester.get(System2.class).isOsWindows()) {
      assertThat(openFiles).isGreaterThanOrEqualTo(0L);
    }
    assertThat(nodeHealth.getJvmUpSince().before(now)).isTrue();

    List<NodeHealth.Performance> performances = nodeHealth.getPerformanceStats();
    assertThat(performances).hasSize(7);
    for (NodeHealth.Performance performance : performances) {
      assertThat(performance.getName()).isNotNull();
      assertThat(performance.getValue()).isNotNull();
      assertThat(performance.getMessage()).isNotNull();
      assertThat(performance.getStatus()).isNotNull();
    }

    Map<String, IndexHealth> indexHealth = health.getIndexHealth();
    assertThat(indexHealth).isNotEmpty();
    for (IndexHealth index : indexHealth.values()) {
      assertThat(index.getDocumentCount()).isGreaterThanOrEqualTo(0L);
      Date lastSync = index.getLastSynchronization();
      if (lastSync != null) {
        assertThat(lastSync.before(now)).isTrue();
      }
      assertThat(index.isOptimized()).isIn(true, false);
    }
  }

}
