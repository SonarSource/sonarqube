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

import co.elastic.clients.transport.TransportOptions;
import co.elastic.clients.transport.http.HeaderMap;
import co.elastic.clients.transport.http.TransportHttpClient;
import co.elastic.clients.util.BinaryData;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.net.URIBuilder;

/**
 * A synchronous {@link TransportHttpClient} for the Elasticsearch Java API client, backed by Apache
 * HttpClient 5's classic (blocking) client.
 * <p>
 * The default {@code Rest5ClientHttpClient} runs on HttpClient 5's <em>async</em> engine: a synchronous ES
 * call submits the request to an I/O reactor thread and blocks the caller on a future. That extra thread
 * hand-off is nearly free on an idle host but costs milliseconds of scheduling latency per request on a
 * CPU-contended host, which - multiplied over the many small bulk/refresh/search round-trips of an
 * analysis - noticeably slows indexing (see SONAR-30530). This client instead performs the request inline
 * on the calling thread, with no hand-off.
 * <p>
 * Requests are round-robined across the configured hosts. On a connection-establishment failure the next
 * host is tried; other failures (including responses received from a node) are propagated without retry, so
 * a non-idempotent request such as a bulk is never re-sent after it reached a node.
 */
public class SynchronousEsHttpClient implements TransportHttpClient {

  private final CloseableHttpClient httpClient;
  private final List<HttpHost> hosts;
  private final AtomicInteger nextHost = new AtomicInteger(0);

  public SynchronousEsHttpClient(CloseableHttpClient httpClient, List<HttpHost> hosts) {
    if (hosts.isEmpty()) {
      throw new IllegalArgumentException("At least one Elasticsearch host is required");
    }
    this.httpClient = httpClient;
    this.hosts = List.copyOf(hosts);
  }

  /**
   * The configured Elasticsearch hosts, in the order they were provided.
   */
  public List<HttpHost> hosts() {
    return hosts;
  }

  @Override
  public Response performRequest(String endpointId, @Nullable Node node, Request request, TransportOptions options) throws IOException {
    Map<String, String> headers = request.headers();
    ContentType contentType = null;
    String ct = headers.get(HeaderMap.CONTENT_TYPE);
    if (ct != null) {
      contentType = ContentType.parse(ct);
    }
    return executeWithFailover(request.method(), request.path(), request.queryParams(), headers, options,
      toByteArray(request.body()), contentType);
  }

  @Override
  public CompletableFuture<Response> performRequestAsync(String endpointId, @Nullable Node node, Request request, TransportOptions options) {
    // The client is synchronous by design; run inline and hand back an already-completed future.
    CompletableFuture<Response> future = new CompletableFuture<>();
    try {
      future.complete(performRequest(endpointId, node, request, options));
    } catch (Exception e) {
      future.completeExceptionally(e);
    }
    return future;
  }

  /**
   * Executes a raw {@code GET} and returns the response body as a UTF-8 string. Used for the few low-level
   * monitoring endpoints not exposed by the typed API client.
   */
  public String rawGet(String path, Map<String, String> queryParams) throws IOException {
    BufferedResponse response = executeWithFailover("GET", path, queryParams, Collections.emptyMap(), null, null, null);
    return response.bodyBytes == null ? "" : new String(response.bodyBytes, StandardCharsets.UTF_8);
  }

  private BufferedResponse executeWithFailover(String method, String path, Map<String, String> queryParams,
    Map<String, String> headers, @Nullable TransportOptions options, @Nullable byte[] body, @Nullable ContentType contentType) throws IOException {
    ClassicRequestBuilder builder = ClassicRequestBuilder.create(method).setUri(buildRelativeUri(path, queryParams));
    headers.forEach((name, value) -> {
      // Content-Type is carried by the entity below; adding it as a header too makes HttpClient5 reject
      // the request with "Content-Type header already present".
      if (!HeaderMap.CONTENT_TYPE.equalsIgnoreCase(name)) {
        builder.addHeader(name, value);
      }
    });
    if (options != null) {
      for (Map.Entry<String, String> header : options.headers()) {
        builder.addHeader(header.getKey(), header.getValue());
      }
    }
    if (body != null) {
      builder.setEntity(new ByteArrayEntity(body, contentType != null ? contentType : ContentType.APPLICATION_JSON));
    }
    var classicRequest = builder.build();

    int count = hosts.size();
    int start = Math.floorMod(nextHost.getAndIncrement(), count);
    IOException connectFailure = null;
    for (int i = 0; i < count; i++) {
      HttpHost host = hosts.get((start + i) % count);
      try {
        // Failover happens only on a connection-establishment error, i.e. before the request is sent, so
        // reusing the (repeatable) request across hosts never re-sends a request that reached a node.
        return httpClient.execute(host, classicRequest, new BufferingResponseHandler(URI.create(host.toURI())));
      } catch (ConnectException e) {
        // Host unreachable (HttpHostConnectException is a ConnectException): try the next host.
        connectFailure = e;
      }
    }
    throw connectFailure;
  }

  private static URI buildRelativeUri(String path, Map<String, String> queryParams) throws IOException {
    try {
      // The Elasticsearch client hands us an already percent-encoded path (path parameters such as
      // document/component/rule ids are escaped by the generated endpoints). Parse it with the
      // URIBuilder(String) constructor so it is preserved as-is; URIBuilder.setPath(String) would
      // percent-encode it a second time (e.g. "a%20b" -> "a%2520b") and break by-id operations.
      URIBuilder uriBuilder = new URIBuilder(path);
      queryParams.forEach(uriBuilder::addParameter);
      return uriBuilder.build();
    } catch (URISyntaxException e) {
      throw new IOException("Invalid Elasticsearch request URI for path " + path, e);
    }
  }

  @Nullable
  private static byte[] toByteArray(@Nullable Iterable<ByteBuffer> body) {
    if (body == null) {
      return null;
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (ByteBuffer buffer : body) {
      ByteBuffer readable = buffer.duplicate();
      byte[] chunk = new byte[readable.remaining()];
      readable.get(chunk);
      out.writeBytes(chunk);
    }
    return out.toByteArray();
  }

  @Override
  public void close() throws IOException {
    httpClient.close();
  }

  private static final class BufferingResponseHandler implements HttpClientResponseHandler<BufferedResponse> {
    private final URI hostUri;

    private BufferingResponseHandler(URI hostUri) {
      this.hostUri = hostUri;
    }

    @Override
    public BufferedResponse handleResponse(ClassicHttpResponse response) throws IOException {
      var entity = response.getEntity();
      byte[] bytes = entity == null ? null : EntityUtils.toByteArray(entity);
      String contentType = entity == null ? null : entity.getContentType();
      return new BufferedResponse(hostUri, response.getCode(), response.getHeaders(), bytes, contentType);
    }
  }

  /**
   * A response fully buffered in memory. The classic client releases the connection as soon as the
   * {@link BufferingResponseHandler} returns, so the body must already be read by then.
   */
  static final class BufferedResponse implements Response {
    private final URI hostUri;
    private final int statusCode;
    private final Header[] headers;
    @Nullable
    private final byte[] bodyBytes;
    @Nullable
    private final String contentType;

    BufferedResponse(URI hostUri, int statusCode, Header[] headers, @Nullable byte[] bodyBytes, @Nullable String contentType) {
      this.hostUri = hostUri;
      this.statusCode = statusCode;
      this.headers = headers;
      this.bodyBytes = bodyBytes;
      this.contentType = contentType;
    }

    @Override
    public Node node() {
      return new Node(hostUri);
    }

    @Override
    public int statusCode() {
      return statusCode;
    }

    @Override
    @Nullable
    public String header(String name) {
      for (Header header : headers) {
        if (header.getName().equalsIgnoreCase(name)) {
          return header.getValue();
        }
      }
      return null;
    }

    @Override
    public List<String> headers(String name) {
      List<String> values = new ArrayList<>();
      for (Header header : headers) {
        if (header.getName().equalsIgnoreCase(name)) {
          values.add(header.getValue());
        }
      }
      return values;
    }

    @Override
    @Nullable
    public BinaryData body() {
      return bodyBytes == null ? null : new ByteArrayBinaryData(bodyBytes, contentType);
    }

    @Override
    public Object originalResponse() {
      // Return the response itself (not the raw byte[]) so it renders meaningfully in error diagnostics.
      return this;
    }

    @Override
    public String toString() {
      return "HTTP " + statusCode + " from " + hostUri + " (" + (bodyBytes == null ? 0 : bodyBytes.length) + " bytes)";
    }

    @Override
    public void close() {
      // The body is already fully buffered and the connection released; nothing to do.
    }
  }

  private static final class ByteArrayBinaryData implements BinaryData {
    private final byte[] bytes;
    private final String contentType;

    private ByteArrayBinaryData(byte[] bytes, @Nullable String contentType) {
      this.bytes = bytes;
      this.contentType = contentType == null ? "application/octet-stream" : contentType;
    }

    @Override
    public String contentType() {
      return contentType;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
      out.write(bytes);
    }

    @Override
    public ByteBuffer asByteBuffer() {
      return ByteBuffer.wrap(bytes);
    }

    @Override
    public InputStream asInputStream() {
      return new ByteArrayInputStream(bytes);
    }

    @Override
    public boolean isRepeatable() {
      return true;
    }

    @Override
    public long size() {
      return bytes.length;
    }
  }
}
