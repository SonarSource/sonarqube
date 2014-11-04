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

package org.sonar.server.search.request;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.profiling.StopWatch;

public class ProxyGetRequestBuilder extends GetRequestBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProxyGetRequestBuilder.class);

  private final Profiling profiling;

  public ProxyGetRequestBuilder(Client client, Profiling profiling) {
    super(client);
    this.profiling = profiling;
  }

  @Override
  public GetResponse get() {
    StopWatch fullProfile = profiling.start("get", Profiling.Level.FULL);
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
  public GetResponse get(TimeValue timeout) throws ElasticsearchException {
    throw new IllegalStateException("Not yet implemented");
  }

  @Override
  public GetResponse get(String timeout) throws ElasticsearchException {
    throw new IllegalStateException("Not yet implemented");
  }

  @Override
  public ListenableActionFuture<GetResponse> execute() {
    throw new UnsupportedOperationException("execute() should not be called as it's used for asynchronous");
  }

  public String toString() {
    StringBuilder message = new StringBuilder().append("ES get request");
    message.append(String.format(" for key '%s'", request.id()));
    message.append(String.format(" on index '%s'", request.index()));
    message.append(String.format(" on type '%s'", request.type()));
    return message.toString();
  }

}
