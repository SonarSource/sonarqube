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
package org.sonar.server.issue.index;

import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.es.BaseIndexer;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsUtils;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_TECHNICAL_UPDATED_AT;
import static org.sonar.server.issue.index.IssueIndexDefinition.INDEX;
import static org.sonar.server.issue.index.IssueIndexDefinition.TYPE_AUTHORIZATION;
import static org.sonar.server.issue.index.IssueIndexDefinition.TYPE_ISSUE;

public class IssueIndexer extends BaseIndexer {

  private static final String DELETE_ERROR_MESSAGE = "Fail to delete some issues of project [%s]";
  private static final int MAX_BATCH_SIZE = 1000;

  private final DbClient dbClient;

  public IssueIndexer(DbClient dbClient, EsClient esClient) {
    super(esClient, 300, INDEX, TYPE_ISSUE, FIELD_ISSUE_TECHNICAL_UPDATED_AT);
    this.dbClient = dbClient;
  }

  @Override
  protected long doIndex(long lastUpdatedAt) {
    return doIndex(createBulkIndexer(false), lastUpdatedAt, null);
  }

  public void indexAll() {
    doIndex(createBulkIndexer(true), 0L, null);
  }

  public void index(String projectUuid) {
    super.index(lastUpdatedAt -> doIndex(createBulkIndexer(false), lastUpdatedAt, projectUuid));
  }

  /**
   * For benchmarks
   */
  public void index(Iterator<IssueDoc> issues) {
    doIndex(createBulkIndexer(false), issues);
  }

  private long doIndex(BulkIndexer bulk, long lastUpdatedAt, @Nullable String projectUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      IssueResultSetIterator rowIt = IssueResultSetIterator.create(dbClient, dbSession, lastUpdatedAt, projectUuid);
      long maxDate = doIndex(bulk, rowIt);
      rowIt.close();
      return maxDate;
    }
  }

  private long doIndex(BulkIndexer bulk, Iterator<IssueDoc> issues) {
    bulk.start();
    long maxDate = 0L;
    while (issues.hasNext()) {
      IssueDoc issue = issues.next();
      bulk.add(newIndexRequest(issue));

      // it's more efficient to sort programmatically than in SQL on some databases (MySQL for instance)
      maxDate = Math.max(maxDate, issue.getTechnicalUpdateDate().getTime());
    }
    bulk.stop();
    return maxDate;
  }

  public void deleteProject(String uuid) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX);
    bulk.setDisableRefresh(false);
    bulk.start();
    SearchRequestBuilder search = esClient.prepareSearch(INDEX)
      .setTypes(TYPE_ISSUE, TYPE_AUTHORIZATION)
      .setRouting(uuid)
      .setQuery(boolQuery().must(termQuery(FIELD_ISSUE_PROJECT_UUID, uuid)));
    bulk.addDeletion(search);
    bulk.stop();
  }

  public void deleteByKeys(String projectUuid, List<String> issueKeys) {
    if (issueKeys.isEmpty()) {
      return;
    }

    int count = 0;
    BulkRequestBuilder builder = esClient.prepareBulk();
    for (String issueKey : issueKeys) {
      builder.add(esClient.prepareDelete(INDEX, TYPE_ISSUE, issueKey)
        .setRefresh(false)
        .setRouting(projectUuid));
      count++;
      if (count >= MAX_BATCH_SIZE) {
        EsUtils.executeBulkRequest(builder, DELETE_ERROR_MESSAGE, projectUuid);
        builder = esClient.prepareBulk();
        count = 0;
      }
    }
    EsUtils.executeBulkRequest(builder, DELETE_ERROR_MESSAGE, projectUuid);
    esClient.prepareRefresh(INDEX).get();
  }

  private BulkIndexer createBulkIndexer(boolean large) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX);
    bulk.setLarge(large);
    return bulk;
  }

  private static IndexRequest newIndexRequest(IssueDoc issue) {
    String projectUuid = issue.projectUuid();

    return new IndexRequest(INDEX, TYPE_ISSUE, issue.key())
      .routing(projectUuid)
      .parent(projectUuid)
      .source(issue.getFields());
  }

}
