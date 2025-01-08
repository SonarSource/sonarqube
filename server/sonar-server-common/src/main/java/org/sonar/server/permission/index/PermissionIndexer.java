/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.permission.index;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.es.EsQueueDto;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EventIndexer;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.Indexers;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.es.OneToOneResilientIndexingListener;
import org.springframework.beans.factory.annotation.Autowired;

import static java.util.Collections.emptyList;

/**
 * Populates the types "authorization" of each index requiring entity
 * authorization.
 */
public class PermissionIndexer implements EventIndexer {
  private final DbClient dbClient;
  private final EsClient esClient;
  private final Collection<AuthorizationScope> authorizationScopes;
  private final Map<String, IndexType> indexTypeByFormat;

  @Autowired(required = false)
  public PermissionIndexer(DbClient dbClient, EsClient esClient, NeedAuthorizationIndexer... needAuthorizationIndexers) {
    this(dbClient, esClient, Arrays.stream(needAuthorizationIndexers)
      .map(NeedAuthorizationIndexer::getAuthorizationScope)
      .toList());
  }

  @VisibleForTesting
  @Autowired(required = false)
  public PermissionIndexer(DbClient dbClient, EsClient esClient, Collection<AuthorizationScope> authorizationScopes) {
    this.dbClient = dbClient;
    this.esClient = esClient;
    this.authorizationScopes = authorizationScopes;
    this.indexTypeByFormat = authorizationScopes.stream()
      .map(AuthorizationScope::getIndexType)
      .collect(Collectors.toMap(IndexType.IndexMainType::format, Function.identity()));
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return ImmutableSet.copyOf(indexTypeByFormat.values());
  }

  @Override
  public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
    // TODO do not load everything in memory. Db rows should be scrolled.
    List<IndexPermissions> authorizations = getAllAuthorizations();
    Stream<AuthorizationScope> scopes = getScopes(uninitializedIndexTypes);
    index(authorizations, scopes, Size.LARGE);
  }

  public void indexAll(Set<IndexType> uninitializedIndexTypes) {
    // TODO do not load everything in memory. Db rows should be scrolled.
    List<IndexPermissions> authorizations = getAllAuthorizations();
    Stream<AuthorizationScope> scopes = getScopes(uninitializedIndexTypes);
    index(authorizations, scopes, Size.REGULAR);
  }

  @VisibleForTesting
  void index(List<IndexPermissions> authorizations) {
    index(authorizations, authorizationScopes.stream(), Size.REGULAR);
  }

  @Override
  public Collection<EsQueueDto> prepareForRecoveryOnEntityEvent(DbSession dbSession, Collection<String> entityUuids, Indexers.EntityEvent cause) {
    return switch (cause) {
      case PROJECT_KEY_UPDATE, PROJECT_TAGS_UPDATE ->
        // nothing to change. project key and tags are not part of this index
        emptyList();
      case CREATION, DELETION, PERMISSION_CHANGE -> insertIntoEsQueue(dbSession, entityUuids);
    };
  }

  @Override
  public Collection<EsQueueDto> prepareForRecoveryOnBranchEvent(DbSession dbSession, Collection<String> branchUuids, Indexers.BranchEvent cause) {
    return emptyList();
  }

  private Collection<EsQueueDto> insertIntoEsQueue(DbSession dbSession, Collection<String> projectUuids) {
    List<EsQueueDto> items = indexTypeByFormat.values().stream()
      .flatMap(indexType -> projectUuids.stream().map(projectUuid -> EsQueueDto.create(indexType.format(), AuthorizationDoc.idOf(projectUuid), null, projectUuid)))
      .toList();

    dbClient.esQueueDao().insert(dbSession, items);
    return items;
  }

  private void index(Collection<IndexPermissions> authorizations, Stream<AuthorizationScope> scopes, Size bulkSize) {
    if (authorizations.isEmpty()) {
      return;
    }

    // index each authorization in each scope
    scopes.forEach(scope -> {
      IndexType indexType = scope.getIndexType();

      BulkIndexer bulkIndexer = new BulkIndexer(esClient, indexType, bulkSize);
      bulkIndexer.start();

      authorizations.stream()
        .filter(scope.getEntityPredicate())
        .map(dto -> AuthorizationDoc.fromDto(indexType, dto).toIndexRequest())
        .forEach(bulkIndexer::add);

      bulkIndexer.stop();
    });
  }

  @Override
  public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
    IndexingResult result = new IndexingResult();

    List<BulkIndexer> bulkIndexers = items.stream()
      .map(EsQueueDto::getDocType)
      .distinct()
      .map(indexTypeByFormat::get)
      .filter(Objects::nonNull)
      .map(indexType -> new BulkIndexer(esClient, indexType, Size.REGULAR, new OneToOneResilientIndexingListener(dbClient, dbSession, items)))
      .toList();

    if (bulkIndexers.isEmpty()) {
      return result;
    }

    bulkIndexers.forEach(BulkIndexer::start);

    PermissionIndexerDao permissionIndexerDao = new PermissionIndexerDao();
    Set<String> remainingEntityUuids = items.stream().map(EsQueueDto::getDocId)
      .map(AuthorizationDoc::entityUuidOf)
      .collect(Collectors.toSet());
    permissionIndexerDao.selectByUuids(dbClient, dbSession, remainingEntityUuids).forEach(p -> {
      remainingEntityUuids.remove(p.getEntityUuid());
      bulkIndexers.forEach(bi -> bi.add(AuthorizationDoc.fromDto(bi.getIndexType(), p).toIndexRequest()));
    });

    // the remaining references on entities that don't exist in db. They must
    // be deleted from the index.
    remainingEntityUuids.forEach(entityUuid -> bulkIndexers.forEach(bi -> {
      String authorizationDocId = AuthorizationDoc.idOf(entityUuid);
      bi.addDeletion(bi.getIndexType(), authorizationDocId, authorizationDocId);
    }));

    bulkIndexers.forEach(b -> result.add(b.stop()));

    return result;
  }

  private Stream<AuthorizationScope> getScopes(Set<IndexType> indexTypes) {
    return authorizationScopes.stream()
      .filter(scope -> indexTypes.contains(scope.getIndexType()));
  }

  private List<IndexPermissions> getAllAuthorizations() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return new PermissionIndexerDao().selectAll(dbClient, dbSession);
    }
  }
}
