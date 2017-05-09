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
package org.sonar.server.es;

import com.google.common.collect.ImmutableMap;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.es.BulkIndexer.Size;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.es.FakeIndexDefinition.INDEX;
import static org.sonar.server.es.FakeIndexDefinition.INDEX_TYPE_FAKE;

public class BulkIndexerTest {

  @Rule
  public EsTester esTester = new EsTester(new FakeIndexDefinition().setReplicas(1));

  @Test
  public void index_nothing() {
    BulkIndexer indexer = new BulkIndexer(esTester.client(), INDEX, Size.REGULAR);
    indexer.start();
    indexer.stop();

    assertThat(count()).isEqualTo(0);
  }

  @Test
  public void index_documents() {
    BulkIndexer indexer = new BulkIndexer(esTester.client(), INDEX, Size.REGULAR);
    indexer.start();
    indexer.add(newIndexRequest(42));
    indexer.add(newIndexRequest(78));

    // request is not sent yet
    assertThat(count()).isEqualTo(0);

    // send remaining requests
    indexer.stop();
    assertThat(count()).isEqualTo(2);
  }

  @Test
  public void large_indexing() {
    // index has one replica
    assertThat(replicas()).isEqualTo(1);

    BulkIndexer indexer = new BulkIndexer(esTester.client(), INDEX, Size.LARGE);
    indexer.start();

    // replicas are temporarily disabled
    assertThat(replicas()).isEqualTo(0);

    for (int i = 0; i < 10; i++) {
      indexer.add(newIndexRequest(i));
    }
    indexer.stop();

    assertThat(count()).isEqualTo(10);

    // replicas are re-enabled
    assertThat(replicas()).isEqualTo(1);
  }

  @Test
  public void bulk_delete() throws Exception {
    int max = 500;
    int removeFrom = 200;
    FakeDoc[] docs = new FakeDoc[max];
    for (int i = 0; i < max; i++) {
      docs[i] = FakeIndexDefinition.newDoc(i);
    }
    esTester.putDocuments(INDEX_TYPE_FAKE, docs);
    assertThat(count()).isEqualTo(max);

    SearchRequestBuilder req = esTester.client().prepareSearch(INDEX_TYPE_FAKE)
      .setQuery(QueryBuilders.rangeQuery(FakeIndexDefinition.INT_FIELD).gte(removeFrom));
    BulkIndexer.delete(esTester.client(), INDEX, req);

    assertThat(count()).isEqualTo(removeFrom);
  }

  private long count() {
    return esTester.countDocuments("fakes", "fake");
  }

  private int replicas() {
    GetSettingsResponse settingsResp = esTester.client().nativeClient().admin().indices()
      .prepareGetSettings(INDEX).get();
    return Integer.parseInt(settingsResp.getSetting(INDEX, IndexMetaData.SETTING_NUMBER_OF_REPLICAS));
  }

  private IndexRequest newIndexRequest(int intField) {
    return new IndexRequest(INDEX, INDEX_TYPE_FAKE.getType())
      .source(ImmutableMap.of(FakeIndexDefinition.INT_FIELD, intField));
  }
}
