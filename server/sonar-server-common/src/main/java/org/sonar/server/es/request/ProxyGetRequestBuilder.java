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

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.get.GetAction;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.sonar.api.utils.log.Profiler;
import org.sonar.server.es.EsClient;

public class ProxyGetRequestBuilder extends GetRequestBuilder {

  public ProxyGetRequestBuilder(Client client) {
    super(client, GetAction.INSTANCE);
  }

  @Override
  public GetResponse get() {
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
  public GetResponse get(TimeValue timeout) {
    throw new IllegalStateException("Not yet implemented");
  }

  @Override
  public GetResponse get(String timeout) {
    throw new IllegalStateException("Not yet implemented");
  }

  @Override
  public ListenableActionFuture<GetResponse> execute() {
    throw new UnsupportedOperationException("execute() should not be called as it's used for asynchronous");
  }

  @Override
  public String toString() {
    StringBuilder message = new StringBuilder().append("ES get request");
    message.append(String.format(" for key '%s'", request.id()));
    message.append(String.format(" on index '%s'", request.index()));
    message.append(String.format(" on type '%s'", request.type()));
    return message.toString();
  }

}
