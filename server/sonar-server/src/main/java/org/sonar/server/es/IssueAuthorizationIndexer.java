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
package org.sonar.server.es;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.dbutils.DbUtils;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;

import java.sql.Connection;
import java.util.Collection;
import java.util.Map;

public class IssueAuthorizationIndexer implements ServerComponent {

  private final DbClient dbClient;
  private final EsClient esClient;
  private long lastUpdatedAt = 0L;

  public IssueAuthorizationIndexer(DbClient dbClient, EsClient esClient) {
    this.dbClient = dbClient;
    this.esClient = esClient;
  }

  public void index() {
    // warning - do not enable large mode, else disabling of replicas
    // will impact the type "issue" which is much bigger than issueAuthorization
    final BulkIndexer bulk = new BulkIndexer(esClient, IssueIndexDefinition.INDEX_ISSUES);

    DbSession dbSession = dbClient.openSession(false);
    Connection dbConnection = dbSession.getConnection();
    try {
      IssueAuthorizationDao dao = new IssueAuthorizationDao();
      Collection<IssueAuthorizationDao.Dto> authorizations = dao.selectAfterDate(dbClient, dbConnection, getLastUpdatedAt());
      index(bulk, authorizations);

    } finally {
      DbUtils.closeQuietly(dbConnection);
      dbSession.close();
    }
  }

  public void index(BulkIndexer bulk, Collection<IssueAuthorizationDao.Dto> authorizations) {
    bulk.start();
    for (IssueAuthorizationDao.Dto authorization : authorizations) {
      bulk.add(toEsRequest(authorization));

      // it's more efficient to sort programmatically than in SQL on some databases (MySQL for instance)
      long updatedAt = authorization.getUpdatedAt();
      if (lastUpdatedAt < updatedAt) {
        lastUpdatedAt = updatedAt;
      }
    }
    bulk.stop();
  }

  private ActionRequest toEsRequest(IssueAuthorizationDao.Dto dto) {
    ActionRequest request;
    if (dto.isEmpty()) {
      request = new DeleteRequest(IssueIndexDefinition.INDEX_ISSUES, IssueIndexDefinition.TYPE_ISSUE_AUTHORIZATION, dto.getProjectUuid())
        .routing(dto.getProjectUuid());
    } else {
      Map<String, Object> doc = ImmutableMap.of(
        IssueIndexDefinition.FIELD_AUTHORIZATION_PROJECT_UUID, dto.getProjectUuid(),
        IssueIndexDefinition.FIELD_AUTHORIZATION_GROUPS, dto.getGroups(),
        IssueIndexDefinition.FIELD_AUTHORIZATION_USERS, dto.getUsers(),
        IssueIndexDefinition.FIELD_AUTHORIZATION_UPDATED_AT, dto.getUpdatedAt());
      request = new UpdateRequest(IssueIndexDefinition.INDEX_ISSUES, IssueIndexDefinition.TYPE_ISSUE_AUTHORIZATION, dto.getProjectUuid())
        .routing(dto.getProjectUuid())
        .doc(doc)
        .upsert(doc);
    }
    return request;
  }

  // TODO remove duplication with IssueIndexer
  private long getLastUpdatedAt() {
    long result;
    if (lastUpdatedAt <= 0L) {
      // request ES to get the max(updatedAt)
      result = esClient.getLastUpdatedAt(IssueIndexDefinition.INDEX_ISSUES, IssueIndexDefinition.TYPE_ISSUE);
    } else {
      // use cache. Will not work with Tomcat cluster.
      result = lastUpdatedAt;
    }
    return result;
  }
}
