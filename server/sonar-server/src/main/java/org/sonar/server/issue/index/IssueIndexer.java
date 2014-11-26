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

import org.apache.commons.dbutils.DbUtils;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.BaseIndexer;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;

import java.sql.Connection;
import java.util.Iterator;

public class IssueIndexer extends BaseIndexer {

  private final DbClient dbClient;

  public IssueIndexer(DbClient dbClient, EsClient esClient) {
    super(esClient, 300, IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_ISSUE);
    this.dbClient = dbClient;
  }

  @Override
  protected long doIndex(long lastUpdatedAt) {
    final BulkIndexer bulk = createBulkIndexer(lastUpdatedAt == 0L);

    DbSession dbSession = dbClient.openSession(false);
    Connection dbConnection = dbSession.getConnection();
    long maxDate;
    try {
      IssueResultSetIterator rowIt = IssueResultSetIterator.create(dbClient, dbConnection, lastUpdatedAt);
      maxDate = index(bulk, rowIt);
      rowIt.close();
      return maxDate;

    } finally {
      DbUtils.closeQuietly(dbConnection);
      dbSession.close();
    }
  }

  public long index(BulkIndexer bulk, Iterator<IssueDoc> issues) {
    bulk.start();
    long maxDate = 0L;
    while (issues.hasNext()) {
      IssueDoc issue = issues.next();
      bulk.add(newUpsertRequest(issue));

      // it's more efficient to sort programmatically than in SQL on some databases (MySQL for instance)
      maxDate = Math.max(maxDate, issue.updateDate().getTime());
    }
    bulk.stop();
    return maxDate;
  }

  public void deleteProject(String uuid, boolean refresh) {
    QueryBuilder query = QueryBuilders.filteredQuery(
      QueryBuilders.matchAllQuery(),
      FilterBuilders.boolFilter().must(FilterBuilders.termsFilter(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID, uuid))
    );
    esClient.prepareDeleteByQuery(IssueIndexDefinition.INDEX).setQuery(query).get();
    if (refresh) {
      esClient.prepareRefresh(IssueIndexDefinition.INDEX).get();
    }
  }

  BulkIndexer createBulkIndexer(boolean large) {
    BulkIndexer bulk = new BulkIndexer(esClient, IssueIndexDefinition.INDEX);
    bulk.setLarge(large);
    return bulk;
  }

  private UpdateRequest newUpsertRequest(IssueDoc issue) {
    String projectUuid = issue.projectUuid();

    // parent doc is issueAuthorization
    issue.setField("_parent", projectUuid);

    return new UpdateRequest(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_ISSUE, issue.key())
      .routing(projectUuid)
      .parent(projectUuid)
      .doc(issue.getFields())
      .upsert(issue.getFields());
  }

}
