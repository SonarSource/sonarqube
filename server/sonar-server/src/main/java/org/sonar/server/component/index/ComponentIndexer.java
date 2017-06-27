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

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.es.StartupIndexer;
import org.sonar.server.permission.index.AuthorizationScope;
import org.sonar.server.permission.index.NeedAuthorizationIndexer;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.server.component.index.ComponentIndexDefinition.INDEX_TYPE_COMPONENT;

public class ComponentIndexer implements ProjectIndexer, NeedAuthorizationIndexer, StartupIndexer {

  private static final AuthorizationScope AUTHORIZATION_SCOPE = new AuthorizationScope(INDEX_TYPE_COMPONENT, project -> true);

  private final DbClient dbClient;
  private final EsClient esClient;

  public ComponentIndexer(DbClient dbClient, EsClient esClient) {
    this.dbClient = dbClient;
    this.esClient = esClient;
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return ImmutableSet.of(ComponentIndexDefinition.INDEX_TYPE_COMPONENT);
  }

  @Override
  public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
    doIndexByProjectUuid(null, Size.LARGE);
  }

  @Override
  public void indexProject(String projectUuid, Cause cause) {
    switch (cause) {
      case PROJECT_TAGS_UPDATE:
        break;
      case PROJECT_CREATION:
      case PROJECT_KEY_UPDATE:
      case NEW_ANALYSIS:
        doIndexByProjectUuid(projectUuid, Size.REGULAR);
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
   * @param projectUuid the uuid of the project to analyze, or {@code null} if all content should be indexed.<br/>
   * <b>Warning:</b> only use {@code null} during startup.
   */
  private void doIndexByProjectUuid(@Nullable String projectUuid, Size bulkSize) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX_TYPE_COMPONENT.getIndex(), bulkSize);

    bulk.start();
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.componentDao()
        .selectForIndexing(dbSession, projectUuid, context -> {
          ComponentDto dto = context.getResultObject();
          bulk.add(newIndexRequest(toDocument(dto)));
        });
    }
    bulk.stop();
  }

  @Override
  public void deleteProject(String projectUuid) {
    BulkIndexer.delete(esClient, INDEX_TYPE_COMPONENT.getIndex(), esClient.prepareSearch(INDEX_TYPE_COMPONENT)
      .setQuery(boolQuery()
        .filter(
          termQuery(ComponentIndexDefinition.FIELD_PROJECT_UUID, projectUuid))));
  }

  public void delete(String projectUuid, Collection<String> disabledComponentUuids) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX_TYPE_COMPONENT.getIndex(), Size.REGULAR);
    bulk.start();
    disabledComponentUuids.forEach(uuid -> bulk.addDeletion(INDEX_TYPE_COMPONENT, uuid, projectUuid));
    bulk.stop();
  }

  void index(ComponentDto... docs) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX_TYPE_COMPONENT.getIndex(), Size.REGULAR);
    bulk.start();
    Arrays.stream(docs)
      .map(ComponentIndexer::toDocument)
      .map(ComponentIndexer::newIndexRequest)
      .forEach(bulk::add);
    bulk.stop();
  }

  private static IndexRequest newIndexRequest(ComponentDoc doc) {
    return new IndexRequest(INDEX_TYPE_COMPONENT.getIndex(), INDEX_TYPE_COMPONENT.getType(), doc.getId())
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
}
