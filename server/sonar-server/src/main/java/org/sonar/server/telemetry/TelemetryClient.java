/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.base.Throwables;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;

@ServerSide
public class TelemetryClient {
  private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  private final OkHttpClient okHttpClient;
  private final TelemetryUrl serverUrl;

  public TelemetryClient(OkHttpClient okHttpClient, Configuration config) {
    this.okHttpClient = okHttpClient;
    this.serverUrl = new TelemetryUrl(config);
  }

  void send(String json) {
    try {
      Request request = buildHttpRequest(json);
      okHttpClient.newCall(request).execute();
    } catch (IOException e) {
      Throwables.propagate(e);
    }
  }

  private Request buildHttpRequest(String json) {
    Request.Builder request = new Request.Builder();
    request.url(serverUrl.get());
    RequestBody body = RequestBody.create(JSON, json);
    request.post(body);
    return request.build();
  }
}
