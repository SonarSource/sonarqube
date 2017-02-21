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
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.picocontainer.Startable;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsUtils;
import org.sonar.server.es.IndexTypeId;
import org.sonar.server.es.ProjectIndexer;

import static com.google.common.base.Preconditions.checkArgument;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

/**
 * Manages the synchronization of indexes with authorization settings defined in database:
 * <ul>
 *   <li>index the projects with recent permission changes</li>
 *   <li>delete project orphans from index</li>
 * </ul>
 */
public class PermissionIndexer implements ProjectIndexer, Startable {

  @VisibleForTesting
  static final int MAX_BATCH_SIZE = 1000;

  private static final Logger LOG = Loggers.get(PermissionIndexer.class);
  private static final String BULK_ERROR_MESSAGE = "Fail to index authorization";

  private final ThreadPoolExecutor executor;
  private final DbClient dbClient;
  private final EsClient esClient;
  private final Collection<AuthorizationScope> authorizationScopes;

  public PermissionIndexer(DbClient dbClient, EsClient esClient, NeedAuthorizationIndexer... needAuthorizationIndexers) {
    this(dbClient, esClient, Arrays.stream(needAuthorizationIndexers)
      .map(NeedAuthorizationIndexer::getAuthorizationScope)
      .collect(Collectors.toList(needAuthorizationIndexers.length)));
  }

  @VisibleForTesting
  public PermissionIndexer(DbClient dbClient, EsClient esClient, Collection<AuthorizationScope> authorizationScopes) {
    this.executor = new ThreadPoolExecutor(0, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    this.dbClient = dbClient;
    this.esClient = esClient;
    this.authorizationScopes = authorizationScopes;
  }

  public void initializeOnStartup() {
    LOG.info("Index authorization");
    indexAllIfEmpty();
  }

  public void indexAllIfEmpty() {
    boolean isEmpty = false;
    for (AuthorizationScope scope : authorizationScopes) {
      isEmpty |= isAuthorizationTypeEmpty(scope.getIndexType());
    }

    if (isEmpty) {
      Future<?> submit = executor.submit(() -> {
        authorizationScopes.stream()
          .map(AuthorizationScope::getIndexType)
          .forEach(this::truncateAuthorizationType);

        try (DbSession dbSession = dbClient.openSession(false)) {
          index(new PermissionIndexerDao().selectAll(dbClient, dbSession));
        }
      });
      try {
        Uninterruptibles.getUninterruptibly(submit);
      } catch (ExecutionException e) {
        Throwables.propagate(e);
      }
    }
  }

  public void indexProjectsByUuids(DbSession dbSession, List<String> viewOrProjectUuids) {
    checkArgument(!viewOrProjectUuids.isEmpty(), "viewOrProjectUuids cannot be empty");
    PermissionIndexerDao dao = new PermissionIndexerDao();
    index(dao.selectByUuids(dbClient, dbSession, viewOrProjectUuids));
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
        // nothing to do, key is not used in this index
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
      .setRefresh(true)
      .get());
  }

  private boolean isAuthorizationTypeEmpty(IndexTypeId indexType) {
    SearchResponse response = esClient.prepareSearch(indexType).setSize(0).get();
    return response.getHits().getTotalHits() == 0;
  }

  private void truncateAuthorizationType(IndexTypeId indexType) {
    BulkIndexer.delete(esClient, indexType.getIndex(), esClient.prepareSearch(indexType).setQuery(matchAllQuery()));
  }

  @VisibleForTesting
  void index(Collection<PermissionIndexerDao.Dto> authorizations) {
    if (authorizations.isEmpty()) {
      return;
    }
    int count = 0;
    BulkRequestBuilder bulkRequest = esClient.prepareBulk().setRefresh(false);
    for (PermissionIndexerDao.Dto dto : authorizations) {
      for (AuthorizationScope scope : authorizationScopes) {
        if (scope.getProjectPredicate().test(dto)) {
          bulkRequest.add(newIndexRequest(dto, scope.getIndexType()));
          count++;
        }
      }
      if (count >= MAX_BATCH_SIZE) {
        EsUtils.executeBulkRequest(bulkRequest, BULK_ERROR_MESSAGE);
        bulkRequest = esClient.prepareBulk().setRefresh(false);
        count = 0;
      }
    }
    if (count > 0) {
      EsUtils.executeBulkRequest(bulkRequest, BULK_ERROR_MESSAGE);
    }
    authorizationScopes.forEach(type -> esClient.prepareRefresh(type.getIndexType().getIndex()).get());
  }

  private static IndexRequest newIndexRequest(PermissionIndexerDao.Dto dto, IndexTypeId indexTypeId) {
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
    return new IndexRequest(indexTypeId.getIndex(), indexTypeId.getType(), dto.getProjectUuid())
      .routing(dto.getProjectUuid())
      .source(doc);
  }

  @Override
  public void start() {
    // nothing to do at startup
  }

  @Override
  public void stop() {
    executor.shutdown();
  }
}
