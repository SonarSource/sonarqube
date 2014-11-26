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

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;
import org.junit.rules.ExternalResource;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.core.profiling.Profiling;
import org.sonar.server.search.BaseDoc;
import org.sonar.test.TestUtils;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class EsTester extends ExternalResource {

  private static int PROCESS_ID = RandomUtils.nextInt(Integer.MAX_VALUE);
  private Node node;
  private EsClient client;
  private final List<IndexDefinition> definitions = Lists.newArrayList();

  public EsTester addDefinitions(IndexDefinition... defs) {
    Collections.addAll(definitions, defs);
    return this;
  }

  protected void before() throws Throwable {
    String nodeName = "es-ram-" + PROCESS_ID;
    node = NodeBuilder.nodeBuilder().local(true).data(true).settings(ImmutableSettings.builder()
      .put("cluster.name", nodeName)
      .put("node.name", nodeName)
      // the two following properties are probably not used because they are
      // declared on indices too
      .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
      .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0)
      // limit the number of threads created (see org.elasticsearch.common.util.concurrent.EsExecutors)
      .put("processors", 1)
      .put("http.enabled", false)
      .put("index.store.type", "memory")
      .put("config.ignore_system_properties", true)
      // reuse the same directory than other tests for faster initialization
      .put("path.home", "target/es-" + PROCESS_ID)
      .put("gateway.type", "none"))
      .build();
    node.start();
    assertThat(DiscoveryNode.localNode(node.settings())).isTrue();

    // delete the indices created by previous tests
    DeleteIndexResponse response = node.client().admin().indices().prepareDelete("_all").get();
    assertThat(response.isAcknowledged()).isTrue();

    client = new EsClient(new Profiling(new Settings()), node.client());
    client.start();

    if (!definitions.isEmpty()) {
      ComponentContainer container = new ComponentContainer();
      container.addSingletons(definitions);
      container.addSingleton(client);
      container.addSingleton(IndexRegistry.class);
      container.addSingleton(IndexCreator.class);
      container.startComponents();
    }
  }

  @Override
  protected void after() {
    if (client != null) {
      client.stop();
    }
    if (node != null) {
      node.stop();
      node.close();
    }
  }

  public void truncateIndices() {
    client.prepareDeleteByQuery(client.prepareState().get()
      .getState().getMetaData().concreteAllIndices())
      .setQuery(QueryBuilders.matchAllQuery())
      .get();
    client.prepareRefresh(client.prepareState().get()
      .getState().getMetaData().concreteAllIndices())
      .setForce(true)
      .get();
    client.prepareFlush(client.prepareState().get()
      .getState().getMetaData().concreteAllIndices())
      .get();
  }

  public void putDocuments(String index, String type, Class<?> testClass, String... jsonPaths) throws Exception {
    BulkRequestBuilder bulk = client.prepareBulk().setRefresh(true);
    for (String path : jsonPaths) {
      bulk.add(new IndexRequest(index, type).source(IOUtils.toString(
        new FileInputStream(TestUtils.getResource(testClass, path)))));
    }
    bulk.get();
  }

  public long countDocuments(String indexName, String typeName) {
    return client().prepareCount(indexName).setTypes(typeName).get().getCount();
  }

  /**
   * Get all the indexed documents (no paginated results). Results are converted to BaseDoc objects.
   * Results are not sorted.
   */
  public <E extends BaseDoc> List<E> getDocuments(String indexName, String typeName, final Class<E> docClass) {
    List<SearchHit> hits = getDocuments(indexName, typeName);
    return Lists.newArrayList(Collections2.transform(hits, new Function<SearchHit, E>() {
      @Override
      public E apply(SearchHit input) {
        try {
          return (E) ConstructorUtils.invokeExactConstructor(docClass, input.getSource());
        } catch (Exception e) {
          throw Throwables.propagate(e);
        }
      }
    }));
  }

  /**
   * Get all the indexed documents (no paginated results). Results are not sorted.
   */
  public List<SearchHit> getDocuments(String indexName, String typeName) {
    SearchRequestBuilder req = client.nativeClient().prepareSearch(indexName).setTypes(typeName).setQuery(QueryBuilders.matchAllQuery());
    req.setSearchType(SearchType.SCAN)
      .setScroll(new TimeValue(60000))
      .setSize(100);

    SearchResponse response = req.get();
    List<SearchHit> result = Lists.newArrayList();
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

  public Node node() {
    return node;
  }

  public EsClient client() {
    return client;
  }

}
