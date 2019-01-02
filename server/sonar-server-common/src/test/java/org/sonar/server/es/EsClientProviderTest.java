/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.net.InetAddress;
import org.assertj.core.api.Condition;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.TransportAddress;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NAME;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_TYPE;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_SEARCH_HOSTS;
import static org.sonar.process.ProcessProperties.Property.SEARCH_HOST;
import static org.sonar.process.ProcessProperties.Property.SEARCH_PORT;

public class EsClientProviderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public LogTester logTester = new LogTester();

  private MapSettings settings = new MapSettings();
  private EsClientProvider underTest = new EsClientProvider();
  private String localhost;

  @Before
  public void setUp() throws Exception {
    // mandatory property
    settings.setProperty(CLUSTER_NAME.getKey(), "the_cluster_name");

    localhost = InetAddress.getLocalHost().getHostAddress();
  }

  @Test
  public void connection_to_local_es_when_cluster_mode_is_disabled() {
    settings.setProperty(CLUSTER_ENABLED.getKey(), false);
    settings.setProperty(SEARCH_HOST.getKey(), localhost);
    settings.setProperty(SEARCH_PORT.getKey(), 8080);

    EsClient client = underTest.provide(settings.asConfig());
    TransportClient transportClient = (TransportClient) client.nativeClient();
    assertThat(transportClient.transportAddresses()).hasSize(1);
    TransportAddress address = transportClient.transportAddresses().get(0);
    assertThat(address.getAddress()).isEqualTo(localhost);
    assertThat(address.getPort()).isEqualTo(8080);
    assertThat(logTester.logs(LoggerLevel.INFO)).has(new Condition<>(s -> s.contains("Connected to local Elasticsearch: [" + localhost + ":8080]"), ""));

    // keep in cache
    assertThat(underTest.provide(settings.asConfig())).isSameAs(client);
  }

  @Test
  public void connection_to_remote_es_nodes_when_cluster_mode_is_enabled_and_local_es_is_disabled() {
    settings.setProperty(CLUSTER_ENABLED.getKey(), true);
    settings.setProperty(CLUSTER_NODE_TYPE.getKey(), "application");
    settings.setProperty(CLUSTER_SEARCH_HOSTS.getKey(), format("%s:8080,%s:8081", localhost, localhost));

    EsClient client = underTest.provide(settings.asConfig());
    TransportClient transportClient = (TransportClient) client.nativeClient();
    assertThat(transportClient.transportAddresses()).hasSize(2);
    TransportAddress address = transportClient.transportAddresses().get(0);
    assertThat(address.getAddress()).isEqualTo(localhost);
    assertThat(address.getPort()).isEqualTo(8080);
    address = transportClient.transportAddresses().get(1);
    assertThat(address.getAddress()).isEqualTo(localhost);
    assertThat(address.getPort()).isEqualTo(8081);
    assertThat(logTester.logs(LoggerLevel.INFO)).has(new Condition<>(s -> s.contains("Connected to remote Elasticsearch: [" + localhost + ":8080, " + localhost + ":8081]"), ""));

    // keep in cache
    assertThat(underTest.provide(settings.asConfig())).isSameAs(client);
  }

  @Test
  public void es_client_provider_must_throw_ISE_when_incorrect_port_is_used_when_search_disabled() {
    settings.setProperty(CLUSTER_ENABLED.getKey(), true);
    settings.setProperty(CLUSTER_NODE_TYPE.getKey(), "application");
    settings.setProperty(CLUSTER_SEARCH_HOSTS.getKey(), format("%s:100000,%s:8081", localhost, localhost));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("Port number out of range: %s:100000", localhost));

    underTest.provide(settings.asConfig());
  }

  @Test
  public void es_client_provider_must_throw_ISE_when_incorrect_port_is_used() {
    settings.setProperty(CLUSTER_ENABLED.getKey(), true);
    settings.setProperty(CLUSTER_NODE_TYPE.getKey(), "search");
    settings.setProperty(SEARCH_HOST.getKey(), "localhost");
    settings.setProperty(SEARCH_PORT.getKey(), "100000");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Port out of range: 100000");

    underTest.provide(settings.asConfig());
  }

  @Test
  public void es_client_provider_must_add_default_port_when_not_specified() {
    settings.setProperty(CLUSTER_ENABLED.getKey(), true);
    settings.setProperty(CLUSTER_NODE_TYPE.getKey(), "application");
    settings.setProperty(CLUSTER_SEARCH_HOSTS.getKey(), format("%s,%s:8081", localhost, localhost));

    EsClient client = underTest.provide(settings.asConfig());
    TransportClient transportClient = (TransportClient) client.nativeClient();
    assertThat(transportClient.transportAddresses()).hasSize(2);
    TransportAddress address = transportClient.transportAddresses().get(0);
    assertThat(address.getAddress()).isEqualTo(localhost);
    assertThat(address.getPort()).isEqualTo(9001);
    address = transportClient.transportAddresses().get(1);
    assertThat(address.getAddress()).isEqualTo(localhost);
    assertThat(address.getPort()).isEqualTo(8081);
    assertThat(logTester.logs(LoggerLevel.INFO)).has(new Condition<>(s -> s.contains("Connected to remote Elasticsearch: [" + localhost + ":9001, " + localhost + ":8081]"), ""));

    // keep in cache
    assertThat(underTest.provide(settings.asConfig())).isSameAs(client);
  }
}
