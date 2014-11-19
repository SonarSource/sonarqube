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
import org.elasticsearch.common.settings.ImmutableSettings;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.Settings;
import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessConstants;
import org.sonar.process.Props;
import org.sonar.search.SearchServer;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

public class BaseIndexTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();
  private static SearchServer searchServer;

  SearchClient searchClient;
  private static String clusterName;
  private static Integer clusterPort;

  @BeforeClass
  public static void setupSearchEngine() {
    clusterName = "cluster-mem-" + System.currentTimeMillis();
    clusterPort = NetworkUtils.freePort();
    Properties properties = new Properties();
    properties.setProperty(ProcessConstants.CLUSTER_NAME, clusterName);
    properties.setProperty(ProcessConstants.CLUSTER_NODE_NAME, "test");
    properties.setProperty(ProcessConstants.SEARCH_PORT, clusterPort.toString());
    properties.setProperty(ProcessConstants.PATH_HOME, temp.getRoot().getAbsolutePath());
    try {
      searchServer = new SearchServer(new Props(properties));
    } catch (Exception e) {
      e.printStackTrace();
    }
    searchServer.start();
  }

  @AfterClass
  public static void teardownSearchEngine() {
    searchServer.stop();
  }

  @Before
  public void setup() throws IOException {
    File dataDir = temp.newFolder();
    Settings settings = new Settings();
    settings.setProperty(ProcessConstants.CLUSTER_ACTIVATE, false);
    settings.setProperty(ProcessConstants.CLUSTER_NAME, clusterName);
    settings.setProperty(ProcessConstants.CLUSTER_NODE_NAME, "test");
    settings.setProperty(ProcessConstants.SEARCH_PORT, clusterPort.toString());
    settings.setProperty(ProcessConstants.PATH_HOME, dataDir.getAbsolutePath());
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
    BaseIndex index = getIndex(this.searchClient);
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
      protected ImmutableSettings.Builder addCustomIndexSettings(ImmutableSettings.Builder baseIndexSettings) {
        return baseIndexSettings.put("index.number_of_replicas", 22);
      }

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
