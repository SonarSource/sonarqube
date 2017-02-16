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
package org.sonar.server.component.index;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.api.Startable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.permission.index.AuthorizationScope;
import org.sonar.server.permission.index.NeedAuthorizationIndexer;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.server.component.index.ComponentIndexDefinition.INDEX_COMPONENTS;
import static org.sonar.server.component.index.ComponentIndexDefinition.TYPE_COMPONENT;

public class ComponentIndexer implements ProjectIndexer, NeedAuthorizationIndexer, Startable {

  private static final AuthorizationScope AUTHORIZATION_SCOPE = new AuthorizationScope(INDEX_COMPONENTS, project -> true);

  private final ThreadPoolExecutor executor;
  private final DbClient dbClient;
  private final EsClient esClient;

  public ComponentIndexer(DbClient dbClient, EsClient esClient) {
    this.executor = new ThreadPoolExecutor(0, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    this.dbClient = dbClient;
    this.esClient = esClient;
  }

  /**
   * Make sure, that the component index is filled.
   */
  public void index() {
    if (isEmpty()) {
      reindex();
    }
  }

  @VisibleForTesting
  boolean isEmpty() {
    return esClient.prepareSearch(INDEX_COMPONENTS).setTypes(TYPE_COMPONENT).setSize(0).get().getHits().getTotalHits() <= 0;
  }

  /**
   * Copy all components of all projects to the elastic search index.
   * <p>
   * <b>Warning</b>: This should only be called on an empty index and it should only be called during startup.
   */
  private void reindex() {
    doIndexByProjectUuid(null);
  }

  @Override
  public void indexProject(String projectUuid, Cause cause) {
    switch (cause) {
      case PROJECT_CREATION:
      case PROJECT_KEY_UPDATE:
      case NEW_ANALYSIS:
        deleteProject(projectUuid);
        doIndexByProjectUuid(projectUuid);
        break;
      default:
        // defensive case
        throw new IllegalStateException("Unsupported cause: " + cause);
    }
  }

  @Override
  public AuthorizationScope getAuthorizationScope() {
    return AUTHORIZATION_SCOPE;
  }

  /**
   * @param projectUuid the uuid of the project to analyze, or <code>null</code> if all content should be indexed.<br/>
   * <b>Warning:</b> only use <code>null</code> during startup.
   */
  private void doIndexByProjectUuid(@Nullable String projectUuid) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX_COMPONENTS);

    // setLarge must be enabled only during server startup because it disables replicas
    bulk.setLarge(projectUuid == null);

    bulk.start();
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.componentDao()
        .selectForIndexing(dbSession, projectUuid, context -> {
          ComponentDto dto = (ComponentDto) context.getResultObject();
          bulk.add(newIndexRequest(toDocument(dto)));
        });
    }
    bulk.stop();
  }

  @Override
  public void deleteProject(String projectUuid) {
    BulkIndexer.delete(esClient, INDEX_COMPONENTS, esClient.prepareSearch(INDEX_COMPONENTS)
      .setQuery(boolQuery()
        .filter(
          termQuery(ComponentIndexDefinition.FIELD_PROJECT_UUID, projectUuid))));
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
