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
package org.sonar.server.search;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.core.profiling.Profiling;
import org.sonar.server.search.action.IndexActionRequest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class IndexQueue extends LinkedBlockingQueue<Runnable>
  implements ServerComponent, WorkQueue<IndexActionRequest> {

  protected final Profiling profiling;

  private final SearchClient searchClient;
  private final ComponentContainer container;

  private static final Logger LOGGER = LoggerFactory.getLogger(IndexQueue.class);

  private static final Integer DEFAULT_QUEUE_SIZE = 200;
  private static final int TIMEOUT = 30000;

  public IndexQueue(Settings settings, SearchClient searchClient, ComponentContainer container) {
    super(DEFAULT_QUEUE_SIZE);
    this.searchClient = searchClient;
    this.container = container;
    this.profiling = new Profiling(settings);
  }

  @Override
  public void enqueue(List<IndexActionRequest> actions) {

    if (actions.isEmpty()) {
      return;
    }
    try {

      long normTime = System.currentTimeMillis();
      BulkRequestBuilder bulkRequestBuilder = new BulkRequestBuilder(searchClient);
      Map<String, Index> indexes = getIndexMap();
      Set<String> indices = new HashSet<String>();
      for (IndexActionRequest action : actions) {
        Index index = indexes.get(action.getIndexType());
        action.setIndex(index);
        indices.add(index.getIndexName());
      }

      ExecutorService executorService = Executors.newFixedThreadPool(10);

      // Do we need to refresh
      boolean requiresRefresh = false;
      for (IndexActionRequest action : actions) {
        if (action.needsRefresh()) {
          requiresRefresh = true;
          break;
        }
      }

      //invokeAll() blocks until ALL tasks submitted to executor complete
      for (Future<List<ActionRequest>> updateRequests : executorService.invokeAll(actions)) {
        for (ActionRequest update : updateRequests.get()) {
          if (UpdateRequest.class.isAssignableFrom(update.getClass())) {
            bulkRequestBuilder.add(((UpdateRequest) update).refresh(false));
          } else if (DeleteRequest.class.isAssignableFrom(update.getClass())) {
            bulkRequestBuilder.add(((DeleteRequest) update).refresh(false));
          } else {
            throw new IllegalStateException("Un-managed request type: " + update.getClass());
          }
        }
      }
      executorService.shutdown();
      normTime = System.currentTimeMillis() - normTime;

      //execute the request
      long indexTime = System.currentTimeMillis();
      BulkResponse response = searchClient.execute(bulkRequestBuilder.setRefresh(false));
      indexTime = System.currentTimeMillis() - indexTime;

      long refreshTime = System.currentTimeMillis();
      if (requiresRefresh) {
        searchClient.admin().indices().prepareRefresh(indices.toArray(new String[indices.size()])).setForce(false).get();
      }
      refreshTime = System.currentTimeMillis() - refreshTime;

      LOGGER.debug("-- submitted {} items with {}ms in normalization, {}ms indexing and {}ms refresh({}). Total: {}ms",
        bulkRequestBuilder.numberOfActions(), normTime, indexTime, refreshTime, indices, (normTime + indexTime + refreshTime));

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private Map<String, Index> getIndexMap() {
    Map<String, Index> indexes = new HashMap<String, Index>();
    for (Index index : container.getComponentsByType(Index.class)) {
      indexes.put(index.getIndexType(), index);
    }
    return indexes;
  }
}
