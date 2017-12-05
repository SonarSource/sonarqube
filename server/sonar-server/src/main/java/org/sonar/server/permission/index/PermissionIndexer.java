/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.es.EsQueueDto;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.es.OneToOneResilientIndexingListener;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.permission.index.PermissionIndexerDao.Dto;

import static java.util.Collections.emptyList;
import static org.sonar.core.util.stream.MoreCollectors.toArrayList;
import static org.sonar.core.util.stream.MoreCollectors.toSet;

/**
 * Populates the types "authorization" of each index requiring project
 * authorization.
 */
public class PermissionIndexer implements ProjectIndexer {

  private final DbClient dbClient;
  private final EsClient esClient;
  private final Collection<AuthorizationScope> authorizationScopes;
  private final Set<IndexType> indexTypes;

  public PermissionIndexer(DbClient dbClient, EsClient esClient, NeedAuthorizationIndexer... needAuthorizationIndexers) {
    this(dbClient, esClient, Arrays.stream(needAuthorizationIndexers)
      .map(NeedAuthorizationIndexer::getAuthorizationScope)
      .collect(MoreCollectors.toList(needAuthorizationIndexers.length)));
  }

  @VisibleForTesting
  public PermissionIndexer(DbClient dbClient, EsClient esClient, Collection<AuthorizationScope> authorizationScopes) {
    this.dbClient = dbClient;
    this.esClient = esClient;
    this.authorizationScopes = authorizationScopes;
    this.indexTypes = authorizationScopes.stream()
      .map(AuthorizationScope::getIndexType)
      .collect(toSet(authorizationScopes.size()));
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return indexTypes;
  }

  @Override
  public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
    // TODO do not load everything in memory. Db rows should be scrolled.
    List<Dto> authorizations = getAllAuthorizations();
    Stream<AuthorizationScope> scopes = getScopes(uninitializedIndexTypes);
    index(authorizations, scopes, Size.LARGE);
  }

  @VisibleForTesting
  void index(List<Dto> authorizations) {
    index(authorizations, authorizationScopes.stream(), Size.REGULAR);
  }

  @Override
  public void indexOnAnalysis(String branchUuid) {
    // nothing to do, permissions don't change during an analysis
  }

  @Override
  public Collection<EsQueueDto> prepareForRecovery(DbSession dbSession, Collection<String> projectUuids, ProjectIndexer.Cause cause) {
    switch (cause) {
      case MEASURE_CHANGE:
      case PROJECT_KEY_UPDATE:
      case PROJECT_TAGS_UPDATE:
        // nothing to change. Measures, project key and tags are not part of this index
        return emptyList();

      case PROJECT_CREATION:
      case PROJECT_DELETION:
      case PERMISSION_CHANGE:
        return insertIntoEsQueue(dbSession, projectUuids);

      default:
        // defensive case
        throw new IllegalStateException("Unsupported cause: " + cause);
    }
  }

  private Collection<EsQueueDto> insertIntoEsQueue(DbSession dbSession, Collection<String> projectUuids) {
    List<EsQueueDto> items = indexTypes.stream()
      .flatMap(indexType -> projectUuids.stream().map(projectUuid -> EsQueueDto.create(indexType.format(), projectUuid, null, projectUuid)))
      .collect(toArrayList());

    dbClient.esQueueDao().insert(dbSession, items);
    return items;
  }

  private void index(Collection<PermissionIndexerDao.Dto> authorizations, Stream<AuthorizationScope> scopes, Size bulkSize) {
    if (authorizations.isEmpty()) {
      return;
    }

    // index each authorization in each scope
    scopes.forEach(scope -> {
      IndexType indexType = scope.getIndexType();

      BulkIndexer bulkIndexer = new BulkIndexer(esClient, indexType, bulkSize);
      bulkIndexer.start();

      authorizations.stream()
        .filter(scope.getProjectPredicate())
        .map(dto -> newIndexRequest(dto, indexType))
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
      .map(IndexType::parse)
      .filter(indexTypes::contains)
      .map(indexType -> new BulkIndexer(esClient, indexType, Size.REGULAR, new OneToOneResilientIndexingListener(dbClient, dbSession, items)))
      .collect(Collectors.toList());

    if (bulkIndexers.isEmpty()) {
      return result;
    }

    bulkIndexers.forEach(BulkIndexer::start);

    PermissionIndexerDao permissionIndexerDao = new PermissionIndexerDao();
    Set<String> remainingProjectUuids = items.stream().map(EsQueueDto::getDocId).collect(MoreCollectors.toHashSet());
    permissionIndexerDao.selectByUuids(dbClient, dbSession, remainingProjectUuids).forEach(p -> {
      remainingProjectUuids.remove(p.getProjectUuid());
      bulkIndexers.forEach(bi -> bi.add(newIndexRequest(p, bi.getIndexType())));
    });

    // the remaining references on projects that don't exist in db. They must
    // be deleted from index.
    remainingProjectUuids.forEach(projectUuid -> bulkIndexers.forEach(bi -> bi.addDeletion(bi.getIndexType(), projectUuid, projectUuid)));

    bulkIndexers.forEach(b -> result.add(b.stop()));

    return result;
  }

  private static IndexRequest newIndexRequest(PermissionIndexerDao.Dto dto, IndexType indexType) {
    Map<String, Object> doc = new HashMap<>();
    if (dto.isAllowAnyone()) {
      doc.put(AuthorizationTypeSupport.FIELD_ALLOW_ANYONE, true);
      // no need to feed users and groups
    } else {
      doc.put(AuthorizationTypeSupport.FIELD_ALLOW_ANYONE, false);
      doc.put(AuthorizationTypeSupport.FIELD_GROUP_IDS, dto.getGroupIds());
      doc.put(AuthorizationTypeSupport.FIELD_USER_IDS, dto.getUserIds());
    }
    return new IndexRequest(indexType.getIndex(), indexType.getType())
      .id(dto.getProjectUuid())
      .routing(dto.getProjectUuid())
      .source(doc);
  }

  private Stream<AuthorizationScope> getScopes(Set<IndexType> indexTypes) {
    return authorizationScopes.stream()
      .filter(scope -> indexTypes.contains(scope.getIndexType()));
  }

  private List<Dto> getAllAuthorizations() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return new PermissionIndexerDao().selectAll(dbClient, dbSession);
    }
  }
}
