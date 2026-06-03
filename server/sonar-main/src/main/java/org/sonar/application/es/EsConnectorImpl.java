/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.google.common.net.HostAndPort;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EsConnectorImpl implements EsConnector {
  private static final String ES_USERNAME = "elastic";
  private static final Time HEALTH_TIMEOUT = Time.of(t -> t.time("30s"));

  private static final Logger LOG = LoggerFactory.getLogger(EsConnectorImpl.class);

  private final AtomicReference<EsClient> esClient = new AtomicReference<>(null);
  private final Set<HostAndPort> hostAndPorts;
  private final String searchPassword;
  private final Path keyStorePath;
  private final String keyStorePassword;

  public EsConnectorImpl(Set<HostAndPort> hostAndPorts, @Nullable String searchPassword, @Nullable Path keyStorePath,
    @Nullable String keyStorePassword) {
    this.hostAndPorts = hostAndPorts;
    this.searchPassword = searchPassword;
    this.keyStorePath = keyStorePath;
    this.keyStorePassword = keyStorePassword;
  }

  @Override
  public Optional<HealthStatus> getClusterHealthStatus() {
    try {
      HealthResponse healthResponse = getEsClient().client.cluster()
        .health(h -> h.waitForStatus(HealthStatus.Yellow).timeout(HEALTH_TIMEOUT));
      return Optional.of(healthResponse.status());
    } catch (IOException e) {
      LOG.trace("Failed to check health status ", e);
      return Optional.empty();
    }
  }

  @Override
  public void stop() {
    EsClient client = esClient.get();
    if (client != null) {
      try {
        client.restClient.close();
      } catch (IOException e) {
        LOG.warn("Error occurred while closing Rest Client", e);
      }
    }
  }

  private EsClient getEsClient() {
    EsClient res = this.esClient.get();
    if (res != null) {
      return res;
    }

    EsClient created = buildEsClient();
    this.esClient.set(created);
    return created;
  }

  private EsClient buildEsClient() {
    HttpHost[] httpHosts = hostAndPorts.stream()
      .map(this::toHttpHost)
      .toArray(HttpHost[]::new);

    LOG.atDebug()
      .addArgument(Arrays.stream(httpHosts)
        .map(t -> t.getHostName() + ":" + t.getPort())
        .collect(Collectors.joining(", ")))
      .log("Connected to Elasticsearch node: [{}]");

    RestClientBuilder builder = RestClient.builder(httpHosts)
      .setHttpClientConfigCallback(httpClientBuilder -> {
        if (searchPassword != null) {
          BasicCredentialsProvider provider = getBasicCredentialsProvider(searchPassword);
          httpClientBuilder.setDefaultCredentialsProvider(provider);
        }

        if (keyStorePath != null) {
          SSLContext sslContext = getSSLContext(keyStorePath, keyStorePassword);
          httpClientBuilder.setSSLContext(sslContext);
        }

        return httpClientBuilder;
      });
    RestClient restClient = builder.build();
    RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
    return new EsClient(restClient, new ElasticsearchClient(transport));
  }

  private HttpHost toHttpHost(HostAndPort host) {
    try {
      String scheme = keyStorePath != null ? "https" : HttpHost.DEFAULT_SCHEME_NAME;
      if ("true".equalsIgnoreCase(System.getProperty("java.net.preferIPv6Addresses"))) {
        // host.getHost() returns IP address. This is required for HttpHost to work as we need to use IP address in the RestClient to
        // correctly resolve the host. Otherwise, RestClient will try to find the ip using hostname, and it might fail in case of IPv6.
        return new HttpHost(InetAddress.getByName(host.getHost()), host.getHost(), host.getPortOrDefault(9001), scheme);
      }
      return new HttpHost(InetAddress.getByName(host.getHost()), host.getPortOrDefault(9001), scheme);
    } catch (UnknownHostException e) {
      throw new IllegalStateException("Can not resolve host [" + host + "]", e);
    }
  }

  private static BasicCredentialsProvider getBasicCredentialsProvider(String searchPassword) {
    BasicCredentialsProvider provider = new BasicCredentialsProvider();
    provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(ES_USERNAME, searchPassword));
    return provider;
  }

  private static SSLContext getSSLContext(Path keyStorePath, @Nullable String keyStorePassword) {
    try {
      KeyStore keyStore = KeyStore.getInstance("pkcs12");
      try (InputStream is = Files.newInputStream(keyStorePath)) {
        keyStore.load(is, keyStorePassword == null ? null : keyStorePassword.toCharArray());
      }
      SSLContextBuilder sslBuilder = SSLContexts.custom().loadTrustMaterial(keyStore, null);
      return sslBuilder.build();
    } catch (IOException | GeneralSecurityException e) {
      throw new IllegalStateException("Failed to setup SSL context on ES client", e);
    }
  }

  /**
   * Holds the ES8 client together with the underlying RestClient so we can close the latter on stop().
   */
  private record EsClient(RestClient restClient, ElasticsearchClient client) {
  }
}
