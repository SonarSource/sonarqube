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
import java.util.Objects;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.GzipSource;
import okio.Okio;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_URL;

class TelemetryClientCompressionTest {

  private final OkHttpClient okHttpClient = new OkHttpClient();
  private final MockWebServer telemetryServer = new MockWebServer();

  @Test
  void payload_is_gzip_encoded() throws IOException, InterruptedException {
    telemetryServer.enqueue(new MockResponse().setResponseCode(200));
    MapSettings settings = new MapSettings();
    settings.setProperty(SONAR_TELEMETRY_URL.getKey(), telemetryServer.url("/").toString());
    TelemetryClient underTest = new TelemetryClient(okHttpClient, settings.asConfig());
    underTest.start();
    underTest.upload("payload compressed with gzip");

    RecordedRequest request = telemetryServer.takeRequest();

    String contentType = Objects.requireNonNull(request.getHeader("content-type"));
    assertThat(MediaType.parse(contentType)).isEqualTo(MediaType.parse("application/json; charset=utf-8"));
    assertThat(request.getHeader("content-encoding")).isEqualTo("gzip");

    GzipSource source = new GzipSource(request.getBody());
    String body = Okio.buffer(source).readUtf8();
    Assertions.assertThat(body).isEqualTo("payload compressed with gzip");
  }
}
