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
package org.sonar.server.health;

import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.cluster.health.NodeDetails;
import org.sonar.cluster.health.NodeHealth;
import org.sonar.server.es.EsTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class EsStatusClusterCheckTest {

  @Rule
  public EsTester esTester = new EsTester();

  private EsStatusClusterCheck underTest = new EsStatusClusterCheck(esTester.client());

  @Test
  public void check_ignores_NodeHealth_arg_and_returns_GREEN_without_cause_if_ES_cluster_status_is_GREEN() {
    Random random = new Random();
    Set<NodeHealth> nodeHealths = IntStream.range(0, random.nextInt(20))
      .mapToObj(i -> NodeHealth.newNodeHealthBuilder()
        .setStatus(NodeHealth.Status.values()[random.nextInt(NodeHealth.Status.values().length)])
        .setDetails(NodeDetails.newNodeDetailsBuilder()
          .setType(random.nextBoolean() ? NodeDetails.Type.APPLICATION : NodeDetails.Type.SEARCH)
          .setName(randomAlphanumeric(23))
          .setHost(randomAlphanumeric(23))
          .setPort(1 + random.nextInt(96))
          .setStarted(1 + random.nextInt(966))
          .build())
        .setDate(1 + random.nextInt(23))
        .build())
      .collect(Collectors.toSet());
    Health health = underTest.check(nodeHealths);

    assertThat(health).isEqualTo(Health.GREEN);
  }

}
