/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.es.EsQueueDto;
import org.sonar.server.es.AnalysisIndexer;
import org.sonar.server.es.BaseDoc;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EventIndexer;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.Indexers;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.es.OneToManyResilientIndexingListener;
import org.sonar.server.permission.index.AuthorizationDoc;
import org.sonar.server.permission.index.AuthorizationScope;
import org.sonar.server.permission.index.NeedAuthorizationIndexer;

import static java.util.Collections.emptyList;
import static org.sonar.server.component.index.ComponentIndexDefinition.TYPE_COMPONENT;

/**
 * Indexes the definition of all entities: projects, applications, portfolios and sub-portfolios.
 */
public class EntityDefinitionIndexer implements EventIndexer, AnalysisIndexer, NeedAuthorizationIndexer {

  private static final AuthorizationScope AUTHORIZATION_SCOPE = new AuthorizationScope(TYPE_COMPONENT, entity -> true);
  private static final Set<IndexType> INDEX_TYPES = Set.of(TYPE_COMPONENT);

  private final DbClient dbClient;
  private final EsClient esClient;

  public EntityDefinitionIndexer(DbClient dbClient, EsClient esClient) {
    this.dbClient = dbClient;
    this.esClient = esClient;
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return INDEX_TYPES;
  }

  @Override
  public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
    doIndexByEntityUuid(Size.LARGE);
  }

  public void indexAll() {
    doIndexByEntityUuid(Size.REGULAR);
  }

  @Override
  public void indexOnAnalysis(String branchUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<BranchDto> branchDto = dbClient.branchDao().selectByUuid(dbSession, branchUuid);

      if (branchDto.isPresent() && !branchDto.get().isMain()) {
        return;
      }
      EntityDto entity = dbClient.entityDao().selectByComponentUuid(dbSession, branchUuid)
        .orElseThrow(() -> new IllegalStateException("Can't find entity for branch " + branchUuid));
      doIndexByEntityUuid(entity);
    }
  }

  @Override
  public AuthorizationScope getAuthorizationScope() {
    return AUTHORIZATION_SCOPE;
  }

  @Override
  public Collection<EsQueueDto> prepareForRecoveryOnEntityEvent(DbSession dbSession, Collection<String> entityUuids, Indexers.EntityEvent cause) {
    return switch (cause) {
      case PROJECT_TAGS_UPDATE, PERMISSION_CHANGE ->
        // measures, tags and permissions does not affect the definition of entities
        emptyList();
      case CREATION, DELETION, PROJECT_KEY_UPDATE -> {
        List<EsQueueDto> items = createEsQueueDtosFromEntities(entityUuids);
        yield dbClient.esQueueDao().insert(dbSession, items);
      }
    };
  }

  private static List<EsQueueDto> createEsQueueDtosFromEntities(Collection<String> entityUuids) {
    return entityUuids.stream()
      .map(entityUuid -> EsQueueDto.create(TYPE_COMPONENT.format(), entityUuid, null, entityUuid))
      .toList();
  }

  @Override
  public Collection<EsQueueDto> prepareForRecoveryOnBranchEvent(DbSession dbSession, Collection<String> branchUuids, Indexers.BranchEvent cause) {
    return emptyList();
  }

  @Override
  public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
    if (items.isEmpty()) {
      return new IndexingResult();
    }

    OneToManyResilientIndexingListener listener = new OneToManyResilientIndexingListener(dbClient, dbSession, items);
    BulkIndexer bulkIndexer = new BulkIndexer(esClient, TYPE_COMPONENT, Size.REGULAR, listener);
    bulkIndexer.start();
    Set<String> entityUuids = items.stream().map(EsQueueDto::getDocId).collect(Collectors.toSet());
    Set<String> remaining = new HashSet<>(entityUuids);

    dbClient.entityDao().selectByUuids(dbSession, entityUuids).forEach(dto -> {
      remaining.remove(dto.getUuid());
      bulkIndexer.add(toDocument(dto).toIndexRequest());
    });

    // the remaining uuids reference projects that don't exist in db. They must
    // be deleted from index.
    remaining.forEach(projectUuid -> addProjectDeletionToBulkIndexer(bulkIndexer, projectUuid));

    return bulkIndexer.stop();
  }

  /**
   * @param entity the entity to analyze, or {@code null} if all content should be indexed.<br/>
   *               <b>Warning:</b> only use {@code null} during startup.
   */
  private void doIndexByEntityUuid(EntityDto entity) {
    BulkIndexer bulk = new BulkIndexer(esClient, TYPE_COMPONENT, Size.REGULAR);
    bulk.start();

    try (DbSession dbSession = dbClient.openSession(false)) {
      bulk.add(toDocument(entity).toIndexRequest());

      if (entity.getQualifier().equals("VW")) {
        dbClient.portfolioDao().selectTree(dbSession, entity.getUuid()).forEach(sub ->
          bulk.add(toDocument(sub).toIndexRequest()));
      }
    }

    bulk.stop();
  }

  private void doIndexByEntityUuid(Size bulkSize) {
    BulkIndexer bulk = new BulkIndexer(esClient, TYPE_COMPONENT, bulkSize);
    bulk.start();
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.entityDao().scrollForIndexing(dbSession, context -> {
        EntityDto dto = context.getResultObject();
        bulk.add(toDocument(dto).toIndexRequest());
      });
    }

    bulk.stop();
  }

  private static void addProjectDeletionToBulkIndexer(BulkIndexer bulkIndexer, String projectUuid) {
    SearchRequest searchRequest = EsClient.prepareSearch(TYPE_COMPONENT.getMainType())
      .source(new SearchSourceBuilder().query(QueryBuilders.termQuery(ComponentIndexDefinition.FIELD_UUID, projectUuid)))
      .routing(AuthorizationDoc.idOf(projectUuid));
    bulkIndexer.addDeletion(searchRequest);
  }

  @VisibleForTesting
  void index(EntityDto... docs) {
    BulkIndexer bulk = new BulkIndexer(esClient, TYPE_COMPONENT, Size.REGULAR);
    bulk.start();
    Arrays.stream(docs)
      .map(EntityDefinitionIndexer::toDocument)
      .map(BaseDoc::toIndexRequest)
      .forEach(bulk::add);
    bulk.stop();
  }

  public static ComponentDoc toDocument(EntityDto entity) {
    return new ComponentDoc()
      .setOrganization(entity.getOrganizationUuid())
      .setId(entity.getUuid())
      .setAuthUuid(entity.getAuthUuid())
      .setName(entity.getName())
      .setKey(entity.getKey())
      .setQualifier(entity.getQualifier());
  }
}
