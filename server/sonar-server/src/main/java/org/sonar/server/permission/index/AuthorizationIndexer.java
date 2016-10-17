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
import java.util.Date;
import java.util.Map;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.es.BaseIndexer;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;
import org.sonar.server.issue.index.IssueIndexDefinition;

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
    super(esClient, 0L, IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION, IssueIndexDefinition.FIELD_ISSUE_TECHNICAL_UPDATED_AT);
    this.dbClient = dbClient;
  }

  @Override
  protected long doIndex(long lastUpdatedAt) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      // warning - do not enable large mode, else disabling of replicas
      // will impact the type "issue" which is much bigger than issueAuthorization
      BulkIndexer bulk = new BulkIndexer(esClient, IssueIndexDefinition.INDEX);

      AuthorizationDao dao = new AuthorizationDao();
      Collection<AuthorizationDao.Dto> authorizations = dao.selectAfterDate(dbClient, dbSession, lastUpdatedAt);
      return doIndex(bulk, authorizations);
    }
  }

  @VisibleForTesting
  public void index(Collection<AuthorizationDao.Dto> authorizations) {
    final BulkIndexer bulk = new BulkIndexer(esClient, IssueIndexDefinition.INDEX);
    doIndex(bulk, authorizations);
  }

  private long doIndex(BulkIndexer bulk, Collection<AuthorizationDao.Dto> authorizations) {
    long maxDate = 0L;
    bulk.start();
    for (AuthorizationDao.Dto authorization : authorizations) {
      bulk.add(newIssueUpdateRequest(authorization));
      maxDate = Math.max(maxDate, authorization.getUpdatedAt());
    }
    bulk.stop();
    return maxDate;
  }

  public void deleteProject(String uuid, boolean refresh) {
    esClient
      .prepareDelete(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION, uuid)
      .setRefresh(refresh)
      .setRouting(uuid)
      .get();
  }

  private static ActionRequest newIssueUpdateRequest(AuthorizationDao.Dto dto) {
    Map<String, Object> doc = ImmutableMap.of(
      IssueIndexDefinition.FIELD_AUTHORIZATION_PROJECT_UUID, dto.getProjectUuid(),
      IssueIndexDefinition.FIELD_AUTHORIZATION_GROUPS, dto.getGroups(),
      IssueIndexDefinition.FIELD_AUTHORIZATION_USERS, dto.getUsers(),
      IssueIndexDefinition.FIELD_AUTHORIZATION_UPDATED_AT, new Date(dto.getUpdatedAt()));
    return new UpdateRequest(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION, dto.getProjectUuid())
      .routing(dto.getProjectUuid())
      .doc(doc)
      .upsert(doc);
  }
}
