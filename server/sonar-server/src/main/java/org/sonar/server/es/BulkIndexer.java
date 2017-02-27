/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.es;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequestBuilder;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkProcessor.Listener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.sort.SortOrder;
import org.picocontainer.Startable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.ProgressLogger;

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
  private Size size = Size.REGULAR;
  private long flushByteSize = FLUSH_BYTE_SIZE;
  private BulkProcessor bulkRequest = null;
  private Map<String, Object> largeInitialSettings = null;
  private final AtomicLong counter = new AtomicLong(0L);
  private final ProgressLogger progress;

  public BulkIndexer(EsClient client, String indexName) {
    this.client = client;
    this.indexName = indexName;
    this.progress = new ProgressLogger(format("Progress[BulkIndexer[%s]]", indexName), counter, LOGGER)
      .setPluralLabel("requests");
  }

  public enum Size {
    /** Use this size for a limited number of documents. */
    REGULAR {

      @Override
      int getConcurrentRequests() {
        // do not parallalize, send request one after another
        return 0;
      }
    },

    /** Use this size for initial indexing and if you expect unusual huge numbers of documents. */
    LARGE {

      @Override
      void beforeStart(BulkIndexer bulkIndexer) {
        bulkIndexer.largeInitialSettings = Maps.newHashMap();
        Map<String, Object> bulkSettings = Maps.newHashMap();
        GetSettingsResponse settingsResp = bulkIndexer.client.nativeClient().admin().indices().prepareGetSettings(bulkIndexer.indexName).get();

        // deactivate replicas
        int initialReplicas = Integer.parseInt(settingsResp.getSetting(bulkIndexer.indexName, IndexMetaData.SETTING_NUMBER_OF_REPLICAS));
        if (initialReplicas > 0) {
          bulkIndexer.largeInitialSettings.put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, initialReplicas);
          bulkSettings.put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0);
        }

        // deactivate periodical refresh
        String refreshInterval = settingsResp.getSetting(bulkIndexer.indexName, REFRESH_INTERVAL_SETTING);
        bulkIndexer.largeInitialSettings.put(REFRESH_INTERVAL_SETTING, refreshInterval);
        bulkSettings.put(REFRESH_INTERVAL_SETTING, "-1");

        updateSettings(bulkIndexer, bulkSettings);
      }

      @Override
      int getConcurrentRequests() {
        return Runtime.getRuntime().availableProcessors() / 5;
      }

      @Override
      void afterStop(BulkIndexer bulkIndexer) {
        // optimize lucene segments and revert index settings
        // Optimization must be done before re-applying replicas:
        // http://www.elasticsearch.org/blog/performance-considerations-elasticsearch-indexing/
        bulkIndexer.client.prepareForceMerge(bulkIndexer.indexName).get();

        updateSettings(bulkIndexer, bulkIndexer.largeInitialSettings);
      }

      private void updateSettings(BulkIndexer bulkIndexer, Map<String, Object> settings) {
        UpdateSettingsRequestBuilder req = bulkIndexer.client.nativeClient().admin().indices().prepareUpdateSettings(bulkIndexer.indexName);
        req.setSettings(settings);
        req.get();
      }
    };

    void beforeStart(BulkIndexer bulkIndexer) {
      // can be overwritten
    }

    /** @see https://jira.sonarsource.com/browse/SONAR-8075 */
    abstract int getConcurrentRequests();

    void afterStop(BulkIndexer bulkIndexer) {
      // can be overwritten
    }
  }

  /**
   * Large indexing is an heavy operation that populates an index generally from scratch. Replicas and
   * automatic refresh are disabled during bulk indexing and lucene segments are optimized at the end.
   */
  public BulkIndexer setSize(Size size) {
    Preconditions.checkState(bulkRequest == null, ALREADY_STARTED_MESSAGE);
    this.size = size;
    return this;
  }

  public BulkIndexer setFlushByteSize(long flushByteSize) {
    this.flushByteSize = flushByteSize;
    return this;
  }

  @Override
  public void start() {
    Preconditions.checkState(bulkRequest == null, ALREADY_STARTED_MESSAGE);
    size.beforeStart(this);
    bulkRequest = client.prepareBulkProcessor(new BulkProcessorListener())
      .setBulkSize(new ByteSizeValue(flushByteSize))
      .setConcurrentRequests(size.getConcurrentRequests())
      .build();
    counter.set(0L);
    progress.start();
  }

  public void add(ActionRequest<?> request) {
    bulkRequest.add(request);
  }

  public void addDeletion(SearchRequestBuilder searchRequest) {
    searchRequest
      .addSort("_doc", SortOrder.ASC)
      .setScroll(TimeValue.timeValueMinutes(5))
      .setSize(100)
      // load only doc ids, not _source fields
      .setFetchSource(false);

    // this search is synchronous. An optimization would be to be non-blocking,
    // but it requires to tracking pending requests in close().
    // Same semaphore can't be reused because of potential deadlock (requires to acquire
    // two locks)
    SearchResponse searchResponse = searchRequest.get();

    while (true) {
      SearchHit[] hits = searchResponse.getHits().getHits();
      for (SearchHit hit : hits) {
        SearchHitField routing = hit.field("_routing");
        DeleteRequestBuilder deleteRequestBuilder = client.prepareDelete(hit.index(), hit.type(), hit.getId());
        if (routing != null) {
          deleteRequestBuilder.setRouting(routing.getValue());
        }
        add(deleteRequestBuilder.request());
      }

      String scrollId = searchResponse.getScrollId();
      searchResponse = client.prepareSearchScroll(scrollId).setScroll(TimeValue.timeValueMinutes(5)).get();
      if (hits.length == 0) {
        client.nativeClient().prepareClearScroll().addScrollId(scrollId).get();
        break;
      }
    }
  }

  public void addDeletion(IndexType indexType, String id) {
    add(client.prepareDelete(indexType, id).request());
  }

  public void addDeletion(IndexType indexType, String id, String routing) {
    add(client.prepareDelete(indexType, id).setRouting(routing).request());
  }

  /**
   * Delete all the documents matching the given search request. This method is blocking.
   * Index is refreshed, so docs are not searchable as soon as method is executed.
   *
   * Note that the parameter indexName could be removed if progress logs are not needed.
   */
  public static void delete(EsClient client, String indexName, SearchRequestBuilder searchRequest) {
    BulkIndexer bulk = new BulkIndexer(client, indexName);
    bulk.start();
    bulk.addDeletion(searchRequest);
    bulk.stop();
  }

  @Override
  public void stop() {
    try {
      bulkRequest.awaitClose(10, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      throw new IllegalStateException("Elasticsearch bulk requests still being executed after 10 minutes", e);
    }
    progress.stop();
    client.prepareRefresh(indexName).get();
    size.afterStop(this);
    bulkRequest = null;
  }

  private final class BulkProcessorListener implements Listener {

    @Override
    public void beforeBulk(long executionId, BulkRequest request) {
      // no action required
    }

    @Override
    public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
      counter.addAndGet(response.getItems().length);

      for (BulkItemResponse item : response.getItems()) {
        if (item.isFailed()) {
          LOGGER.error("index [{}], type [{}], id [{}], message [{}]", item.getIndex(), item.getType(), item.getId(), item.getFailureMessage());
        }
      }
    }

    @Override
    public void afterBulk(long executionId, BulkRequest req, Throwable e) {
      LOGGER.error("Fail to execute bulk index request: " + req, e);
    }
  }
}
