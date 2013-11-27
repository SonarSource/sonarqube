/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.cluster.ClusterState;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.ServerFileSystem;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchNodeTest {

  ServerFileSystem fs;
  File homedir;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void createMocks() throws IOException {
    homedir = temp.newFolder();
    fs = mock(ServerFileSystem.class);
    when(fs.getHomeDir()).thenReturn(homedir);
  }

  @Test
  public void start_and_stop_es_node() throws Exception {
    File dataDir = new File(homedir, SearchNode.DATA_DIR);
    assertThat(dataDir).doesNotExist();

    SearchNode node = new SearchNode(fs, new Settings());
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

  @Test
  public void should_fail_to_get_client_if_not_started() {
    SearchNode node = new SearchNode(fs, new Settings());
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
    SearchNode node = new SearchNode(fs, settings);
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
