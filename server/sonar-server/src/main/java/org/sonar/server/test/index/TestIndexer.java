/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import com.google.common.collect.ImmutableSet;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nullable;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.es.StartupIndexer;
import org.sonar.server.source.index.FileSourcesUpdaterHelper;

import static org.sonar.server.test.index.TestIndexDefinition.FIELD_FILE_UUID;
import static org.sonar.server.test.index.TestIndexDefinition.INDEX_TYPE_TEST;

/**
 * Add to Elasticsearch index {@link TestIndexDefinition} the rows of
 * db table FILE_SOURCES of type TEST that are not indexed yet
 */
public class TestIndexer implements ProjectIndexer, StartupIndexer {

  private final DbClient dbClient;
  private final EsClient esClient;

  public TestIndexer(DbClient dbClient, EsClient esClient) {
    this.dbClient = dbClient;
    this.esClient = esClient;
  }

  @Override
  public void indexProject(String projectUuid, Cause cause) {
    switch (cause) {
      case PROJECT_CREATION:
        // no need to index, not tests at that time
      case PROJECT_KEY_UPDATE:
      case PROJECT_TAGS_UPDATE:
        // no need to index, project key and tags are not used
        break;
      case NEW_ANALYSIS:
        deleteProject(projectUuid);
        doIndex(projectUuid, Size.REGULAR);
        break;
      default:
        // defensive case
        throw new IllegalStateException("Unsupported cause: " + cause);
    }
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return ImmutableSet.of(INDEX_TYPE_TEST);
  }

  @Override
  public void indexOnStartup(Set<IndexType> emptyIndexTypes) {
    doIndex(null, Size.LARGE);
  }

  public long index(Iterator<FileSourcesUpdaterHelper.Row> dbRows) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX_TYPE_TEST.getIndex(), Size.REGULAR);
    return doIndex(bulk, dbRows);
  }

  private long doIndex(@Nullable String projectUuid, Size bulkSize) {
    final BulkIndexer bulk = new BulkIndexer(esClient, INDEX_TYPE_TEST.getIndex(), bulkSize);

    try (DbSession dbSession = dbClient.openSession(false)) {
      TestResultSetIterator rowIt = TestResultSetIterator.create(dbClient, dbSession, projectUuid);
      long maxUpdatedAt = doIndex(bulk, rowIt);
      rowIt.close();
      return maxUpdatedAt;
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
    SearchRequestBuilder searchRequest = esClient.prepareSearch(INDEX_TYPE_TEST)
      .setQuery(QueryBuilders.termsQuery(FIELD_FILE_UUID, fileUuid));
    BulkIndexer.delete(esClient, INDEX_TYPE_TEST.getIndex(), searchRequest);
  }

  @Override
  public void deleteProject(String projectUuid) {
    SearchRequestBuilder searchRequest = esClient.prepareSearch(INDEX_TYPE_TEST)
      .setTypes(INDEX_TYPE_TEST.getType())
      .setQuery(QueryBuilders.termQuery(TestIndexDefinition.FIELD_PROJECT_UUID, projectUuid));
    BulkIndexer.delete(esClient, INDEX_TYPE_TEST.getIndex(), searchRequest);
  }
}
