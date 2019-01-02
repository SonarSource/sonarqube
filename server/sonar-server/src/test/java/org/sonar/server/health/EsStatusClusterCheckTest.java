/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.health;

import com.google.common.collect.ImmutableSet;
import java.util.Random;
import java.util.Set;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.process.cluster.health.NodeDetails;
import org.sonar.process.cluster.health.NodeHealth;
import org.sonar.server.es.EsClient;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;

public class EsStatusClusterCheckTest {

  private EsClient esClient = Mockito.mock(EsClient.class, RETURNS_DEEP_STUBS);
  private Random random = new Random();
  private EsStatusClusterCheck underTest = new EsStatusClusterCheck(esClient);

  @Test
  public void check_ignores_NodeHealth_arg_and_returns_RED_with_cause_if_an_exception_occurs_checking_ES_cluster_status() {
    Set<NodeHealth> nodeHealths = ImmutableSet.of(newNodeHealth(NodeHealth.Status.GREEN));
    when(esClient.prepareClusterStats()).thenThrow(new RuntimeException("Faking an exception occurring while using the EsClient"));

    Health health = new EsStatusClusterCheck(esClient).check(nodeHealths);

    assertThat(health.getStatus()).isEqualTo(Health.Status.RED);
    assertThat(health.getCauses()).containsOnly("Elasticsearch status is RED (unavailable)");
  }

  @Test
  public void check_ignores_NodeHealth_arg_and_returns_GREEN_without_cause_if_ES_cluster_status_is_GREEN() {
    Set<NodeHealth> nodeHealths = ImmutableSet.of(newNodeHealth(NodeHealth.Status.YELLOW));
    when(esClient.prepareClusterStats().get().getStatus()).thenReturn(ClusterHealthStatus.GREEN);

    Health health = underTest.check(nodeHealths);

    assertThat(health).isEqualTo(Health.GREEN);
  }

  private NodeHealth newNodeHealth(NodeHealth.Status status) {
    return NodeHealth.newNodeHealthBuilder()
      .setStatus(status)
      .setDetails(NodeDetails.newNodeDetailsBuilder()
        .setType(random.nextBoolean() ? NodeDetails.Type.APPLICATION : NodeDetails.Type.SEARCH)
        .setName(randomAlphanumeric(23))
        .setHost(randomAlphanumeric(23))
        .setPort(1 + random.nextInt(96))
        .setStartedAt(1 + random.nextInt(966))
        .build())
      .build();
  }

}
