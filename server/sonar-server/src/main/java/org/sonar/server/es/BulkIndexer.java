/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.es;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequestBuilder;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.picocontainer.Startable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.ProgressLogger;

import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;

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
  private static final long FLUSH_BYTE_SIZE = new ByteSizeValue(1, ByteSizeUnit.MB).bytes();
  private static final String REFRESH_INTERVAL_SETTING = "index.refresh_interval";
  private static final String ALREADY_STARTED_MESSAGE = "Bulk indexing is already started";

  private final EsClient client;
  private final String indexName;
  private boolean large = false;
  private long flushByteSize = FLUSH_BYTE_SIZE;
  private boolean disableRefresh = false;
  private BulkRequestBuilder bulkRequest = null;
  private Map<String, Object> largeInitialSettings = null;
  private final AtomicLong counter = new AtomicLong(0L);
  private final int concurrentRequests;
  private final Semaphore semaphore;
  private final ProgressLogger progress;

  public BulkIndexer(EsClient client, String indexName) {
    this.client = client;
    this.indexName = indexName;
    this.progress = new ProgressLogger(format("Progress[BulkIndexer[%s]]", indexName), counter, LOGGER)
      .setPluralLabel("requests");

    this.concurrentRequests = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    this.semaphore = new Semaphore(concurrentRequests);
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

  public BulkIndexer setFlushByteSize(long flushByteSize) {
    this.flushByteSize = flushByteSize;
    return this;
  }

  /**
   * By default refresh of index is executed in method {@link #stop()}. Set to true
   * to disable refresh.
   */
  public BulkIndexer setDisableRefresh(boolean b) {
    this.disableRefresh = b;
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
    bulkRequest = client.prepareBulk().setRefresh(false);
    counter.set(0L);
    progress.start();
  }

  public void add(ActionRequest request) {
    bulkRequest.request().add(request);
    if (bulkRequest.request().estimatedSizeInBytes() >= flushByteSize) {
      executeBulk();
    }
  }

  public void addDeletion(SearchRequestBuilder searchRequest) {
    searchRequest
      .setScroll(TimeValue.timeValueMinutes(5))
      .setSearchType(SearchType.SCAN)
      .setSize(100)
      // load only doc ids, not _source fields
      .setFetchSource(false);

    // this search is synchronous. An optimization would be to be non-blocking,
    // but it requires to tracking pending requests in close().
    // Same semaphore can't be reused because of potential deadlock (requires to acquire
    // two locks)
    SearchResponse searchResponse = searchRequest.get();

    while (true) {
      searchResponse = client.prepareSearchScroll(searchResponse.getScrollId())
        .setScroll(TimeValue.timeValueMinutes(5))
        .get();
      SearchHit[] hits = searchResponse.getHits().getHits();
      for (SearchHit hit : hits) {
        add(client.prepareDelete(hit.index(), hit.type(), hit.getId()).request());
      }
      if (hits.length == 0) {
        break;
      }
    }
  }

  /**
   * Delete all the documents matching the given search request. This method is blocking.
   * Index is refreshed, so docs are not searchable as soon as method is executed.
   */
  public static void delete(EsClient client, String indexName, SearchRequestBuilder searchRequest) {
    BulkIndexer bulk = new BulkIndexer(client, indexName);
    bulk.start();
    bulk.addDeletion(searchRequest);
    bulk.stop();
  }

  @Override
  public void stop() {
    if (bulkRequest.numberOfActions() > 0) {
      executeBulk();
    }
    try {
      if (semaphore.tryAcquire(concurrentRequests, 10, TimeUnit.MINUTES)) {
        semaphore.release(concurrentRequests);
      }
    } catch (InterruptedException e) {
      throw new IllegalStateException("Elasticsearch bulk requests still being executed after 10 minutes", e);
    }
    progress.stop();

    if (!disableRefresh) {
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

  private void executeBulk() {
    final BulkRequestBuilder req = this.bulkRequest;
    this.bulkRequest = client.prepareBulk().setRefresh(false);
    semaphore.acquireUninterruptibly();
    req.execute(new BulkResponseActionListener(req));
  }

  private class BulkResponseActionListener implements ActionListener<BulkResponse> {
    private final BulkRequestBuilder req;

    public BulkResponseActionListener(BulkRequestBuilder req) {
      this.req = req;
    }

    @Override
    public void onResponse(BulkResponse response) {
      semaphore.release();
      counter.addAndGet(response.getItems().length);

      for (BulkItemResponse item : response.getItems()) {
        if (item.isFailed()) {
          LOGGER.error("index [{}], type [{}], id [{}], message [{}]", item.getIndex(), item.getType(), item.getId(), item.getFailureMessage());
        }
      }
    }

    @Override
    public void onFailure(Throwable e) {
      semaphore.release();
      LOGGER.error("Fail to execute bulk index request: " + req, e);
    }
  }
}
