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

import co.elastic.clients.transport.http.HeaderMap;
import co.elastic.clients.transport.http.TransportHttpClient;
import co.elastic.clients.util.BinaryData;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.hc.core5.http.HttpHost;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SynchronousEsHttpClientTest {

  @Rule
  public MockWebServer mockWebServer = new MockWebServer();

  private SynchronousEsHttpClient newClient() {
    HttpHost host = new HttpHost("http", mockWebServer.getHostName(), mockWebServer.getPort());
    return new SynchronousEsHttpClient(EsClient.buildClassicHttpClient(null, null, null), List.of(host));
  }

  private static TransportHttpClient.Request get(String path, Map<String, String> queryParams) {
    return new TransportHttpClient.Request("GET", path, queryParams, Map.of(), null);
  }

  @Test
  public void constructor_rejects_empty_hosts() {
    var client = EsClient.buildClassicHttpClient(null, null, null);
    assertThatThrownBy(() -> new SynchronousEsHttpClient(client, List.of()))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void performRequest_preserves_already_encoded_path() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, "{}"));
    SynchronousEsHttpClient client = newClient();
    try {
      // The ES client passes an already percent-encoded path; it must reach the server unchanged (not "a%2520b").
      client.performRequest("id", null, get("/index/_doc/a%20b", Map.of()), null);
    } finally {
      client.close();
    }

    RecordedRequest recorded = mockWebServer.takeRequest();
    assertThat(recorded.getPath()).isEqualTo("/index/_doc/a%20b");
  }

  @Test
  public void performRequest_appends_and_encodes_query_parameters() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, "{}"));
    SynchronousEsHttpClient client = newClient();
    try {
      client.performRequest("id", null, get("/_stats", Map.of("level", "shards")), null);
    } finally {
      client.close();
    }

    RecordedRequest recorded = mockWebServer.takeRequest();
    assertThat(recorded.getPath()).isEqualTo("/_stats?level=shards");
  }

  @Test
  public void performRequest_sends_body_and_exposes_response() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(201)
      .setHeader("Content-Type", "application/json")
      .setHeader("X-Single", "v1")
      .addHeader("X-Multi", "a")
      .addHeader("X-Multi", "b")
      .setBody("{\"ok\":true}"));

    SynchronousEsHttpClient client = newClient();
    try {
      Iterable<ByteBuffer> body = List.of(ByteBuffer.wrap("{\"doc\":1}".getBytes(StandardCharsets.UTF_8)));
      var request = new TransportHttpClient.Request("POST", "/index/_doc", Map.of(),
        Map.of(HeaderMap.CONTENT_TYPE, "application/json"), body);

      TransportHttpClient.Response response = client.performRequest("id", null, request, null);

      assertThat(response.statusCode()).isEqualTo(201);
      assertThat(response.header("X-Single")).isEqualTo("v1");
      assertThat(response.headers("X-Multi")).containsExactly("a", "b");
      assertThat(response.headers("X-Absent")).isEmpty();
      assertThat(response.node().uri().getPort()).isEqualTo(mockWebServer.getPort());
      assertThat(response.toString()).contains("HTTP 201");
      assertThat(response.originalResponse()).isSameAs(response);

      BinaryData data = response.body();
      assertThat(data.contentType()).contains("application/json");
      assertThat(data.isRepeatable()).isTrue();
      assertThat(data.size()).isPositive();
      assertThat(new String(data.asInputStream().readAllBytes(), StandardCharsets.UTF_8)).contains("\"ok\":true");
      assertThat(new String(data.asByteBuffer().array(), StandardCharsets.UTF_8)).contains("\"ok\":true");
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      data.writeTo(out);
      assertThat(out.toString(StandardCharsets.UTF_8)).contains("\"ok\":true");

      response.close();
    } finally {
      client.close();
    }

    RecordedRequest recorded = mockWebServer.takeRequest();
    assertThat(recorded.getMethod()).isEqualTo("POST");
    assertThat(recorded.getBody().readUtf8()).isEqualTo("{\"doc\":1}");
    assertThat(recorded.getHeader("Content-Type")).contains("application/json");
  }

  @Test
  public void performRequestAsync_completes_with_response() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, "{}"));
    SynchronousEsHttpClient client = newClient();
    try {
      CompletableFuture<TransportHttpClient.Response> future = client.performRequestAsync("id", null, get("/", Map.of()), null);
      assertThat(future).isCompleted();
      assertThat(future.get().statusCode()).isEqualTo(200);
    } finally {
      client.close();
    }
  }

  @Test
  public void performRequestAsync_completes_exceptionally_on_connection_failure() throws Exception {
    HttpHost dead = new HttpHost("http", "localhost", findClosedPort());
    SynchronousEsHttpClient client = new SynchronousEsHttpClient(EsClient.buildClassicHttpClient(null, null, null), List.of(dead));
    try {
      CompletableFuture<TransportHttpClient.Response> future = client.performRequestAsync("id", null, get("/", Map.of()), null);
      assertThat(future).isCompletedExceptionally();
    } finally {
      client.close();
    }
  }

  @Test
  public void performRequest_fails_over_to_next_host_on_connection_error() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, "{}"));
    // First host points to a closed port, second is the running mock server: the request must succeed on the second.
    HttpHost dead = new HttpHost("http", "localhost", findClosedPort());
    HttpHost alive = new HttpHost("http", mockWebServer.getHostName(), mockWebServer.getPort());
    SynchronousEsHttpClient client = new SynchronousEsHttpClient(EsClient.buildClassicHttpClient(null, null, null), List.of(dead, alive));
    try {
      TransportHttpClient.Response response = client.performRequest("id", null, get("/_cluster/health", Map.of()), null);
      assertThat(response.statusCode()).isEqualTo(200);
    } finally {
      client.close();
    }
  }

  @Test
  public void hosts_returns_configured_hosts_in_order() {
    HttpHost first = new HttpHost("http", "localhost", 9001);
    HttpHost second = new HttpHost("http", "localhost", 9002);
    SynchronousEsHttpClient client = new SynchronousEsHttpClient(EsClient.buildClassicHttpClient(null, null, null), List.of(first, second));

    assertThat(client.hosts()).containsExactly(first, second);
  }

  private static MockResponse jsonResponse(int code, String body) {
    return new MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body);
  }

  private static int findClosedPort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }
}
