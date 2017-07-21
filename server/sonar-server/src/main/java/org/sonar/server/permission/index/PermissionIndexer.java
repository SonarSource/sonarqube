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
package org.sonar.server.permission.index;

import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.es.StartupIndexer;
import org.sonar.server.permission.index.PermissionIndexerDao.Dto;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.server.es.DefaultIndexSettings.REFRESH_IMMEDIATE;

/**
 * Manages the synchronization of indexes with authorization settings defined in database:
 * <ul>
 *   <li>index the projects with recent permission changes</li>
 *   <li>delete project orphans from index</li>
 * </ul>
 */
public class PermissionIndexer implements ProjectIndexer, StartupIndexer {

  @VisibleForTesting
  static final int MAX_BATCH_SIZE = 1000;

  private final DbClient dbClient;
  private final EsClient esClient;
  private final Collection<AuthorizationScope> authorizationScopes;

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
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return authorizationScopes.stream()
      .map(AuthorizationScope::getIndexType)
      .collect(toSet(authorizationScopes.size()));
  }

  @Override
  public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
    List<Dto> authorizations = getAllAuthorizations();
    Stream<AuthorizationScope> scopes = getScopes(uninitializedIndexTypes);
    index(authorizations, scopes, Size.LARGE);
  }

  private List<Dto> getAllAuthorizations() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return new PermissionIndexerDao().selectAll(dbClient, dbSession);
    }
  }

  public void indexProjectsByUuids(DbSession dbSession, List<String> viewOrProjectUuids) {
    checkArgument(!viewOrProjectUuids.isEmpty(), "viewOrProjectUuids cannot be empty");
    PermissionIndexerDao dao = new PermissionIndexerDao();
    List<Dto> authorizations = dao.selectByUuids(dbClient, dbSession, viewOrProjectUuids);
    index(authorizations);
  }

  @VisibleForTesting
  void index(List<Dto> authorizations) {
    index(authorizations, authorizationScopes.stream(), Size.REGULAR);
  }

  @Override
  public void indexProject(String projectUuid, Cause cause) {
    switch (cause) {
      case PROJECT_CREATION:
        // nothing to do, permissions are indexed explicitly
        // when permission template is applied after project creation
      case NEW_ANALYSIS:
        // nothing to do, permissions don't change during an analysis
      case PROJECT_KEY_UPDATE:
      case PROJECT_TAGS_UPDATE:
        // nothing to do, key and tags are not used in this index
        break;
      default:
        // defensive case
        throw new IllegalStateException("Unsupported cause: " + cause);
    }
  }

  @Override
  public void deleteProject(String projectUuid) {
    authorizationScopes.forEach(scope -> esClient
      .prepareDelete(scope.getIndexType(), projectUuid)
      .setRouting(projectUuid)
      .setRefreshPolicy(REFRESH_IMMEDIATE)
      .get());
  }

  private Stream<AuthorizationScope> getScopes(Set<IndexType> indexTypes) {
    return authorizationScopes.stream()
      .filter(scope -> indexTypes.contains(scope.getIndexType()));
  }

  private void index(Collection<PermissionIndexerDao.Dto> authorizations, Stream<AuthorizationScope> scopes, Size bulkSize) {
    if (authorizations.isEmpty()) {
      return;
    }

    // index each authorization in each scope
    scopes.forEach(scope -> index(authorizations, scope, bulkSize));
  }

  private void index(Collection<PermissionIndexerDao.Dto> authorizations, AuthorizationScope scope, Size bulkSize) {
    IndexType indexType = scope.getIndexType();

    BulkIndexer bulkIndexer = new BulkIndexer(esClient, indexType.getIndex(), bulkSize);
    bulkIndexer.start();

    authorizations.stream()
      .filter(scope.getProjectPredicate())
      .map(dto -> newIndexRequest(dto, indexType))
      .forEach(bulkIndexer::add);

    bulkIndexer.stop();
  }

  private static IndexRequest newIndexRequest(PermissionIndexerDao.Dto dto, IndexType indexType) {
    Map<String, Object> doc = new HashMap<>();
    doc.put(AuthorizationTypeSupport.FIELD_UPDATED_AT, DateUtils.longToDate(dto.getUpdatedAt()));
    if (dto.isAllowAnyone()) {
      doc.put(AuthorizationTypeSupport.FIELD_ALLOW_ANYONE, true);
      // no need to feed users and groups
    } else {
      doc.put(AuthorizationTypeSupport.FIELD_ALLOW_ANYONE, false);
      doc.put(AuthorizationTypeSupport.FIELD_GROUP_IDS, dto.getGroupIds());
      doc.put(AuthorizationTypeSupport.FIELD_USER_IDS, dto.getUserIds());
    }
    return new IndexRequest(indexType.getIndex(), indexType.getType(), dto.getProjectUuid())
      .routing(dto.getProjectUuid())
      .source(doc);
  }
}
