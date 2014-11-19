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

import org.elasticsearch.action.update.UpdateRequest;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.index.IssueDoc;

import java.sql.Connection;
import java.util.Iterator;

/**
 * Not thread-safe
 */
public class IssueIndexer implements ServerComponent {

  private final DbClient dbClient;
  private final EsClient esClient;
  private long lastUpdatedAt = 0L;

  public IssueIndexer(DbClient dbClient, EsClient esClient) {
    this.dbClient = dbClient;
    this.esClient = esClient;
  }

  public void indexProjectPermissions() {
    final BulkIndexer bulk = new BulkIndexer(esClient, IssueIndexDefinition.INDEX_ISSUES);

    DbSession dbSession = dbClient.openSession(false);
    Connection dbConnection = dbSession.getConnection();
    try {
      IssueResultSetIterator rowIt = IssueResultSetIterator.create(dbClient, dbConnection, getLastUpdatedAt());
      indexIssues(bulk, rowIt);
      rowIt.close();

    } finally {
      dbSession.close();
    }
  }

  public void indexIssues(boolean large) {
    // TODO support timezones
    final BulkIndexer bulk = new BulkIndexer(esClient, IssueIndexDefinition.INDEX_ISSUES);
    bulk.setLarge(large);

    DbSession dbSession = dbClient.openSession(false);
    Connection dbConnection = dbSession.getConnection();
    try {
      IssueResultSetIterator rowIt = IssueResultSetIterator.create(dbClient, dbConnection, getLastUpdatedAt());
      indexIssues(bulk, rowIt);
      rowIt.close();

    } finally {
      dbSession.close();
    }
  }

  public void indexIssues(BulkIndexer bulk, Iterator<IssueDoc> issues) {
    bulk.start();
    while (issues.hasNext()) {
      IssueDoc issue = issues.next();
      bulk.add(newUpsertRequest(issue));

      // it's more efficient to sort programmatically than in SQL on some databases (MySQL for instance)
      long dtoUpdatedAt = issue.updateDate().getTime();
      if (lastUpdatedAt < dtoUpdatedAt) {
        lastUpdatedAt = dtoUpdatedAt;
      }
    }
    bulk.stop();
  }

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

  private UpdateRequest newUpsertRequest(IssueDoc issue) {
    String projectUuid = issue.projectUuid();
    issue.setField("_parent", projectUuid);
    return new UpdateRequest(IssueIndexDefinition.INDEX_ISSUES, IssueIndexDefinition.TYPE_ISSUE, issue.key())
      .routing(projectUuid)
      .parent(projectUuid)
      .doc(issue.getFields())
      .upsert(issue.getFields());
  }
}
