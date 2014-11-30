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
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
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

public class EsTester extends ExternalResource {

  private EsClient client;
  private final List<IndexDefinition> definitions = Lists.newArrayList();
  private TransportClient transportClient;

  public EsTester addDefinitions(IndexDefinition... defs) {
    Collections.addAll(definitions, defs);
    return this;
  }

  protected void before() throws Throwable {
    EsServerHolder holder = EsServerHolder.get();
    transportClient = holder.newClient();
    client = new EsClient(new Profiling(new Settings()), transportClient);
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
    if (transportClient != null) {
      // TODO to be removed as soon as EsClient.stop() is implemented correctly
      transportClient.close();
      transportClient = null;
    }
    if (client != null) {
      client.stop();
      client = null;
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
          return (E) ConstructorUtils.invokeConstructor(docClass, input.getSource());
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

  public EsClient client() {
    return client;
  }

}
