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
package org.sonar.server.search;

import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.Settings;
import org.sonar.process.ProcessProperties;
import org.sonar.server.es.EsServerHolder;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class BaseIndexTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  SearchClient searchClient;

  @Before
  public void setup() throws IOException {
    EsServerHolder holder = EsServerHolder.get();
    Settings settings = new Settings();
    settings.setProperty(ProcessProperties.CLUSTER_ACTIVATE, false);
    settings.setProperty(ProcessProperties.CLUSTER_NAME, holder.getClusterName());
    settings.setProperty(ProcessProperties.CLUSTER_NODE_NAME, holder.getNodeName());
    settings.setProperty(ProcessProperties.SEARCH_PORT, String.valueOf(holder.getPort()));
    settings.setProperty(ProcessProperties.SEARCH_HOST, String.valueOf(holder.getHostName()));
    searchClient = new SearchClient(settings);
  }

  @After
  public void tearDown() throws Exception {
    if (searchClient != null) {
      searchClient.stop();
    }
  }

  @Test
  public void can_load() {
    BaseIndex index = getIndex(searchClient);
    assertThat(index).isNotNull();
  }

  @Test
  public void creates_domain_index() {
    BaseIndex index = getIndex(this.searchClient);

    IndicesExistsResponse indexExistsResponse = index.getClient().admin().indices()
      .prepareExists(IndexDefinition.TEST.getIndexName()).execute().actionGet();

    assertThat(indexExistsResponse.isExists()).isTrue();
  }

  @Test
  public void settings_has_no_replication_factor() {
    BaseIndex index = getIndex(this.searchClient);

    // base case, there are no replication factors.
    assertThat(index.getIndexSettings().get("index.number_of_replicas")).isNull();

    // replication factor removed from settings when set in index
    BaseIndex newIndex = new BaseIndex(
      IndexDefinition.TEST,
      null, searchClient) {

      @Override
      protected String getKeyValue(Serializable key) {
        return null;
      }

      @Override
      protected Map mapProperties() {
        return Collections.emptyMap();
      }

      @Override
      protected Map mapKey() {
        return Collections.emptyMap();
      }

      @Override
      public Object toDoc(Map fields) {
        return null;
      }
    };
    newIndex.start();

    assertThat(index.getIndexSettings().get("index.number_of_replicas")).isNull();

  }

  private BaseIndex getIndex(final SearchClient searchClient) {
    BaseIndex index = new BaseIndex(
      IndexDefinition.TEST,
      null, searchClient) {
      @Override
      protected String getKeyValue(Serializable key) {
        return null;
      }

      @Override
      protected Map mapProperties() {
        return Collections.emptyMap();
      }

      @Override
      protected Map mapKey() {
        return Collections.emptyMap();
      }

      @Override
      public Object toDoc(Map fields) {
        return null;
      }
    };
    index.start();
    return index;
  }
}
