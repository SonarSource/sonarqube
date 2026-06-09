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

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.http.HttpHost;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.component.index.ComponentIndexDefinition;
import org.sonar.server.es.IndexDefinition.IndexDefinitionContext;
import org.sonar.server.es.IndexType.IndexRelationType;
import org.sonar.server.es.newindex.BuiltIndex;
import org.sonar.server.es.newindex.NewIndex;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.measure.index.ProjectMeasuresIndexDefinition;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.view.index.ViewIndexDefinition;

import static org.assertj.core.api.Assertions.fail;
import static org.sonar.server.es.Index.ALL_INDICES;
import static org.sonar.server.es.IndexType.FIELD_INDEX_TYPE;

/**
 * JUnit test fixture that connects to a real Elasticsearch 8.x server and exposes the SonarQube
 * {@link EsClient} bound to it. Replaces the previous embedded ES7 {@code Node}.
 *
 * <p>The ES host is read from the {@code ES_HOST} and {@code ES_PORT} environment variables
 * (defaulting to {@code localhost:9200}). In CI the {@code ES JUnit} job provides an
 * Elasticsearch service container that sets those variables. Locally, start an ES 8.x instance
 * and export them accordingly.
 */
public class EsTester extends ExternalResource implements AfterEachCallback {

  private static final Logger LOG = LoggerFactory.getLogger(EsTester.class);
  private static final ObjectMapper JSON = new ObjectMapper();

  private static final EsClient ES_REST_CLIENT = createEsRestClient();

  private static final AtomicBoolean CORE_INDICES_CREATED = new AtomicBoolean(false);
  private static final Set<String> CORE_INDICES_NAMES = new HashSet<>();

  private final boolean isCustom;

  private EsTester(boolean isCustom) {
    this.isCustom = isCustom;
  }

  /**
   * New instance which contains the core indices (rules, issues, ...).
   */
  public static EsTester create() {
    if (!CORE_INDICES_CREATED.get()) {
      List<BuiltIndex> createdIndices = createIndices(
        ComponentIndexDefinition.createForTest(),
        IssueIndexDefinition.createForTest(),
        ProjectMeasuresIndexDefinition.createForTest(),
        RuleIndexDefinition.createForTest(),
        ViewIndexDefinition.createForTest());

      CORE_INDICES_CREATED.set(true);
      createdIndices.stream().map(t -> t.getMainType().getIndex().getName()).forEach(CORE_INDICES_NAMES::add);
    }
    return new EsTester(false);
  }

  /**
   * New instance which contains the specified indices. Note that
   * core indices may exist.
   */
  public static EsTester createCustom(IndexDefinition... definitions) {
    createIndices(definitions);
    return new EsTester(true);
  }

  public void recreateIndexes() {
    deleteIndexIfExists(ALL_INDICES.getName());
    CORE_INDICES_CREATED.set(false);
    create();
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) throws Exception {
    after();
  }

  @Override
  protected void after() {
    if (isCustom) {
      for (String index : getIndicesNames()) {
        if (!CORE_INDICES_NAMES.contains(index)) {
          deleteIndexIfExists(index);
        }
      }
    }

    deleteAllDocumentsInIndexes();
  }

  private void deleteAllDocumentsInIndexes() {
    try {
      // Defensive: reset any write-block left by a test that crashed before its unlockWrites() call.
      // Without this, deleteByQuery below would fail with 403 cluster_block_exception and the pollution
      // would cascade into all subsequent tests sharing the long-lived ES container.
      resetWriteBlocks();

      ES_REST_CLIENT.deleteByQueryV2(d -> d
        .index(ALL_INDICES.getName())
        .query(Query.of(q -> q.matchAll(m -> m)))
        .refresh(true)
        .waitForCompletion(true));
      ES_REST_CLIENT.forcemergeV2(req -> req);
    } catch (ElasticsearchException e) {
      // Ignore 404 — no indices exist yet, nothing to delete
      Throwable cause = e.getCause();
      if (!(cause instanceof co.elastic.clients.elasticsearch._types.ElasticsearchException esException
        && esException.status() == 404)) {
        throw e;
      }
    }
  }

  private static void resetWriteBlocks() {
    try {
      ES_REST_CLIENT.putSettingsV2(req -> req
        .index(ALL_INDICES.getName())
        .settings(s -> s.otherSettings(Map.of("index.blocks.write", JsonData.of(false)))));
    } catch (ElasticsearchException e) {
      // Ignore 404 — no indices yet, nothing to reset
      Throwable cause = e.getCause();
      if (!(cause instanceof co.elastic.clients.elasticsearch._types.ElasticsearchException esException
        && esException.status() == 404)) {
        throw e;
      }
    }
  }

  private static List<String> getIndicesNames() {
    try {
      return new ArrayList<>(ES_REST_CLIENT.getIndexV2(ALL_INDICES.getName()).result().keySet());
    } catch (org.sonar.server.es.ElasticsearchException e) {
      Throwable cause = e.getCause();
      if (cause instanceof co.elastic.clients.elasticsearch._types.ElasticsearchException esException
        && esException.status() == 404) {
        return List.of();
      }
      throw e;
    }
  }

  private static EsClient createEsRestClient() {
    String host = System.getenv().getOrDefault("ES_HOST", "localhost");
    int port = Integer.parseInt(System.getenv().getOrDefault("ES_PORT", "9200"));
    LOG.info("EsTester connecting to Elasticsearch at {}:{}", host, port);
    return new EsClient(new HttpHost(host, port, "http"));
  }

  public EsClient client() {
    return ES_REST_CLIENT;
  }

  @SafeVarargs
  public final void putDocuments(IndexType indexType, BaseDoc... docs) {
    co.elastic.clients.elasticsearch.core.BulkResponse bulkResponse = ES_REST_CLIENT.bulkV2(b -> {
      b.refresh(Refresh.True);
      for (BaseDoc doc : docs) {
        IndexType.IndexMainType mainType = indexType.getMainType();
        String routing = doc.getRouting().orElse(null);
        b.operations(op -> op.index(idx -> {
          idx.index(mainType.getIndex().getName())
            .id(doc.getId())
            .document(doc.getFields());
          if (routing != null) {
            idx.routing(routing);
          }
          return idx;
        }));
      }
      return b;
    });

    if (bulkResponse.errors()) {
      StringBuilder errorMsg = new StringBuilder("Bulk indexing of documents failed:");
      bulkResponse.items().forEach(item -> {
        if (item.error() != null) {
          errorMsg.append("\n- ").append(item.error().reason());
        }
      });
      fail(errorMsg.toString());
    }
  }

  @SafeVarargs
  public final void putDocuments(IndexType indexType, Map<String, Object>... docs) {
    try {
      co.elastic.clients.elasticsearch.core.BulkResponse bulkResponse = ES_REST_CLIENT.bulkV2(b -> {
        b.refresh(Refresh.True);
        for (Map<String, Object> doc : docs) {
          IndexType.IndexMainType mainType = indexType.getMainType();
          b.operations(op -> op.index(idx -> idx
            .index(mainType.getIndex().getName())
            .document(doc)
          ));
        }
        return b;
      });

      if (bulkResponse.errors()) {
        StringBuilder errorMsg = new StringBuilder("Bulk indexing of documents failed:");
        bulkResponse.items().forEach(item -> {
          if (item.error() != null) {
            errorMsg.append("\n- ").append(item.error().reason());
          }
        });
        throw new IllegalStateException(errorMsg.toString());
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public long countDocuments(Index index) {
    SearchResponse<Void> response = ES_REST_CLIENT.searchV2(s -> s
      .index(index.getName())
      .query(Query.of(q -> q.matchAll(m -> m)))
      .size(0)
      .trackTotalHits(t -> t.enabled(true)), Void.class);
    return response.hits().total() == null ? 0L : response.hits().total().value();
  }

  public long countDocuments(IndexType indexType) {
    IndexType.IndexMainType mainType = indexType.getMainType();
    SearchResponse<Void> response = ES_REST_CLIENT.searchV2(s -> s
      .index(mainType.getIndex().getName())
      .query(getDocumentsQuery(indexType))
      .size(0)
      .trackTotalHits(t -> t.enabled(true)), Void.class);
    return response.hits().total() == null ? 0L : response.hits().total().value();
  }

  /**
   * Get all the indexed documents (no paginated results). Results are converted to BaseDoc objects.
   * Results are not sorted.
   */
  public <E extends BaseDoc> List<E> getDocuments(IndexType indexType, final Class<E> docClass) {
    List<Hit<Map<String, Object>>> hits = getDocuments(indexType);
    List<E> result = new ArrayList<>(hits.size());
    for (Hit<Map<String, Object>> hit : hits) {
      try {
        @SuppressWarnings("unchecked")
        E doc = (E) ConstructorUtils.invokeConstructor(docClass, hit.source());
        result.add(doc);
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }
    return result;
  }

  /**
   * Get all the indexed documents (no paginated results) of the specified type. Results are not sorted.
   */
  public List<Hit<Map<String, Object>>> getDocuments(IndexType indexType) {
    IndexType.IndexMainType mainType = indexType.getMainType();
    return searchAllPaginated(mainType.getIndex().getName(), getDocumentsQuery(indexType));
  }

  private static List<Hit<Map<String, Object>>> searchAllPaginated(String indexName, Query query) {
    int pageSize = 100;
    List<Hit<Map<String, Object>>> all = new ArrayList<>();
    List<FieldValue> searchAfter = null;
    while (true) {
      final List<FieldValue> sa = searchAfter;
      @SuppressWarnings({"rawtypes", "unchecked"})
      Class<Map<String, Object>> mapClass = (Class) Map.class;
      SearchResponse<Map<String, Object>> response = ES_REST_CLIENT.searchV2(s -> {
        s.index(indexName)
          .query(query)
          .size(pageSize)
          .sort(so -> so.field(f -> f.field("_doc").order(SortOrder.Asc)));
        if (sa != null) {
          s.searchAfter(sa);
        }
        return s;
      }, mapClass);
      List<Hit<Map<String, Object>>> hits = response.hits().hits();
      if (hits.isEmpty()) {
        return all;
      }
      all.addAll(hits);
      searchAfter = hits.get(hits.size() - 1).sort();
    }
  }

  private static Query getDocumentsQuery(IndexType indexType) {
    if (!indexType.getMainType().getIndex().acceptsRelations()) {
      return Query.of(q -> q.matchAll(m -> m));
    }
    if (indexType instanceof IndexRelationType relation) {
      return Query.of(q -> q.term(t -> t.field(FIELD_INDEX_TYPE).value(relation.getName())));
    }
    if (indexType instanceof IndexType.IndexMainType mainType) {
      return Query.of(q -> q.term(t -> t.field(FIELD_INDEX_TYPE).value(mainType.getType())));
    }
    throw new IllegalArgumentException("Unsupported IndexType " + indexType.getClass());
  }

  /**
   * Get a list of a specific field from all indexed documents.
   */
  @SuppressWarnings("unchecked")
  public <T> List<T> getDocumentFieldValues(IndexType indexType, final String fieldNameToReturn) {
    return getDocuments(indexType)
      .stream()
      .map(hit -> (T) hit.source().get(fieldNameToReturn))
      .toList();
  }

  public List<String> getIds(IndexType indexType) {
    return getDocuments(indexType).stream().map(Hit::id).toList();
  }

  public void lockWrites(IndexType index) {
    setIndexSettings(index.getMainType().getIndex().getName(), ImmutableMap.of("index.blocks.write", "true"));
  }

  public void unlockWrites(IndexType index) {
    setIndexSettings(index.getMainType().getIndex().getName(), ImmutableMap.of("index.blocks.write", "false"));
  }

  private static void setIndexSettings(String index, Map<String, Object> settings) {
    Map<String, JsonData> jsonSettings = new HashMap<>();
    settings.forEach((key, value) -> jsonSettings.put(key, JsonData.of(value)));

    ES_REST_CLIENT.putSettingsV2(req -> req
      .index(index)
      .settings(s -> s.otherSettings(jsonSettings))
    );
  }

  private static void deleteIndexIfExists(String name) {
    try {
      ES_REST_CLIENT.deleteIndexV2(name);
    } catch (org.sonar.server.es.ElasticsearchException e) {
      // EsClient.execute() wraps the ES8 exception; unwrap to check for 404 (index not found).
      Throwable cause = e.getCause();
      if (cause instanceof co.elastic.clients.elasticsearch._types.ElasticsearchException esException
        && esException.status() == 404) {
        // index doesn't exist — nothing to delete, that's fine
        return;
      }
      throw e;
    }
  }

  private static List<BuiltIndex> createIndices(IndexDefinition... definitions) {
    IndexDefinitionContext context = new IndexDefinitionContext();
    Stream.of(definitions).forEach(d -> d.define(context));

    List<BuiltIndex> result = new ArrayList<>();
    for (NewIndex newIndex : context.getIndices().values()) {
      BuiltIndex index = newIndex.build();
      String indexName = index.getMainType().getIndex().getName();
      deleteIndexIfExists(indexName);

      String settingsJson;
      try {
        settingsJson = JSON.writeValueAsString(Map.of("settings", index.getSettings()));
      } catch (JsonProcessingException e) {
        throw new IllegalStateException("Cannot serialize settings for index " + indexName, e);
      }
      boolean acked = ES_REST_CLIENT.createIndexV2(cir -> cir
        .index(indexName)
        .withJson(new StringReader(settingsJson))
      ).acknowledged();
      if (!acked) {
        throw new IllegalStateException("Failed to create index " + indexName);
      }

      ES_REST_CLIENT.waitForStatusV2(HealthStatus.Yellow);

      String mappingJson;
      try {
        mappingJson = JSON.writeValueAsString(index.getAttributes());
      } catch (JsonProcessingException e) {
        throw new IllegalStateException("Cannot serialize mapping for index " + indexName, e);
      }
      boolean mappingAcked = ES_REST_CLIENT.putMappingV2(pmr -> pmr
        .index(indexName)
        .withJson(new StringReader(mappingJson))
      ).acknowledged();
      if (!mappingAcked) {
        throw new IllegalStateException("Failed to create mapping for index " + indexName);
      }

      ES_REST_CLIENT.waitForStatusV2(HealthStatus.Yellow);
      result.add(index);
    }
    return result;
  }
}
