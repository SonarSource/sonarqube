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
package org.sonar.search;

import org.elasticsearch.common.settings.Settings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.MessageException;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class SearchSettingsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void test_default_settings() throws Exception {
    File homeDir = temp.newFolder();
    Props props = new Props(new Properties());
    props.set(ProcessProperties.SEARCH_PORT, "1234");
    props.set(ProcessProperties.SEARCH_HOST, "127.0.0.1");
    props.set(ProcessProperties.PATH_HOME, homeDir.getAbsolutePath());
    props.set(ProcessProperties.CLUSTER_NAME, "tests");
    props.set(ProcessProperties.CLUSTER_NODE_NAME, "test");

    SearchSettings searchSettings = new SearchSettings(props);
    assertThat(searchSettings.inCluster()).isFalse();

    Settings generated = searchSettings.build();
    assertThat(generated.get("transport.tcp.port")).isEqualTo("1234");
    assertThat(generated.get("transport.host")).isEqualTo("127.0.0.1");
    assertThat(generated.get("cluster.name")).isEqualTo("tests");
    assertThat(generated.get("node.name")).isEqualTo("test");

    assertThat(generated.get("path.data")).isNotNull();
    assertThat(generated.get("path.logs")).isNotNull();
    assertThat(generated.get("path.work")).isNotNull();

    // http is disabled for security reasons
    assertThat(generated.get("http.enabled")).isEqualTo("false");

    // no cluster, but node name is set though
    assertThat(generated.get("index.number_of_replicas")).isEqualTo("0");
    assertThat(generated.get("discovery.zen.ping.unicast.hosts")).isNull();
  }
  
  @Test
  public void test_default_hosts() throws Exception {
    Props props = minProps();

    SearchSettings searchSettings = new SearchSettings(props);
    assertThat(searchSettings.inCluster()).isFalse();

    Settings generated = searchSettings.build();
    assertThat(generated.get("transport.tcp.port")).isEqualTo("9001");
    assertThat(generated.get("transport.host")).isEqualTo("127.0.0.1");
    assertThat(generated.get("cluster.name")).isEqualTo("sonarqube");
    assertThat(generated.get("node.name")).startsWith("sonar-");
  }

  @Test
  public void override_dirs() throws Exception {
    File dataDir = temp.newFolder();
    File logDir = temp.newFolder();
    File tempDir = temp.newFolder();
    Props props = minProps();
    props.set(ProcessProperties.PATH_DATA, dataDir.getAbsolutePath());
    props.set(ProcessProperties.PATH_LOGS, logDir.getAbsolutePath());
    props.set(ProcessProperties.PATH_TEMP, tempDir.getAbsolutePath());

    Settings settings = new SearchSettings(props).build();

    assertThat(settings.get("path.data")).isEqualTo(new File(dataDir, "es").getAbsolutePath());
    assertThat(settings.get("path.logs")).isEqualTo(logDir.getAbsolutePath());
    assertThat(settings.get("path.work")).isEqualTo(tempDir.getAbsolutePath());
  }

  @Test
  public void test_cluster_master() throws Exception {
    Props props = minProps();
    props.set(ProcessProperties.CLUSTER_ACTIVATE, "true");
    props.set(ProcessProperties.CLUSTER_MASTER, "true");
    Settings settings = new SearchSettings(props).build();

    assertThat(settings.get("index.number_of_replicas")).isEqualTo("1");
    assertThat(settings.get("discovery.zen.ping.unicast.hosts")).isNull();
    assertThat(settings.get("node.master")).isEqualTo("true");
  }

  @Test
  public void test_cluster_slave() throws Exception {
    Props props = minProps();
    props.set(ProcessProperties.CLUSTER_ACTIVATE, "true");
    props.set(ProcessProperties.CLUSTER_MASTER_HOST, "127.0.0.2,127.0.0.3");
    Settings settings = new SearchSettings(props).build();

    assertThat(settings.get("discovery.zen.ping.unicast.hosts")).isEqualTo("127.0.0.2,127.0.0.3");
    assertThat(settings.get("node.master")).isEqualTo("false");
  }

  @Test
  public void bad_cluster_configuration() throws Exception {
    Props props = minProps();
    props.set(ProcessProperties.CLUSTER_ACTIVATE, "true");
    try {
      new SearchSettings(props).build();
      fail();
    } catch (MessageException ignored) {
      // expected
    }
  }

  @Test
  public void enable_marvel() throws Exception {
    Props props = minProps();
    props.set(SearchSettings.PROP_MARVEL_HOSTS, "127.0.0.2,127.0.0.3");
    Settings settings = new SearchSettings(props).build();

    assertThat(settings.get("marvel.agent.exporter.es.hosts")).isEqualTo("127.0.0.2,127.0.0.3");
  }

  @Test
  public void enable_http_connector() throws Exception {
    Props props = minProps();
    props.set(ProcessProperties.SEARCH_HTTP_PORT, "9010");
    Settings settings = new SearchSettings(props).build();

    assertThat(settings.get("http.port")).isEqualTo("9010");
    assertThat(settings.get("http.host")).isEqualTo("127.0.0.1");
    assertThat(settings.get("http.enabled")).isEqualTo("true");
  }
  
  @Test
  public void enable_http_connector_different_host() throws Exception {
    Props props = minProps();
    props.set(ProcessProperties.SEARCH_HTTP_PORT, "9010");
    props.set(ProcessProperties.SEARCH_HOST, "127.0.0.2");
    Settings settings = new SearchSettings(props).build();

    assertThat(settings.get("http.port")).isEqualTo("9010");
    assertThat(settings.get("http.host")).isEqualTo("127.0.0.2");
    assertThat(settings.get("http.enabled")).isEqualTo("true");
  }

  private Props minProps() throws IOException {
    File homeDir = temp.newFolder();
    Props props = new Props(new Properties());
    ProcessProperties.completeDefaults(props);
    props.set(ProcessProperties.PATH_HOME, homeDir.getAbsolutePath());
    return props;
  }
}
