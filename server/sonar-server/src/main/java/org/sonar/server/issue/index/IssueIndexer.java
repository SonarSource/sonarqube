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

import java.util.Iterator;
import javax.annotation.Nullable;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.es.BaseIndexer;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;

public class IssueIndexer extends BaseIndexer {

  private final DbClient dbClient;

  public IssueIndexer(DbClient dbClient, EsClient esClient) {
    super(esClient, 300, IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_ISSUE, IssueIndexDefinition.FIELD_ISSUE_TECHNICAL_UPDATED_AT);
    this.dbClient = dbClient;
  }

  @Override
  protected long doIndex(long lastUpdatedAt) {
    return doIndex(createBulkIndexer(false), lastUpdatedAt, null);
  }

  public void indexAll() {
    doIndex(createBulkIndexer(true), 0L, null);
  }

  public void index(final String projectUuid) {
    super.index(new IndexerTask() {
      @Override
      public long index(long lastUpdatedAt) {
        return doIndex(createBulkIndexer(false), lastUpdatedAt, projectUuid);
      }
    });
  }

  /**
   * For benchmarks
   */
  public void index(Iterator<IssueDoc> issues) {
    doIndex(createBulkIndexer(false), issues);
  }

  private long doIndex(BulkIndexer bulk, long lastUpdatedAt, @Nullable String projectUuid) {
    DbSession dbSession = dbClient.openSession(false);
    long maxDate;
    try {
      IssueResultSetIterator rowIt = IssueResultSetIterator.create(dbClient, dbSession, lastUpdatedAt, projectUuid);
      maxDate = doIndex(bulk, rowIt);
      rowIt.close();
      return maxDate;
    } finally {
      dbSession.close();
    }
  }

  private long doIndex(BulkIndexer bulk, Iterator<IssueDoc> issues) {
    bulk.start();
    long maxDate = 0L;
    while (issues.hasNext()) {
      IssueDoc issue = issues.next();
      bulk.add(newUpsertRequest(issue));

      // it's more efficient to sort programmatically than in SQL on some databases (MySQL for instance)
      maxDate = Math.max(maxDate, issue.getTechnicalUpdateDate().getTime());
    }
    bulk.stop();
    return maxDate;
  }

  public void deleteProject(String uuid, boolean refresh) {
    BulkIndexer bulk = new BulkIndexer(esClient, IssueIndexDefinition.INDEX);
    bulk.setDisableRefresh(!refresh);
    bulk.start();
    SearchRequestBuilder search = esClient.prepareSearch(IssueIndexDefinition.INDEX)
      .setRouting(uuid)
      .setQuery(QueryBuilders.filteredQuery(
        QueryBuilders.matchAllQuery(),
        FilterBuilders.boolFilter().must(FilterBuilders.termsFilter(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID, uuid))
        ));
    bulk.addDeletion(search);
    bulk.stop();
  }

  BulkIndexer createBulkIndexer(boolean large) {
    BulkIndexer bulk = new BulkIndexer(esClient, IssueIndexDefinition.INDEX);
    bulk.setLarge(large);
    return bulk;
  }

  private UpdateRequest newUpsertRequest(IssueDoc issue) {
    String projectUuid = issue.projectUuid();

    // type of parent doc is "authorization"
    issue.setField("_parent", projectUuid);

    return new UpdateRequest(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_ISSUE, issue.key())
      .routing(projectUuid)
      .parent(projectUuid)
      .doc(issue.getFields())
      .upsert(issue.getFields());
  }

}
