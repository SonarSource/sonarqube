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

import com.google.common.collect.ImmutableMap;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.sonar.process.MessageException;
import org.sonar.process.NetworkUtils;
import org.sonar.process.NetworkUtilsImpl;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.spy;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ES_HOSTS;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_HZ_HOSTS;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_ES_HOST;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_HOST;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_SEARCH_HOST;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_TYPE;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_SEARCH_HOSTS;
import static org.sonar.process.ProcessProperties.Property.JDBC_URL;

public class ClusterSettingsLoopbackTest {

  private final InetAddress loopback = InetAddress.getLoopbackAddress();
  private final NetworkUtils network = spy(NetworkUtilsImpl.INSTANCE);

  private InetAddress nonLoopbackLocal;

  @Before
  public void setUp() {
    Optional<InetAddress> opt = network.getLocalNonLoopbackIpv4Address();
    assumeThat(opt.isPresent(), CoreMatchers.is(true));

    nonLoopbackLocal = opt.get();
  }

  @Test
  public void ClusterSettings_throws_MessageException_if_es_http_host_of_search_node_is_loopback() {
    TestAppSettings settings = newSettingsForSearchNode(ImmutableMap.of(CLUSTER_NODE_SEARCH_HOST.getKey(), loopback.getHostAddress()));
    Props props = settings.getProps();
    ClusterSettings clusterSettings = new ClusterSettings(network);

    assertThatThrownBy(() -> clusterSettings.accept(props))
      .isInstanceOf(MessageException.class)
      .hasMessage("Property " + CLUSTER_NODE_SEARCH_HOST.getKey() + " must be a local non-loopback address: " + loopback.getHostAddress());
  }

  @Test
  public void ClusterSettings_throws_MessageException_if_es_transport_host_of_search_node_is_loopback() {
    TestAppSettings settings = newSettingsForSearchNode(ImmutableMap.of(CLUSTER_NODE_ES_HOST.getKey(), loopback.getHostAddress()));
    Props props = settings.getProps();
    ClusterSettings clusterSettings = new ClusterSettings(network);

    assertThatThrownBy(() -> clusterSettings.accept(props))
      .isInstanceOf(MessageException.class)
      .hasMessage("Property " + CLUSTER_NODE_ES_HOST.getKey() + " must be a local non-loopback address: " + loopback.getHostAddress());
  }

  @Test
  public void ClusterSettings_throws_MessageException_if_host_of_app_node_is_loopback() {
    TestAppSettings settings = newSettingsForAppNode(ImmutableMap.of(CLUSTER_NODE_HOST.getKey(), loopback.getHostAddress()));
    Props props = settings.getProps();
    ClusterSettings clusterSettings = new ClusterSettings(network);

    assertThatThrownBy(() -> clusterSettings.accept(props))
      .isInstanceOf(MessageException.class)
      .hasMessage("Property " + CLUSTER_NODE_HOST.getKey() + " must be a local non-loopback address: " + loopback.getHostAddress());
  }

  private TestAppSettings newSettingsForAppNode(ImmutableMap<String, String> settings) {
    Map<String, String> result = new HashMap<>();
    result.put(CLUSTER_ENABLED.getKey(), "true");
    result.put(CLUSTER_NODE_TYPE.getKey(), "application");
    result.put(CLUSTER_NODE_HOST.getKey(), nonLoopbackLocal.getHostAddress());
    result.put(CLUSTER_HZ_HOSTS.getKey(), nonLoopbackLocal.getHostAddress());
    result.put(CLUSTER_SEARCH_HOSTS.getKey(), nonLoopbackLocal.getHostAddress());
    result.put("sonar.auth.jwtBase64Hs256Secret", "abcde");
    result.put(JDBC_URL.getKey(), "jdbc:postgresql://localhost:3306/sonar");
    result.putAll(settings);
    return new TestAppSettings(result);
  }

  private TestAppSettings newSettingsForSearchNode(ImmutableMap<String, String> settings) {
    Map<String, String> result = new HashMap<>();
    result.put(CLUSTER_ENABLED.getKey(), "true");
    result.put(CLUSTER_NODE_TYPE.getKey(), "search");
    result.put(CLUSTER_ES_HOSTS.getKey(), nonLoopbackLocal.getHostAddress());
    result.put(CLUSTER_NODE_SEARCH_HOST.getKey(), nonLoopbackLocal.getHostAddress());
    result.put(CLUSTER_NODE_ES_HOST.getKey(), nonLoopbackLocal.getHostAddress());
    result.putAll(settings);
    return new TestAppSettings(result);
  }
}
