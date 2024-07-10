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
package org.sonar.telemetry;

import java.io.IOException;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;

import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_COMPRESSION;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_URL;

@ServerSide
public class TelemetryClient implements Startable {
  private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
  private static final Logger LOG = LoggerFactory.getLogger(TelemetryClient.class);

  private final OkHttpClient okHttpClient;
  private final Configuration config;
  private String serverUrl;
  private boolean compression;

  public TelemetryClient(OkHttpClient okHttpClient, Configuration config) {
    this.config = config;
    this.okHttpClient = okHttpClient;
  }

  void upload(String json) throws IOException {
    Request request = buildHttpRequest(json);
    execute(okHttpClient.newCall(request));
  }

  void optOut(String json) {
    Request.Builder request = new Request.Builder();
    request.url(serverUrl);
    RequestBody body = RequestBody.create(JSON, json);
    request.delete(body);

    try {
      execute(okHttpClient.newCall(request.build()));
    } catch (IOException e) {
      LOG.debug("Error when sending opt-out usage statistics: {}", e.getMessage());
    }
  }

  private Request buildHttpRequest(String json) {
    Request.Builder request = new Request.Builder();
    request.addHeader("Content-Encoding", "gzip");
    request.addHeader("Content-Type", "application/json");
    request.url(serverUrl);
    RequestBody body = RequestBody.create(JSON, json);
    if (compression) {
      request.post(gzip(body));
    } else {
      request.post(body);
    }
    return request.build();
  }

  private static RequestBody gzip(final RequestBody body) {
    return new RequestBody() {
      @Override
      public MediaType contentType() {
        return body.contentType();
      }

      @Override
      public long contentLength() {
        // We don't know the compressed length in advance!
        return -1;
      }

      @Override
      public void writeTo(BufferedSink sink) throws IOException {
        BufferedSink gzipSink = Okio.buffer(new GzipSink(sink));
        body.writeTo(gzipSink);
        gzipSink.close();
      }
    };
  }

  private static void execute(Call call) throws IOException {
    try (Response ignored = call.execute()) {
      // auto close connection to avoid leaked connection
    }
  }

  @Override
  public void start() {
    this.serverUrl = config.get(SONAR_TELEMETRY_URL.getKey())
      .orElseThrow(() -> new IllegalStateException(String.format("Setting '%s' must be provided.", SONAR_TELEMETRY_URL)));
    this.compression = config.getBoolean(SONAR_TELEMETRY_COMPRESSION.getKey()).orElse(true);
  }

  @Override
  public void stop() {
    // Nothing to do
  }
}
