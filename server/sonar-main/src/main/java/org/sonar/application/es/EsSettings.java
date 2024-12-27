/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.application.es;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.MessageException;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;
import org.sonar.process.System2;

import static java.lang.String.valueOf;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ES_DISCOVERY_SEED_HOSTS;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ES_HOSTS;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ES_HTTP_KEYSTORE;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ES_KEYSTORE;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ES_TRUSTSTORE;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NAME;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_ES_HOST;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_ES_PORT;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_NAME;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_SEARCH_HOST;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_SEARCH_PORT;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_SEARCH_PASSWORD;
import static org.sonar.process.ProcessProperties.Property.ES_PORT;
import static org.sonar.process.ProcessProperties.Property.SEARCH_HOST;
import static org.sonar.process.ProcessProperties.Property.SEARCH_INITIAL_STATE_TIMEOUT;
import static org.sonar.process.ProcessProperties.Property.SEARCH_PORT;

public class EsSettings {
  private static final String ES_HTTP_HOST_KEY = "http.host";
  private static final String ES_HTTP_PORT_KEY = "http.port";
  private static final String ES_TRANSPORT_HOST_KEY = "transport.host";
  private static final String ES_TRANSPORT_PORT_KEY = "transport.port";
  private static final String ES_NETWORK_HOST_KEY = "network.host";

  private static final Logger LOGGER = LoggerFactory.getLogger(EsSettings.class);
  private static final String STANDALONE_NODE_NAME = "sonarqube";
  private static final String SECCOMP_PROPERTY = "bootstrap.system_call_filter";
  private static final String ALLOW_MMAP = "node.store.allow_mmap";

  private static final String JAVA_ADDITIONAL_OPS_PROPERTY = "sonar.search.javaAdditionalOpts";

  private final Props props;
  private final EsInstallation fileSystem;

  private final boolean clusterEnabled;
  private final String clusterName;
  private final String nodeName;
  private final InetAddress loopbackAddress;

  public EsSettings(Props props, EsInstallation fileSystem, System2 system2) {
    this.props = props;
    this.fileSystem = fileSystem;

    this.clusterName = props.nonNullValue(CLUSTER_NAME.getKey());
    this.clusterEnabled = props.valueAsBoolean(CLUSTER_ENABLED.getKey());
    if (this.clusterEnabled) {
      this.nodeName = props.value(CLUSTER_NODE_NAME.getKey(), "sonarqube-" + UUID.randomUUID());
    } else {
      this.nodeName = STANDALONE_NODE_NAME;
    }
    this.loopbackAddress = InetAddress.getLoopbackAddress();
    String esJvmOptions = system2.getenv("ES_JVM_OPTIONS");
    if (esJvmOptions != null && !esJvmOptions.trim().isEmpty()) {
      LOGGER.warn("ES_JVM_OPTIONS is defined but will be ignored. " +
        "Use sonar.search.javaOpts and/or sonar.search.javaAdditionalOpts in sonar.properties to specify jvm options for Elasticsearch");
    }
  }

  public Map<String, String> build() {
    Map<String, String> builder = new HashMap<>();
    configureFileSystem(builder);
    configureNetwork(builder);
    configureCluster(builder);
    configureSecurity(builder);
    configureOthers(builder);
    LOGGER.atInfo()
      .addArgument(() -> builder.get(ES_HTTP_HOST_KEY))
      .addArgument(() -> builder.get(ES_HTTP_PORT_KEY))
      .addArgument(() -> builder.get(ES_TRANSPORT_HOST_KEY))
      .log("Elasticsearch listening on [HTTP: {}:{}, TCP: {}:{}]");
    return builder;
  }

  private void configureFileSystem(Map<String, String> builder) {
    builder.put("path.data", fileSystem.getDataDirectory().getAbsolutePath());
    builder.put("path.logs", fileSystem.getLogDirectory().getAbsolutePath());
  }

  private void configureSecurity(Map<String, String> builder) {
    if (clusterEnabled && props.value((CLUSTER_SEARCH_PASSWORD.getKey())) != null) {

      String clusterESKeystoreFileName = getFileNameFromPathProperty(CLUSTER_ES_KEYSTORE);
      String clusterESTruststoreFileName = getFileNameFromPathProperty(CLUSTER_ES_TRUSTSTORE);

      builder.put("xpack.security.enabled", "true");
      builder.put("xpack.security.transport.ssl.enabled", "true");
      builder.put("xpack.security.transport.ssl.supported_protocols", "TLSv1.3,TLSv1.2");
      builder.put("xpack.security.transport.ssl.verification_mode", "certificate");
      builder.put("xpack.security.transport.ssl.keystore.path", clusterESKeystoreFileName);
      builder.put("xpack.security.transport.ssl.truststore.path", clusterESTruststoreFileName);

      if (props.value(CLUSTER_ES_HTTP_KEYSTORE.getKey()) != null) {
        String clusterESHttpKeystoreFileName = getFileNameFromPathProperty(CLUSTER_ES_HTTP_KEYSTORE);

        builder.put("xpack.security.http.ssl.enabled", Boolean.TRUE.toString());
        builder.put("xpack.security.http.ssl.keystore.path", clusterESHttpKeystoreFileName);
      }
    } else {
      builder.put("xpack.security.autoconfiguration.enabled", Boolean.FALSE.toString());
      builder.put("xpack.security.enabled", Boolean.FALSE.toString());
    }
  }

  private String getFileNameFromPathProperty(ProcessProperties.Property processProperty) {
    String processPropertyPath = props.value(processProperty.getKey());

    if (processPropertyPath == null) {
      throw new MessageException(processProperty.name() + " property need to be set " +
        "when using elastic search authentication");
    }
    Path path = Paths.get(processPropertyPath);
    if (!path.toFile().exists()) {
      throw new MessageException("Unable to configure: " + processProperty.getKey() + ". "
        + "File specified in [" + processPropertyPath + "] does not exist");
    }
    if (!path.toFile().canRead()) {
      throw new MessageException("Unable to configure: " + processProperty.getKey() + ". "
        + "Could not get read access to [" + processPropertyPath + "]");
    }
    return path.getFileName().toString();
  }

  private void configureNetwork(Map<String, String> builder) {
    if (!clusterEnabled) {
      InetAddress searchHost = resolveAddress(SEARCH_HOST);
      int searchPort = Integer.parseInt(props.nonNullValue(SEARCH_PORT.getKey()));
      builder.put(ES_HTTP_HOST_KEY, searchHost.getHostAddress());
      builder.put(ES_HTTP_PORT_KEY, valueOf(searchPort));
      builder.put("discovery.type", "single-node");

      int transportPort = Integer.parseInt(props.nonNullValue(ES_PORT.getKey()));

      // we have no use of transport port in non-DCE editions
      // but specified host must be the one listed in: discovery.seed_hosts
      // otherwise elasticsearch cannot elect master node
      // by default it will be localhost, see: org.sonar.process.ProcessProperties.completeDefaults
      builder.put(ES_TRANSPORT_HOST_KEY, searchHost.getHostAddress());
      builder.put(ES_TRANSPORT_PORT_KEY, valueOf(transportPort));
    }

    // see https://github.com/lmenezes/elasticsearch-kopf/issues/195
    builder.put("http.cors.enabled", valueOf(true));
    builder.put("http.cors.allow-origin", "*");

    // Elasticsearch sets the default value of TCP reuse address to true only on non-MSWindows machines, but why ?
    builder.put("network.tcp.reuse_address", valueOf(true));
  }

  private InetAddress resolveAddress(ProcessProperties.Property prop) {
    return resolveAddress(prop, null);
  }

  private InetAddress resolveAddress(ProcessProperties.Property prop, @Nullable InetAddress defaultAddress) {
    String address;
    if (defaultAddress == null) {
      address = props.nonNullValue(prop.getKey());
    } else {
      address = props.value(prop.getKey());
      if (address == null) {
        return defaultAddress;
      }
    }

    try {
      return InetAddress.getByName(address);
    } catch (UnknownHostException e) {
      throw new IllegalStateException("Can not resolve host [" + address + "]. Please check network settings and property " + prop.getKey(), e);
    }
  }

  private void configureCluster(Map<String, String> builder) {
    // Default value in a standalone mode, not overridable

    String initialStateTimeOut = "30s";

    if (clusterEnabled) {
      initialStateTimeOut = props.value(SEARCH_INITIAL_STATE_TIMEOUT.getKey(), "120s");

      String nodeSearchHost = resolveAddress(CLUSTER_NODE_SEARCH_HOST, loopbackAddress).getHostAddress();
      int nodeSearchPort = props.valueAsInt(CLUSTER_NODE_SEARCH_PORT.getKey(), 9001);
      builder.put(ES_HTTP_HOST_KEY, nodeSearchHost);
      builder.put(ES_HTTP_PORT_KEY, valueOf(nodeSearchPort));

      String nodeTransportHost = resolveAddress(CLUSTER_NODE_ES_HOST, loopbackAddress).getHostAddress();
      int nodeTransportPort = props.valueAsInt(CLUSTER_NODE_ES_PORT.getKey(), 9002);
      builder.put(ES_TRANSPORT_HOST_KEY, nodeTransportHost);
      builder.put(ES_TRANSPORT_PORT_KEY, valueOf(nodeTransportPort));
      builder.put(ES_NETWORK_HOST_KEY, nodeTransportHost);

      String hosts = props.value(CLUSTER_ES_HOSTS.getKey(), loopbackAddress.getHostAddress());
      String discoveryHosts = props.value(CLUSTER_ES_DISCOVERY_SEED_HOSTS.getKey(), hosts);
      LOGGER.info("Elasticsearch cluster enabled. Connect to hosts [{}]", hosts);
      builder.put("discovery.seed_hosts", discoveryHosts);
      builder.put("cluster.initial_master_nodes", hosts);
    }

    builder.put("discovery.initial_state_timeout", initialStateTimeOut);
    builder.put("cluster.name", clusterName);
    builder.put("cluster.routing.allocation.awareness.attributes", "rack_id");
    builder.put("node.attr.rack_id", nodeName);
    builder.put("node.name", nodeName);
  }

  private void configureOthers(Map<String, String> builder) {
    builder.put("action.auto_create_index", String.valueOf(false));
    if (props.value(JAVA_ADDITIONAL_OPS_PROPERTY, "").contains("-D" + SECCOMP_PROPERTY + "=" + Boolean.FALSE)) {
      builder.put(SECCOMP_PROPERTY, Boolean.FALSE.toString());
    }

    if (props.value(JAVA_ADDITIONAL_OPS_PROPERTY, "").contains("-Dnode.store.allow_mmapfs=" + Boolean.FALSE)) {
      throw new MessageException("Property 'node.store.allow_mmapfs' is no longer supported. Use 'node.store.allow_mmap' instead.");
    }

    if (props.value(JAVA_ADDITIONAL_OPS_PROPERTY, "").contains("-D" + ALLOW_MMAP + "=" + Boolean.FALSE)) {
      builder.put(ALLOW_MMAP, Boolean.FALSE.toString());
    }
  }
}
