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

package org.sonar.server.component.index;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.sonar.api.Startable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.server.component.index.ComponentIndexDefinition.INDEX_COMPONENTS;
import static org.sonar.server.component.index.ComponentIndexDefinition.TYPE_AUTHORIZATION;
import static org.sonar.server.component.index.ComponentIndexDefinition.TYPE_COMPONENT;

public class ComponentIndexer implements Startable {

  private final ThreadPoolExecutor executor;
  private final DbClient dbClient;
  private final EsClient esClient;

  public ComponentIndexer(DbClient dbClient, EsClient esClient) {
    this.executor = new ThreadPoolExecutor(0, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    this.dbClient = dbClient;
    this.esClient = esClient;
  }

  /**
   * Copy all components of all projects to the elastic search index.
   * <p>
   * <b>Warning</b>: This should only be called on an empty index. It does not delete anything.
   */
  public void index() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      BulkIndexer bulk = new BulkIndexer(esClient, INDEX_COMPONENTS);
      bulk.setLarge(true);
      bulk.start();
      dbClient.componentDao()
        .selectAll(dbSession, context -> {
          ComponentDto dto = (ComponentDto) context.getResultObject();
          bulk.add(newIndexRequest(toDocument(dto)));
        });
      bulk.stop();
    }
  }

  /**
   * Update the index for one specific project. The current data from the database is used.
   */
  public void indexByProjectUuid(String projectUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      deleteComponentsByProjectUuid(projectUuid);
      index(
        dbClient
          .componentDao()
          .selectByProjectUuid(projectUuid, dbSession)
          .toArray(new ComponentDto[0]));
    }
  }

  public void deleteByProjectUuid(String uuid) {
    deleteComponentsByProjectUuid(uuid);
    deleteAuthorizationByProjectUuid(uuid);
  }

  private void deleteComponentsByProjectUuid(String projectUuid) {
    BulkIndexer.delete(esClient, INDEX_COMPONENTS, projectUuidSearch(projectUuid));
  }

  private SearchRequestBuilder projectUuidSearch(String projectUuid) {
    return esClient.prepareSearch(INDEX_COMPONENTS)
      .setQuery(projectUuidFilter(projectUuid));
  }

  private static BoolQueryBuilder projectUuidFilter(String projectUuid) {
    return boolQuery()
      .filter(
        termQuery(ComponentIndexDefinition.FIELD_PROJECT_UUID, projectUuid));
  }

  private void deleteAuthorizationByProjectUuid(String projectUuid) {
    esClient
      .prepareDelete(INDEX_COMPONENTS, TYPE_AUTHORIZATION, projectUuid)
      .setRouting(projectUuid)
      .setRefresh(true)
      .get();
  }

  void index(ComponentDto... docs) {
    Future<?> submit = executor.submit(() -> indexNow(docs));
    try {
      Uninterruptibles.getUninterruptibly(submit);
    } catch (ExecutionException e) {
      Throwables.propagate(e);
    }
  }

  private void indexNow(ComponentDto... docs) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX_COMPONENTS);
    bulk.setLarge(false);
    bulk.start();
    Arrays.stream(docs)
      .map(ComponentIndexer::toDocument)
      .map(ComponentIndexer::newIndexRequest)
      .forEach(bulk::add);
    bulk.stop();
  }

  private static IndexRequest newIndexRequest(ComponentDoc doc) {
    return new IndexRequest(INDEX_COMPONENTS, TYPE_COMPONENT, doc.getId())
      .routing(doc.getRouting())
      .parent(doc.getParent())
      .source(doc.getFields());
  }

  public static ComponentDoc toDocument(ComponentDto component) {
    return new ComponentDoc()
      .setId(component.uuid())
      .setName(component.name())
      .setKey(component.key())
      .setProjectUuid(component.projectUuid())
      .setQualifier(component.qualifier());
  }

  @Override
  public void start() {
    // no action required, setup is done in constructor
  }

  @Override
  public void stop() {
    executor.shutdown();
  }
}
