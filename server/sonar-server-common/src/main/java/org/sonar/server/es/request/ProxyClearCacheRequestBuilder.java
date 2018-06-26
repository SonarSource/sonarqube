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
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheAction;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheRequestBuilder;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.sonar.api.utils.log.Profiler;
import org.sonar.server.es.EsClient;

public class ProxyClearCacheRequestBuilder extends ClearIndicesCacheRequestBuilder {

  public ProxyClearCacheRequestBuilder(Client client) {
    super(client.admin().indices(), ClearIndicesCacheAction.INSTANCE);
  }

  @Override
  public ClearIndicesCacheResponse get() {
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
  public ClearIndicesCacheResponse get(TimeValue timeout) {
    throw new IllegalStateException("Not yet implemented");
  }

  @Override
  public ClearIndicesCacheResponse get(String timeout) {
    throw new IllegalStateException("Not yet implemented");
  }

  @Override
  public ListenableActionFuture<ClearIndicesCacheResponse> execute() {
    throw new UnsupportedOperationException("execute() should not be called as it's used for asynchronous");
  }

  @Override
  public String toString() {
    StringBuilder message = new StringBuilder();
    message.append("ES clear cache request");
    if (request.indices().length > 0) {
      message.append(String.format(" on indices '%s'", StringUtils.join(request.indices(), ",")));
    }
    String[] fields = request.fields();
    if (fields != null && fields.length > 0) {
      message.append(String.format(" on fields '%s'", StringUtils.join(fields, ",")));
    }
    if (request.queryCache()) {
      message.append(" with filter cache");
    }
    if (request.fieldDataCache()) {
      message.append(" with field data cache");
    }
    if (request.requestCache()) {
      message.append(" with request cache");
    }
    return message.toString();
  }
}
