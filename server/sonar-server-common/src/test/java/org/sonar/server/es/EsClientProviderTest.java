/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.net.InetAddress;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import org.assertj.core.api.Condition;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.event.Level;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.testfixtures.log.LogTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ES_HTTP_KEYSTORE;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NAME;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_TYPE;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_SEARCH_HOSTS;
import static org.sonar.process.ProcessProperties.Property.ES_PORT;
import static org.sonar.process.ProcessProperties.Property.SEARCH_HOST;
import static org.sonar.process.ProcessProperties.Property.SEARCH_PORT;

public class EsClientProviderTest {
  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private MapSettings settings = new MapSettings();
  private EsClientProvider underTest = new EsClientProvider();
  private String localhostHostname;

  @Before
  public void setUp() throws Exception {
    // mandatory property
    settings.setProperty(CLUSTER_NAME.getKey(), "the_cluster_name");

    localhostHostname = InetAddress.getLocalHost().getHostName();
  }

  @Test
  public void connection_to_local_es_when_cluster_mode_is_disabled() {
    settings.setProperty(CLUSTER_ENABLED.getKey(), false);
    settings.setProperty(SEARCH_HOST.getKey(), localhostHostname);
    settings.setProperty(SEARCH_PORT.getKey(), 9000);
    settings.setProperty(ES_PORT.getKey(), 8080);

    EsClient client = underTest.provide(settings.asConfig());
    RestHighLevelClient nativeClient = client.nativeClient();
    assertThat(nativeClient.getLowLevelClient().getNodes()).hasSize(1);
    Node node = nativeClient.getLowLevelClient().getNodes().get(0);
    assertThat(node.getHost().getAddress().getHostName()).isEqualTo(localhostHostname);
    assertThat(node.getHost().getPort()).isEqualTo(9000);

    assertThat(logTester.logs(Level.INFO)).has(new Condition<>(s -> s.contains("Connected to local Elasticsearch: [http://" + localhostHostname + ":9000]"), ""));
  }

  @Test
  public void connection_to_remote_es_nodes_when_cluster_mode_is_enabled_and_local_es_is_disabled() {
    settings.setProperty(CLUSTER_ENABLED.getKey(), true);
    settings.setProperty(CLUSTER_NODE_TYPE.getKey(), "application");
    settings.setProperty(CLUSTER_SEARCH_HOSTS.getKey(), format("%s:8080,%s:8081", localhostHostname, localhostHostname));

    EsClient client = underTest.provide(settings.asConfig());
    RestHighLevelClient nativeClient = client.nativeClient();
    assertThat(nativeClient.getLowLevelClient().getNodes()).hasSize(2);

    Node node = nativeClient.getLowLevelClient().getNodes().get(0);
    assertThat(node.getHost().getAddress().getHostName()).isEqualTo(localhostHostname);
    assertThat(node.getHost().getPort()).isEqualTo(8080);

    node = nativeClient.getLowLevelClient().getNodes().get(1);
    assertThat(node.getHost().getAddress().getHostName()).isEqualTo(localhostHostname);
    assertThat(node.getHost().getPort()).isEqualTo(8081);

    assertThat(logTester.logs(Level.INFO))
      .has(new Condition<>(s -> s.contains("Connected to remote Elasticsearch: [http://" + localhostHostname + ":8080, http://" + localhostHostname + ":8081]"), ""));
  }

  @Test
  public void es_client_provider_must_throw_IAE_when_incorrect_port_is_used_when_search_disabled() {
    settings.setProperty(CLUSTER_ENABLED.getKey(), true);
    settings.setProperty(CLUSTER_NODE_TYPE.getKey(), "application");
    settings.setProperty(CLUSTER_SEARCH_HOSTS.getKey(), format("%s:100000,%s:8081", localhostHostname, localhostHostname));

    assertThatThrownBy(() -> underTest.provide(settings.asConfig()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(format("Port number out of range: %s:100000", localhostHostname));
  }

  @Test
  public void es_client_provider_must_throw_IAE_when_incorrect_port_is_used() {
    settings.setProperty(CLUSTER_ENABLED.getKey(), true);
    settings.setProperty(CLUSTER_NODE_TYPE.getKey(), "search");
    settings.setProperty(SEARCH_HOST.getKey(), "localhost");
    settings.setProperty(SEARCH_PORT.getKey(), "100000");

    assertThatThrownBy(() -> underTest.provide(settings.asConfig()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Port out of range: 100000");
  }

  @Test
  public void es_client_provider_must_add_default_port_when_not_specified() {
    settings.setProperty(CLUSTER_ENABLED.getKey(), true);
    settings.setProperty(CLUSTER_NODE_TYPE.getKey(), "application");
    settings.setProperty(CLUSTER_SEARCH_HOSTS.getKey(), format("%s,%s:8081", localhostHostname, localhostHostname));

    EsClient client = underTest.provide(settings.asConfig());
    RestHighLevelClient nativeClient = client.nativeClient();
    assertThat(nativeClient.getLowLevelClient().getNodes()).hasSize(2);

    Node node = nativeClient.getLowLevelClient().getNodes().get(0);
    assertThat(node.getHost().getAddress().getHostName()).isEqualTo(localhostHostname);
    assertThat(node.getHost().getPort()).isEqualTo(9001);

    node = nativeClient.getLowLevelClient().getNodes().get(1);
    assertThat(node.getHost().getAddress().getHostName()).isEqualTo(localhostHostname);
    assertThat(node.getHost().getPort()).isEqualTo(8081);

    assertThat(logTester.logs(Level.INFO))
      .has(new Condition<>(s -> s.contains("Connected to remote Elasticsearch: [http://" + localhostHostname + ":9001, http://" + localhostHostname + ":8081]"), ""));
  }

  @Test
  public void provide_whenHttpEncryptionEnabled_shouldUseHttps() throws GeneralSecurityException, IOException {
    settings.setProperty(CLUSTER_ENABLED.getKey(), true);
    Path keyStorePath = temp.newFile("keystore.p12").toPath();
    EsClientTest.createCertificate("localhost", keyStorePath, "password");
    settings.setProperty(CLUSTER_ES_HTTP_KEYSTORE.getKey(), keyStorePath.toString());
    settings.setProperty(CLUSTER_NODE_TYPE.getKey(), "application");
    settings.setProperty(CLUSTER_SEARCH_HOSTS.getKey(), format("%s,%s:8081", localhostHostname, localhostHostname));

    EsClient client = underTest.provide(settings.asConfig());
    RestHighLevelClient nativeClient = client.nativeClient();

    Node node = nativeClient.getLowLevelClient().getNodes().get(0);
    assertThat(node.getHost().getSchemeName()).isEqualTo("https");

    assertThat(logTester.logs(Level.INFO))
      .has(new Condition<>(s -> s.contains("Connected to remote Elasticsearch: [https://" + localhostHostname + ":9001, https://" + localhostHostname + ":8081]"), ""));
  }
}
