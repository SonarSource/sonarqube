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
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.es.BaseIndexer;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_AUTHORIZATION_GROUPS;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_AUTHORIZATION_PROJECT_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_AUTHORIZATION_UPDATED_AT;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_AUTHORIZATION_USERS;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_TECHNICAL_UPDATED_AT;
import static org.sonar.server.issue.index.IssueIndexDefinition.INDEX;
import static org.sonar.server.issue.index.IssueIndexDefinition.TYPE_AUTHORIZATION;

/**
 * Manages the synchronization of index issues/authorization with authorization settings defined in database :
 * <ul>
 *   <li>index the projects with recent permission changes</li>
 *   <li>delete project orphans from index</li>
 * </ul>
 */
public class AuthorizationIndexer extends BaseIndexer {

  private final DbClient dbClient;

  public AuthorizationIndexer(DbClient dbClient, EsClient esClient) {
    super(esClient, 0L, INDEX, TYPE_AUTHORIZATION, FIELD_ISSUE_TECHNICAL_UPDATED_AT);
    this.dbClient = dbClient;
  }

  @Override
  protected long doIndex(long lastUpdatedAt) {
    return doIndex(createBulkIndexer(), Collections.<String>emptyList());
  }

  public void index(String projectUuid) {
    index(asList(projectUuid));
  }

  public void index(List<String> projectUuids) {
    checkArgument(!projectUuids.isEmpty(), "ProjectUuids cannot be empty");
    super.index(lastUpdatedAt -> doIndex(createBulkIndexer(), projectUuids));
  }

  private long doIndex(BulkIndexer bulk, List<String> projectUuids) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      AuthorizationDao dao = new AuthorizationDao();
      Collection<AuthorizationDao.Dto> authorizations = dao.selectAfterDate(dbClient, dbSession, projectUuids);
      return doIndex(bulk, authorizations);
    }
  }

  @VisibleForTesting
  public void index(Collection<AuthorizationDao.Dto> authorizations) {
    final BulkIndexer bulk = new BulkIndexer(esClient, INDEX);
    doIndex(bulk, authorizations);
  }

  private static long doIndex(BulkIndexer bulk, Collection<AuthorizationDao.Dto> authorizations) {
    long maxDate = 0L;
    bulk.start();
    for (AuthorizationDao.Dto authorization : authorizations) {
      bulk.add(newIndexRequest(authorization));
      maxDate = Math.max(maxDate, authorization.getUpdatedAt());
    }
    bulk.stop();
    return maxDate;
  }

  public void deleteProject(String uuid, boolean refresh) {
    esClient
      .prepareDelete(INDEX, TYPE_AUTHORIZATION, uuid)
      .setRefresh(refresh)
      .setRouting(uuid)
      .get();
  }

  private BulkIndexer createBulkIndexer() {
    // warning - do not enable large mode, else disabling of replicas
    // will impact the type "issue" which is much bigger than issueAuthorization
    return new BulkIndexer(esClient, INDEX);
  }

  private static IndexRequest newIndexRequest(AuthorizationDao.Dto dto) {
    Map<String, Object> doc = ImmutableMap.of(
      FIELD_AUTHORIZATION_PROJECT_UUID, dto.getProjectUuid(),
      FIELD_AUTHORIZATION_GROUPS, dto.getGroups(),
      FIELD_AUTHORIZATION_USERS, dto.getUsers(),
      FIELD_AUTHORIZATION_UPDATED_AT, new Date(dto.getUpdatedAt()));
    return new IndexRequest(INDEX, TYPE_AUTHORIZATION, dto.getProjectUuid())
      .routing(dto.getProjectUuid())
      .source(doc);
  }
}
