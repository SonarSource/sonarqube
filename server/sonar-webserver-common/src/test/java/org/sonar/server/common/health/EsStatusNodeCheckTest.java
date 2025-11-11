/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.common.health;

import co.elastic.clients.elasticsearch._types.HealthStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonar.server.es.EsClient;
import org.sonar.server.health.Health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EsStatusNodeCheckTest {

  private final EsClient esClient = mock(EsClient.class, Mockito.RETURNS_DEEP_STUBS);
  private final EsStatusNodeCheck underTest = new EsStatusNodeCheck(esClient);

  @Test
  void check_ignores_NodeHealth_arg_and_returns_RED_with_cause_if_an_exception_occurs_checking_ES_cluster_status() {
    when(esClient.clusterHealthV2(any())).thenThrow(new RuntimeException("Faking an exception occurring while using the EsClient"));

    Health health = new EsStatusNodeCheck(esClient).check();

    assertThat(health.getStatus()).isEqualTo(Health.Status.RED);
    assertThat(health.getCauses()).containsOnly("Elasticsearch status is RED (unavailable)");
  }

  @Test
  void check_returns_GREEN_without_cause_if_ES_cluster_status_is_GREEN() {
    when(esClient.clusterHealthV2(any()).status()).thenReturn(HealthStatus.Green);

    Health health = underTest.check();

    assertThat(health).isEqualTo(Health.GREEN);
  }

}
