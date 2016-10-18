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
package org.sonar.server.permission.index;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Collection;
import java.util.Date;
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
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.component.es.ProjectMeasuresIndexDefinition;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsUtils;
import org.sonar.server.issue.index.IssueIndexDefinition;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

/**
 * Manages the synchronization of index issues/authorization with authorization settings defined in database :
 * <ul>
 *   <li>index the projects with recent permission changes</li>
 *   <li>delete project orphans from index</li>
 * </ul>
 */
public class AuthorizationIndexer implements Startable {

  private static final int MAX_BATCH_SIZE = 1000;

  private static final String BULK_ERROR_MESSAGE = "Fail to index authorization";

  private final ThreadPoolExecutor executor;
  private final DbClient dbClient;
  private final EsClient esClient;

  public AuthorizationIndexer(DbClient dbClient, EsClient esClient) {
    this.executor = new ThreadPoolExecutor(0, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    this.dbClient = dbClient;
    this.esClient = esClient;
  }

  /**
   * Index issues authorization and project measures authorization indexes only when they are empty
   */
  public void indexAllIfEmpty() {
    Future submit = executor.submit(() -> {
      if (isIndexEmpty(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION) ||
        isIndexEmpty(ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES, ProjectMeasuresIndexDefinition.TYPE_AUTHORIZATION)) {
        truncate(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION);
        truncate(ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES, ProjectMeasuresIndexDefinition.TYPE_AUTHORIZATION);
        try (DbSession dbSession = dbClient.openSession(false)) {
          index(new AuthorizationDao().selectAll(dbClient, dbSession));
        }
      }
    });
    try {
      Uninterruptibles.getUninterruptibly(submit);
    } catch (ExecutionException e) {
      Throwables.propagate(e);
    }
  }

  private boolean isIndexEmpty(String index, String type) {
    SearchResponse issuesAuthorizationResponse = esClient.prepareSearch(index).setTypes(type).setSize(0).get();
    return issuesAuthorizationResponse.getHits().getTotalHits() == 0;
  }

  private void truncate(String index, String type) {
    BulkIndexer.delete(esClient, index, esClient.prepareSearch(index).setTypes(type).setQuery(matchAllQuery()));
  }

  public void index(List<String> projectUuids) {
    checkArgument(!projectUuids.isEmpty(), "ProjectUuids cannot be empty");
    try (DbSession dbSession = dbClient.openSession(false)) {
      AuthorizationDao dao = new AuthorizationDao();
      index(dao.selectByProjects(dbClient, dbSession, projectUuids));
    }
  }

  private void index(Collection<AuthorizationDao.Dto> authorizations) {
    if (authorizations.isEmpty()) {
      return;
    }
    int count = 0;
    BulkRequestBuilder bulkRequest = esClient.prepareBulk().setRefresh(false);
    for (AuthorizationDao.Dto dto : authorizations) {
      bulkRequest.add(newIssuesAuthorizationIndexRequest(dto));
      bulkRequest.add(newProjectMeasuresAuthorizationIndexRequest(dto));
      count++;
      if (count >= MAX_BATCH_SIZE) {
        EsUtils.executeBulkRequest(bulkRequest, BULK_ERROR_MESSAGE);
        bulkRequest = esClient.prepareBulk().setRefresh(false);
        count = 0;
      }
    }
    EsUtils.executeBulkRequest(bulkRequest, BULK_ERROR_MESSAGE);
    esClient.prepareRefresh(IssueIndexDefinition.INDEX).get();
    esClient.prepareRefresh(ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES).get();
  }

  public void index(String projectUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      AuthorizationDao dao = new AuthorizationDao();
      List<AuthorizationDao.Dto> dtos = dao.selectByProjects(dbClient, dbSession, singletonList(projectUuid));
      if (dtos.size() == 1) {
        index(dtos.get(0));
      }
    }
  }

  @VisibleForTesting
  void index(AuthorizationDao.Dto dto) {
    index(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION, newIssuesAuthorizationIndexRequest(dto));
    index(ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES, ProjectMeasuresIndexDefinition.TYPE_AUTHORIZATION, newProjectMeasuresAuthorizationIndexRequest(dto));
  }

  private void index(String index, String type, IndexRequest indexRequest) {
    esClient.prepareIndex(index, type)
      .setId(indexRequest.id())
      .setRouting(indexRequest.routing())
      .setSource(indexRequest.source())
      .setRefresh(true)
      .get();
  }

  public void deleteProject(String uuid, boolean refresh) {
    esClient
      .prepareDelete(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION, uuid)
      .setRefresh(refresh)
      .setRouting(uuid)
      .get();
    esClient
      .prepareDelete(ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES, ProjectMeasuresIndexDefinition.TYPE_AUTHORIZATION, uuid)
      .setRefresh(refresh)
      .setRouting(uuid)
      .get();
  }

  private static IndexRequest newIssuesAuthorizationIndexRequest(AuthorizationDao.Dto dto) {
    Map<String, Object> doc = ImmutableMap.of(
      IssueIndexDefinition.FIELD_AUTHORIZATION_PROJECT_UUID, dto.getProjectUuid(),
      IssueIndexDefinition.FIELD_AUTHORIZATION_GROUPS, dto.getGroups(),
      IssueIndexDefinition.FIELD_AUTHORIZATION_USERS, dto.getUsers(),
      IssueIndexDefinition.FIELD_AUTHORIZATION_UPDATED_AT, new Date(dto.getUpdatedAt()));
    return new IndexRequest(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION, dto.getProjectUuid())
      .routing(dto.getProjectUuid())
      .source(doc);
  }

  private static IndexRequest newProjectMeasuresAuthorizationIndexRequest(AuthorizationDao.Dto dto) {
    Map<String, Object> doc = ImmutableMap.of(
      ProjectMeasuresIndexDefinition.FIELD_AUTHORIZATION_PROJECT_UUID, dto.getProjectUuid(),
      ProjectMeasuresIndexDefinition.FIELD_AUTHORIZATION_GROUPS, dto.getGroups(),
      ProjectMeasuresIndexDefinition.FIELD_AUTHORIZATION_USERS, dto.getUsers(),
      ProjectMeasuresIndexDefinition.FIELD_AUTHORIZATION_UPDATED_AT, new Date(dto.getUpdatedAt()));
    return new IndexRequest(ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES, ProjectMeasuresIndexDefinition.TYPE_AUTHORIZATION, dto.getProjectUuid())
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
