/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.es;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.DbTester;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.newindex.FakeIndexDefinition;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.es.newindex.FakeIndexDefinition.EXCPECTED_TYPE_FAKE;
import static org.sonar.server.es.newindex.FakeIndexDefinition.INDEX;
import static org.sonar.server.es.newindex.FakeIndexDefinition.TYPE_FAKE;

public class BulkIndexerIT {

  private final TestSystem2 testSystem2 = new TestSystem2().setNow(1_000L);

  @Rule
  public EsTester es = EsTester.createCustom(new FakeIndexDefinition().setReplicas(1));
  @Rule
  public DbTester dbTester = DbTester.create(testSystem2);
  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void index_nothing() {
    BulkIndexer indexer = new BulkIndexer(es.client(), TYPE_FAKE, Size.REGULAR);
    indexer.start();
    indexer.stop();

    assertThat(count()).isZero();
  }

  @Test
  public void index_documents() {
    BulkIndexer indexer = new BulkIndexer(es.client(), TYPE_FAKE, Size.REGULAR);
    indexer.start();
    indexer.add(newIndexRequest(42));
    indexer.add(newIndexRequest(78));

    // request is not sent yet
    assertThat(count()).isZero();

    // send remaining requests
    indexer.stop();
    assertThat(count()).isEqualTo(2);
  }

  @Test
  public void large_indexing() {
    // index has one replica
    assertThat(replicas()).isOne();

    BulkIndexer indexer = new BulkIndexer(es.client(), TYPE_FAKE, Size.LARGE);
    indexer.start();

    // replicas are temporarily disabled
    assertThat(replicas()).isZero();

    for (int i = 0; i < 10; i++) {
      indexer.add(newIndexRequest(i));
    }
    IndexingResult result = indexer.stop();

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSuccess()).isEqualTo(10);
    assertThat(result.getFailures()).isZero();
    assertThat(result.getTotal()).isEqualTo(10);
    assertThat(count()).isEqualTo(10);
    // replicas are re-enabled
    assertThat(replicas()).isOne();
  }

  @Test
  public void bulk_delete() {
    int max = 500;
    int removeFrom = 200;
    FakeDoc[] docs = new FakeDoc[max];
    for (int i = 0; i < max; i++) {
      docs[i] = FakeIndexDefinition.newDoc(i);
    }
    es.putDocuments(TYPE_FAKE, docs);
    assertThat(count()).isEqualTo(max);

    SearchRequest req = EsClient.prepareSearch(TYPE_FAKE)
      .source(new SearchSourceBuilder().query(QueryBuilders.rangeQuery(FakeIndexDefinition.INT_FIELD).gte(removeFrom)));
    BulkIndexer.delete(es.client(), TYPE_FAKE, req);

    assertThat(count()).isEqualTo(removeFrom);
  }

  @Test
  public void listener_is_called_on_successful_requests() {
    FakeListener listener = new FakeListener();
    BulkIndexer indexer = new BulkIndexer(es.client(), TYPE_FAKE, Size.REGULAR, listener);
    indexer.start();
    indexer.addDeletion(TYPE_FAKE, "foo");
    indexer.stop();
    assertThat(listener.calledDocIds)
      .containsExactlyInAnyOrder(newDocId(EXCPECTED_TYPE_FAKE, "foo"));
    assertThat(listener.calledResult.getSuccess()).isOne();
    assertThat(listener.calledResult.getTotal()).isOne();
  }

  @Test
  public void listener_is_called_even_if_deleting_a_doc_that_does_not_exist() {
    FakeListener listener = new FakeListener();
    BulkIndexer indexer = new BulkIndexer(es.client(), TYPE_FAKE, Size.REGULAR, listener);
    indexer.start();
    indexer.add(newIndexRequestWithDocId("foo"));
    indexer.add(newIndexRequestWithDocId("bar"));
    indexer.stop();
    assertThat(listener.calledDocIds)
      .containsExactlyInAnyOrder(newDocId(EXCPECTED_TYPE_FAKE, "foo"), newDocId(EXCPECTED_TYPE_FAKE, "bar"));
    assertThat(listener.calledResult.getSuccess()).isEqualTo(2);
    assertThat(listener.calledResult.getTotal()).isEqualTo(2);
  }

  @Test
  public void listener_is_not_called_with_errors() {
    FakeListener listener = new FakeListener();
    BulkIndexer indexer = new BulkIndexer(es.client(), TYPE_FAKE, Size.REGULAR, listener);
    indexer.start();
    indexer.add(newIndexRequestWithDocId("foo"));
    indexer.add(new IndexRequest("index_does_not_exist").id("bar").source(emptyMap()));
    indexer.stop();
    assertThat(listener.calledDocIds).containsExactly(newDocId(EXCPECTED_TYPE_FAKE, "foo"));
    assertThat(listener.calledResult.getSuccess()).isOne();
    assertThat(listener.calledResult.getTotal()).isEqualTo(2);
  }

  @Test
  public void log_requests_when_TRACE_level_is_enabled() {
    logTester.setLevel(LoggerLevel.TRACE);

    BulkIndexer indexer = new BulkIndexer(es.client(), TYPE_FAKE, Size.REGULAR, new FakeListener());
    indexer.start();
    indexer.add(newIndexRequestWithDocId("foo"));
    indexer.addDeletion(TYPE_FAKE, "foo");
    indexer.add(newIndexRequestWithDocId("bar"));
    indexer.stop();

    assertThat(logTester.logs(Level.TRACE)
      .stream()
      .filter(log -> log.contains("Bulk[2 index requests on fakes/_doc, 1 delete requests on fakes/_doc]"))
      .count()).isNotZero();

  }

  private static class FakeListener implements IndexingListener {
    private final List<DocId> calledDocIds = new ArrayList<>();
    private IndexingResult calledResult;

    @Override
    public void onSuccess(List<DocId> docIds) {
      calledDocIds.addAll(docIds);
    }

    @Override
    public void onFinish(IndexingResult result) {
      calledResult = result;
    }
  }

  private long count() {
    return es.countDocuments(IndexType.main(Index.simple("fakes"), "fake"));
  }

  private int replicas() {
    try {
      GetSettingsResponse settingsResp = es.client().nativeClient().indices()
          .getSettings(new GetSettingsRequest().indices(INDEX), RequestOptions.DEFAULT);
      return Integer.parseInt(settingsResp.getSetting(INDEX, IndexMetadata.SETTING_NUMBER_OF_REPLICAS));
    } catch (IOException e) {
      throw new IllegalStateException("Could not get index settings", e);
    }
  }

  private IndexRequest newIndexRequest(int intField) {
    return new IndexRequest(INDEX)
      .source(Map.of(FakeIndexDefinition.INT_FIELD, intField));
  }

  private IndexRequest newIndexRequestWithDocId(String id) {
    return new IndexRequest(INDEX)
      .id(id)
      .source(Map.of(FakeIndexDefinition.INT_FIELD, 42));
  }

  private static DocId newDocId(IndexType.IndexMainType mainType, String id) {
    return new DocId(TYPE_FAKE.getIndex().getName(), mainType.getType(), id);
  }
}
