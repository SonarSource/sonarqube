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
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.elasticsearch.core.TimeValue.timeValueSeconds;

public class EsConnectorImpl implements EsConnector {
  private static final String ES_USERNAME = "elastic";

  private static final Logger LOG = LoggerFactory.getLogger(EsConnectorImpl.class);

  private final AtomicReference<RestHighLevelClient> restClient = new AtomicReference<>(null);
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
  public Optional<ClusterHealthStatus> getClusterHealthStatus() {
    try {
      ClusterHealthResponse healthResponse = getRestHighLevelClient().cluster()
        .health(new ClusterHealthRequest().waitForYellowStatus().timeout(timeValueSeconds(30)), RequestOptions.DEFAULT);
      return Optional.of(healthResponse.getStatus());
    } catch (IOException e) {
      LOG.trace("Failed to check health status ", e);
      return Optional.empty();
    }
  }

  @Override
  public void stop() {
    RestHighLevelClient restHighLevelClient = restClient.get();
    if (restHighLevelClient != null) {
      try {
        restHighLevelClient.close();
      } catch (IOException e) {
        LOG.warn("Error occurred while closing Rest Client", e);
      }
    }
  }

  private RestHighLevelClient getRestHighLevelClient() {
    RestHighLevelClient res = this.restClient.get();
    if (res != null) {
      return res;
    }

    RestHighLevelClient restHighLevelClient = buildRestHighLevelClient();
    this.restClient.set(restHighLevelClient);
    return restHighLevelClient;
  }

  private RestHighLevelClient buildRestHighLevelClient() {
    HttpHost[] httpHosts = hostAndPorts.stream()
      .map(this::toHttpHost)
      .toArray(HttpHost[]::new);

    if (LOG.isDebugEnabled()) {
      String addresses = Arrays.stream(httpHosts)
        .map(t -> t.getHostName() + ":" + t.getPort())
        .collect(Collectors.joining(", "));
      LOG.debug("Connected to Elasticsearch node: [{}]", addresses);
    }

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
    return new RestHighLevelClient(builder);
  }

  private HttpHost toHttpHost(HostAndPort host) {
    try {
      String scheme = keyStorePath != null ? "https" : HttpHost.DEFAULT_SCHEME_NAME;
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
}
