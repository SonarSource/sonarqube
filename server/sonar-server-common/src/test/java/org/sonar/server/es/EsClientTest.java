/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Objects;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EsClientTest {
  private static final String EXAMPLE_CLUSTER_STATS_JSON = "{" +
    "  \"status\": \"yellow\"," +
    "  \"nodes\": {" +
    "    \"count\": {" +
    "      \"total\": 3" +
    "    }" +
    "  }" +
    "}";

  private static final String EXAMPLE_INDICES_STATS_JSON = "{" +
    "  \"indices\": {" +
    "    \"index-1\": {" +
    "      \"primaries\": {" +
    "        \"docs\": {" +
    "          \"count\": 1234" +
    "        }," +
    "        \"store\": {" +
    "          \"size_in_bytes\": 56789" +
    "        }" +
    "      }," +
    "      \"shards\": {" +
    "        \"shard-1\": {}," +
    "        \"shard-2\": {}" +
    "      }" +
    "    }," +
    "    \"index-2\": {" +
    "      \"primaries\": {" +
    "        \"docs\": {" +
    "          \"count\": 42" +
    "        }," +
    "        \"store\": {" +
    "          \"size_in_bytes\": 123" +
    "        }" +
    "      }," +
    "      \"shards\": {" +
    "        \"shard-1\": {}," +
    "        \"shard-2\": {}" +
    "      }" +
    "    }" +
    "  }" +
    "}";

  private static final String EXAMPLE_NODE_STATS_JSON = "{" +
    "  \"nodes\": {" +
    "    \"YnKPZcbGRamRQGxjErLWoQ\": {" +
    "      \"name\": \"sonarqube\"," +
    "      \"host\": \"127.0.0.1\"," +
    "      \"indices\": {" +
    "        \"docs\": {" +
    "          \"count\": 13557" +
    "        }," +
    "        \"store\": {" +
    "          \"size_in_bytes\": 8670970" +
    "        }," +
    "        \"query_cache\": {" +
    "          \"memory_size_in_bytes\": 0" +
    "        }," +
    "        \"fielddata\": {" +
    "          \"memory_size_in_bytes\": 4880" +
    "        }," +
    "        \"translog\": {" +
    "          \"size_in_bytes\": 8274137" +
    "        }," +
    "        \"request_cache\": {" +
    "          \"memory_size_in_bytes\": 0" +
    "        }" +
    "      }," +
    "      \"process\": {" +
    "        \"open_file_descriptors\": 296," +
    "        \"max_file_descriptors\": 10240," +
    "        \"cpu\": {" +
    "          \"percent\": 7" +
    "        }" +
    "      }," +
    "      \"jvm\": {" +
    "        \"mem\": {" +
    "          \"heap_used_in_bytes\": 158487160," +
    "          \"heap_used_percent\": 30," +
    "          \"heap_max_in_bytes\": 518979584," +
    "          \"non_heap_used_in_bytes\": 109066592" +
    "        }," +
    "        \"threads\": {" +
    "          \"count\": 70" +
    "        }" +
    "      }," +
    "      \"fs\": {" +
    "        \"total\": {" +
    "          \"total_in_bytes\": 250685575168," +
    "          \"free_in_bytes\": 142843138048," +
    "          \"available_in_bytes\": 136144027648" +
    "        }" +
    "      }," +
    "      \"breakers\": {" +
    "        \"request\": {" +
    "          \"limit_size_in_bytes\": 311387750," +
    "          \"estimated_size_in_bytes\": 0" +
    "        }," +
    "        \"fielddata\": {" +
    "          \"limit_size_in_bytes\": 207591833," +
    "          \"estimated_size_in_bytes\": 4880" +
    "        }" +
    "      }" +
    "    }" +
    "  }" +
    "}";

  @Rule
  public MockWebServer mockWebServer = new MockWebServer();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  RestClient restClient = mock(RestClient.class);
  RestHighLevelClient client = new EsClient.MinimalRestHighLevelClient(restClient);

  EsClient underTest = new EsClient(client);

  @Test
  public void should_close_client() throws IOException {
    underTest.close();
    verify(restClient).close();
  }

  @Test
  public void should_rethrow_ex_when_close_client_throws() throws IOException {
    doThrow(IOException.class).when(restClient).close();
    assertThatThrownBy(() -> underTest.close())
      .isInstanceOf(ElasticsearchException.class);
  }

  @Test
  public void should_call_node_stats_api() throws Exception {
    HttpEntity entity = mock(HttpEntity.class);
    when(entity.getContent()).thenReturn(new ByteArrayInputStream(EXAMPLE_NODE_STATS_JSON.getBytes()));
    Response response = mock(Response.class);
    when(response.getEntity()).thenReturn(entity);
    when(restClient.performRequest(argThat(new RawRequestMatcher(
      "GET",
      "/_nodes/stats/fs,process,jvm,indices,breaker"))))
      .thenReturn(response);

    assertThat(underTest.nodesStats()).isNotNull();
  }

  @Test
  public void should_rethrow_ex_on_node_stat_fail() throws Exception {
    when(restClient.performRequest(argThat(new RawRequestMatcher(
      "GET",
      "/_nodes/stats/fs,process,jvm,indices,breaker"))))
      .thenThrow(IOException.class);

    assertThatThrownBy(() -> underTest.nodesStats())
      .isInstanceOf(ElasticsearchException.class);
  }

  @Test
  public void should_call_indices_stat_api() throws Exception {
    HttpEntity entity = mock(HttpEntity.class);
    when(entity.getContent()).thenReturn(new ByteArrayInputStream(EXAMPLE_INDICES_STATS_JSON.getBytes()));
    Response response = mock(Response.class);
    when(response.getEntity()).thenReturn(entity);
    when(restClient.performRequest(argThat(new RawRequestMatcher(
      "GET",
      "/_stats"))))
      .thenReturn(response);

    assertThat(underTest.indicesStats()).isNotNull();
  }

  @Test
  public void should_rethrow_ex_on_indices_stat_fail() throws Exception {
    when(restClient.performRequest(argThat(new RawRequestMatcher(
      "GET",
      "/_stats"))))
      .thenThrow(IOException.class);

    assertThatThrownBy(() -> underTest.indicesStats())
      .isInstanceOf(ElasticsearchException.class);
  }

  @Test
  public void should_call_cluster_stat_api() throws Exception {
    HttpEntity entity = mock(HttpEntity.class);
    when(entity.getContent()).thenReturn(new ByteArrayInputStream(EXAMPLE_CLUSTER_STATS_JSON.getBytes()));

    Response response = mock(Response.class);
    when(response.getEntity()).thenReturn(entity);
    when(restClient.performRequest(argThat(new RawRequestMatcher(
      "GET",
      "/_cluster/stats"))))
      .thenReturn(response);

    assertThat(underTest.clusterStats()).isNotNull();
  }

  @Test
  public void should_rethrow_ex_on_cluster_stat_fail() throws Exception {
    when(restClient.performRequest(argThat(new RawRequestMatcher(
      "GET",
      "/_cluster/stats"))))
      .thenThrow(IOException.class);

    assertThatThrownBy(() -> underTest.clusterStats())
      .isInstanceOf(ElasticsearchException.class);
  }

  @Test
  public void should_add_authentication_header() throws InterruptedException {
    mockWebServer.enqueue(new MockResponse()
      .setResponseCode(200)
      .setBody(EXAMPLE_CLUSTER_STATS_JSON)
      .setHeader("Content-Type", "application/json"));

    String password = "test-elasticsearch-password";
    String expectedAuth = java.util.Base64.getEncoder().encodeToString(("elastic:" + password).getBytes());
    EsClient esClient = new EsClient(password, null, null, new HttpHost(mockWebServer.getHostName(), mockWebServer.getPort()));

    assertThat(esClient.clusterStats()).isNotNull();

    assertThat(mockWebServer.takeRequest().getHeader("Authorization")).isEqualTo("Basic " + expectedAuth);
  }

  @Test
  public void newInstance_whenKeyStorePassed_shouldCreateClient() throws GeneralSecurityException, IOException {
    mockWebServer.enqueue(new MockResponse()
      .setResponseCode(200)
      .setBody(EXAMPLE_CLUSTER_STATS_JSON)
      .setHeader("Content-Type", "application/json"));

    Path keyStorePath = temp.newFile("keystore.p12").toPath();
    String password = "test-keystore-password";

    HandshakeCertificates certificate = createCertificate(mockWebServer.getHostName(), keyStorePath, password);
    mockWebServer.useHttps(certificate.sslSocketFactory(), false);

    EsClient esClient = new EsClient(null, keyStorePath.toString(), password,
      new HttpHost(mockWebServer.getHostName(), mockWebServer.getPort(), "https"));

    assertThat(esClient.clusterStats()).isNotNull();
  }

  static HandshakeCertificates createCertificate(String hostName, Path keyStorePath, String password) throws GeneralSecurityException, IOException {
    HeldCertificate localhostCertificate = new HeldCertificate.Builder()
      .addSubjectAlternativeName(hostName)
      .build();

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      KeyStore ks = KeyStore.getInstance("PKCS12");
      ks.load(null);
      ks.setKeyEntry("alias", localhostCertificate.keyPair().getPrivate(), password.toCharArray(),
        new java.security.cert.Certificate[]{localhostCertificate.certificate()});
      ks.store(baos, password.toCharArray());

      try (OutputStream outputStream = Files.newOutputStream(keyStorePath)) {
        outputStream.write(baos.toByteArray());
      }
    }

    return new HandshakeCertificates.Builder()
      .heldCertificate(localhostCertificate)
      .build();
  }

  static class RawRequestMatcher implements ArgumentMatcher<Request> {
    String endpoint;
    String method;

    RawRequestMatcher(String method, String endpoint) {
      Objects.requireNonNull(endpoint);
      Objects.requireNonNull(method);
      this.endpoint = endpoint;
      this.method = method;
    }

    @Override
    public boolean matches(Request request) {
      return endpoint.equals(request.getEndpoint()) && method.equals(request.getMethod());
    }
  }

}
