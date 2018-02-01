/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.telemetry;

import java.io.IOException;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_URL;

@ServerSide
public class TelemetryClient implements Startable {
  private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
  private static final Logger LOG = Loggers.get(TelemetryClient.class);

  private final OkHttpClient okHttpClient;
  private final Configuration config;
  private String serverUrl;

  public TelemetryClient(OkHttpClient okHttpClient, Configuration config) {
    this.okHttpClient = okHttpClient;
    this.config = config;
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
    request.url(serverUrl);
    RequestBody body = RequestBody.create(JSON, json);
    request.post(body);
    return request.build();
  }

  private static void execute(Call call) throws IOException {
    try (Response ignored = call.execute()) {
      // auto close connection to avoid leaked connection
    }
  }

  @Override
  public void start() {
    this.serverUrl = config.get(SONAR_TELEMETRY_URL.getKey()).orElseThrow(() -> new IllegalStateException(String.format("Setting '%s' must be provided.", SONAR_TELEMETRY_URL)));
  }

  @Override
  public void stop() {
    // Nothing to do
  }
}
