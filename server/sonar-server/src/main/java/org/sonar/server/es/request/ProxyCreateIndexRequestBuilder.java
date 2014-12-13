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

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.profiling.StopWatch;

public class ProxyCreateIndexRequestBuilder extends CreateIndexRequestBuilder {

  private final Profiling profiling;
  private final String index;

  public ProxyCreateIndexRequestBuilder(Client client, Profiling profiling, String index) {
    super(client.admin().indices(), index);
    this.profiling = profiling;
    this.index = index;
  }

  @Override
  public CreateIndexResponse get() {
    StopWatch fullProfile = profiling.start("create index", Profiling.Level.FULL);
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
  public CreateIndexResponse get(TimeValue timeout) {
    throw new IllegalStateException("Not yet implemented");
  }

  @Override
  public CreateIndexResponse get(String timeout) {
    throw new IllegalStateException("Not yet implemented");
  }

  @Override
  public ListenableActionFuture<CreateIndexResponse> execute() {
    throw new UnsupportedOperationException("execute() should not be called as it's used for asynchronous");
  }

  @Override
  public String toString() {
    return String.format("ES create index '%s'", index);
  }
}
