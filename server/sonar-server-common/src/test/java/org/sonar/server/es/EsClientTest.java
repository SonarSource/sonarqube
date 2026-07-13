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
package org.sonar.server.es;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;
import org.apache.hc.core5.http.HttpHost;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EsClientTest {
  private static final String EXAMPLE_CLUSTER_STATS_JSON = """
    {
      "status": "yellow",
      "nodes": {
        "count": {
          "total": 3
        }
      }
    }""";

  private static final String EXAMPLE_INDICES_STATS_JSON = """
    {
      "indices": {
        "index-1": {
          "primaries": {
            "docs": {
              "count": 1234
            },
            "store": {
              "size_in_bytes": 56789
            }
          },
          "shards": {
            "shard-1": {},
            "shard-2": {}
          }
        },
        "index-2": {
          "primaries": {
            "docs": {
              "count": 42
            },
            "store": {
              "size_in_bytes": 123
            }
          },
          "shards": {
            "shard-1": {},
            "shard-2": {}
          }
        }
      }
    }""";

  private static final String EXAMPLE_NODE_STATS_JSON = """
    {
      "nodes": {
        "YnKPZcbGRamRQGxjErLWoQ": {
          "name": "sonarqube",
          "host": "127.0.0.1",
          "indices": {
            "docs": {
              "count": 13557
            },
            "store": {
              "size_in_bytes": 8670970
            },
            "query_cache": {
              "memory_size_in_bytes": 0
            },
            "fielddata": {
              "memory_size_in_bytes": 4880
            },
            "translog": {
              "size_in_bytes": 8274137
            },
            "request_cache": {
              "memory_size_in_bytes": 0
            }
          },
          "process": {
            "open_file_descriptors": 296,
            "max_file_descriptors": 10240,
            "cpu": {
              "percent": 7
            }
          },
          "jvm": {
            "mem": {
              "heap_used_in_bytes": 158487160,
              "heap_used_percent": 30,
              "heap_max_in_bytes": 518979584,
              "non_heap_used_in_bytes": 109066592
            },
            "threads": {
              "count": 70
            }
          },
          "fs": {
            "total": {
              "total_in_bytes": 250685575168,
              "free_in_bytes": 142843138048,
              "available_in_bytes": 136144027648
            }
          },
          "breakers": {
            "request": {
              "limit_size_in_bytes": 311387750,
              "estimated_size_in_bytes": 0
            },
            "fielddata": {
              "limit_size_in_bytes": 207591833,
              "estimated_size_in_bytes": 4880
            }
          }
        }
      }
    }""";

  @Rule
  public MockWebServer mockWebServer = new MockWebServer();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  SynchronousEsHttpClient httpClient = mock(SynchronousEsHttpClient.class);

  EsClient underTest = new EsClient(httpClient);

  @Test
  public void should_close_client() throws IOException {
    underTest.close();
    verify(httpClient).close();
  }

  @Test
  public void should_rethrow_ex_when_close_client_throws() throws IOException {
    doThrow(IOException.class).when(httpClient).close();
    assertThatThrownBy(() -> underTest.close())
      .isInstanceOf(ElasticsearchException.class);
  }

  @Test
  public void should_call_node_stats_api() throws Exception {
    when(httpClient.rawGet(eq("/_nodes/stats/fs,process,jvm,indices,breaker"), any())).thenReturn(EXAMPLE_NODE_STATS_JSON);

    assertThat(underTest.nodesStats()).isNotNull();
  }

  @Test
  public void should_rethrow_ex_on_node_stat_fail() throws Exception {
    when(httpClient.rawGet(eq("/_nodes/stats/fs,process,jvm,indices,breaker"), any())).thenThrow(IOException.class);

    assertThatThrownBy(() -> underTest.nodesStats())
      .isInstanceOf(ElasticsearchException.class);
  }

  @Test
  public void should_call_indices_stat_api() throws Exception {
    when(httpClient.rawGet(eq("/_stats"), any())).thenReturn(EXAMPLE_INDICES_STATS_JSON);

    assertThat(underTest.indicesStats()).isNotNull();
  }

  @Test
  public void should_rethrow_ex_on_indices_stat_fail() throws Exception {
    when(httpClient.rawGet(eq("/_stats"), any())).thenThrow(IOException.class);

    assertThatThrownBy(() -> underTest.indicesStats())
      .isInstanceOf(ElasticsearchException.class);
  }

  @Test
  public void should_call_cluster_stat_api() throws Exception {
    when(httpClient.rawGet(eq("/_cluster/stats"), any())).thenReturn(EXAMPLE_CLUSTER_STATS_JSON);

    assertThat(underTest.clusterStats()).isNotNull();
  }

  @Test
  public void should_rethrow_ex_on_cluster_stat_fail() throws Exception {
    when(httpClient.rawGet(eq("/_cluster/stats"), any())).thenThrow(IOException.class);

    assertThatThrownBy(() -> underTest.clusterStats())
      .isInstanceOf(ElasticsearchException.class);
  }

  @Test
  public void execute_wraps_exception_in_ElasticsearchException() {
    EsClient.EsRequestExecutor<Void> failingExecutor = () -> {
      throw new IOException("network failure");
    };
    assertThatThrownBy(() -> underTest.execute(failingExecutor))
      .isInstanceOf(ElasticsearchException.class)
      .hasMessageContaining("Fail to execute es request")
      .hasCauseInstanceOf(IOException.class);
  }

  @Test
  public void nativeClientV2_returns_non_null_client() {
    assertThat(underTest.nativeClientV2()).isNotNull();
  }

  @Test
  public void waitForStatusV2_wraps_underlying_failure_in_ElasticsearchException() throws IOException {
    // The high-level cluster().health() call routes through the synchronous Rest5Client.performRequest(),
    // so failing that low-level call exercises waitForStatusV2 -> clusterHealthV2 -> execute() error path.
    when(httpClient.performRequest(any(), any(), any(), any())).thenThrow(new IOException("boom"));

    assertThatThrownBy(() -> underTest.waitForStatusV2(co.elastic.clients.elasticsearch._types.HealthStatus.Yellow))
      .isInstanceOf(ElasticsearchException.class);
  }

  @Test
  public void newInstance_with_hosts_only_constructor_succeeds() {
    EsClient esClient = new EsClient(new HttpHost("http", "localhost", 9200));
    assertThat(esClient.nativeClientV2()).isNotNull();
    assertThat(esClient.nativeHttpClient()).isNotNull();
  }

  @Test
  public void newInstance_whenKeyStorePathInvalid_shouldThrowIllegalState() {
    HttpHost host = new HttpHost("http", "localhost", 9200);
    assertThatThrownBy(() -> new EsClient(null, "/path/does/not/exist.p12", "ignored", host))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Failed to setup SSL context");
  }

  @Test
  public void nativeHttpClient_returns_injected_http_client() {
    assertThat(underTest.nativeHttpClient()).isSameAs(httpClient);
  }

  @Test
  public void should_call_indices_stat_api_with_specific_indices() throws Exception {
    when(httpClient.rawGet(eq("/index-1,index-2/_stats"), any())).thenReturn(EXAMPLE_INDICES_STATS_JSON);

    assertThat(underTest.indicesStats("index-1", "index-2")).isNotNull();
  }

  @Test
  public void should_add_authentication_header() throws InterruptedException {
    mockWebServer.enqueue(new MockResponse()
      .setResponseCode(200)
      .setBody(EXAMPLE_CLUSTER_STATS_JSON)
      .setHeader("Content-Type", "application/json"));

    String password = "test-elasticsearch-password";
    String expectedAuth = java.util.Base64.getEncoder().encodeToString(("elastic:" + password).getBytes());
    EsClient esClient = new EsClient(password, null, null, new HttpHost("http", mockWebServer.getHostName(), mockWebServer.getPort()));

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
      new HttpHost("https", mockWebServer.getHostName(), mockWebServer.getPort()));

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

}
