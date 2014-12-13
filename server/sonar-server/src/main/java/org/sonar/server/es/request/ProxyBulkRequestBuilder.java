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

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.profiling.StopWatch;

public class ProxyBulkRequestBuilder extends BulkRequestBuilder {

  private final Profiling profiling;

  public ProxyBulkRequestBuilder(Client client, Profiling profiling) {
    super(client);
    this.profiling = profiling;
  }

  @Override
  public BulkResponse get() {
    StopWatch fullProfile = profiling.start("bulk", Profiling.Level.FULL);
    try {
      return super.execute().actionGet();
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Fail to execute %s", toString()), e);
    } finally {
      if (profiling.isProfilingEnabled(Profiling.Level.BASIC)) {
        fullProfile.stop("%s", toString());
      }
    }
  }

  @Override
  public BulkResponse get(TimeValue timeout) {
    throw unsupported();
  }

  @Override
  public BulkResponse get(String timeout) {
    // easy to implement if needed (copy get())
    throw unsupported();
  }

  @Override
  public ListenableActionFuture<BulkResponse> execute() {
    throw new UnsupportedOperationException("execute() should not be called as it's used for asynchronous");
  }

  private UnsupportedOperationException unsupported() {
    throw new UnsupportedOperationException("See " + ProxyBulkRequestBuilder.class.getName());
  }

  @Override
  public String toString() {
    StringBuilder message = new StringBuilder();
    message.append("Bulk[");
    for (int i=0 ; i<request.requests().size() ; i++) {
      ActionRequest item = request.requests().get(i);
      if (item instanceof IndexRequest) {
        IndexRequest request = (IndexRequest) item;
        message.append(String.format("index id '%s' in %s/%s", request.id(), request.index(), request.type()));
      } else if (item instanceof UpdateRequest) {
        UpdateRequest request = (UpdateRequest) item;
        message.append(String.format("update id '%s' in %s/%s", request.id(), request.index(), request.type()));
      } else if (item instanceof DeleteRequest) {
        DeleteRequest request = (DeleteRequest) item;
        message.append(String.format("delete id '%s' from %s/%s", request.id(), request.index(), request.type()));
      }
      if (i < request.requests().size()-1) {
        message.append(", ");
      }
    }
    message.append("]");
    return message.toString();
  }
}
