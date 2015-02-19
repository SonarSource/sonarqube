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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequestBuilder;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.picocontainer.Startable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.util.ProgressLogger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Helper to bulk requests in an efficient way :
 * <ul>
 *   <li>bulk request is sent on the wire when its size is higher than 5Mb</li>
 *   <li>on large table indexing, replicas and automatic refresh can be temporarily disabled</li>
 *   <li>index refresh is optional (enabled by default)</li>
 * </ul>
 */
public class BulkIndexer implements Startable {
  private static final Logger LOGGER = Loggers.get(BulkIndexer.class);
  private static final long FLUSH_BYTE_SIZE = new ByteSizeValue(5, ByteSizeUnit.MB).bytes();
  private static final String REFRESH_INTERVAL_SETTING = "index.refresh_interval";
  private static final String ALREADY_STARTED_MESSAGE = "Bulk indexing is already started";

  private final EsClient client;
  private final String indexName;
  private boolean large = false;
  private boolean refresh = true;
  private long flushByteSize = FLUSH_BYTE_SIZE;
  private BulkRequestBuilder bulkRequest = null;
  private Map<String, Object> largeInitialSettings = null;

  private final AtomicLong counter = new AtomicLong(0L);
  private final ProgressLogger progress;

  public BulkIndexer(EsClient client, String indexName) {
    this.client = client;
    this.indexName = indexName;
    this.progress = new ProgressLogger(String.format("Progress[BulkIndexer[%s]]", indexName), counter, LOGGER)
      .setPluralLabel("requests");
  }

  /**
   * Large indexing is an heavy operation that populates an index generally from scratch. Replicas and
   * automatic refresh are disabled during bulk indexing and lucene segments are optimized at the end.
   */

  public BulkIndexer setLarge(boolean b) {
    Preconditions.checkState(bulkRequest == null, ALREADY_STARTED_MESSAGE);
    this.large = b;
    return this;
  }

  public BulkIndexer setRefresh(boolean b) {
    Preconditions.checkState(bulkRequest == null, ALREADY_STARTED_MESSAGE);
    this.refresh = b;
    return this;
  }

  /**
   * Default value is {@link org.sonar.server.es.BulkIndexer#FLUSH_BYTE_SIZE}
   * @see org.elasticsearch.common.unit.ByteSizeValue
   */
  public BulkIndexer setFlushByteSize(long l) {
    this.flushByteSize = l;
    return this;
  }

  @Override
  public void start() {
    Preconditions.checkState(bulkRequest == null, ALREADY_STARTED_MESSAGE);
    if (large) {
      largeInitialSettings = Maps.newHashMap();
      Map<String, Object> bulkSettings = Maps.newHashMap();
      GetSettingsResponse settingsResp = client.nativeClient().admin().indices().prepareGetSettings(indexName).get();

      // deactivate replicas
      int initialReplicas = Integer.parseInt(settingsResp.getSetting(indexName, IndexMetaData.SETTING_NUMBER_OF_REPLICAS));
      if (initialReplicas > 0) {
        largeInitialSettings.put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, initialReplicas);
        bulkSettings.put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0);
      }

      // deactivate periodical refresh
      String refreshInterval = settingsResp.getSetting(indexName, REFRESH_INTERVAL_SETTING);
      largeInitialSettings.put(REFRESH_INTERVAL_SETTING, refreshInterval);
      bulkSettings.put(REFRESH_INTERVAL_SETTING, "-1");

      updateSettings(bulkSettings);
    }
    bulkRequest = client.prepareBulk();
    counter.set(0L);
    progress.start();
  }

  public void add(ActionRequest request) {
    bulkRequest.request().add(request);
    counter.getAndIncrement();
    if (bulkRequest.request().estimatedSizeInBytes() >= flushByteSize) {
      executeBulk(bulkRequest);
      bulkRequest = client.prepareBulk();
    }
  }

  @Override
  public void stop() {
    try {
      if (bulkRequest.numberOfActions() > 0) {
        executeBulk(bulkRequest);
      }
    } finally {
      progress.stop();
    }

    if (refresh) {
      client.prepareRefresh(indexName).get();
    }
    if (large) {
      // optimize lucene segments and revert index settings
      // Optimization must be done before re-applying replicas:
      // http://www.elasticsearch.org/blog/performance-considerations-elasticsearch-indexing/
      client.prepareOptimize(indexName).get();

      updateSettings(largeInitialSettings);
    }
    bulkRequest = null;
  }

  private void updateSettings(Map<String, Object> settings) {
    UpdateSettingsRequestBuilder req = client.nativeClient().admin().indices().prepareUpdateSettings(indexName);
    req.setSettings(settings);
    req.get();
  }

  private void executeBulk(BulkRequestBuilder bulkRequest) {
    List<ActionRequest> retries = Lists.newArrayList();
    BulkResponse response = bulkRequest.get();

    for (BulkItemResponse item : response.getItems()) {
      if (item.isFailed()) {
        ActionRequest retry = bulkRequest.request().requests().get(item.getItemId());
        retries.add(retry);
      }
    }

    if (!retries.isEmpty()) {
      LOGGER.warn(String.format("%d index requests failed. Trying again.", retries.size()));
      BulkRequestBuilder retryBulk = client.prepareBulk();
      for (ActionRequest retry : retries) {
        retryBulk.request().add(retry);
      }
      BulkResponse retryBulkResponse = retryBulk.get();
      if (retryBulkResponse.hasFailures()) {
        LOGGER.error("New attempt to index documents failed");
        for (int index = 0; index < retryBulkResponse.getItems().length; index++) {
          BulkItemResponse item = retryBulkResponse.getItems()[index];
          if (item.isFailed()) {
            StringBuilder sb = new StringBuilder();
            String msg = sb.append("\n[").append(index)
              .append("]: index [").append(item.getIndex()).append("], type [").append(item.getType()).append("], id [").append(item.getId())
              .append("], message [").append(item.getFailureMessage()).append("]").toString();
            LOGGER.error(msg);
          }
        }
      } else {
        LOGGER.info("New index attempt succeeded");
      }
    }
  }
}
