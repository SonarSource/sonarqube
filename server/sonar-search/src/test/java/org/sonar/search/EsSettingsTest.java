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
package org.sonar.search;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore
// FIXME enable back right now!
public class EsSettingsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void test_default_settings() throws Exception {
    File homeDir = temp.newFolder();
    Props props = new Props(new Properties());
    props.set(ProcessProperties.SEARCH_PORT, "1234");
    props.set(ProcessProperties.SEARCH_HOST, "127.0.0.1");
    props.set(ProcessProperties.PATH_HOME, homeDir.getAbsolutePath());
    props.set(ProcessProperties.CLUSTER_NAME, "sonarqube");

    EsSettings esSettings = new EsSettings(props);

    Map<String, String> generated = esSettings.build();
    assertThat(generated.get("transport.tcp.port")).isEqualTo("1234");
    assertThat(generated.get("transport.host")).isEqualTo("127.0.0.1");

    // no cluster, but cluster and node names are set though
    assertThat(generated.get("cluster.name")).isEqualTo("sonarqube");
    assertThat(generated.get("node.name")).isEqualTo("sonarqube");

    assertThat(generated.get("path.data")).isNotNull();
    assertThat(generated.get("path.logs")).isNotNull();
    assertThat(generated.get("path.home")).isNotNull();

    // http is disabled for security reasons
    assertThat(generated.get("http.enabled")).isEqualTo("false");

    assertThat(generated.get("index.number_of_replicas")).isEqualTo("0");
    assertThat(generated.get("discovery.zen.ping.unicast.hosts")).isNull();
    assertThat(generated.get("discovery.zen.minimum_master_nodes")).isEqualTo("1");
    assertThat(generated.get("discovery.initial_state_timeout")).isEqualTo("30s");
  }

  @Test
  public void override_dirs() throws Exception {
    File dataDir = temp.newFolder();
    File logDir = temp.newFolder();
    File tempDir = temp.newFolder();
    Props props = minProps(false);
    props.set(ProcessProperties.PATH_DATA, dataDir.getAbsolutePath());
    props.set(ProcessProperties.PATH_LOGS, logDir.getAbsolutePath());
    props.set(ProcessProperties.PATH_TEMP, tempDir.getAbsolutePath());

    Map<String, String> settings = new EsSettings(props).build();

    assertThat(settings.get("path.data")).isEqualTo(new File(dataDir, "es").getAbsolutePath());
    assertThat(settings.get("path.logs")).isEqualTo(logDir.getAbsolutePath());
    assertThat(settings.get("path.home")).isEqualTo(new File(tempDir, "es").getAbsolutePath());
  }

  @Test
  public void cluster_is_enabled() throws Exception {
    Props props = minProps(true);
    props.set(ProcessProperties.CLUSTER_SEARCH_HOSTS, "1.2.3.4:9000,1.2.3.5:8080");
    Map<String, String> settings = new EsSettings(props).build();

    assertThat(settings.get("index.number_of_replicas")).isEqualTo("1");
    assertThat(settings.get("discovery.zen.ping.unicast.hosts")).isEqualTo("1.2.3.4:9000,1.2.3.5:8080");
    assertThat(settings.get("discovery.zen.minimum_master_nodes")).isEqualTo("2");
    assertThat(settings.get("discovery.initial_state_timeout")).isEqualTo("120s");
  }

  @Test
  public void incorrect_values_of_minimum_master_nodes() throws Exception {
    Props props = minProps(true);
    props.set(ProcessProperties.SEARCH_MINIMUM_MASTER_NODES, "ꝱꝲꝳପ");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Value of property sonar.search.minimumMasterNodes is not an integer:");
    Map<String, String> settings = new EsSettings(props).build();
  }

  @Test
  public void cluster_is_enabled_with_defined_minimum_master_nodes() throws Exception {
    Props props = minProps(true);
    props.set(ProcessProperties.SEARCH_MINIMUM_MASTER_NODES, "5");
    Map<String, String> settings = new EsSettings(props).build();

    assertThat(settings.get("discovery.zen.minimum_master_nodes")).isEqualTo("5");
  }

  @Test
  public void cluster_is_enabled_with_defined_initialTimeout() throws Exception {
    Props props = minProps(true);
    props.set(ProcessProperties.SEARCH_INITIAL_STATE_TIMEOUT, "10s");
    Map<String, String> settings = new EsSettings(props).build();

    assertThat(settings.get("discovery.initial_state_timeout")).isEqualTo("10s");
  }

  @Test
  public void in_standalone_initialTimeout_is_not_overridable() throws Exception {
    Props props = minProps(false);
    props.set(ProcessProperties.SEARCH_INITIAL_STATE_TIMEOUT, "10s");
    Map<String, String> settings = new EsSettings(props).build();

    assertThat(settings.get("discovery.initial_state_timeout")).isEqualTo("30s");
  }

  @Test
  public void in_standalone_minimumMasterNodes_is_not_overridable() throws Exception {
    Props props = minProps(false);
    props.set(ProcessProperties.SEARCH_MINIMUM_MASTER_NODES, "5");
    Map<String, String> settings = new EsSettings(props).build();

    assertThat(settings.get("discovery.zen.minimum_master_nodes")).isEqualTo("1");
  }


  @Test
  public void in_standalone_searchReplicas_is_not_overridable() throws Exception {
    Props props = minProps(false);
    props.set(ProcessProperties.SEARCH_REPLICAS, "5");
    Map<String, String> settings = new EsSettings(props).build();

    assertThat(settings.get("index.number_of_replicas")).isEqualTo("0");
  }

  @Test
  public void cluster_is_enabled_with_defined_replicas() throws Exception {
    Props props = minProps(true);
    props.set(ProcessProperties.SEARCH_REPLICAS, "5");
    Map<String, String> settings = new EsSettings(props).build();

    assertThat(settings.get("index.number_of_replicas")).isEqualTo("5");
  }

  @Test
  public void incorrect_values_of_replicas() throws Exception {
    Props props = minProps(true);

    props.set(ProcessProperties.SEARCH_REPLICAS, "ꝱꝲꝳପ");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Value of property sonar.search.replicas is not an integer:");
    Map<String, String> settings = new EsSettings(props).build();
  }

  @Test
  public void enable_marvel() throws Exception {
    Props props = minProps(false);
    props.set(EsSettings.PROP_MARVEL_HOSTS, "127.0.0.2,127.0.0.3");
    Map<String, String> settings = new EsSettings(props).build();

    assertThat(settings.get("marvel.agent.exporter.es.hosts")).isEqualTo("127.0.0.2,127.0.0.3");
  }

  @Test
  public void enable_http_connector() throws Exception {
    Props props = minProps(false);
    props.set(ProcessProperties.SEARCH_HTTP_PORT, "9010");
    Map<String, String> settings = new EsSettings(props).build();

    assertThat(settings.get("http.port")).isEqualTo("9010");
    assertThat(settings.get("http.host")).isEqualTo("127.0.0.1");
    assertThat(settings.get("http.enabled")).isEqualTo("true");
  }
  
  @Test
  public void enable_http_connector_different_host() throws Exception {
    Props props = minProps(false);
    props.set(ProcessProperties.SEARCH_HTTP_PORT, "9010");
    props.set(ProcessProperties.SEARCH_HOST, "127.0.0.2");
    Map<String, String> settings = new EsSettings(props).build();

    assertThat(settings.get("http.port")).isEqualTo("9010");
    assertThat(settings.get("http.host")).isEqualTo("127.0.0.2");
    assertThat(settings.get("http.enabled")).isEqualTo("true");
  }

  private Props minProps(boolean cluster) throws IOException {
    File homeDir = temp.newFolder();
    Props props = new Props(new Properties());
    ProcessProperties.completeDefaults(props);
    props.set(ProcessProperties.PATH_HOME, homeDir.getAbsolutePath());
    props.set(ProcessProperties.CLUSTER_ENABLED, Boolean.toString(cluster));
    return props;
  }
}
