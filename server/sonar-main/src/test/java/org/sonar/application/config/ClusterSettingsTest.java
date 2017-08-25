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
package org.sonar.application.config;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.process.MessageException;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.cluster.ClusterProperties.CLUSTER_ENABLED;
import static org.sonar.cluster.ClusterProperties.CLUSTER_HOSTS;
import static org.sonar.cluster.ClusterProperties.CLUSTER_NODE_TYPE;
import static org.sonar.cluster.ClusterProperties.CLUSTER_SEARCH_HOSTS;
import static org.sonar.process.ProcessId.COMPUTE_ENGINE;
import static org.sonar.process.ProcessId.ELASTICSEARCH;
import static org.sonar.process.ProcessId.WEB_SERVER;
import static org.sonar.process.ProcessProperties.JDBC_URL;
import static org.sonar.process.ProcessProperties.SEARCH_HOST;

public class ClusterSettingsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void test_isClusterEnabled() {
    TestAppSettings settings = newSettingsForAppNode().set(CLUSTER_ENABLED, "true");
    assertThat(ClusterSettings.isClusterEnabled(settings)).isTrue();

    settings = new TestAppSettings().set(CLUSTER_ENABLED, "false");
    assertThat(ClusterSettings.isClusterEnabled(settings)).isFalse();
  }

  @Test
  public void isClusterEnabled_returns_false_by_default() {
    assertThat(ClusterSettings.isClusterEnabled(new TestAppSettings())).isFalse();
  }

  @Test
  public void getEnabledProcesses_returns_all_processes_in_standalone_mode() {
    TestAppSettings settings = new TestAppSettings().set(CLUSTER_ENABLED, "false");
    assertThat(ClusterSettings.getEnabledProcesses(settings)).containsOnly(COMPUTE_ENGINE, ELASTICSEARCH, WEB_SERVER);
  }

  @Test
  public void getEnabledProcesses_fails_if_no_node_type_is_set_for_a_cluster_node() {
    TestAppSettings settings = new TestAppSettings();
    settings.set(CLUSTER_ENABLED, "true");
    settings.set(CLUSTER_NODE_TYPE, "foo");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid value for [sonar.cluster.node.type]: [foo]");

    ClusterSettings.getEnabledProcesses(settings);
  }

  @Test
  public void getEnabledProcesses_returns_configured_processes_in_cluster_mode() {
    TestAppSettings settings = newSettingsForAppNode();
    assertThat(ClusterSettings.getEnabledProcesses(settings)).containsOnly(COMPUTE_ENGINE, WEB_SERVER);

    settings = newSettingsForSearchNode();
    assertThat(ClusterSettings.getEnabledProcesses(settings)).containsOnly(ELASTICSEARCH);
  }

  @Test
  public void accept_throws_MessageException_if_no_node_type_is_configured() {
    TestAppSettings settings = new TestAppSettings();
    settings.set(CLUSTER_ENABLED, "true");

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Property [sonar.cluster.node.type] is mandatory");

    new ClusterSettings().accept(settings.getProps());
  }

  @Test
  public void accept_throws_MessageException_if_node_type_is_not_correct() {
    TestAppSettings settings = new TestAppSettings();
    settings.set(CLUSTER_ENABLED, "true");
    settings.set(CLUSTER_NODE_TYPE, "bla");

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Invalid value for property [sonar.cluster.node.type]: [bla], only [application, search] are allowed");

    new ClusterSettings().accept(settings.getProps());
  }

  @Test
  public void accept_throws_MessageException_if_internal_property_for_web_leader_is_configured() {
    TestAppSettings settings = newSettingsForAppNode();
    settings.set("sonar.cluster.web.startupLeader", "true");

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Property [sonar.cluster.web.startupLeader] is forbidden");

    new ClusterSettings().accept(settings.getProps());
  }

  @Test
  public void accept_throws_MessageException_if_search_enabled_with_loopback() {
    TestAppSettings settings = newSettingsForSearchNode();
    settings.set(CLUSTER_SEARCH_HOSTS, "192.168.1.1,192.168.1.2");
    settings.set(SEARCH_HOST, "::1");

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("The interface address [::1] of [sonar.search.host] must not be a loopback address");

    new ClusterSettings().accept(settings.getProps());
  }

  @Test
  public void accept_does_nothing_if_cluster_is_disabled() {
    TestAppSettings settings = new TestAppSettings();
    settings.set(CLUSTER_ENABLED, "false");
    // this property is supposed to fail if cluster is enabled
    settings.set("sonar.cluster.web.startupLeader", "true");

    new ClusterSettings().accept(settings.getProps());
  }

  @Test
  public void accept_throws_MessageException_if_h2_on_application_node() {
    TestAppSettings settings = newSettingsForAppNode();
    settings.set("sonar.jdbc.url", "jdbc:h2:mem");

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Embedded database is not supported in cluster mode");

    new ClusterSettings().accept(settings.getProps());
  }

  @Test
  public void accept_does_not_verify_h2_on_search_node() {
    TestAppSettings settings = newSettingsForSearchNode();
    settings.set("sonar.jdbc.url", "jdbc:h2:mem");

    // do not fail
    new ClusterSettings().accept(settings.getProps());
  }

  @Test
  public void accept_throws_MessageException_on_application_node_if_default_jdbc_url() {
    TestAppSettings settings = newSettingsForAppNode();
    settings.clearProperty(JDBC_URL);

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Embedded database is not supported in cluster mode");

    new ClusterSettings().accept(settings.getProps());
  }

  @Test
  public void isLocalElasticsearchEnabled_returns_true_in_standalone_mode() {
    TestAppSettings settings = new TestAppSettings();
    assertThat(ClusterSettings.isLocalElasticsearchEnabled(settings)).isTrue();
  }

  @Test
  public void isLocalElasticsearchEnabled_returns_true_on_search_node() {
    TestAppSettings settings = newSettingsForSearchNode();

    assertThat(ClusterSettings.isLocalElasticsearchEnabled(settings)).isTrue();
  }

  @Test
  public void isLocalElasticsearchEnabled_returns_true_for_a_application_node() {
    TestAppSettings settings = newSettingsForAppNode();

    assertThat(ClusterSettings.isLocalElasticsearchEnabled(settings)).isFalse();
  }

  @Test
  public void accept_throws_MessageException_if_searchHost_is_missing() {
    TestAppSettings settings = newSettingsForSearchNode();
    settings.clearProperty(SEARCH_HOST);
    assertThatPropertyIsMandatory(settings, SEARCH_HOST);
  }

  @Test
  public void accept_throws_MessageException_if_searchHost_is_blank() {
    TestAppSettings settings = newSettingsForSearchNode();
    settings.set(SEARCH_HOST, " ");
    assertThatPropertyIsMandatory(settings, SEARCH_HOST);
  }

  @Test
  public void accept_throws_MessageException_if_clusterHosts_is_missing() {
    TestAppSettings settings = newSettingsForSearchNode();
    settings.clearProperty(CLUSTER_HOSTS);
    assertThatPropertyIsMandatory(settings, CLUSTER_HOSTS);
  }

  @Test
  public void accept_throws_MessageException_if_clusterHosts_is_blank() {
    TestAppSettings settings = newSettingsForSearchNode();
    settings.set(CLUSTER_HOSTS, " ");
    assertThatPropertyIsMandatory(settings, CLUSTER_HOSTS);
  }

  @Test
  public void accept_throws_MessageException_if_clusterSearchHosts_is_missing() {
    TestAppSettings settings = newSettingsForSearchNode();
    settings.clearProperty(CLUSTER_SEARCH_HOSTS);
    assertThatPropertyIsMandatory(settings, CLUSTER_SEARCH_HOSTS);
  }

  @Test
  public void accept_throws_MessageException_if_clusterSearchHosts_is_blank() {
    TestAppSettings settings = newSettingsForSearchNode();
    settings.set(CLUSTER_SEARCH_HOSTS, " ");
    assertThatPropertyIsMandatory(settings, CLUSTER_SEARCH_HOSTS);
  }

  @Test
  public void accept_throws_MessageException_if_jwt_token_is_not_set_on_application_nodes() {
    TestAppSettings settings = newSettingsForAppNode();
    settings.clearProperty("sonar.auth.jwtBase64Hs256Secret");
    assertThatPropertyIsMandatory(settings, "sonar.auth.jwtBase64Hs256Secret");
  }

  private void assertThatPropertyIsMandatory(TestAppSettings settings, String key) {
    expectedException.expect(MessageException.class);
    expectedException.expectMessage(format("Property [%s] is mandatory", key));

    new ClusterSettings().accept(settings.getProps());
  }

  private static TestAppSettings newSettingsForAppNode() {
    return new TestAppSettings()
      .set(CLUSTER_ENABLED, "true")
      .set(CLUSTER_NODE_TYPE, "application")
      .set(CLUSTER_SEARCH_HOSTS, "localhost")
      .set(CLUSTER_HOSTS, "192.168.233.1, 192.168.233.2,192.168.233.3")
      .set("sonar.auth.jwtBase64Hs256Secret", "abcde")
      .set(JDBC_URL, "jdbc:mysql://localhost:3306/sonar");
  }

  private static TestAppSettings newSettingsForSearchNode() {
    return new TestAppSettings()
      .set(CLUSTER_ENABLED, "true")
      .set(CLUSTER_NODE_TYPE, "search")
      .set(CLUSTER_SEARCH_HOSTS, "192.168.233.1")
      .set(CLUSTER_HOSTS, "192.168.233.1, 192.168.233.2,192.168.233.3")
      .set(SEARCH_HOST, getNonLoopbackIpv4Address().getHostName());
  }

  private static InetAddress getNonLoopbackIpv4Address() {
    try {
      Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
      for (NetworkInterface networkInterface : Collections.list(nets)) {
        if (!networkInterface.isLoopback() && networkInterface.isUp()) {
          Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
          while (inetAddresses.hasMoreElements()) {
            InetAddress inetAddress = inetAddresses.nextElement();
            if (inetAddress instanceof Inet4Address) {
              return inetAddress;
            }
          }
        }
      }
    } catch (SocketException se) {
      throw new RuntimeException("Cannot find a non loopback card required for tests", se);
    }
    throw new RuntimeException("Cannot find a non loopback card required for tests");
  }
}
