/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.issue.index;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.dbutils.DbUtils;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.BaseIndexer;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;

import java.sql.Connection;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Manages the synchronization of index issues/authorization with authorization settings defined in database :
 * <ul>
 *   <li>index the projects with recent permission changes</li>
 *   <li>delete project orphans from index</li>
 * </ul>
 */
public class IssueAuthorizationIndexer extends BaseIndexer {

  private final DbClient dbClient;

  public IssueAuthorizationIndexer(DbClient dbClient, EsClient esClient) {
    super(esClient, 0L, IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION, IssueIndexDefinition.FIELD_ISSUE_TECHNICAL_UPDATED_AT);
    this.dbClient = dbClient;
  }

  @Override
  protected long doIndex(long lastUpdatedAt) {
    // warning - do not enable large mode, else disabling of replicas
    // will impact the type "issue" which is much bigger than issueAuthorization
    final BulkIndexer bulk = new BulkIndexer(esClient, IssueIndexDefinition.INDEX);

    DbSession dbSession = dbClient.openSession(false);
    Connection dbConnection = dbSession.getConnection();
    try {
      IssueAuthorizationDao dao = new IssueAuthorizationDao();
      Collection<IssueAuthorizationDao.Dto> authorizations = dao.selectAfterDate(dbClient, dbConnection, lastUpdatedAt);
      return doIndex(bulk, authorizations);

    } finally {
      DbUtils.closeQuietly(dbConnection);
      dbSession.close();
    }
  }

  @VisibleForTesting
  public void index(Collection<IssueAuthorizationDao.Dto> authorizations) {
    final BulkIndexer bulk = new BulkIndexer(esClient, IssueIndexDefinition.INDEX);
    doIndex(bulk, authorizations);
  }

  private long doIndex(BulkIndexer bulk, Collection<IssueAuthorizationDao.Dto> authorizations) {
    long maxDate = 0L;
    bulk.start();
    for (IssueAuthorizationDao.Dto authorization : authorizations) {
      bulk.add(newUpdateRequest(authorization));
      maxDate = Math.max(maxDate, authorization.getUpdatedAt());
    }
    bulk.stop();
    return maxDate;
  }

  public void deleteProject(String uuid, boolean refresh) {
    esClient
      .prepareDelete(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION, uuid)
      .setRefresh(refresh)
      .get();
  }

  private ActionRequest newUpdateRequest(IssueAuthorizationDao.Dto dto) {
    ActionRequest request;
    Map<String, Object> doc = ImmutableMap.of(
      IssueIndexDefinition.FIELD_AUTHORIZATION_PROJECT_UUID, dto.getProjectUuid(),
      IssueIndexDefinition.FIELD_AUTHORIZATION_GROUPS, dto.getGroups(),
      IssueIndexDefinition.FIELD_AUTHORIZATION_USERS, dto.getUsers(),
      IssueIndexDefinition.FIELD_AUTHORIZATION_UPDATED_AT, new Date(dto.getUpdatedAt()));
    request = new UpdateRequest(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION, dto.getProjectUuid())
      .routing(dto.getProjectUuid())
      .doc(doc)
      .upsert(doc);
    return request;
  }
}
