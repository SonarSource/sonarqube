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
package org.sonar.server.search.action;


import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.sonar.core.cluster.ClusterAction;
import org.sonar.server.search.Index;

import java.util.ArrayList;
import java.util.List;

public abstract class IndexActionRequest implements ClusterAction<List<ActionRequest>> {

  protected final String indexType;
  private final boolean requiresRefresh;
  private Index index;

  protected IndexActionRequest(String indexType) {
    this(indexType, true);
  }

  protected IndexActionRequest(String indexType, boolean requiresRefresh) {
    super();
    this.indexType = indexType;
    this.requiresRefresh = requiresRefresh;
  }

  public abstract String getKey();

  public abstract Class<?> getPayloadClass();

  public String getIndexType() {
    return indexType;
  }


  public void setIndex(Index index) {
    this.index = index;
  }

  @Override
  public final List<ActionRequest> call() throws Exception {
    if (index == null) {
      throw new IllegalStateException("Cannot execute request - Index is null");
    }
    List<ActionRequest> finalRequests = new ArrayList<ActionRequest>();
    for (ActionRequest request : doCall(index)) {
      if (request.getClass().isAssignableFrom(UpdateRequest.class)) {
        ((UpdateRequest) request)
          .type(index.getIndexType())
          .index(index.getIndexName())
          .refresh(false);
      }
      finalRequests.add(request);
    }
    return finalRequests;
  }

  public abstract List<ActionRequest> doCall(Index index) throws Exception;

  public boolean needsRefresh() {
    return this.requiresRefresh;
  }
}
