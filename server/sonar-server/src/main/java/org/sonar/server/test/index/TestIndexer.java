/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.es.EsQueueDto;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexingListener;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.es.OneToManyResilientIndexingListener;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.source.index.FileSourcesUpdaterHelper;

import static java.util.Collections.emptyList;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_FILE_UUID;
import static org.sonar.server.test.index.TestIndexDefinition.INDEX_TYPE_TEST;

/**
 * Add to Elasticsearch index {@link TestIndexDefinition} the rows of
 * db table FILE_SOURCES of type TEST that are not indexed yet
 * <p>
 * This indexer is not resilient by itself since it's called by Compute Engine
 */
public class TestIndexer implements ProjectIndexer {

  private final DbClient dbClient;
  private final EsClient esClient;

  public TestIndexer(DbClient dbClient, EsClient esClient) {
    this.dbClient = dbClient;
    this.esClient = esClient;
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return ImmutableSet.of(INDEX_TYPE_TEST);
  }

  @Override
  public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
    try (DbSession dbSession = dbClient.openSession(false);
      TestResultSetIterator rowIt = TestResultSetIterator.create(dbClient, dbSession, null)) {

      BulkIndexer bulkIndexer = new BulkIndexer(esClient, INDEX_TYPE_TEST, Size.LARGE);
      bulkIndexer.start();
      addTestsToBulkIndexer(rowIt, bulkIndexer);
      bulkIndexer.stop();
    }
  }

  @Override
  public void indexOnAnalysis(String branchUuid) {
    BulkIndexer bulkIndexer = new BulkIndexer(esClient, INDEX_TYPE_TEST, Size.REGULAR);
    bulkIndexer.start();
    addProjectDeletionToBulkIndexer(bulkIndexer, branchUuid);
    try (DbSession dbSession = dbClient.openSession(false);
      TestResultSetIterator rowIt = TestResultSetIterator.create(dbClient, dbSession, branchUuid)) {
      addTestsToBulkIndexer(rowIt, bulkIndexer);
    }
    bulkIndexer.stop();
  }

  @Override
  public Collection<EsQueueDto> prepareForRecovery(DbSession dbSession, Collection<String> projectUuids, Cause cause) {
    switch (cause) {
      case PROJECT_CREATION:
        // no tests at that time
      case MEASURE_CHANGE:
      case PROJECT_KEY_UPDATE:
      case PROJECT_TAGS_UPDATE:
      case PERMISSION_CHANGE:
        // Measures, project key, tags and permissions are not part of tests/test
        return emptyList();

      case PROJECT_DELETION:
        List<EsQueueDto> items = projectUuids.stream()
          .map(projectUuid -> EsQueueDto.create(INDEX_TYPE_TEST.format(), projectUuid, null, projectUuid))
          .collect(MoreCollectors.toArrayList(projectUuids.size()));
        return dbClient.esQueueDao().insert(dbSession, items);

      default:
        // defensive case
        throw new IllegalStateException("Unsupported cause: " + cause);
    }
  }

  public void deleteByFile(String fileUuid) {
    SearchRequestBuilder searchRequest = esClient.prepareSearch(INDEX_TYPE_TEST)
      .setQuery(QueryBuilders.termQuery(FIELD_FILE_UUID, fileUuid));
    BulkIndexer.delete(esClient, INDEX_TYPE_TEST, searchRequest);
  }

  @Override
  public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
    // The items are to be deleted
    if (items.isEmpty()) {
      return new IndexingResult();
    }

    IndexingListener listener = new OneToManyResilientIndexingListener(dbClient, dbSession, items);
    BulkIndexer bulkIndexer = new BulkIndexer(esClient, INDEX_TYPE_TEST, Size.REGULAR, listener);
    bulkIndexer.start();
    items.forEach(i -> {
      String projectUuid = i.getDocId();
      addProjectDeletionToBulkIndexer(bulkIndexer, projectUuid);
    });

    return bulkIndexer.stop();
  }

  private void addProjectDeletionToBulkIndexer(BulkIndexer bulkIndexer, String projectUuid) {
    SearchRequestBuilder searchRequest = esClient.prepareSearch(INDEX_TYPE_TEST)
      .setQuery(QueryBuilders.termQuery(TestIndexDefinition.FIELD_PROJECT_UUID, projectUuid));
    bulkIndexer.addDeletion(searchRequest);
  }

  private static void addTestsToBulkIndexer(TestResultSetIterator rowIt, BulkIndexer bulkIndexer) {
    while (rowIt.hasNext()) {
      FileSourcesUpdaterHelper.Row row = rowIt.next();
      row.getUpdateRequests().forEach(bulkIndexer::add);
    }
  }
}
