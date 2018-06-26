/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.es.request;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsAction;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsRequestBuilder;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.sonar.api.utils.log.Profiler;
import org.sonar.server.es.EsClient;

public class ProxyClusterStatsRequestBuilder extends ClusterStatsRequestBuilder {

  public ProxyClusterStatsRequestBuilder(Client client) {
    super(client.admin().cluster(), ClusterStatsAction.INSTANCE);
  }

  @Override
  public ClusterStatsResponse get() {
    Profiler profiler = Profiler.createIfTrace(EsClient.LOGGER).start();
    try {
      return super.execute().actionGet();
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Fail to execute %s", toString()), e);
    } finally {
      if (profiler.isTraceEnabled()) {
        profiler.stopTrace(toString());
      }
    }
  }

  @Override
  public ClusterStatsResponse get(TimeValue timeout) {
    throw new IllegalStateException("Not yet implemented");
  }

  @Override
  public ClusterStatsResponse get(String timeout) {
    throw new IllegalStateException("Not yet implemented");
  }

  @Override
  public ListenableActionFuture<ClusterStatsResponse> execute() {
    throw new UnsupportedOperationException("execute() should not be called as it's used for asynchronous");
  }

  @Override
  public String toString() {
    StringBuilder message = new StringBuilder();
    message.append("ES cluster stats request");
    if (request.nodesIds() != null) {
      message.append(String.format(" on nodes '%s'", StringUtils.join(request.nodesIds(), ",")));
    }
    return message.toString();
  }
}
