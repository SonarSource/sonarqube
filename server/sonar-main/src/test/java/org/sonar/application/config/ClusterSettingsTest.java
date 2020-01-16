/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.sonar.process.MessageException;
import org.sonar.process.NetworkUtils;

import static com.google.common.collect.ImmutableList.of;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessId.COMPUTE_ENGINE;
import static org.sonar.process.ProcessId.ELASTICSEARCH;
import static org.sonar.process.ProcessId.WEB_SERVER;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_HZ_HOSTS;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_HOST;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_TYPE;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_SEARCH_HOSTS;
import static org.sonar.process.ProcessProperties.Property.JDBC_URL;
import static org.sonar.process.ProcessProperties.Property.SEARCH_HOST;

@RunWith(DataProviderRunner.class)
public class ClusterSettingsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private NetworkUtils network = Mockito.mock(NetworkUtils.class);

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void test_isClusterEnabled(String host) {
    TestAppSettings settings = newSettingsForAppNode(host).set(CLUSTER_ENABLED.getKey(), "true");
    assertThat(ClusterSettings.isClusterEnabled(settings)).isTrue();

    settings = new TestAppSettings().set(CLUSTER_ENABLED.getKey(), "false");
    assertThat(ClusterSettings.isClusterEnabled(settings)).isFalse();
  }

  @Test
  public void isClusterEnabled_returns_false_by_default() {
    assertThat(ClusterSettings.isClusterEnabled(new TestAppSettings())).isFalse();
  }

  @Test
  public void getEnabledProcesses_returns_all_processes_in_standalone_mode() {
    TestAppSettings settings = new TestAppSettings().set(CLUSTER_ENABLED.getKey(), "false");
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
  public void accept_throws_MessageException_if_no_node_type_is_configured() {
    TestAppSettings settings = new TestAppSettings();
    settings.set(CLUSTER_ENABLED.getKey(), "true");

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Property sonar.cluster.node.type is mandatory");

    new ClusterSettings(network).accept(settings.getProps());
  }

  @Test
  public void accept_throws_MessageException_if_node_type_is_not_correct() {
    TestAppSettings settings = new TestAppSettings();
    settings.set(CLUSTER_ENABLED.getKey(), "true");
    settings.set(CLUSTER_NODE_TYPE.getKey(), "bla");

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Invalid value for property sonar.cluster.node.type: [bla], only [application, search] are allowed");

    new ClusterSettings(network).accept(settings.getProps());
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void accept_throws_MessageException_if_internal_property_for_startup_leader_is_configured(String host) {
    TestAppSettings settings = newSettingsForAppNode(host);
    settings.set("sonar.cluster.web.startupLeader", "true");

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Property [sonar.cluster.web.startupLeader] is forbidden");

    new ClusterSettings(network).accept(settings.getProps());
  }

  @Test
  public void accept_does_nothing_if_cluster_is_disabled() {
    TestAppSettings settings = new TestAppSettings();
    settings.set(CLUSTER_ENABLED.getKey(), "false");
    // this property is supposed to fail if cluster is enabled
    settings.set("sonar.cluster.web.startupLeader", "true");

    new ClusterSettings(network).accept(settings.getProps());
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void accept_throws_MessageException_if_h2_on_application_node(String host) {
    TestAppSettings settings = newSettingsForAppNode(host);
    settings.set("sonar.jdbc.url", "jdbc:h2:mem");

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Embedded database is not supported in cluster mode");

    new ClusterSettings(network).accept(settings.getProps());
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void accept_does_not_verify_h2_on_search_node(String host) {
    mockValidHost(host);
    mockLocalNonLoopback(host);
    TestAppSettings settings = newSettingsForSearchNode(host);
    settings.set("sonar.jdbc.url", "jdbc:h2:mem");

    // do not fail
    new ClusterSettings(network).accept(settings.getProps());
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void accept_throws_MessageException_on_application_node_if_default_jdbc_url(String host) {
    TestAppSettings settings = newSettingsForAppNode(host);
    settings.clearProperty(JDBC_URL.getKey());

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Embedded database is not supported in cluster mode");

    new ClusterSettings(network).accept(settings.getProps());
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
    TestAppSettings settings = newSettingsForSearchNode(host);
    settings.set(CLUSTER_ENABLED.getKey(), "false");
    assertThat(ClusterSettings.shouldStartHazelcast(settings)).isFalse();

    settings = newSettingsForAppNode(host);
    settings.set(CLUSTER_ENABLED.getKey(), "false");
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
  public void accept_throws_MessageException_if_searchHost_is_missing(String host) {
    TestAppSettings settings = newSettingsForSearchNode(host);
    settings.clearProperty(SEARCH_HOST.getKey());
    assertThatPropertyIsMandatory(settings, SEARCH_HOST.getKey());
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void accept_throws_MessageException_if_searchHost_is_empty(String host) {
    TestAppSettings settings = newSettingsForSearchNode(host);
    settings.set(SEARCH_HOST.getKey(), "");
    assertThatPropertyIsMandatory(settings, SEARCH_HOST.getKey());
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
  public void accept_throws_MessageException_if_clusterSearchHosts_is_missing(String host) {
    mockValidHost(host);
    mockLocalNonLoopback(host);
    TestAppSettings settings = newSettingsForSearchNode(host);
    settings.clearProperty(CLUSTER_SEARCH_HOSTS.getKey());
    assertThatPropertyIsMandatory(settings, CLUSTER_SEARCH_HOSTS.getKey());
  }

  @Test
  @UseDataProvider("validIPv4andIPv6Addresses")
  public void accept_throws_MessageException_if_clusterSearchHosts_is_empty(String host) {
    mockValidHost(host);
    mockLocalNonLoopback(host);
    TestAppSettings settings = newSettingsForSearchNode(host);
    settings.set(CLUSTER_SEARCH_HOSTS.getKey(), "");
    assertThatPropertyIsMandatory(settings, CLUSTER_SEARCH_HOSTS.getKey());
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
    TestAppSettings settings = new TestAppSettings()
      .set(CLUSTER_ENABLED.getKey(), "true")
      .set(CLUSTER_NODE_TYPE.getKey(), "application")
      .set(CLUSTER_NODE_HOST.getKey(), "hz_host")
      .set(CLUSTER_HZ_HOSTS.getKey(), "remote_hz_host_1,remote_hz_host_2")
      .set(CLUSTER_SEARCH_HOSTS.getKey(), "remote_search_host_1:9001, remote_search_host_2:9001")
      .set(JDBC_URL.getKey(), "jdbc:postgresql://localhost/sonar")
      .set("sonar.auth.jwtBase64Hs256Secret", "abcde");

    verifyHostIsChecked(settings, of("hz_host"), "Address in property sonar.cluster.node.host is not a valid address: hz_host");
    verifyHostIsChecked(settings, of("remote_hz_host_1"), "Address in property sonar.cluster.hosts is not a valid address: remote_hz_host_1");
    verifyHostIsChecked(settings, of("remote_hz_host_2"), "Address in property sonar.cluster.hosts is not a valid address: remote_hz_host_2");
    verifyHostIsChecked(settings,
      of("remote_hz_host_1", "remote_hz_host_2"),
      "Address in property sonar.cluster.hosts is not a valid address: remote_hz_host_1, remote_hz_host_2");
    verifyHostIsChecked(settings, of("remote_search_host_1"), "Address in property sonar.cluster.search.hosts is not a valid address: remote_search_host_1");
    verifyHostIsChecked(settings, of("remote_search_host_2"), "Address in property sonar.cluster.search.hosts is not a valid address: remote_search_host_2");
    verifyHostIsChecked(settings,
      of("remote_search_host_1", "remote_search_host_2"),
      "Address in property sonar.cluster.search.hosts is not a valid address: remote_search_host_1, remote_search_host_2");
  }

  @Test
  public void validate_host_resolved_in_any_cluster_property_of_SEARCH_node() {
    TestAppSettings settings = new TestAppSettings()
      .set(CLUSTER_ENABLED.getKey(), "true")
      .set(CLUSTER_NODE_TYPE.getKey(), "search")
      .set(CLUSTER_NODE_HOST.getKey(), "hz_host")
      .set(CLUSTER_SEARCH_HOSTS.getKey(), "remote_search_host_1:9001, remote_search_host_2:9001")
      .set(SEARCH_HOST.getKey(), "search_host");

    verifyHostIsChecked(settings, of("hz_host"), "Address in property sonar.cluster.node.host is not a valid address: hz_host");
    verifyHostIsChecked(settings, of("remote_search_host_1"), "Address in property sonar.cluster.search.hosts is not a valid address: remote_search_host_1");
    verifyHostIsChecked(settings, of("remote_search_host_2"), "Address in property sonar.cluster.search.hosts is not a valid address: remote_search_host_2");
    verifyHostIsChecked(settings, of("search_host"), "Address in property sonar.search.host is not a valid address: search_host");
  }

  private void verifyHostIsChecked(TestAppSettings settings, Collection<String> invalidHosts, String expectedMessage) {
    reset(network);
    mockAllHostsValidBut(invalidHosts);
    mockLocalNonLoopback("hz_host", "search_host");
    assertThatThrownBy(() -> new ClusterSettings(network).accept(settings.getProps()))
      .isInstanceOf(MessageException.class)
      .hasMessage(expectedMessage);
  }

  @Test
  public void ensure_no_loopback_host_in_properties_of_APP_node() {
    TestAppSettings settings = new TestAppSettings()
      .set(CLUSTER_ENABLED.getKey(), "true")
      .set(CLUSTER_NODE_TYPE.getKey(), "application")
      .set(CLUSTER_NODE_HOST.getKey(), "hz_host")
      .set(CLUSTER_HZ_HOSTS.getKey(), "remote_hz_host_1,remote_hz_host_2")
      .set(CLUSTER_SEARCH_HOSTS.getKey(), "remote_search_host_1:9001, remote_search_host_2:9001")
      .set(JDBC_URL.getKey(), "jdbc:postgresql://localhost/sonar")
      .set("sonar.auth.jwtBase64Hs256Secret", "abcde");

    verifyLoopbackChecked(settings, of("hz_host"), "Property sonar.cluster.node.host must be a local non-loopback address: hz_host");
    verifyLoopbackChecked(settings, of("remote_search_host_1"), "Property sonar.cluster.search.hosts must not contain a loopback address: remote_search_host_1");
    verifyLoopbackChecked(settings, of("remote_search_host_2"), "Property sonar.cluster.search.hosts must not contain a loopback address: remote_search_host_2");
    verifyLoopbackChecked(settings,
      of("remote_search_host_1", "remote_search_host_2"),
      "Property sonar.cluster.search.hosts must not contain a loopback address: remote_search_host_1, remote_search_host_2");
    verifyLoopbackChecked(settings, of("remote_hz_host_1"), "Property sonar.cluster.hosts must not contain a loopback address: remote_hz_host_1");
    verifyLoopbackChecked(settings, of("remote_hz_host_2"), "Property sonar.cluster.hosts must not contain a loopback address: remote_hz_host_2");
    verifyLoopbackChecked(settings,
      of("remote_hz_host_1", "remote_hz_host_2"),
      "Property sonar.cluster.hosts must not contain a loopback address: remote_hz_host_1, remote_hz_host_2");
  }

  @Test
  public void ensure_no_loopback_host_in_properties_of_SEARCH_node() {
    TestAppSettings settings = new TestAppSettings()
      .set(CLUSTER_ENABLED.getKey(), "true")
      .set(CLUSTER_NODE_TYPE.getKey(), "search")
      .set(CLUSTER_NODE_HOST.getKey(), "hz_host")
      .set(CLUSTER_SEARCH_HOSTS.getKey(), "remote_search_host_1:9001, remote_search_host_2:9001")
      .set(SEARCH_HOST.getKey(), "search_host");

    verifyLoopbackChecked(settings, of("hz_host"), "Property sonar.cluster.node.host must be a local non-loopback address: hz_host");
    verifyLoopbackChecked(settings, of("search_host"), "Property sonar.search.host must be a local non-loopback address: search_host");
    verifyLoopbackChecked(settings, of("remote_search_host_1"), "Property sonar.cluster.search.hosts must not contain a loopback address: remote_search_host_1");
    verifyLoopbackChecked(settings, of("remote_search_host_2"), "Property sonar.cluster.search.hosts must not contain a loopback address: remote_search_host_2");
    verifyLoopbackChecked(settings,
      of("remote_search_host_1", "remote_search_host_2"),
      "Property sonar.cluster.search.hosts must not contain a loopback address: remote_search_host_1, remote_search_host_2");
  }

  private void verifyLoopbackChecked(TestAppSettings settings, Collection<String> hosts, String expectedMessage) {
    reset(network);
    mockAllHostsValid();
    mockLocalNonLoopback("hz_host", "search_host");
    // will overwrite above move if necessary
    hosts.forEach(this::mockLoopback);
    assertThatThrownBy(() -> new ClusterSettings(network).accept(settings.getProps()))
      .isInstanceOf(MessageException.class)
      .hasMessage(expectedMessage);
  }

  @Test
  public void ensure_HZ_HOST_is_local_non_loopback_in_properties_of_APP_node() {
    TestAppSettings settings = new TestAppSettings()
      .set(CLUSTER_ENABLED.getKey(), "true")
      .set(CLUSTER_NODE_TYPE.getKey(), "application")
      .set(CLUSTER_NODE_HOST.getKey(), "hz_host")
      .set(CLUSTER_HZ_HOSTS.getKey(), "remote_hz_host_1,remote_hz_host_2")
      .set(CLUSTER_SEARCH_HOSTS.getKey(), "remote_search_host_1:9001, remote_search_host_2:9001")
      .set(JDBC_URL.getKey(), "jdbc:postgresql://localhost/sonar")
      .set("sonar.auth.jwtBase64Hs256Secret", "abcde");

    verifyLocalChecked(settings, "hz_host", "Property sonar.cluster.node.host must be a local non-loopback address: hz_host");
  }

  @Test
  public void ensure_HZ_HOST_and_SEARCH_HOST_are_local_non_loopback_in_properties_of_SEARCH_node() {
    TestAppSettings settings = new TestAppSettings()
      .set(CLUSTER_ENABLED.getKey(), "true")
      .set(CLUSTER_NODE_TYPE.getKey(), "search")
      .set(CLUSTER_NODE_HOST.getKey(), "hz_host")
      .set(CLUSTER_SEARCH_HOSTS.getKey(), "remote_search_host_1:9001, remote_search_host_2:9001")
      .set(SEARCH_HOST.getKey(), "search_host");

    verifyLocalChecked(settings, "hz_host", "Property sonar.cluster.node.host must be a local non-loopback address: hz_host");
    verifyLocalChecked(settings, "search_host", "Property sonar.search.host must be a local non-loopback address: search_host");
  }

  private void verifyLocalChecked(TestAppSettings settings, String host, String expectedMessage) {
    reset(network);
    mockAllHostsValid();
    mockLocalNonLoopback("hz_host", "search_host");
    // will overwrite above move if necessary
    mockAllNonLoopback();
    mockNonLocal(host);
    assertThatThrownBy(() -> new ClusterSettings(network).accept(settings.getProps()))
      .isInstanceOf(MessageException.class)
      .hasMessage(expectedMessage);
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
    expectedException.expect(MessageException.class);
    expectedException.expectMessage(format("Property %s is mandatory", key));

    new ClusterSettings(network).accept(settings.getProps());
  }

  private TestAppSettings newSettingsForAppNode(String host) {
    return new TestAppSettings()
      .set(CLUSTER_ENABLED.getKey(), "true")
      .set(CLUSTER_NODE_TYPE.getKey(), "application")
      .set(CLUSTER_NODE_HOST.getKey(), host)
      .set(CLUSTER_HZ_HOSTS.getKey(), host)
      .set(CLUSTER_SEARCH_HOSTS.getKey(), host + ":9001")
      .set("sonar.auth.jwtBase64Hs256Secret", "abcde")
      .set(JDBC_URL.getKey(), "jdbc:postgresql://localhost/sonar");
  }

  private TestAppSettings newSettingsForSearchNode(String host) {
    return new TestAppSettings()
      .set(CLUSTER_ENABLED.getKey(), "true")
      .set(CLUSTER_NODE_TYPE.getKey(), "search")
      .set(CLUSTER_NODE_HOST.getKey(), host)
      .set(CLUSTER_HZ_HOSTS.getKey(), host)
      .set(CLUSTER_SEARCH_HOSTS.getKey(), host + ":9001")
      .set(SEARCH_HOST.getKey(), host);
  }

}
