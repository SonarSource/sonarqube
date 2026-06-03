/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkListener;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.json.JsonData;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.util.ProgressLogger;

import static java.lang.String.format;

/**
 * Helper to bulk requests in an efficient way :
 * <ul>
 * <li>bulk request is sent on the wire when its size is higher than 1Mb</li>
 * <li>on large table indexing, replicas and automatic refresh can be temporarily disabled</li>
 * </ul>
 */
public class BulkIndexer {

  private static final Logger LOGGER = LoggerFactory.getLogger(BulkIndexer.class);
  // Approximate flush boundary for the synchronous path. The ES7 BulkProcessor batched by serialized
  // byte size; here we batch by operation count, which is cheap to track and produces request sizes
  // in the same ballpark for typical document sizes.
  private static final int FLUSH_ACTIONS = 1000;
  // 1 MB — used by the async (BulkIngester) mode
  private static final long FLUSH_BYTE_SIZE = 1024L * 1024L;
  private static final String REFRESH_INTERVAL_SETTING = "index.refresh_interval";
  private static final String SETTING_NUMBER_OF_REPLICAS = "index.number_of_replicas";
  private static final int DEFAULT_NUMBER_OF_SHARDS = 5;
  private static final String DOC_TYPE = "_doc";
  // Retry parameters matching the ES7 BulkProcessor BackoffPolicy.exponentialBackoff() defaults
  // (50ms initial delay, 8 retries with exponential growth). ES rejects bulk items under load
  // with HTTP 429 / es_rejected_execution_exception; retrying with backoff is what kept large
  // recovery and initial indexing reliable on busy clusters.
  private static final int MAX_RETRIES = 8;
  private static final long INITIAL_BACKOFF_MS = 50;
  private static final String REJECTED_EXECUTION_TYPE = "es_rejected_execution_exception";

  private final EsClient esClient;

  private final IndexType indexType;
  // Set when sizeHandler.getConcurrentRequests() >= 1 — async path via the ES8 BulkIngester helper.
  @Nullable
  private final BulkIngester<Void> bulkIngester;
  // Set when sizeHandler.getConcurrentRequests() == 0 — synchronous path that flushes in the calling
  // thread. The ES7 BulkProcessor allowed setConcurrentRequests(0); the ES8 BulkIngester does not
  // (maxConcurrentRequests(0) hangs — see https://github.com/elastic/elasticsearch-java/issues/532),
  // so synchronous callers go through esClient.bulkV2 directly. This preserves the contract that
  // IndexingListener#onSuccess (which may touch the caller's DbSession) runs on the calling thread.
  @Nullable
  private final List<BulkOperation> pendingOperations;
  private final IndexingResult result = new IndexingResult();
  private final IndexingListener indexingListener;
  private final SizeHandler sizeHandler;
  private final BulkIngesterListener listener;

  public BulkIndexer(EsClient client, IndexType indexType, Size size) {
    this(client, indexType, size, IndexingListener.FAIL_ON_ERROR);
  }

  public BulkIndexer(EsClient client, IndexType indexType, Size size, IndexingListener indexingListener) {
    this.esClient = client;
    this.indexType = indexType;
    this.sizeHandler = size.createHandler(Runtime2.INSTANCE);
    this.indexingListener = indexingListener;
    this.listener = new BulkIngesterListener();
    int concurrentRequests = sizeHandler.getConcurrentRequests();
    if (concurrentRequests == 0) {
      this.bulkIngester = null;
      this.pendingOperations = new ArrayList<>();
    } else {
      this.pendingOperations = null;
      this.bulkIngester = BulkIngester.of(b -> b
        .client(client.nativeClientV2())
        .maxSize(FLUSH_BYTE_SIZE)
        .maxConcurrentRequests(concurrentRequests)
        .listener(listener));
    }
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
    if (bulkIngester != null) {
      bulkIngester.close();
    } else {
      flushPending();
    }

    esClient.refreshV2(indexType.getMainType().getIndex());

    sizeHandler.afterStop(this);
    indexingListener.onFinish(result);
    return result;
  }

  public void add(BulkOperation operation) {
    result.incrementRequests();
    if (bulkIngester != null) {
      bulkIngester.add(operation);
    } else {
      pendingOperations.add(operation);
      if (pendingOperations.size() >= FLUSH_ACTIONS) {
        flushPending();
      }
    }
  }

  private void flushPending() {
    if (pendingOperations.isEmpty()) {
      return;
    }
    // Snapshot the operations and clear the buffer before sending: the ES8 BulkRequest.Builder
    // stores the list by reference (via _listAddAll), so clearing pendingOperations after passing
    // it to the builder would also empty the builder's internal list — yielding an empty bulk
    // body and "[parse_exception] request body is required" from ES.
    List<BulkOperation> batch = List.copyOf(pendingOperations);
    pendingOperations.clear();
    BulkRequest request = BulkRequest.of(b -> b.operations(batch));
    long executionId = System.nanoTime();
    listener.beforeBulk(executionId, request, Collections.emptyList());
    try {
      BulkResponse response = executeBulkWithRetry(batch);
      listener.afterBulk(executionId, request, Collections.emptyList(), response);
    } catch (Exception e) {
      listener.afterBulk(executionId, request, Collections.emptyList(), e);
      throw e;
    }
  }

  /**
   * Execute a bulk request, retrying rejected items with exponential backoff. Matches the
   * resilience the ES7 BulkProcessor provided via {@code BackoffPolicy.exponentialBackoff()}.
   * Operations that succeed or fail with a non-retryable error are kept as-is in the response;
   * only items rejected with {@code es_rejected_execution_exception} are resubmitted.
   */
  private BulkResponse executeBulkWithRetry(List<BulkOperation> batch) {
    BulkResponse response = esClient.bulkV2(b -> b.operations(batch));
    List<BulkResponseItem> mergedItems = new ArrayList<>(response.items());
    long backoffMs = INITIAL_BACKOFF_MS;
    for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
      List<Integer> retryIndices = new ArrayList<>();
      for (int i = 0; i < mergedItems.size(); i++) {
        if (isRetryable(mergedItems.get(i))) {
          retryIndices.add(i);
        }
      }
      if (retryIndices.isEmpty()) {
        break;
      }
      sleepForBackoff(backoffMs);
      backoffMs *= 2;
      List<BulkOperation> retryOps = retryIndices.stream().map(batch::get).toList();
      BulkResponse retryResponse = esClient.bulkV2(b -> b.operations(retryOps));
      for (int k = 0; k < retryIndices.size(); k++) {
        mergedItems.set(retryIndices.get(k), retryResponse.items().get(k));
      }
    }
    BulkResponse original = response;
    return BulkResponse.of(b -> b.took(original.took()).errors(mergedItems.stream().anyMatch(i -> i.error() != null)).items(mergedItems));
  }

  private static boolean isRetryable(BulkResponseItem item) {
    return item.error() != null && REJECTED_EXECUTION_TYPE.equals(item.error().type());
  }

  private static void sleepForBackoff(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting to retry bulk request", e);
    }
  }

  public void addDeletion(SearchRequest searchRequest) {
    // Search via search_after pagination and delete each hit by its own index/id/routing.
    // Reading the index per-hit matters when the search targets more than one index:
    // delete operations must point to the same index the hit came from, otherwise ES will
    // silently drop the operation. We also read routing per-hit so that documents in
    // parent/child indices are deleted with the correct routing value.
    List<FieldValue> lastSortValues = null;
    while (true) {
      final List<FieldValue> searchAfter = lastSortValues;
      SearchResponse<Void> response = esClient.searchV2(b -> {
        b.index(searchRequest.index())
          .query(searchRequest.query())
          .size(searchRequest.size())
          .source(s -> s.fetch(false))
          .storedFields("_routing");
        if (searchRequest.sort() != null && !searchRequest.sort().isEmpty()) {
          b.sort(searchRequest.sort());
        }
        if (searchAfter != null) {
          b.searchAfter(searchAfter);
        }
        return b;
      }, Void.class);

      List<Hit<Void>> hits = response.hits().hits();
      if (hits.isEmpty()) {
        return;
      }
      for (Hit<Void> hit : hits) {
        String hitIndex = hit.index();
        String id = hit.id();
        String routing = hit.routing();
        add(BulkOperation.of(b -> b.delete(d -> d.index(hitIndex).id(id).routing(routing))));
      }
      lastSortValues = hits.get(hits.size() - 1).sort();
    }
  }

  public void addDeletion(IndexType indexType, String id) {
    addDeletion(indexType, id, null);
  }

  public void addDeletion(IndexType indexType, String id, @Nullable String routing) {
    String index = indexType.getMainType().getIndex().getName();
    add(BulkOperation.of(b -> b.delete(d -> d.index(index).id(id).routing(routing))));
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

  private final class BulkIngesterListener implements BulkListener<Void> {
    // a map containing per executionId the associated profiler
    private final Map<Long, Profiler> profilerByExecutionId = new ConcurrentHashMap<>();

    @Override
    public void beforeBulk(long executionId, BulkRequest request, List<Void> contexts) {
      final Profiler profiler = Profiler.createIfTrace(EsClient.LOGGER);
      profiler.start();
      profilerByExecutionId.put(executionId, profiler);
    }

    @Override
    public void afterBulk(long executionId, BulkRequest request, List<Void> contexts, BulkResponse response) {
      stopProfiler(executionId, request);
      List<DocId> successDocIds = new ArrayList<>();
      // ES8 BulkIngester does not retry rejected items natively. Re-submit items that came back
      // with es_rejected_execution_exception so we keep the resilience the ES7 BulkProcessor had
      // via BackoffPolicy.exponentialBackoff() — see SONAR-26745. Note: we do not apply a backoff
      // delay here because we are on the ingester's listener thread; the natural throttling
      // happens through BulkIngester.maxConcurrentRequests.
      List<BulkOperation> operations = request.operations();
      for (int i = 0; i < response.items().size(); i++) {
        BulkResponseItem item = response.items().get(i);
        if (isRetryable(item)) {
          if (bulkIngester != null && i < operations.size()) {
            bulkIngester.add(operations.get(i));
          } else {
            LOGGER.error("index [{}], id [{}], message [{}]", item.index(), item.id(), item.error().reason());
          }
        } else if (item.error() != null) {
          LOGGER.error("index [{}], id [{}], message [{}]", item.index(), item.id(), item.error().reason());
        } else {
          result.incrementSuccess();
          successDocIds.add(new DocId(item.index(), DOC_TYPE, item.id()));
        }
      }
      indexingListener.onSuccess(successDocIds);
    }

    @Override
    public void afterBulk(long executionId, BulkRequest request, List<Void> contexts, Throwable failure) {
      LOGGER.error("Fail to execute bulk index request: {}", request, failure);
      stopProfiler(executionId, request);
    }

    private void stopProfiler(long executionId, BulkRequest request) {
      final Profiler profiler = profilerByExecutionId.get(executionId);
      if (Objects.nonNull(profiler) && profiler.isTraceEnabled()) {
        profiler.stopTrace(toString(request));
      }
      profilerByExecutionId.remove(executionId);
    }

    private String toString(BulkRequest bulkRequest) {
      StringBuilder message = new StringBuilder();
      message.append("Bulk[");
      Multiset<BulkRequestKey> groupedRequests = LinkedHashMultiset.create();
      for (BulkOperation op : bulkRequest.operations()) {
        String requestType;
        String index;
        if (op.isIndex()) {
          requestType = "index";
          index = op.index().index();
        } else if (op.isUpdate()) {
          requestType = "update";
          index = op.update().index();
        } else if (op.isDelete()) {
          requestType = "delete";
          index = op.delete().index();
        } else if (op.isCreate()) {
          requestType = "create";
          index = op.create().index();
        } else {
          // Cannot happen, not allowed by BulkRequest's contract
          throw new IllegalStateException("Unsupported bulk operation kind: " + op._kind());
        }
        groupedRequests.add(new BulkRequestKey(requestType, index, DOC_TYPE));
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
     * @see BulkIngester.Builder#maxConcurrentRequests(Integer)
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

      co.elastic.clients.elasticsearch.indices.GetIndicesSettingsResponse settingsResp =
        bulkIndexer.esClient.getSettingsV2(req -> req.index(index));

      IndexSettings indexSettings = settingsResp.get(index).settings();

      // deactivate replicas
      int initialReplicas = Integer.parseInt(indexSettings.index().numberOfReplicas());
      if (initialReplicas > 0) {
        initialSettings.put(SETTING_NUMBER_OF_REPLICAS, initialReplicas);
        temporarySettings.put(SETTING_NUMBER_OF_REPLICAS, 0);
      }

      // deactivate periodical refresh
      Time refreshInterval = indexSettings.index().refreshInterval();
      initialSettings.put(REFRESH_INTERVAL_SETTING, refreshInterval);
      temporarySettings.put(REFRESH_INTERVAL_SETTING, "-1");

      updateSettings(bulkIndexer, temporarySettings);
    }

    @Override
    void afterStop(BulkIndexer bulkIndexer) {
      // optimize lucene segments and revert index settings
      // Optimization must be done before re-applying replicas:
      // http://www.elasticsearch.org/blog/performance-considerations-elasticsearch-indexing/
      String indexName = bulkIndexer.indexType.getMainType().getIndex().getName();
      bulkIndexer.esClient.forcemergeV2(req -> req.index(indexName));

      updateSettings(bulkIndexer, initialSettings);
      this.progress.stop();
    }

    private static void updateSettings(BulkIndexer bulkIndexer, Map<String, Object> settings) {
      String indexName = bulkIndexer.indexType.getMainType().getIndex().getName();

      Map<String, JsonData> jsonSettings = new HashMap<>();
      settings.forEach((key, value) -> jsonSettings.put(key, JsonData.of(value)));

      bulkIndexer.esClient.putSettingsV2(req -> req
        .index(indexName)
        .settings(s -> s.otherSettings(jsonSettings))
        );
    }
  }
}
