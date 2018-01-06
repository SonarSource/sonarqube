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
package org.sonar.server.es;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequestBuilder;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkProcessor.Listener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.ProgressLogger;

import static java.lang.String.format;

/**
 * Helper to bulk requests in an efficient way :
 * <ul>
 *   <li>bulk request is sent on the wire when its size is higher than 5Mb</li>
 *   <li>on large table indexing, replicas and automatic refresh can be temporarily disabled</li>
 * </ul>
 */
public class BulkIndexer {

  private static final Logger LOGGER = Loggers.get(BulkIndexer.class);
  private static final ByteSizeValue FLUSH_BYTE_SIZE = new ByteSizeValue(1, ByteSizeUnit.MB);
  private static final int FLUSH_ACTIONS = -1;
  private static final String REFRESH_INTERVAL_SETTING = "index.refresh_interval";
  private static final int DEFAULT_NUMBER_OF_SHARDS = 5;

  private final EsClient client;
  private final IndexType indexType;
  private final BulkProcessor bulkProcessor;
  private final IndexingResult result = new IndexingResult();
  private final IndexingListener indexingListener;
  private final SizeHandler sizeHandler;

  public BulkIndexer(EsClient client, IndexType indexType, Size size) {
    this(client, indexType, size, IndexingListener.FAIL_ON_ERROR);
  }

  public BulkIndexer(EsClient client, IndexType indexType, Size size, IndexingListener indexingListener) {
    this.client = client;
    this.indexType = indexType;
    this.sizeHandler = size.createHandler(Runtime2.INSTANCE);
    this.indexingListener = indexingListener;
    BulkProcessorListener bulkProcessorListener = new BulkProcessorListener();
    this.bulkProcessor = BulkProcessor.builder(client.nativeClient(), bulkProcessorListener)
      .setBackoffPolicy(BackoffPolicy.exponentialBackoff())
      .setBulkSize(FLUSH_BYTE_SIZE)
      .setBulkActions(FLUSH_ACTIONS)
      .setConcurrentRequests(sizeHandler.getConcurrentRequests())
      .build();
  }

  public IndexType getIndexType() {
    return indexType;
  }

  public void start() {
    result.clear();
    sizeHandler.beforeStart(this);
  }

  /**
   * @return the number of documents successfully indexed
   */
  public IndexingResult stop() {
    try {
      bulkProcessor.awaitClose(1, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Elasticsearch bulk requests still being executed after 1 minute", e);
    }
    client.prepareRefresh(indexType.getIndex()).get();
    sizeHandler.afterStop(this);
    indexingListener.onFinish(result);
    return result;
  }

  public void add(IndexRequest request) {
    result.incrementRequests();
    bulkProcessor.add(request);
  }

  public void add(DeleteRequest request) {
    result.incrementRequests();
    bulkProcessor.add(request);
  }

  public void add(DocWriteRequest request) {
    result.incrementRequests();
    bulkProcessor.add(request);
  }

  public void addDeletion(SearchRequestBuilder searchRequest) {
    // TODO to be replaced by delete_by_query that is back in ES5
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
        SearchHitField routing = hit.getField("_routing");
        DeleteRequestBuilder deleteRequestBuilder = client.prepareDelete(hit.getIndex(), hit.getType(), hit.getId());
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

  public void addDeletion(IndexType indexType, String id, @Nullable String routing) {
    add(client.prepareDelete(indexType, id).setRouting(routing).request());
  }

  /**
   * Delete all the documents matching the given search request. This method is blocking.
   * Index is refreshed, so docs are not searchable as soon as method is executed.
   *
   * Note that the parameter indexType could be removed if progress logs are not needed.
   */
  public static IndexingResult delete(EsClient client, IndexType indexType, SearchRequestBuilder searchRequest) {
    BulkIndexer bulk = new BulkIndexer(client, indexType, Size.REGULAR);
    bulk.start();
    bulk.addDeletion(searchRequest);
    return bulk.stop();
  }

  private final class BulkProcessorListener implements Listener {
    @Override
    public void beforeBulk(long executionId, BulkRequest request) {
      // no action required
    }

    @Override
    public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
      List<DocId> successDocIds = new ArrayList<>();
      for (BulkItemResponse item : response.getItems()) {
        if (item.isFailed()) {
          LOGGER.error("index [{}], type [{}], id [{}], message [{}]", item.getIndex(), item.getType(), item.getId(), item.getFailureMessage());
        } else {
          result.incrementSuccess();
          successDocIds.add(new DocId(item.getIndex(), item.getType(), item.getId()));
        }
      }
      indexingListener.onSuccess(successDocIds);
    }

    @Override
    public void afterBulk(long executionId, BulkRequest req, Throwable e) {
      LOGGER.error("Fail to execute bulk index request: " + req, e);
    }
  }

  public enum Size {
    /** Use this size for a limited number of documents. */
    REGULAR {
      @Override
      SizeHandler createHandler(Runtime2 runtime2) {
        return new SizeHandler();
      }
    },

    /**
     * Large indexing is an heavy operation that populates an index generally from scratch. Replicas and
     * automatic refresh are disabled during bulk indexing and lucene segments are optimized at the end.
     * Use this size for initial indexing and if you expect unusual huge numbers of documents.
     */
    LARGE {
      @Override
      SizeHandler createHandler(Runtime2 runtime2) {
        return new LargeSizeHandler(runtime2);
      }
    };

    abstract SizeHandler createHandler(Runtime2 runtime2);
  }

  @VisibleForTesting
  static class Runtime2 {
    private static final Runtime2 INSTANCE = new Runtime2();

    int getCores() {
      return Runtime.getRuntime().availableProcessors();
    }
  }

  static class SizeHandler {
    /**
     * @see BulkProcessor.Builder#setConcurrentRequests(int)
     */
    int getConcurrentRequests() {
      // in the same thread by default
      return 0;
    }

    void beforeStart(BulkIndexer bulkIndexer) {
      // nothing to do, to be overridden if needed
    }

    void afterStop(BulkIndexer bulkIndexer) {
      // nothing to do, to be overridden if needed
    }
  }

  static class LargeSizeHandler extends SizeHandler {

    private final Map<String, Object> initialSettings = new HashMap<>();
    private final Runtime2 runtime2;
    private ProgressLogger progress;

    LargeSizeHandler(Runtime2 runtime2) {
      this.runtime2 = runtime2;
    }

    @Override
    int getConcurrentRequests() {
      // see SONAR-8075
      int cores = runtime2.getCores();
      // FIXME do not use DEFAULT_NUMBER_OF_SHARDS
      return Math.max(1, cores / DEFAULT_NUMBER_OF_SHARDS) - 1;
    }

    @Override
    void beforeStart(BulkIndexer bulkIndexer) {
      this.progress = new ProgressLogger(format("Progress[BulkIndexer[%s]]", bulkIndexer.indexType.getIndex()), bulkIndexer.result.total, LOGGER)
        .setPluralLabel("requests");
      this.progress.start();
      Map<String, Object> temporarySettings = new HashMap<>();
      GetSettingsResponse settingsResp = bulkIndexer.client.nativeClient().admin().indices().prepareGetSettings(bulkIndexer.indexType.getIndex()).get();

      // deactivate replicas
      int initialReplicas = Integer.parseInt(settingsResp.getSetting(bulkIndexer.indexType.getIndex(), IndexMetaData.SETTING_NUMBER_OF_REPLICAS));
      if (initialReplicas > 0) {
        initialSettings.put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, initialReplicas);
        temporarySettings.put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0);
      }

      // deactivate periodical refresh
      String refreshInterval = settingsResp.getSetting(bulkIndexer.indexType.getIndex(), REFRESH_INTERVAL_SETTING);
      initialSettings.put(REFRESH_INTERVAL_SETTING, refreshInterval);
      temporarySettings.put(REFRESH_INTERVAL_SETTING, "-1");

      updateSettings(bulkIndexer, temporarySettings);
    }

    @Override
    void afterStop(BulkIndexer bulkIndexer) {
      // optimize lucene segments and revert index settings
      // Optimization must be done before re-applying replicas:
      // http://www.elasticsearch.org/blog/performance-considerations-elasticsearch-indexing/
      bulkIndexer.client.prepareForceMerge(bulkIndexer.indexType.getIndex()).get();

      updateSettings(bulkIndexer, initialSettings);
      this.progress.stop();
    }

    private static void updateSettings(BulkIndexer bulkIndexer, Map<String, Object> settings) {
      UpdateSettingsRequestBuilder req = bulkIndexer.client.nativeClient().admin().indices().prepareUpdateSettings(bulkIndexer.indexType.getIndex());
      req.setSettings(settings);
      req.get();
    }
  }
}
