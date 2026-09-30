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
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.System2;

public class EsClusterOperationalChecker implements EsClusterOperational {

  static final long CACHE_TTL_MS = 5_000L;
  static final Duration HEALTH_HTTP_TIMEOUT = Duration.ofSeconds(3);

  private static final Logger LOG = LoggerFactory.getLogger(EsClusterOperationalChecker.class);

  private final EsClient esClient;
  private final System2 system2;
  private final Object lock = new Object();
  private Long cachedAtMs;
  private boolean cachedOperational;
  private boolean refreshInProgress;

  public EsClusterOperationalChecker(EsClient esClient, System2 system2) {
    this.esClient = esClient;
    this.system2 = system2;
  }

  @Override
  public boolean isOperational() {
    synchronized (lock) {
      if (isFresh(system2.now())) {
        return cachedOperational;
      }
      if (refreshInProgress) {
        return false;
      }
      refreshInProgress = true;
    }

    try {
      boolean operational = queryCluster();
      synchronized (lock) {
        cachedOperational = operational;
        cachedAtMs = system2.now();
        return operational;
      }
    } finally {
      synchronized (lock) {
        refreshInProgress = false;
      }
    }
  }

  private boolean isFresh(long now) {
    return cachedAtMs != null && now >= cachedAtMs && now - cachedAtMs < CACHE_TTL_MS;
  }

  private boolean queryCluster() {
    try {
      HealthStatus status = esClient.clusterHealthStatus(HEALTH_HTTP_TIMEOUT);
      return status != null
        && status != HealthStatus.Red
        && status != HealthStatus.Unknown
        && status != HealthStatus.Unavailable;
    } catch (Exception e) {
      LOG.debug("Failed to query Elasticsearch cluster health", e);
      return false;
    }
  }

}
