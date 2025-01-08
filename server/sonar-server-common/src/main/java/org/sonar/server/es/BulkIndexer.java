/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkProcessor.Listener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.util.ProgressLogger;

import static java.lang.String.format;

/**
 * Helper to bulk requests in an efficient way :
 * <ul>
 * <li>bulk request is sent on the wire when its size is higher than 5Mb</li>
 * <li>on large table indexing, replicas and automatic refresh can be temporarily disabled</li>
 * </ul>
 */
public class BulkIndexer {

  private static final Logger LOGGER = LoggerFactory.getLogger(BulkIndexer.class);
  private static final ByteSizeValue FLUSH_BYTE_SIZE = new ByteSizeValue(1, ByteSizeUnit.MB);
  private static final int FLUSH_ACTIONS = -1;
  private static final String REFRESH_INTERVAL_SETTING = "index.refresh_interval";
  private static final int DEFAULT_NUMBER_OF_SHARDS = 5;

  private final EsClient esClient;

  private final IndexType indexType;
  private final BulkProcessor bulkProcessor;
  private final IndexingResult result = new IndexingResult();
  private final IndexingListener indexingListener;
  private final SizeHandler sizeHandler;

  public BulkIndexer(EsClient client, IndexType indexType, Size size) {
    this(client, indexType, size, IndexingListener.FAIL_ON_ERROR);
  }

  public BulkIndexer(EsClient client, IndexType indexType, Size size, IndexingListener indexingListener) {
    this.esClient = client;
    this.indexType = indexType;
    this.sizeHandler = size.createHandler(Runtime2.INSTANCE);
    this.indexingListener = indexingListener;
    BulkProcessorListener bulkProcessorListener = new BulkProcessorListener();
    this.bulkProcessor = BulkProcessor.builder(
      client::bulkAsync,
      bulkProcessorListener)
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

    esClient.refresh(indexType.getMainType().getIndex());

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

  public void addDeletion(SearchRequest searchRequest) {
    // TODO to be replaced by delete_by_query that is back in ES5
    searchRequest
      .scroll(TimeValue.timeValueMinutes(5))
      .source()
      .sort("_doc", SortOrder.ASC)
      .size(100)
      // load only doc ids, not _source fields
      .fetchSource(false);

    // this search is synchronous. An optimization would be to be non-blocking,
    // but it requires to tracking pending requests in close().
    // Same semaphore can't be reused because of potential deadlock (requires to acquire
    // two locks)
    SearchResponse searchResponse = esClient.search(searchRequest);

    while (true) {
      SearchHit[] hits = searchResponse.getHits().getHits();
      for (SearchHit hit : hits) {
        DocumentField routing = hit.field("_routing");
        DeleteRequest deleteRequest = new DeleteRequest(hit.getIndex(), hit.getType(), hit.getId());
        if (routing != null) {
          deleteRequest.routing(routing.getValue());
        }
        add(deleteRequest);
      }

      String scrollId = searchResponse.getScrollId();
      if (scrollId == null) {
        break;
      }
      searchResponse = esClient.scroll(new SearchScrollRequest(scrollId).scroll(TimeValue.timeValueMinutes(5)));
      if (hits.length == 0) {
        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(scrollId);
        esClient.clearScroll(clearScrollRequest);
        break;
      }
    }
  }

  public void addDeletion(IndexType indexType, String id) {
    add(new DeleteRequest(indexType.getMainType().getIndex().getName())
      .id(id));
  }

  public void addDeletion(IndexType indexType, String id, @Nullable String routing) {
    add(new DeleteRequest(indexType.getMainType().getIndex().getName())
      .id(id)
      .routing(routing));
  }

  /**
   * Delete all the documents matching the given search request. This method is blocking.
   * Index is refreshed, so docs are not searchable as soon as method is executed.
   * <p>
   * Note that the parameter indexType could be removed if progress logs are not needed.
   */
  public static IndexingResult delete(EsClient client, IndexType indexType, SearchRequest searchRequest) {
    BulkIndexer bulk = new BulkIndexer(client, indexType, Size.REGULAR);
    bulk.start();
    bulk.addDeletion(searchRequest);
    return bulk.stop();
  }

  private final class BulkProcessorListener implements Listener {
    // a map containing per each request the associated profiler
    private final Map<BulkRequest, Profiler> profilerByRequest = new ConcurrentHashMap<>();

    @Override
    public void beforeBulk(long executionId, BulkRequest request) {
      final Profiler profiler = Profiler.createIfTrace(EsClient.LOGGER);
      profiler.start();
      profilerByRequest.put(request, profiler);
    }

    @Override
    public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
      stopProfiler(request);
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
    public void afterBulk(long executionId, BulkRequest request, Throwable e) {
      LOGGER.error("Fail to execute bulk index request: {}", request, e);
      stopProfiler(request);
    }

    private void stopProfiler(BulkRequest request) {
      final Profiler profiler = profilerByRequest.get(request);
      if (Objects.nonNull(profiler) && profiler.isTraceEnabled()) {
        profiler.stopTrace(toString(request));
      }
      profilerByRequest.remove(request);
    }

    private String toString(BulkRequest bulkRequest) {
      StringBuilder message = new StringBuilder();
      message.append("Bulk[");
      Multiset<BulkRequestKey> groupedRequests = LinkedHashMultiset.create();
      for (int i = 0; i < bulkRequest.requests().size(); i++) {
        DocWriteRequest item = bulkRequest.requests().get(i);
        String requestType;
        if (item instanceof IndexRequest) {
          requestType = "index";
        } else if (item instanceof UpdateRequest) {
          requestType = "update";
        } else if (item instanceof DeleteRequest) {
          requestType = "delete";
        } else {
          // Cannot happen, not allowed by BulkRequest's contract
          throw new IllegalStateException("Unsupported bulk request type: " + item.getClass());
        }
        groupedRequests.add(new BulkRequestKey(requestType, item.index(), item.type()));
      }

      Set<Multiset.Entry<BulkRequestKey>> entrySet = groupedRequests.entrySet();
      int size = entrySet.size();
      int current = 0;
      for (Multiset.Entry<BulkRequestKey> requestEntry : entrySet) {
        message.append(requestEntry.getCount()).append(" ").append(requestEntry.getElement().toString());
        current++;
        if (current < size) {
          message.append(", ");
        }
      }

      message.append("]");
      return message.toString();
    }
  }

  private static class BulkRequestKey {
    private String requestType;
    private String index;
    private String docType;

    private BulkRequestKey(String requestType, String index, String docType) {
      this.requestType = requestType;
      this.index = index;
      this.docType = docType;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      BulkRequestKey that = (BulkRequestKey) o;
      return Objects.equals(docType, that.docType) && Objects.equals(index, that.index) && Objects.equals(requestType, that.requestType);
    }

    @Override
    public int hashCode() {
      return Objects.hash(requestType, index, docType);
    }

    @Override
    public String toString() {
      return String.format("%s requests on %s/%s", requestType, index, docType);
    }
  }

  public enum Size {
    /**
     * Use this size for a limited number of documents.
     */
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
      String index = bulkIndexer.indexType.getMainType().getIndex().getName();
      this.progress = new ProgressLogger(format("Progress[BulkIndexer[%s]]", index), bulkIndexer.result.total, LOGGER)
        .setPluralLabel("requests");
      this.progress.start();
      Map<String, Object> temporarySettings = new HashMap<>();

      GetSettingsResponse settingsResp = bulkIndexer.esClient.getSettings(new GetSettingsRequest());

      // deactivate replicas
      int initialReplicas = Integer.parseInt(settingsResp.getSetting(index, IndexMetadata.SETTING_NUMBER_OF_REPLICAS));
      if (initialReplicas > 0) {
        initialSettings.put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, initialReplicas);
        temporarySettings.put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0);
      }

      // deactivate periodical refresh
      String refreshInterval = settingsResp.getSetting(index, REFRESH_INTERVAL_SETTING);
      initialSettings.put(REFRESH_INTERVAL_SETTING, refreshInterval);
      temporarySettings.put(REFRESH_INTERVAL_SETTING, "-1");

      updateSettings(bulkIndexer, temporarySettings);
    }

    @Override
    void afterStop(BulkIndexer bulkIndexer) {
      // optimize lucene segments and revert index settings
      // Optimization must be done before re-applying replicas:
      // http://www.elasticsearch.org/blog/performance-considerations-elasticsearch-indexing/
      bulkIndexer.esClient.forcemerge(new ForceMergeRequest(bulkIndexer.indexType.getMainType().getIndex().getName()));

      updateSettings(bulkIndexer, initialSettings);
      this.progress.stop();
    }

    private static void updateSettings(BulkIndexer bulkIndexer, Map<String, Object> settings) {
      UpdateSettingsRequest req = new UpdateSettingsRequest(bulkIndexer.indexType.getMainType().getIndex().getName());
      req.settings(settings);
      bulkIndexer.esClient.putSettings(req);
    }
  }
}
