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
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BulkIndexerTest {

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new FakeIndexDefinition().setReplicas(1));

  @Test
  public void index_nothing() throws Exception {
    esTester.truncateIndices();

    BulkIndexer indexer = new BulkIndexer(esTester.client(), FakeIndexDefinition.INDEX);
    indexer.start();
    indexer.stop();

    assertThat(count()).isEqualTo(0);
  }

  @Test
  public void index_documents() throws Exception {
    BulkIndexer indexer = new BulkIndexer(esTester.client(), FakeIndexDefinition.INDEX)
      .setRefresh(true);
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
  public void large_indexing() throws Exception {
    // index has one replica
    assertThat(replicas()).isEqualTo(1);

    BulkIndexer indexer = new BulkIndexer(esTester.client(), FakeIndexDefinition.INDEX)
      .setLarge(true)
      .setFlushByteSize(new ByteSizeValue(1, ByteSizeUnit.BYTES).bytes());
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

  private long count() {
    return esTester.countDocuments("fakes", "fake");
  }

  private int replicas() {
    GetSettingsResponse settingsResp = esTester.client().nativeClient().admin().indices()
      .prepareGetSettings(FakeIndexDefinition.INDEX).get();
    return Integer.parseInt(settingsResp.getSetting(FakeIndexDefinition.INDEX, IndexMetaData.SETTING_NUMBER_OF_REPLICAS));
  }

  private IndexRequest newIndexRequest(int intField) {
    return new IndexRequest(FakeIndexDefinition.INDEX, FakeIndexDefinition.TYPE)
      .source(ImmutableMap.of(FakeIndexDefinition.INT_FIELD, intField));
  }
}
