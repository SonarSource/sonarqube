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
package org.sonar.server.es;

import com.google.common.net.HostAndPort;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.common.settings.Settings;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.cluster.NodeType;
import org.springframework.context.annotation.Bean;

import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ES_HTTP_KEYSTORE;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ES_HTTP_KEYSTORE_PASSWORD;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NAME;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_TYPE;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_SEARCH_HOSTS;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_SEARCH_PASSWORD;
import static org.sonar.process.ProcessProperties.Property.SEARCH_HOST;
import static org.sonar.process.ProcessProperties.Property.SEARCH_PORT;
import static org.sonar.process.cluster.NodeType.SEARCH;

@ComputeEngineSide
@ServerSide
public class EsClientProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(EsClientProvider.class);

  @Bean("EsClient")
  public EsClient provide(Configuration config) {
    Settings.Builder esSettings = Settings.builder();

    // mandatory property defined by bootstrap process
    esSettings.put("cluster.name", config.get(CLUSTER_NAME.getKey()).get());

    //boolean clusterEnabled = config.getBoolean(CLUSTER_ENABLED.getKey()).orElse(false);
    //boolean searchNode = !clusterEnabled || SEARCH.equals(NodeType.parse(config.get(CLUSTER_NODE_TYPE.getKey()).orElse(null)));
    List<HttpHost> httpHosts;
    if (StringUtils.isNotBlank(config.get(CLUSTER_SEARCH_HOSTS.getKey()).orElse(null))){
      httpHosts = getHttpHosts(config);

      LOGGER.info("Connected to remote Elasticsearch: [{}]", displayedAddresses(httpHosts));
    } else {
      // defaults provided in:
      // * in org.sonar.process.ProcessProperties.Property.SEARCH_HOST
      // * in org.sonar.process.ProcessProperties.Property.SEARCH_PORT
      HostAndPort host = HostAndPort.fromParts(config.get(SEARCH_HOST.getKey()).get(), config.getInt(SEARCH_PORT.getKey()).get());
      httpHosts = Collections.singletonList(toHttpHost(host, config));
      LOGGER.info("Connected to local Elasticsearch: [{}]", displayedAddresses(httpHosts));
    }

    return new EsClient(config.get(CLUSTER_SEARCH_PASSWORD.getKey()).orElse(null),
      config.get(CLUSTER_ES_HTTP_KEYSTORE.getKey()).orElse(null),
      config.get(CLUSTER_ES_HTTP_KEYSTORE_PASSWORD.getKey()).orElse(null),
      httpHosts.toArray(new HttpHost[0]));
  }

  private static List<HttpHost> getHttpHosts(Configuration config) {
    return Arrays.stream(config.getStringArray(CLUSTER_SEARCH_HOSTS.getKey()))
      .map(HostAndPort::fromString)
      .map(host -> toHttpHost(host, config))
      .toList();
  }

  private static HttpHost toHttpHost(HostAndPort host, Configuration config) {
    try {
      String scheme = config.get(CLUSTER_ES_HTTP_KEYSTORE.getKey()).isPresent() ? "https" : HttpHost.DEFAULT_SCHEME_NAME;
      return new HttpHost(InetAddress.getByName(host.getHost()), host.getPortOrDefault(9001), scheme);
    } catch (UnknownHostException e) {
      throw new IllegalStateException("Can not resolve host [" + host + "]", e);
    }
  }

  private static String displayedAddresses(List<HttpHost> httpHosts) {
    return httpHosts.stream().map(HttpHost::toString).collect(Collectors.joining(", "));
  }
}
