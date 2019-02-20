/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.es.EsQueueDto;
import org.sonar.server.es.BaseDoc;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.es.OneToManyResilientIndexingListener;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.permission.index.AuthorizationDoc;
import org.sonar.server.permission.index.AuthorizationScope;
import org.sonar.server.permission.index.NeedAuthorizationIndexer;

import static java.util.Collections.emptyList;
import static org.sonar.server.component.index.ComponentIndexDefinition.TYPE_COMPONENT;

public class ComponentIndexer implements ProjectIndexer, NeedAuthorizationIndexer {

  private static final AuthorizationScope AUTHORIZATION_SCOPE = new AuthorizationScope(TYPE_COMPONENT, project -> true);
  private static final ImmutableSet<IndexType> INDEX_TYPES = ImmutableSet.of(TYPE_COMPONENT);

  private final DbClient dbClient;
  private final EsClient esClient;

  public ComponentIndexer(DbClient dbClient, EsClient esClient) {
    this.dbClient = dbClient;
    this.esClient = esClient;
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return INDEX_TYPES;
  }

  @Override
  public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
    doIndexByProjectUuid(null, Size.LARGE);
  }

  @Override
  public void indexOnAnalysis(String branchUuid) {
    doIndexByProjectUuid(branchUuid, Size.REGULAR);
  }

  @Override
  public AuthorizationScope getAuthorizationScope() {
    return AUTHORIZATION_SCOPE;
  }

  @Override
  public Collection<EsQueueDto> prepareForRecovery(DbSession dbSession, Collection<String> projectUuids, Cause cause) {
    switch (cause) {
      case MEASURE_CHANGE:
      case PROJECT_TAGS_UPDATE:
      case PERMISSION_CHANGE:
        // measures, tags and permissions are not part of type components/component
        return emptyList();

      case PROJECT_CREATION:
      case PROJECT_DELETION:
      case PROJECT_KEY_UPDATE:
        List<EsQueueDto> items = projectUuids.stream()
          .map(branchUuid -> EsQueueDto.create(TYPE_COMPONENT.format(), branchUuid, null, branchUuid))
          .collect(MoreCollectors.toArrayList(projectUuids.size()));
        return dbClient.esQueueDao().insert(dbSession, items);

      default:
        // defensive case
        throw new IllegalStateException("Unsupported cause: " + cause);
    }
  }

  @Override
  public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
    if (items.isEmpty()) {
      return new IndexingResult();
    }

    OneToManyResilientIndexingListener listener = new OneToManyResilientIndexingListener(dbClient, dbSession, items);
    BulkIndexer bulkIndexer = new BulkIndexer(esClient, TYPE_COMPONENT, Size.REGULAR, listener);
    bulkIndexer.start();
    Set<String> branchUuids = items.stream().map(EsQueueDto::getDocId).collect(MoreCollectors.toHashSet(items.size()));
    Set<String> remaining = new HashSet<>(branchUuids);

    for (String branchUuid : branchUuids) {
      // TODO allow scrolling multiple projects at the same time
      dbClient.componentDao().scrollForIndexing(dbSession, branchUuid, context -> {
        ComponentDto dto = context.getResultObject();
        bulkIndexer.add(toDocument(dto).toIndexRequest());
        remaining.remove(dto.projectUuid());
      });
    }

    // the remaining uuids reference projects that don't exist in db. They must
    // be deleted from index.
    remaining.forEach(projectUuid -> addProjectDeletionToBulkIndexer(bulkIndexer, projectUuid));

    return bulkIndexer.stop();
  }

  /**
   * @param projectUuid the uuid of the project to analyze, or {@code null} if all content should be indexed.<br/>
   * <b>Warning:</b> only use {@code null} during startup.
   */
  private void doIndexByProjectUuid(@Nullable String projectUuid, Size bulkSize) {
    BulkIndexer bulk = new BulkIndexer(esClient, TYPE_COMPONENT, bulkSize);

    bulk.start();
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.componentDao()
        .scrollForIndexing(dbSession, projectUuid, context -> {
          ComponentDto dto = context.getResultObject();
          bulk.add(toDocument(dto).toIndexRequest());
        });
    }
    bulk.stop();
  }

  private void addProjectDeletionToBulkIndexer(BulkIndexer bulkIndexer, String projectUuid) {
    SearchRequestBuilder searchRequest = esClient.prepareSearch(TYPE_COMPONENT.getMainType())
      .setQuery(QueryBuilders.termQuery(ComponentIndexDefinition.FIELD_PROJECT_UUID, projectUuid))
      .setRouting(AuthorizationDoc.idOf(projectUuid));
    bulkIndexer.addDeletion(searchRequest);
  }

  public void delete(String projectUuid, Collection<String> disabledComponentUuids) {
    BulkIndexer bulk = new BulkIndexer(esClient, TYPE_COMPONENT, Size.REGULAR);
    bulk.start();
    disabledComponentUuids.forEach(uuid -> bulk.addDeletion(TYPE_COMPONENT, uuid, AuthorizationDoc.idOf(projectUuid)));
    bulk.stop();
  }

  @VisibleForTesting
  void index(ComponentDto... docs) {
    BulkIndexer bulk = new BulkIndexer(esClient, TYPE_COMPONENT, Size.REGULAR);
    bulk.start();
    Arrays.stream(docs)
      .map(ComponentIndexer::toDocument)
      .map(BaseDoc::toIndexRequest)
      .forEach(bulk::add);
    bulk.stop();
  }

  public static ComponentDoc toDocument(ComponentDto component) {
    return new ComponentDoc()
      .setId(component.uuid())
      .setName(component.name())
      .setKey(component.getDbKey())
      .setProjectUuid(component.projectUuid())
      .setOrganization(component.getOrganizationUuid())
      .setLanguage(component.language())
      .setQualifier(component.qualifier());
  }
}
