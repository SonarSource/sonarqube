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
package org.sonar.server.test.index;

import java.util.Iterator;
import javax.annotation.Nullable;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.es.BaseIndexer;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;
import org.sonar.server.source.index.FileSourcesUpdaterHelper;

import static org.sonar.server.test.index.TestIndexDefinition.FIELD_FILE_UUID;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_UPDATED_AT;
import static org.sonar.server.test.index.TestIndexDefinition.INDEX;
import static org.sonar.server.test.index.TestIndexDefinition.TYPE;

/**
 * Add to Elasticsearch index {@link TestIndexDefinition} the rows of
 * db table FILE_SOURCES of type TEST that are not indexed yet
 */
public class TestIndexer extends BaseIndexer {

  private final DbClient dbClient;

  public TestIndexer(System2 system2, DbClient dbClient, EsClient esClient) {
    super(system2, esClient, 0L, INDEX, TYPE, FIELD_UPDATED_AT);
    this.dbClient = dbClient;
  }

  public void index(final String projectUuid) {
    deleteByProject(projectUuid);
    super.index(lastUpdatedAt -> doIndex(lastUpdatedAt, projectUuid));
  }

  public long index(Iterator<FileSourcesUpdaterHelper.Row> dbRows) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX);
    return doIndex(bulk, dbRows);
  }

  @Override
  protected long doIndex(long lastUpdatedAt) {
    return doIndex(lastUpdatedAt, null);
  }

  private long doIndex(long lastUpdatedAt, @Nullable String projectUuid) {
    final BulkIndexer bulk = new BulkIndexer(esClient, INDEX);
    bulk.setLarge(lastUpdatedAt == 0L);

    DbSession dbSession = dbClient.openSession(false);
    try {
      TestResultSetIterator rowIt = TestResultSetIterator.create(dbClient, dbSession, lastUpdatedAt, projectUuid);
      long maxUpdatedAt = doIndex(bulk, rowIt);
      rowIt.close();
      return maxUpdatedAt;

    } finally {
      dbSession.close();
    }
  }

  private static long doIndex(BulkIndexer bulk, Iterator<FileSourcesUpdaterHelper.Row> dbRows) {
    long maxUpdatedAt = 0L;
    bulk.start();
    while (dbRows.hasNext()) {
      FileSourcesUpdaterHelper.Row row = dbRows.next();
      row.getUpdateRequests().forEach(bulk::add);
      maxUpdatedAt = Math.max(maxUpdatedAt, row.getUpdatedAt());
    }
    bulk.stop();
    return maxUpdatedAt;
  }

  public void deleteByFile(String fileUuid) {
    SearchRequestBuilder searchRequest = esClient.prepareSearch(INDEX)
      .setTypes(TYPE)
      .setQuery(QueryBuilders.termsQuery(FIELD_FILE_UUID, fileUuid));
    BulkIndexer.delete(esClient, INDEX, searchRequest);
  }

  public void deleteByProject(String projectUuid) {
    SearchRequestBuilder searchRequest = esClient.prepareSearch(INDEX)
      .setRouting(projectUuid)
      .setTypes(TYPE)
      .setQuery(QueryBuilders.termQuery(TestIndexDefinition.FIELD_PROJECT_UUID, projectUuid));
    BulkIndexer.delete(esClient, INDEX, searchRequest);
  }
}
