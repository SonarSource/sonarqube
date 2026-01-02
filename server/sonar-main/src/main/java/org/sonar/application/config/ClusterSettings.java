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

import com.google.common.net.HostAndPort;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.process.MessageException;
import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessProperties.Property;
import org.sonar.process.Props;
import org.sonar.process.cluster.NodeType;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.sonar.process.ProcessProperties.Property.AUTH_JWT_SECRET;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ES_HOSTS;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_HZ_HOSTS;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_ES_HOST;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_HOST;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_SEARCH_HOST;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_TYPE;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_SEARCH_HOSTS;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_WEB_STARTUP_LEADER;
import static org.sonar.process.ProcessProperties.Property.JDBC_URL;
import static org.sonar.process.ProcessProperties.Property.SEARCH_HOST;
import static org.sonar.process.ProcessProperties.Property.SEARCH_PORT;

public class ClusterSettings implements Consumer<Props> {
  private static final Set<Property> FORBIDDEN_SEARCH_NODE_SETTINGS = EnumSet.of(SEARCH_HOST, SEARCH_PORT);

  private final NetworkUtils network;

  public ClusterSettings(NetworkUtils network) {
    this.network = network;
  }

  @Override
  public void accept(Props props) {
    if (isClusterEnabled(props)) {
      checkClusterProperties(props);
    }
  }

  private void checkClusterProperties(Props props) {
    // for internal use
    if (props.value(CLUSTER_WEB_STARTUP_LEADER.getKey()) != null) {
      throw new MessageException(format("Property [%s] is forbidden", CLUSTER_WEB_STARTUP_LEADER.getKey()));
    }

    NodeType nodeType = toNodeType(props);
    switch (nodeType) {
      case APPLICATION:
        checkForApplicationNode(props);
        break;
      case SEARCH:
        checkForSearchNode(props);
        break;
      default:
        throw new UnsupportedOperationException("Unknown value: " + nodeType);
    }
  }

  private void checkForApplicationNode(Props props) {
    ensureNotH2(props);
    requireValue(props, AUTH_JWT_SECRET);
    Set<AddressAndPort> hzNodes = parseAndCheckHosts(CLUSTER_HZ_HOSTS, requireValue(props, CLUSTER_HZ_HOSTS));
    ensureNotLoopbackAddresses(CLUSTER_HZ_HOSTS, hzNodes);
    checkClusterNodeHost(props);
    checkClusterSearchHosts(props);
  }

  private void checkForSearchNode(Props props) {
    ensureNoSearchNodeForbiddenSettings(props);
    AddressAndPort searchHost = parseAndCheckHost(CLUSTER_NODE_SEARCH_HOST, requireValue(props, CLUSTER_NODE_SEARCH_HOST));
    ensureLocalButNotLoopbackAddress(CLUSTER_NODE_SEARCH_HOST, searchHost);
    AddressAndPort esHost = parseAndCheckHost(CLUSTER_NODE_ES_HOST, requireValue(props, CLUSTER_NODE_ES_HOST));
    ensureLocalButNotLoopbackAddress(CLUSTER_NODE_ES_HOST, esHost);
    checkClusterEsHosts(props);
  }

  private void checkClusterNodeHost(Props props) {
    AddressAndPort clusterNodeHost = parseAndCheckHost(CLUSTER_NODE_HOST, requireValue(props, CLUSTER_NODE_HOST));
    ensureLocalButNotLoopbackAddress(CLUSTER_NODE_HOST, clusterNodeHost);
  }

  private void checkClusterSearchHosts(Props props) {
    Set<AddressAndPort> searchHosts = parseAndCheckHosts(CLUSTER_SEARCH_HOSTS, requireValue(props, CLUSTER_SEARCH_HOSTS));
    ensureNotLoopbackAddresses(CLUSTER_SEARCH_HOSTS, searchHosts);
  }

  private void checkClusterEsHosts(Props props) {
    Set<AddressAndPort> esHosts = parseHosts(requireValue(props, CLUSTER_ES_HOSTS));
    ensureNotLoopbackAddresses(CLUSTER_ES_HOSTS, esHosts);
    ensureEitherPortsAreProvidedOrOnlyHosts(CLUSTER_ES_HOSTS, esHosts);
  }

  private static Set<AddressAndPort> parseHosts(String value) {
    return stream(value.split(","))
      .filter(Objects::nonNull)
      .map(String::trim)
      .map(ClusterSettings::parseHost)
      .collect(toSet());
  }

  private Set<AddressAndPort> parseAndCheckHosts(Property property, String value) {
    Set<AddressAndPort> res = parseHosts(value);
    checkValidHosts(property, res);
    return res;
  }

  private void checkValidHosts(Property property, Set<AddressAndPort> addressAndPorts) {
    List<String> invalidHosts = addressAndPorts.stream()
      .map(AddressAndPort::getHost)
      .filter(t -> !network.toInetAddress(t).isPresent())
      .sorted()
      .toList();
    if (!invalidHosts.isEmpty()) {
      throw new MessageException(format("Address in property %s is not a valid address: %s",
        property.getKey(), String.join(", ", invalidHosts)));
    }
  }

  private static AddressAndPort parseHost(String value) {
    HostAndPort hostAndPort = HostAndPort.fromString(value);
    return new AddressAndPort(hostAndPort.getHost(), hostAndPort.hasPort() ? hostAndPort.getPort() : null);
  }

  private AddressAndPort parseAndCheckHost(Property property, String value) {
    AddressAndPort addressAndPort = parseHost(value);
    checkValidHosts(property, singleton(addressAndPort));
    return addressAndPort;
  }

  public static NodeType toNodeType(Props props) {
    String nodeTypeValue = requireValue(props, CLUSTER_NODE_TYPE);
    if (!NodeType.isValid(nodeTypeValue)) {
      throw new MessageException(format("Invalid value for property %s: [%s], only [%s] are allowed", CLUSTER_NODE_TYPE.getKey(), nodeTypeValue,
        Arrays.stream(NodeType.values()).map(NodeType::getValue).collect(joining(", "))));
    }
    return NodeType.parse(nodeTypeValue);
  }

  private static String requireValue(Props props, Property property) {
    String key = property.getKey();
    String value = props.value(key);
    String trimmedValue = value == null ? null : value.trim();
    if (trimmedValue == null || trimmedValue.isEmpty()) {
      throw new MessageException(format("Property %s is mandatory", key));
    }
    return trimmedValue;
  }

  private static void ensureNoSearchNodeForbiddenSettings(Props props) {
    List<String> violations = FORBIDDEN_SEARCH_NODE_SETTINGS.stream()
      .filter(setting -> props.value(setting.getKey()) != null)
      .map(Property::getKey)
      .toList();

    if (!violations.isEmpty()) {
      throw new MessageException(format("Properties [%s] are not allowed when running SonarQube in cluster mode.", String.join(", ", violations)));
    }
  }

  private static void ensureNotH2(Props props) {
    String jdbcUrl = props.value(JDBC_URL.getKey());
    String trimmedJdbcUrl = jdbcUrl == null ? null : jdbcUrl.trim();
    if (trimmedJdbcUrl == null || trimmedJdbcUrl.isEmpty() || trimmedJdbcUrl.startsWith("jdbc:h2:")) {
      throw new MessageException("Embedded database is not supported in cluster mode");
    }
  }

  private void ensureNotLoopbackAddresses(Property property, Set<AddressAndPort> hostAndPorts) {
    Set<AddressAndPort> loopbackAddresses = hostAndPorts.stream()
      .filter(t -> network.isLoopback(t.getHost()))
      .collect(toSet());
    if (!loopbackAddresses.isEmpty()) {
      throw new MessageException(format("Property %s must not contain a loopback address: %s", property.getKey(),
        loopbackAddresses.stream().map(AddressAndPort::getHost).sorted().collect(Collectors.joining(", "))));
    }
  }

  private void ensureLocalButNotLoopbackAddress(Property property, AddressAndPort addressAndPort) {
    String host = addressAndPort.getHost();
    if (!network.isLocal(host) || network.isLoopback(host)) {
      throw new MessageException(format("Property %s must be a local non-loopback address: %s", property.getKey(), addressAndPort.getHost()));
    }
  }

  private static void ensureEitherPortsAreProvidedOrOnlyHosts(Property property, Set<AddressAndPort> addressAndPorts) {
    Set<AddressAndPort> hostsWithoutPort = addressAndPorts.stream()
      .filter(t -> !t.hasPort())
      .collect(toSet());
    if (!hostsWithoutPort.isEmpty() && hostsWithoutPort.size() != addressAndPorts.size()) {
      throw new MessageException(format("Entries in property %s must not mix 'host:port' and 'host'. Provide hosts without port only or hosts with port only.", property.getKey()));
    }
  }

  private static class AddressAndPort {
    private static final int NO_PORT = -1;

    /**
     * the host from setting, can be a hostname or an IP address
     */
    private final String host;
    private final int port;

    private AddressAndPort(String host, @Nullable Integer port) {
      this.host = host;
      this.port = port == null ? NO_PORT : port;
    }

    public String getHost() {
      return host;
    }

    public boolean hasPort() {
      return port != NO_PORT;
    }
  }

  public static boolean isClusterEnabled(AppSettings settings) {
    return isClusterEnabled(settings.getProps());
  }

  private static boolean isClusterEnabled(Props props) {
    return props.valueAsBoolean(CLUSTER_ENABLED.getKey());
  }

  /**
   * Hazelcast must be started when cluster is activated on all nodes but search ones
   */
  public static boolean shouldStartHazelcast(AppSettings appSettings) {
    return isClusterEnabled(appSettings.getProps()) && toNodeType(appSettings.getProps()) == NodeType.APPLICATION;
  }

  public static List<ProcessId> getEnabledProcesses(AppSettings settings) {
    if (!isClusterEnabled(settings)) {
      return asList(ProcessId.ELASTICSEARCH, ProcessId.WEB_SERVER, ProcessId.COMPUTE_ENGINE);
    }
    NodeType nodeType = NodeType.parse(settings.getValue(CLUSTER_NODE_TYPE.getKey()).orElse(""));
    switch (nodeType) {
      case APPLICATION:
        return asList(ProcessId.WEB_SERVER, ProcessId.COMPUTE_ENGINE);
      case SEARCH:
        return singletonList(ProcessId.ELASTICSEARCH);
      default:
        throw new IllegalArgumentException("Unexpected node type " + nodeType);
    }
  }

  public static boolean isLocalElasticsearchEnabled(AppSettings settings) {
    // elasticsearch is enabled on "search" nodes, but disabled on "application" nodes
    if (isClusterEnabled(settings.getProps())) {
      return NodeType.parse(settings.getValue(CLUSTER_NODE_TYPE.getKey()).orElse("")) == NodeType.SEARCH;
    }

    // elasticsearch is enabled in standalone mode
    return true;
  }
}
