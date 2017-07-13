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

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nonnull;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;
import org.elasticsearch.search.SearchHit;
import org.junit.rules.ExternalResource;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.core.config.ConfigurationProvider;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.es.metadata.MetadataIndex;
import org.sonar.server.es.metadata.MetadataIndexDefinition;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.sonar.server.es.DefaultIndexSettings.REFRESH_IMMEDIATE;

public class EsTester extends ExternalResource {

  private final List<IndexDefinition> indexDefinitions;
  private final EsClient client = new EsClient(NodeHolder.INSTANCE.node.client());
  private ComponentContainer container;

  public EsTester(IndexDefinition... defs) {
    this.indexDefinitions = asList(defs);
  }

  @Override
  protected void before() throws Throwable {
    deleteIndices();

    if (!indexDefinitions.isEmpty()) {
      container = new ComponentContainer();
      container.addSingleton(new MapSettings());
      container.addSingleton(new ConfigurationProvider());
      container.addSingletons(indexDefinitions);
      container.addSingleton(client);
      container.addSingleton(IndexDefinitions.class);
      container.addSingleton(IndexCreator.class);
      container.addSingleton(MetadataIndex.class);
      container.addSingleton(MetadataIndexDefinition.class);
      container.startComponents();
    }
  }

  @Override
  protected void after() {
    if (container != null) {
      container.stopComponents();
    }
    if (client != null) {
      client.close();
    }
  }

  private void deleteIndices() {
    client.nativeClient().admin().indices().prepareDelete("_all").get();
  }

  public void deleteIndex(String indexName) {
    client.nativeClient().admin().indices().prepareDelete(indexName).get();
  }

  public void putDocuments(String index, String type, BaseDoc... docs) {
    putDocuments(new IndexType(index, type), docs);
  }

  public void putDocuments(IndexType indexType, BaseDoc... docs) {
    try {
      BulkRequestBuilder bulk = client.prepareBulk()
        .setRefreshPolicy(REFRESH_IMMEDIATE);
      for (BaseDoc doc : docs) {
        bulk.add(new IndexRequest(indexType.getIndex(), indexType.getType(), doc.getId())
          .parent(doc.getParent())
          .routing(doc.getRouting())
          .source(doc.getFields()));
      }
      EsUtils.executeBulkRequest(bulk, "");
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public long countDocuments(String index, String type) {
    return countDocuments(new IndexType(index, type));
  }

  public long countDocuments(IndexType indexType) {
    return client().prepareSearch(indexType).setSize(0).get().getHits().totalHits();
  }

  /**
   * Get all the indexed documents (no paginated results). Results are converted to BaseDoc objects.
   * Results are not sorted.
   */
  public <E extends BaseDoc> List<E> getDocuments(IndexType indexType, final Class<E> docClass) {
    List<SearchHit> hits = getDocuments(indexType);
    return newArrayList(Collections2.transform(hits, input -> {
      try {
        return (E) ConstructorUtils.invokeConstructor(docClass, input.getSource());
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }));
  }

  /**
   * Get all the indexed documents (no paginated results). Results are not sorted.
   */
  public List<SearchHit> getDocuments(IndexType indexType) {
    SearchRequestBuilder req = client.nativeClient().prepareSearch(indexType.getIndex()).setTypes(indexType.getType()).setQuery(QueryBuilders.matchAllQuery());
    EsUtils.optimizeScrollRequest(req);
    req.setScroll(new TimeValue(60000))
      .setSize(100);

    SearchResponse response = req.get();
    List<SearchHit> result = newArrayList();
    while (true) {
      Iterables.addAll(result, response.getHits());
      response = client.nativeClient().prepareSearchScroll(response.getScrollId()).setScroll(new TimeValue(600000)).execute().actionGet();
      // Break condition: No hits are returned
      if (response.getHits().getHits().length == 0) {
        break;
      }
    }
    return result;
  }

  /**
   * Get a list of a specific field from all indexed documents.
   */
  public <T> List<T> getDocumentFieldValues(IndexType indexType, final String fieldNameToReturn) {
    return newArrayList(Iterables.transform(getDocuments(indexType), new Function<SearchHit, T>() {
      @Override
      public T apply(SearchHit input) {
        return (T) input.sourceAsMap().get(fieldNameToReturn);
      }
    }));
  }

  public List<String> getIds(IndexType indexType) {
    return FluentIterable.from(getDocuments(indexType)).transform(SearchHitToId.INSTANCE).toList();
  }

  public EsClient client() {
    return client;
  }

  private enum SearchHitToId implements Function<SearchHit, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull org.elasticsearch.search.SearchHit input) {
      return input.id();
    }
  }

  private static class NodeHolder {
    private static final NodeHolder INSTANCE = new NodeHolder();

    private final Node node;

    private NodeHolder() {
      String nodeName = "tmp-es-" + RandomUtils.nextInt();
      Path tmpDir;
      try {
        tmpDir = Files.createTempDirectory("tmp-es");
      } catch (IOException e) {
        throw new RuntimeException("Cannot create elasticsearch temporary directory", e);
      }

      tmpDir.toFile().deleteOnExit();

      Settings.Builder settings = Settings.builder()
        .put("transport.type", "local")
        .put("node.data", true)
        .put("cluster.name", nodeName)
        .put("node.name", nodeName)
        // the two following properties are probably not used because they are
        // declared on indices too
        .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
        .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0)
        // limit the number of threads created (see org.elasticsearch.common.util.concurrent.EsExecutors)
        .put("processors", 1)
        .put("http.enabled", false)
        .put("config.ignore_system_properties", true)
        .put("path.home", tmpDir);
      node = new Node(settings.build());
      try {
        node.start();
      } catch (NodeValidationException e) {
        throw new RuntimeException("Cannot start Elasticsearch node", e);
      }
      checkState(!node.isClosed());

      // wait for node to be ready
      node.client().admin().cluster().prepareHealth().setWaitForGreenStatus().get();

      // delete the indices (should not exist)
      DeleteIndexResponse response = node.client().admin().indices().prepareDelete("_all").get();
      checkState(response.isAcknowledged());
    }
  }
}
