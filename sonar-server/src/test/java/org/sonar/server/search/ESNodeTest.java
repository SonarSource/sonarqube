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
import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.mapper.StrictDynamicMappingException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.utils.ZipUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ESNodeTest {

  ServerFileSystem fs;
  File homedir;
  File dataDir;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void createMocks() throws IOException {
    homedir = temp.newFolder();
    fs = mock(ServerFileSystem.class);
    when(fs.getHomeDir()).thenReturn(homedir);
    dataDir = new File(homedir, ESNode.DATA_DIR);
  }

  @After
  public void cleanUp() {
    FileUtils.deleteQuietly(homedir);
  }

  @Test
  public void start_and_stop_es_node() throws Exception {
    assertThat(dataDir).doesNotExist();

    ESNode node = new ESNode(fs, new Settings());
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
    ESNode node = new ESNode(fs, new Settings());
    node.start();

    node.client().admin().indices().prepareCreate("polop")
      .addMapping("type1", "{\"type1\": {\"properties\": {\"value\": {\"type\": \"string\"}}}}")
      .execute().actionGet();
    node.client().admin().cluster().prepareHealth("polop").setWaitForYellowStatus().get(TimeValue.timeValueMillis(1000));

    // strict mapping is enforced
    try {
      node.client().prepareIndex("polop", "type1", "666").setSource(
        XContentFactory.jsonBuilder().startObject().field("unknown", "plouf").endObject()
      ).get();
    } finally {
      node.stop();
    }
  }

  @Test
  public void check_analyzer() throws Exception {
    ESNode node = new ESNode(fs, new Settings());
    node.start();

    node.client().admin().indices().prepareCreate("polop")
      .addMapping("type1", "{\"type1\": {\"properties\": {\"value\": {\"type\": \"string\"}}}}")
      .execute().actionGet();
    node.client().admin().cluster().prepareHealth("polop").setWaitForYellowStatus().get(TimeValue.timeValueMillis(1000));

    // default "sortable" analyzer is defined for all indices
    assertThat(node.client().admin().indices()
      .prepareAnalyze("polop", "This Is A Wonderful Text").setAnalyzer("sortable").get()
      .getTokens().get(0).getTerm()).isEqualTo("this is a ");

    // default "string_gram" analyzer is defined for all indices
    AnalyzeResponse response = node.client().admin().indices()
      .prepareAnalyze("polop", "he.llo w@rl#d").setAnalyzer("string_gram").get();
    assertThat(response.getTokens()).hasSize(10);
    assertThat(response.getTokens().get(0).getTerm()).isEqualTo("he");
    assertThat(response.getTokens().get(7).getTerm()).isEqualTo("w@rl");

    // default "path_analyzer" analyzer is defined for all indices
    response = node.client().admin().indices()
      .prepareAnalyze("polop", "/temp/65236/test path/MyFile.java").setAnalyzer("path_analyzer").get();
    // default "path_analyzer" analyzer is defined for all indices
    assertThat(response.getTokens()).hasSize(4);
    assertThat(response.getTokens().get(0).getTerm()).isEqualTo("/temp");
    assertThat(response.getTokens().get(1).getTerm()).isEqualTo("/temp/65236");
    assertThat(response.getTokens().get(2).getTerm()).isEqualTo("/temp/65236/test path");
    assertThat(response.getTokens().get(3).getTerm()).isEqualTo("/temp/65236/test path/MyFile.java");
  }

  @Test
  public void should_restore_status_on_startup() throws Exception {
    File zip = new File(Resources.getResource(getClass(), "ESNodeTest/data-es-clean.zip").toURI());
    ZipUtils.unzip(zip, dataDir);

    ESNode node = new ESNode(fs, new Settings());
    node.start();

    AdminClient admin = node.client().admin();
    assertThat(admin.indices().prepareExists("myindex").execute().actionGet().isExists()).isTrue();
    assertThat(admin.cluster().prepareHealth("myindex").setWaitForYellowStatus().execute().actionGet().getStatus()).isIn(ClusterHealthStatus.GREEN, ClusterHealthStatus.YELLOW);

    node.stop();
  }

  @Test(expected = IllegalStateException.class)
  @Ignore //TODO should use the Mng Index
  public void should_fail_on_corrupt_index() throws Exception {
    File zip = new File(Resources.getResource(getClass(), "ESNodeTest/data-es-corrupt.zip").toURI());
    ZipUtils.unzip(zip, dataDir);

    ESNode node = new ESNode(fs, new Settings(), "5s");
    try {
      node.start();
    } finally {
      node.stop();
    }
  }

  @Test
  public void should_fail_to_get_client_if_not_started() {
    ESNode node = new ESNode(fs, new Settings());
    try {
      node.client();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Elasticsearch is not started");
    }
  }

  @Test
  public void should_enable_rest_console() throws Exception {
    Settings settings = new Settings();
    int httpPort = NetworkUtils.freePort();
    settings.setProperty("sonar.es.http.port", httpPort);
    ESNode node = new ESNode(fs, settings);
    node.start();

    URL url = URI.create("http://localhost:" + httpPort).toURL();
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.connect();
    assertThat(connection.getResponseCode()).isEqualTo(200);

    node.stop();
    connection = (HttpURLConnection) url.openConnection();
    try {
      connection.connect();
      fail();
    } catch (Exception e) {
      // ok, console is down
    }
  }
}
