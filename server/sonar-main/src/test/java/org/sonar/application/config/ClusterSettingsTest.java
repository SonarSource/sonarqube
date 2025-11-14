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
package org.sonar.application.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.sonar.process.MessageException;
import org.sonar.process.NetworkUtils;
import org.sonar.process.Props;

import static com.google.common.collect.ImmutableMap.of;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessId.COMPUTE_ENGINE;
import static org.sonar.process.ProcessId.ELASTICSEARCH;
import static org.sonar.process.ProcessId.WEB_SERVER;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ES_HOSTS;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_HZ_HOSTS;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_ES_HOST;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_HOST;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_SEARCH_HOST;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_TYPE;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_SEARCH_HOSTS;
import static org.sonar.process.ProcessProperties.Property.JDBC_URL;

@RunWith(DataProviderRunner.class)
public class ClusterSettingsTest {

  private final NetworkUtils network = Mockito.mock(NetworkUtils.class);

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void test_isClusterEnabled(String host) {
    TestAppSettings settings = newSettingsForAppNode(host, of(CLUSTER_ENABLED.getKey(), "true"));
    assertThat(ClusterSettings.isClusterEnabled(settings)).isTrue();

    settings = new TestAppSettings(of(CLUSTER_ENABLED.getKey(), "false"));
    assertThat(ClusterSettings.isClusterEnabled(settings)).isFalse();
  }

  @Test
  public void isClusterEnabled_returns_false_by_default() {
    assertThat(ClusterSettings.isClusterEnabled(new TestAppSettings())).isFalse();
  }

  @Test
  public void getEnabledProcesses_returns_all_processes_in_standalone_mode() {
    TestAppSettings settings = new TestAppSettings(of(CLUSTER_ENABLED.getKey(), "false"));
    assertThat(ClusterSettings.getEnabledProcesses(settings)).containsOnly(COMPUTE_ENGINE, ELASTICSEARCH, WEB_SERVER);
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void getEnabledProcesses_returns_configured_processes_in_cluster_mode(String host) {
    TestAppSettings settings = newSettingsForAppNode(host);
    assertThat(ClusterSettings.getEnabledProcesses(settings)).containsOnly(COMPUTE_ENGINE, WEB_SERVER);

    settings = newSettingsForSearchNode(host);
    assertThat(ClusterSettings.getEnabledProcesses(settings)).containsOnly(ELASTICSEARCH);
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void valid_configuration_of_app_node_does_not_throw_exception(String host) {
    mockValidHost(host);
    mockLocalNonLoopback(host);
    TestAppSettings settings = newSettingsForAppNode(host);
    ClusterSettings clusterSettings = new ClusterSettings(network);
    Props props = settings.getProps();

    assertThatCode(() -> clusterSettings.accept(props))
      .doesNotThrowAnyException();
  }

  @Test
  public void accept_throws_MessageException_if_no_node_type_is_configured() {
    TestAppSettings settings = new TestAppSettings(of(CLUSTER_ENABLED.getKey(), "true"));
    ClusterSettings clusterSettings = new ClusterSettings(network);
    Props props = settings.getProps();

    assertThatThrownBy(() -> clusterSettings.accept(props))
      .isInstanceOf(MessageException.class)
      .hasMessage("Property sonar.cluster.node.type is mandatory");
  }

  @Test
  public void accept_throws_MessageException_if_node_type_is_not_correct() {
    TestAppSettings settings = new TestAppSettings(of(CLUSTER_ENABLED.getKey(), "true", CLUSTER_NODE_TYPE.getKey(), "bla"));
    ClusterSettings clusterSettings = new ClusterSettings(network);
    Props props = settings.getProps();

    assertThatThrownBy(() -> clusterSettings.accept(props))
      .isInstanceOf(MessageException.class)
      .hasMessage("Invalid value for property sonar.cluster.node.type: [bla], only [application, search] are allowed");
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void accept_throws_MessageException_if_internal_property_for_startup_leader_is_configured(String host) {
    TestAppSettings settings = newSettingsForAppNode(host, of("sonar.cluster.web.startupLeader", "true"));
    ClusterSettings clusterSettings = new ClusterSettings(network);
    Props props = settings.getProps();

    assertThatThrownBy(() -> clusterSettings.accept(props))
      .isInstanceOf(MessageException.class)
      .hasMessage("Property [sonar.cluster.web.startupLeader] is forbidden");
  }

  @Test
  public void accept_does_nothing_if_cluster_is_disabled() {
    TestAppSettings settings = new TestAppSettings(of(
      CLUSTER_ENABLED.getKey(), "false",
      // this property is supposed to fail if cluster is enabled
      "sonar.cluster.web.startupLeader", "true"));
    new ClusterSettings(network).accept(settings.getProps());
  }

  @Test
  public void accept_throws_MessageException_if_a_cluster_forbidden_property_is_defined_in_a_cluster_search_node() {
    TestAppSettings settings = new TestAppSettings(of(
      CLUSTER_ENABLED.getKey(), "true",
      CLUSTER_NODE_TYPE.getKey(), "search",
      "sonar.search.host", "localhost"));
    Props props = settings.getProps();
    ClusterSettings clusterSettings = new ClusterSettings(network);

    assertThatThrownBy(() -> clusterSettings.accept(props))
      .isInstanceOf(MessageException.class)
      .hasMessage("Properties [sonar.search.host] are not allowed when running SonarQube in cluster mode.");
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void accept_throws_MessageException_if_h2_on_application_node(String host) {
    TestAppSettings settings = newSettingsForAppNode(host, of("sonar.jdbc.url", "jdbc:h2:mem"));
    ClusterSettings clusterSettings = new ClusterSettings(network);
    Props props = settings.getProps();

    assertThatThrownBy(() -> clusterSettings.accept(props))
      .isInstanceOf(MessageException.class)
      .hasMessage("Embedded database is not supported in cluster mode");
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void accept_does_not_verify_h2_on_search_node(String host) {
    mockValidHost(host);
    mockLocalNonLoopback(host);
    TestAppSettings settings = newSettingsForSearchNode(host, of("sonar.jdbc.url", "jdbc:h2:mem"));

    // do not fail
    new ClusterSettings(network).accept(settings.getProps());
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void accept_throws_MessageException_on_application_node_if_default_jdbc_url(String host) {
    TestAppSettings settings = newSettingsForAppNode(host);
    settings.clearProperty(JDBC_URL.getKey());
    ClusterSettings clusterSettings = new ClusterSettings(network);
    Props props = settings.getProps();

    assertThatThrownBy(() -> clusterSettings.accept(props))
      .isInstanceOf(MessageException.class)
      .hasMessage("Embedded database is not supported in cluster mode");
  }

  @Test
  public void isLocalElasticsearchEnabled_returns_true_in_standalone_mode() {
    TestAppSettings settings = new TestAppSettings();
    assertThat(ClusterSettings.isLocalElasticsearchEnabled(settings)).isTrue();
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void isLocalElasticsearchEnabled_returns_true_on_search_node(String host) {
    TestAppSettings settings = newSettingsForSearchNode(host);

    assertThat(ClusterSettings.isLocalElasticsearchEnabled(settings)).isTrue();
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void shouldStartHazelcast_must_be_true_on_AppNode(String host) {
    TestAppSettings settings = newSettingsForAppNode(host);

    assertThat(ClusterSettings.shouldStartHazelcast(settings)).isTrue();
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void shouldStartHazelcast_must_be_false_on_SearchNode(String host) {
    TestAppSettings settings = newSettingsForSearchNode(host);

    assertThat(ClusterSettings.shouldStartHazelcast(settings)).isFalse();
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void shouldStartHazelcast_must_be_false_when_cluster_not_activated(String host) {
    TestAppSettings settings = newSettingsForSearchNode(host, of(CLUSTER_ENABLED.getKey(), "false"));
    assertThat(ClusterSettings.shouldStartHazelcast(settings)).isFalse();

    settings = newSettingsForAppNode(host, of(CLUSTER_ENABLED.getKey(), "false"));
    assertThat(ClusterSettings.shouldStartHazelcast(settings)).isFalse();
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void isLocalElasticsearchEnabled_returns_true_for_a_application_node(String host) {
    TestAppSettings settings = newSettingsForAppNode(host);

    assertThat(ClusterSettings.isLocalElasticsearchEnabled(settings)).isFalse();
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void accept_throws_MessageException_if_clusterNodeEsHost_is_missing(String host) {
    String searchHost = "search_host";
    TestAppSettings settings = newSettingsForSearchNode(host, of(CLUSTER_NODE_SEARCH_HOST.getKey(), searchHost));
    mockValidHost(searchHost);
    mockLocalNonLoopback(searchHost);

    settings.clearProperty(CLUSTER_NODE_ES_HOST.getKey());
    assertThatPropertyIsMandatory(settings, CLUSTER_NODE_ES_HOST.getKey());
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void accept_throws_MessageException_if_clusterNodeSearchHost_is_missing(String host) {
    String esHost = "es_host";
    TestAppSettings settings = newSettingsForSearchNode(host, of(CLUSTER_NODE_ES_HOST.getKey(), esHost));
    mockValidHost(esHost);
    mockLocalNonLoopback(esHost);

    settings.clearProperty(CLUSTER_NODE_SEARCH_HOST.getKey());
    assertThatPropertyIsMandatory(settings, CLUSTER_NODE_SEARCH_HOST.getKey());
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void accept_throws_MessageException_if_sonarClusterNodeSearchHost_is_empty(String host) {
    String esHost = "es_host";
    TestAppSettings settings = newSettingsForSearchNode(host, of(
      CLUSTER_NODE_SEARCH_HOST.getKey(), "",
      CLUSTER_NODE_ES_HOST.getKey(), esHost));
    mockValidHost(esHost);
    mockLocalNonLoopback(esHost);

    assertThatPropertyIsMandatory(settings, CLUSTER_NODE_SEARCH_HOST.getKey());
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void accept_throws_MessageException_if_sonarClusterNodeEsHost_is_empty(String host) {
    String searchHost = "search_host";
    TestAppSettings settings = newSettingsForSearchNode(host, of(
      CLUSTER_NODE_ES_HOST.getKey(), "",
      CLUSTER_NODE_SEARCH_HOST.getKey(), searchHost));
    mockValidHost(searchHost);
    mockLocalNonLoopback(searchHost);

    assertThatPropertyIsMandatory(settings, CLUSTER_NODE_ES_HOST.getKey());
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void accept_throws_MessageException_on_app_node_if_clusterHosts_is_missing(String host) {
    TestAppSettings settings = newSettingsForAppNode(host);
    settings.clearProperty(CLUSTER_HZ_HOSTS.getKey());
    assertThatPropertyIsMandatory(settings, CLUSTER_HZ_HOSTS.getKey());
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void accept_throws_MessageException_if_clusterEsHosts_is_missing(String host) {
    mockValidHost(host);
    mockLocalNonLoopback(host);
    TestAppSettings settings = newSettingsForSearchNode(host);
    settings.clearProperty(CLUSTER_ES_HOSTS.getKey());
    assertThatPropertyIsMandatory(settings, CLUSTER_ES_HOSTS.getKey());
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void accept_throws_MessageException_if_clusterEsHosts_is_empty(String host) {
    mockValidHost(host);
    mockLocalNonLoopback(host);
    TestAppSettings settings = newSettingsForSearchNode(host, of(CLUSTER_ES_HOSTS.getKey(), ""));
    assertThatPropertyIsMandatory(settings, CLUSTER_ES_HOSTS.getKey());
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void accept_throws_MessageException_if_jwt_token_is_not_set_on_application_nodes(String host) {
    TestAppSettings settings = newSettingsForAppNode(host);
    settings.clearProperty("sonar.auth.jwtBase64Hs256Secret");
    assertThatPropertyIsMandatory(settings, "sonar.auth.jwtBase64Hs256Secret");
  }

  @Test
  public void shouldStartHazelcast_should_return_false_when_cluster_not_enabled() {
    TestAppSettings settings = new TestAppSettings();
    assertThat(ClusterSettings.shouldStartHazelcast(settings)).isFalse();
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void shouldStartHazelcast_should_return_false_on_SearchNode(String host) {
    assertThat(ClusterSettings.shouldStartHazelcast(newSettingsForSearchNode(host))).isFalse();
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void shouldStartHazelcast_should_return_true_on_AppNode(String host) {
    assertThat(ClusterSettings.shouldStartHazelcast(newSettingsForAppNode(host))).isTrue();
  }

  @Test
  public void validate_host_in_any_cluster_property_of_APP_node() {
    TestAppSettings settings = new TestAppSettings(ImmutableMap.<String, String>builder()
      .put(CLUSTER_ENABLED.getKey(), "true")
      .put(CLUSTER_NODE_TYPE.getKey(), "application")
      .put(CLUSTER_NODE_HOST.getKey(), "hz_host")
      .put(CLUSTER_HZ_HOSTS.getKey(), "remote_hz_host_1,remote_hz_host_2")
      .put(CLUSTER_SEARCH_HOSTS.getKey(), "remote_search_host_1:9001, remote_search_host_2:9001")
      .put(JDBC_URL.getKey(), "jdbc:postgresql://localhost/sonar")
      .put("sonar.auth.jwtBase64Hs256Secret", "abcde")
      .build());

    verifyHostIsChecked(settings, ImmutableList.of("hz_host"), "Address in property sonar.cluster.node.host is not a valid address: hz_host");
    verifyHostIsChecked(settings, ImmutableList.of("remote_hz_host_1"), "Address in property sonar.cluster.hosts is not a valid address: remote_hz_host_1");
    verifyHostIsChecked(settings, ImmutableList.of("remote_hz_host_2"), "Address in property sonar.cluster.hosts is not a valid address: remote_hz_host_2");
    verifyHostIsChecked(settings,
      ImmutableList.of("remote_hz_host_1", "remote_hz_host_2"),
      "Address in property sonar.cluster.hosts is not a valid address: remote_hz_host_1, remote_hz_host_2");
    verifyHostIsChecked(settings, ImmutableList.of("remote_search_host_1"), "Address in property sonar.cluster.search.hosts is not a valid address: remote_search_host_1");
    verifyHostIsChecked(settings, ImmutableList.of("remote_search_host_2"), "Address in property sonar.cluster.search.hosts is not a valid address: remote_search_host_2");
    verifyHostIsChecked(settings,
      ImmutableList.of("remote_search_host_1", "remote_search_host_2"),
      "Address in property sonar.cluster.search.hosts is not a valid address: remote_search_host_1, remote_search_host_2");
  }

  @Test
  public void validate_host_resolved_in_node_search_host_property_of_SEARCH_node() {
    TestAppSettings settings = new TestAppSettings(ImmutableMap.<String, String>builder()
      .put(CLUSTER_ENABLED.getKey(), "true")
      .put(CLUSTER_NODE_TYPE.getKey(), "search")
      .put(CLUSTER_ES_HOSTS.getKey(), "remote_search_host_1:9001, remote_search_host_2:9001")
      .put(CLUSTER_NODE_SEARCH_HOST.getKey(), "search_host")
      .put(CLUSTER_NODE_ES_HOST.getKey(), "search_host").build());

    verifyHostIsChecked(settings, ImmutableList.of("search_host"), "Address in property sonar.cluster.node.search.host is not a valid address: search_host");
  }

  private void verifyHostIsChecked(TestAppSettings settings, Collection<String> invalidHosts, String expectedMessage) {
    reset(network);
    mockAllHostsValidBut(invalidHosts);
    mockLocalNonLoopback("hz_host", "search_host");
    ClusterSettings clusterSettings = new ClusterSettings(network);
    Props props = settings.getProps();
    assertThatThrownBy(() -> clusterSettings.accept(props))
      .isInstanceOf(MessageException.class)
      .hasMessage(expectedMessage);
  }

  @Test
  public void ensure_no_loopback_host_in_properties_of_APP_node() {
    TestAppSettings settings = new TestAppSettings(ImmutableMap.<String, String>builder()
      .put(CLUSTER_ENABLED.getKey(), "true")
      .put(CLUSTER_NODE_TYPE.getKey(), "application")
      .put(CLUSTER_NODE_HOST.getKey(), "hz_host")
      .put(CLUSTER_HZ_HOSTS.getKey(), "remote_hz_host_1,remote_hz_host_2")
      .put(CLUSTER_SEARCH_HOSTS.getKey(), "remote_search_host_1:9001, remote_search_host_2:9001")
      .put(JDBC_URL.getKey(), "jdbc:postgresql://localhost/sonar")
      .put("sonar.auth.jwtBase64Hs256Secret", "abcde")
      .build());

    verifyLoopbackChecked(settings, ImmutableList.of("hz_host"), "Property sonar.cluster.node.host must be a local non-loopback address: hz_host");
    verifyLoopbackChecked(settings, ImmutableList.of("remote_search_host_1"), "Property sonar.cluster.search.hosts must not contain a loopback address: remote_search_host_1");
    verifyLoopbackChecked(settings, ImmutableList.of("remote_search_host_2"), "Property sonar.cluster.search.hosts must not contain a loopback address: remote_search_host_2");
    verifyLoopbackChecked(settings,
      ImmutableList.of("remote_search_host_1", "remote_search_host_2"),
      "Property sonar.cluster.search.hosts must not contain a loopback address: remote_search_host_1, remote_search_host_2");
    verifyLoopbackChecked(settings, ImmutableList.of("remote_hz_host_1"), "Property sonar.cluster.hosts must not contain a loopback address: remote_hz_host_1");
    verifyLoopbackChecked(settings, ImmutableList.of("remote_hz_host_2"), "Property sonar.cluster.hosts must not contain a loopback address: remote_hz_host_2");
    verifyLoopbackChecked(settings,
      ImmutableList.of("remote_hz_host_1", "remote_hz_host_2"),
      "Property sonar.cluster.hosts must not contain a loopback address: remote_hz_host_1, remote_hz_host_2");
  }

  @Test
  public void ensure_no_loopback_host_in_properties_of_SEARCH_node() {
    TestAppSettings settings = new TestAppSettings(ImmutableMap.<String, String>builder()
      .put(CLUSTER_ENABLED.getKey(), "true")
      .put(CLUSTER_NODE_TYPE.getKey(), "search")
      .put(CLUSTER_ES_HOSTS.getKey(), "remote_search_host_1:9001, remote_search_host_2:9001")
      .put(CLUSTER_NODE_SEARCH_HOST.getKey(), "search_host")
      .put(CLUSTER_NODE_ES_HOST.getKey(), "transport_host")
      .build());

    verifyLoopbackChecked(settings, ImmutableList.of("search_host"), "Property sonar.cluster.node.search.host must be a local non-loopback address: search_host");
    verifyLoopbackChecked(settings, ImmutableList.of("transport_host"), "Property sonar.cluster.node.es.host must be a local non-loopback address: transport_host");
    verifyLoopbackChecked(settings, ImmutableList.of("remote_search_host_1"), "Property sonar.cluster.es.hosts must not contain a loopback address: remote_search_host_1");
    verifyLoopbackChecked(settings, ImmutableList.of("remote_search_host_2"), "Property sonar.cluster.es.hosts must not contain a loopback address: remote_search_host_2");
    verifyLoopbackChecked(settings,
      ImmutableList.of("remote_search_host_1", "remote_search_host_2"),
      "Property sonar.cluster.es.hosts must not contain a loopback address: remote_search_host_1, remote_search_host_2");
  }

  private void verifyLoopbackChecked(TestAppSettings settings, Collection<String> hosts, String expectedMessage) {
    reset(network);
    mockAllHostsValid();
    mockLocalNonLoopback("hz_host", "search_host", "transport_host");
    // will overwrite above move if necessary
    hosts.forEach(this::mockLoopback);
    ClusterSettings clusterSettings = new ClusterSettings(network);
    Props props = settings.getProps();
    assertThatThrownBy(() -> clusterSettings.accept(props))
      .isInstanceOf(MessageException.class)
      .hasMessage(expectedMessage);
  }

  @Test
  public void ensure_HZ_HOST_is_local_non_loopback_in_properties_of_APP_node() {
    TestAppSettings settings = new TestAppSettings(ImmutableMap.<String, String>builder()
      .put(CLUSTER_ENABLED.getKey(), "true")
      .put(CLUSTER_NODE_TYPE.getKey(), "application")
      .put(CLUSTER_NODE_HOST.getKey(), "hz_host")
      .put(CLUSTER_HZ_HOSTS.getKey(), "remote_hz_host_1,remote_hz_host_2")
      .put(CLUSTER_SEARCH_HOSTS.getKey(), "remote_search_host_1:9001, remote_search_host_2:9001")
      .put(JDBC_URL.getKey(), "jdbc:postgresql://localhost/sonar")
      .put("sonar.auth.jwtBase64Hs256Secret", "abcde")
      .build());

    verifyLocalChecked(settings, "hz_host", "Property sonar.cluster.node.host must be a local non-loopback address: hz_host");
  }

  @Test
  public void ensure_HZ_HOST_and_SEARCH_HOST_are_local_non_loopback_in_properties_of_SEARCH_node() {
    TestAppSettings settings = new TestAppSettings(ImmutableMap.<String, String>builder()
      .put(CLUSTER_ENABLED.getKey(), "true")
      .put(CLUSTER_NODE_TYPE.getKey(), "search")
      .put(CLUSTER_ES_HOSTS.getKey(), "remote_search_host_1:9001, remote_search_host_2:9001")
      .put(CLUSTER_NODE_SEARCH_HOST.getKey(), "search_host")
      .put(CLUSTER_NODE_ES_HOST.getKey(), "search_host")
      .build());

    verifyLocalChecked(settings, "search_host", "Property sonar.cluster.node.search.host must be a local non-loopback address: search_host");
  }

  private void verifyLocalChecked(TestAppSettings settings, String host, String expectedMessage) {
    reset(network);
    mockAllHostsValid();
    mockLocalNonLoopback("hz_host", "search_host");
    // will overwrite above move if necessary
    mockAllNonLoopback();
    mockNonLocal(host);
    ClusterSettings clusterSettings = new ClusterSettings(network);
    Props props = settings.getProps();

    assertThatThrownBy(() -> clusterSettings.accept(props))
      .isInstanceOf(MessageException.class)
      .hasMessage(expectedMessage);
  }

  @Test
  public void accept_hosts_only_or_hosts_and_ports_only_in_property_SONAR_CLUSTER_ES_HOSTS_of_search_node() {
    mockAllHostsValid();
    mockLocalNonLoopback("search_host", "transport_host");

    verifyAllHostsWithPortsOrAllHostsWithoutPortsIsValid("remote_search_host_1, remote_search_host_2");
    verifyAllHostsWithPortsOrAllHostsWithoutPortsIsValid("remote_search_host_1:9001, remote_search_host_2:9001");
  }

  private void verifyAllHostsWithPortsOrAllHostsWithoutPortsIsValid(String searchPropertyValue) {
    TestAppSettings settings = new TestAppSettings(ImmutableMap.<String, String>builder()
      .put(CLUSTER_ENABLED.getKey(), "true")
      .put(CLUSTER_NODE_TYPE.getKey(), "search")
      .put(CLUSTER_NODE_SEARCH_HOST.getKey(), "search_host")
      .put(CLUSTER_NODE_ES_HOST.getKey(), "transport_host")
      .put(CLUSTER_ES_HOSTS.getKey(), searchPropertyValue)
      .build());
    ClusterSettings clusterSettings = new ClusterSettings(network);
    Props props = settings.getProps();

    assertThatCode(() -> clusterSettings.accept(props))
      .doesNotThrowAnyException();
  }

  @Test
  public void accept_any_properties_configuration_in_SONAR_CLUSTER_SEARCH_HOSTS_of_search_node() {
    mockAllHostsValid();
    mockLocalNonLoopback("search_host", "transport_host");

    verifyAnyHostsConfigurationIsValid("remote_search_host_1, remote_search_host_2");
    verifyAnyHostsConfigurationIsValid("remote_search_host_1:9001, remote_search_host_2:9001");
    verifyAnyHostsConfigurationIsValid("remote_search_host_1, remote_search_host_2:9001");
    verifyAnyHostsConfigurationIsValid("remote_search_host_1, remote_search_host_2");
  }

  private void verifyAnyHostsConfigurationIsValid(String searchPropertyValue) {
    TestAppSettings settings = new TestAppSettings(ImmutableMap.<String, String>builder()
      .put(CLUSTER_ENABLED.getKey(), "true")
      .put(CLUSTER_NODE_TYPE.getKey(), "search")
      .put(CLUSTER_NODE_SEARCH_HOST.getKey(), "search_host")
      .put(CLUSTER_ES_HOSTS.getKey(), "transport_host,transport_host")
      .put(CLUSTER_NODE_ES_HOST.getKey(), "transport_host")
      .put(CLUSTER_SEARCH_HOSTS.getKey(), searchPropertyValue)
      .build());
    ClusterSettings clusterSettings = new ClusterSettings(network);
    Props props = settings.getProps();

    assertThatCode(() -> clusterSettings.accept(props))
      .doesNotThrowAnyException();
  }

  @Test
  public void ensure_no_mixed_settings_in_ES_HOSTS_in_properties_of_SEARCH_node() {
    mockAllHostsValid();
    mockLocalNonLoopback("hz_host", "search_host", "transport_host");

    verifyPortsAreCheckedOnEsNode("remote_search_host_1,remote_search_host_2:9001");
    verifyPortsAreCheckedOnEsNode("remote_search_host_1:9002, remote_search_host_2");
  }

  private void verifyPortsAreCheckedOnEsNode(String searchPropertyValue) {
    TestAppSettings settings = new TestAppSettings(ImmutableMap.<String, String>builder()
      .put(CLUSTER_ENABLED.getKey(), "true")
      .put(CLUSTER_NODE_TYPE.getKey(), "search")
      .put(CLUSTER_NODE_SEARCH_HOST.getKey(), "search_host")
      .put(CLUSTER_NODE_ES_HOST.getKey(), "transport_host")
      .put(CLUSTER_ES_HOSTS.getKey(), searchPropertyValue)
      .build());
    ClusterSettings clusterSettings = new ClusterSettings(network);
    Props props = settings.getProps();

    assertThatThrownBy(() -> clusterSettings.accept(props))
      .isInstanceOf(MessageException.class)
      .hasMessage("Entries in property sonar.cluster.es.hosts must not mix 'host:port' and 'host'. Provide hosts without port only or hosts with port only.");
  }

  private void mockAllNonLoopback() {
    when(network.isLoopback(anyString())).thenReturn(false);
  }

  private void mockNonLocal(String search_host) {
    when(network.isLocal(search_host)).thenReturn(false);
  }

  private void mockLoopback(String host) {
    when(network.isLoopback(host)).thenReturn(true);
  }

  private void mockValidHost(String host) {
    String unbracketedHost = host.startsWith("[") ? host.substring(1, host.length() - 1) : host;
    when(network.toInetAddress(unbracketedHost)).thenReturn(Optional.of(InetAddress.getLoopbackAddress()));
  }

  public void mockAllHostsValid() {
    when(network.toInetAddress(anyString())).thenReturn(Optional.of(InetAddress.getLoopbackAddress()));
  }

  public void mockAllHostsValidBut(Collection<String> hosts) {
    when(network.toInetAddress(anyString()))
      .thenAnswer((Answer<Optional<InetAddress>>) invocation -> {
        Object arg = invocation.getArgument(0);
        if (hosts.contains(arg)) {
          return Optional.empty();
        }
        return Optional.of(InetAddress.getLoopbackAddress());
      });
  }

  private void mockLocalNonLoopback(String host, String... otherhosts) {
    Stream.concat(Stream.of(host), Arrays.stream(otherhosts))
      .forEach(h -> {
        String unbracketedHost = h.startsWith("[") ? h.substring(1, h.length() - 1) : h;
        when(network.isLocal(unbracketedHost)).thenReturn(true);
        when(network.isLoopback(unbracketedHost)).thenReturn(false);
      });

  }

  @DataProvider
  public static Object[][] validIPv4andIPv6Addresses() {
    return new Object[][] {
      {"10.150.0.188"},
      {"[fe80::fde2:607e:ae56:e636]"},
    };
  }

  private void assertThatPropertyIsMandatory(TestAppSettings settings, String key) {
    ClusterSettings clusterSettings = new ClusterSettings(network);
    Props props = settings.getProps();
    assertThatThrownBy(() -> clusterSettings.accept(props))
      .isInstanceOf(MessageException.class)
      .hasMessage(format("Property %s is mandatory", key));
  }

  private TestAppSettings newSettingsForAppNode(String host) {
    return newSettingsForAppNode(host, of());
  }

  private TestAppSettings newSettingsForAppNode(String host, ImmutableMap<String, String> settings) {
    Map<String, String> result = new HashMap<>();
    result.put(CLUSTER_ENABLED.getKey(), "true");
    result.put(CLUSTER_NODE_TYPE.getKey(), "application");
    result.put(CLUSTER_NODE_HOST.getKey(), host);
    result.put(CLUSTER_HZ_HOSTS.getKey(), host);
    result.put(CLUSTER_SEARCH_HOSTS.getKey(), host + ":9001");
    result.put("sonar.auth.jwtBase64Hs256Secret", "abcde");
    result.put(JDBC_URL.getKey(), "jdbc:postgresql://localhost/sonar");
    result.putAll(settings);
    return new TestAppSettings(result);
  }

  private TestAppSettings newSettingsForSearchNode(String host) {
    return newSettingsForSearchNode(host, of());
  }

  private TestAppSettings newSettingsForSearchNode(String host, ImmutableMap<String, String> settings) {
    Map<String, String> result = new HashMap<>();
    result.put(CLUSTER_ENABLED.getKey(), "true");
    result.put(CLUSTER_NODE_TYPE.getKey(), "search");
    result.put(CLUSTER_NODE_HOST.getKey(), host);
    result.put(CLUSTER_HZ_HOSTS.getKey(), host);
    result.put(CLUSTER_ES_HOSTS.getKey(), host + ":9001");
    result.put(CLUSTER_NODE_SEARCH_HOST.getKey(), host);
    result.put(CLUSTER_NODE_ES_HOST.getKey(), host);
    result.putAll(settings);
    return new TestAppSettings(result);
  }

}
