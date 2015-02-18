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

package org.sonar.server.es.request;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilder;
import org.sonar.api.utils.log.Profiler;
import org.sonar.server.es.EsClient;

import java.io.IOException;

public class ProxyDeleteByQueryRequestBuilder extends DeleteByQueryRequestBuilder {

  private QueryBuilder internalBuilder;

  public ProxyDeleteByQueryRequestBuilder(Client client) {
    super(client);
  }

  @Override
  public DeleteByQueryResponse get() {
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
  public DeleteByQueryResponse get(TimeValue timeout) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public DeleteByQueryResponse get(String timeout) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public ListenableActionFuture<DeleteByQueryResponse> execute() {
    throw new UnsupportedOperationException("execute() should not be called as it's used for asynchronous");
  }

  @Override
  public DeleteByQueryRequestBuilder setQuery(QueryBuilder queryBuilder) {
    this.internalBuilder = queryBuilder;
    return super.setQuery(queryBuilder);
  }

  @Override
  public String toString() {
    StringBuilder message = new StringBuilder();
    message.append(String.format("ES delete by query request '%s'", xContentToString(internalBuilder)));
    if (request.indices().length > 0) {
      message.append(String.format(" on indices '%s'", StringUtils.join(request.indices(), ",")));
    }
    return message.toString();
  }

  private String xContentToString(ToXContent toXContent) {
    if (internalBuilder == null) {
      return "";
    }
    try {
      XContentBuilder builder = XContentFactory.jsonBuilder();
      toXContent.toXContent(builder, ToXContent.EMPTY_PARAMS);
      return builder.string();
    } catch (IOException e) {
      throw new IllegalStateException("Fail to convert request to string", e);
    }
  }
}
