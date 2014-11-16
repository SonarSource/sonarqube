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
package org.sonar.server.es;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequestBuilder;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequestBuilder;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.sonar.server.search.SearchClient;

import java.util.Iterator;

/**
 *
 */
public class BulkIndexer {

  private static final long FLUSH_BYTE_SIZE = new ByteSizeValue(5, ByteSizeUnit.MB).bytes();
  
  private final SearchClient client;

  public BulkIndexer(SearchClient client) {
    this.client = client;
  }

  /**
   * Heavy operation that populates an index from scratch. Replicas are disabled during
   * the bulk indexation and lucene segments are optimized at the end. No need
   * to call {@link #refresh(String)} after this method.
   * 
   * @see BulkIndexRequestIterator
   */
  public void fullIndex(String index, Iterator<ActionRequest> requests) {
    // deactivate replicas
    GetSettingsRequestBuilder replicaRequest = client.admin().indices().prepareGetSettings(index);
    String initialRequestSetting = replicaRequest.get().getSetting(index, IndexMetaData.SETTING_NUMBER_OF_REPLICAS);
    int initialReplicas = Integer.parseInt(StringUtils.defaultIfEmpty(initialRequestSetting, "0"));
    if (initialReplicas > 0) {
      setNumberOfReplicas(index, 0);
    }

    index(requests);
    refresh(index);
    optimize(index);

    if (initialReplicas > 0) {
      // re-enable replicas
      setNumberOfReplicas(index, initialReplicas);
    }
  }

  private void setNumberOfReplicas(String index, int replicas) {
    UpdateSettingsRequestBuilder req = client.admin().indices().prepareUpdateSettings(index);
    req.setSettings(ImmutableMap.<String, Object>of(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, String.valueOf(replicas)));
    req.get();
  }

  /**
   * @see BulkIndexRequestIterator
   */
  public void index(Iterator<ActionRequest> requests) {
    BulkRequest bulkRequest = client.prepareBulk().request();
    while (requests.hasNext()) {
      ActionRequest request = requests.next();
      bulkRequest.add(request);
      if (bulkRequest.estimatedSizeInBytes() >= FLUSH_BYTE_SIZE) {
        executeBulk(bulkRequest);
        bulkRequest = client.prepareBulk().request();
      }
    }
    if (bulkRequest.numberOfActions() > 0) {
      executeBulk(bulkRequest);
    }
  }

  private void executeBulk(BulkRequest bulkRequest) {
    try {
      BulkResponse response = client.bulk(bulkRequest).get();

      // TODO check failures
      // WARNING - complexity of response#hasFailures() and #buildFailureMessages() is O(n)
    } catch (Exception e) {
      throw new IllegalStateException("TODO", e);
    }
  }

  public void refresh(String index) {
    client.prepareRefresh(index).get();
  }

  private void optimize(String index) {
    client.admin().indices().prepareOptimize(index)
      .setMaxNumSegments(1)
      .setWaitForMerge(true)
      .get();
  }

}
