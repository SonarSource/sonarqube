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

import com.google.common.io.Resources;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.mapper.StrictDynamicMappingException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.ZipUtils;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ESNodeTest {

  File dataDir;
  Settings settings;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void createMocks() throws IOException {
    dataDir = temp.newFolder();
    settings = new Settings();
    settings.setProperty("sonar.path.data", dataDir.getAbsolutePath());
  }

  @Test
  @Ignore("Need to update this test for remote ES.")
  public void start_and_stop_es_node() throws Exception {
    assertThat(dataDir).doesNotExist();

    ESNode node = new ESNode(settings);
    node.start();

    ClusterAdminClient cluster = node.client().admin().cluster();
    ClusterState state = cluster.state(cluster.prepareState().request()).actionGet().getState();
    assertThat(state.getNodes().size()).isEqualTo(1);
    assertThat(state.getNodes().getMasterNode().isDataNode()).isTrue();
    assertThat(dataDir).exists().isDirectory();

    // REST console is disabled by default
    assertThat(state.getMetaData().settings().get("http.port")).isNull();

    node.stop();

    // data dir is persistent
    assertThat(dataDir).exists().isDirectory();
  }

  @Test(expected = StrictDynamicMappingException.class)
  public void should_use_default_settings_for_index() throws Exception {
    ESNode node = new ESNode(settings);
    node.start();

    node.client().admin().indices().prepareCreate("strict")
      .addMapping("type1", "{\"type1\": {\"properties\": {\"value\": {\"type\": \"string\"}}}}")
      .execute().actionGet();
    node.client().admin().cluster().prepareHealth("strict").setWaitForYellowStatus().get(TimeValue.timeValueMillis(1000));

    // strict mapping is enforced
    try {
      node.client().prepareIndex("strict", "type1", "666").setSource(
        XContentFactory.jsonBuilder().startObject().field("unknown", "plouf").endObject()
      ).get();
    } finally {
      node.stop();
    }
  }

  @Test
  public void check_path_analyzer() throws Exception {
    ESNode node = new ESNode(settings);
    node.start();

    node.client().admin().indices().prepareCreate("path")
      .addMapping("type1", "{\"type1\": {\"properties\": {\"value\": {\"type\": \"string\"}}}}")
      .execute().actionGet();
    node.client().admin().cluster().prepareHealth("path").setWaitForYellowStatus().get(TimeValue.timeValueMillis(1000));

    // default "path_analyzer" analyzer is defined for all indices
    AnalyzeResponse response = node.client().admin().indices()
      .prepareAnalyze("path", "/temp/65236/test path/MyFile.java").setAnalyzer("path_analyzer").get();
    // default "path_analyzer" analyzer is defined for all indices
    assertThat(response.getTokens()).hasSize(4);
    assertThat(response.getTokens().get(0).getTerm()).isEqualTo("/temp");
    assertThat(response.getTokens().get(1).getTerm()).isEqualTo("/temp/65236");
    assertThat(response.getTokens().get(2).getTerm()).isEqualTo("/temp/65236/test path");
    assertThat(response.getTokens().get(3).getTerm()).isEqualTo("/temp/65236/test path/MyFile.java");

    node.stop();
  }

  @Test
  public void check_sortable_analyzer() throws Exception {
    ESNode node = new ESNode(settings);
    node.start();

    node.client().admin().indices().prepareCreate("sort")
      .addMapping("type1", "{\"type1\": {\"properties\": {\"value\": {\"type\": \"string\"}}}}")
      .execute().actionGet();
    node.client().admin().cluster().prepareHealth("sort").setWaitForYellowStatus().get(TimeValue.timeValueMillis(1000));

    // default "sortable" analyzer is defined for all indices
    assertThat(node.client().admin().indices()
      .prepareAnalyze("sort", "This Is A Wonderful Text").setAnalyzer("sortable").get()
      .getTokens().get(0).getTerm()).isEqualTo("this is a ");

    node.stop();
  }

  @Test
  public void check_gram_analyzer() throws Exception {
    ESNode node = new ESNode(settings);
    node.start();

    node.client().admin().indices().prepareCreate("gram")
      .addMapping("type1", "{\"type1\": {\"properties\": {\"value\": {\"type\": \"string\"}}}}")
      .execute().actionGet();
    node.client().admin().cluster().prepareHealth("gram").setWaitForYellowStatus().get(TimeValue.timeValueMillis(1000));

    // default "string_gram" analyzer is defined for all indices
    AnalyzeResponse response = node.client().admin().indices()
      .prepareAnalyze("gram", "he.llo w@rl#d").setAnalyzer("index_grams").get();
    assertThat(response.getTokens()).hasSize(10);
    assertThat(response.getTokens().get(0).getTerm()).isEqualTo("he");
    assertThat(response.getTokens().get(7).getTerm()).isEqualTo("w@rl");

    node.stop();
  }

  @Test
  public void should_restore_status_on_startup() throws Exception {
    File zip = new File(Resources.getResource(getClass(), "ESNodeTest/data-es-clean.zip").toURI());
    ZipUtils.unzip(zip, new File(dataDir, "es"));

    ESNode node = new ESNode(settings);
    node.start();

    AdminClient admin = node.client().admin();
    assertThat(admin.indices().prepareExists("myindex").execute().actionGet().isExists()).isTrue();
    assertThat(admin.cluster().prepareHealth("myindex").setWaitForYellowStatus().execute().actionGet().getStatus()).isIn(ClusterHealthStatus.GREEN, ClusterHealthStatus.YELLOW);

    node.stop();
  }

  @Test(expected = IllegalStateException.class)
  public void should_fail_on_corrupt_index() throws Exception {
    File zip = new File(Resources.getResource(getClass(), "ESNodeTest/data-es-corrupt.zip").toURI());
    ZipUtils.unzip(zip, new File(dataDir, "es"));

    ESNode node = new ESNode(settings, "5s");
    try {
      node.start();
    } finally {
      node.stop();
    }
  }

  @Test
  public void should_fail_to_get_client_if_not_started() {
    ESNode node = new ESNode(settings);
    try {
      node.client();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Elasticsearch is not started");
    }
  }
}
