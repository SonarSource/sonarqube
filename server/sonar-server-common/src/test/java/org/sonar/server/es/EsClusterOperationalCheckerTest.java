/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.es;

import co.elastic.clients.elasticsearch._types.HealthStatus;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.sonar.api.impl.utils.TestSystem2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EsClusterOperationalCheckerTest {

  private final EsClient esClient = mock(EsClient.class);
  private final TestSystem2 system2 = new TestSystem2().setNow(1_000L);
  private final EsClusterOperationalChecker underTest = new EsClusterOperationalChecker(esClient, system2);

  @Test
  void isOperational_is_false_when_cluster_health_is_red_unknown_unavailable_or_missing() {
    stubHealth(HealthStatus.Red);
    assertThat(underTest.isOperational()).isFalse();

    system2.setNow(1_000L + EsClusterOperationalChecker.CACHE_TTL_MS);
    stubHealth(HealthStatus.Unknown);
    assertThat(underTest.isOperational()).isFalse();

    system2.setNow(1_000L + 2 * EsClusterOperationalChecker.CACHE_TTL_MS);
    stubHealth(HealthStatus.Unavailable);
    assertThat(underTest.isOperational()).isFalse();

    system2.setNow(1_000L + 3 * EsClusterOperationalChecker.CACHE_TTL_MS);
    stubHealth(null);
    assertThat(underTest.isOperational()).isFalse();
  }

  @Test
  void isOperational_is_false_when_elasticsearch_cannot_be_reached() {
    when(esClient.clusterHealthStatus(any())).thenThrow(new RuntimeException("connection refused"));

    assertThat(underTest.isOperational()).isFalse();
  }

  @Test
  void isOperational_reuses_the_previous_result_until_the_cache_expires() {
    stubHealth(HealthStatus.Green);

    assertThat(underTest.isOperational()).isTrue();
    system2.setNow(1_000L + EsClusterOperationalChecker.CACHE_TTL_MS - 1);
    assertThat(underTest.isOperational()).isTrue();

    verify(esClient, times(1)).clusterHealthStatus(EsClusterOperationalChecker.HEALTH_HTTP_TIMEOUT);

    system2.setNow(1_000L + EsClusterOperationalChecker.CACHE_TTL_MS);
    stubHealth(HealthStatus.Red);
    assertThat(underTest.isOperational()).isFalse();

    verify(esClient, times(2)).clusterHealthStatus(EsClusterOperationalChecker.HEALTH_HTTP_TIMEOUT);
  }

  @Test
  void isOperational_returns_false_to_other_callers_while_an_expired_cache_is_refreshing() throws Exception {
    stubHealth(HealthStatus.Green);
    assertThat(underTest.isOperational()).isTrue();
    system2.setNow(1_000L + EsClusterOperationalChecker.CACHE_TTL_MS);

    CountDownLatch queryStarted = new CountDownLatch(1);
    CountDownLatch releaseQuery = new CountDownLatch(1);
    when(esClient.clusterHealthStatus(any())).thenAnswer(invocation -> {
      queryStarted.countDown();
      assertThat(releaseQuery.await(5, TimeUnit.SECONDS)).isTrue();
      return HealthStatus.Red;
    });

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<Boolean> refresh = executor.submit(underTest::isOperational);
      assertThat(queryStarted.await(5, TimeUnit.SECONDS)).isTrue();

      assertThat(underTest.isOperational()).isFalse();
      verify(esClient, times(2)).clusterHealthStatus(any());

      releaseQuery.countDown();
      assertThat(refresh.get(5, TimeUnit.SECONDS)).isFalse();
    } finally {
      releaseQuery.countDown();
      executor.shutdownNow();
    }
  }

  private void stubHealth(HealthStatus status) {
    when(esClient.clusterHealthStatus(any())).thenReturn(status);
  }

}
