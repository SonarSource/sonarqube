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
package org.sonar.telemetry.core;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.Buffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_COMPRESSION;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_METRICS_URL;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_URL;

class TelemetryClientTest {

  private static final String JSON = "{\"key\":\"value\"}";
  private static final String TELEMETRY_URL = "https://telemetry.com/url";
  private static final String METRICS_TELEMETRY_URL = "https://telemetry.com/url/metrics";

  private final OkHttpClient okHttpClient = Mockito.mock(OkHttpClient.class, Mockito.RETURNS_DEEP_STUBS);
  private final MapSettings settings = new MapSettings();

  private final TelemetryClient underTest = new TelemetryClient(okHttpClient, settings.asConfig());

  @BeforeEach
  void setProperties() {
    settings.setProperty(SONAR_TELEMETRY_URL.getKey(), TELEMETRY_URL);
    settings.setProperty(SONAR_TELEMETRY_METRICS_URL.getKey(), METRICS_TELEMETRY_URL);
  }

  @Test
  void upload() throws IOException {
    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
    settings.setProperty(SONAR_TELEMETRY_COMPRESSION.getKey(), false);
    underTest.start();

    underTest.upload(JSON);

    Mockito.verify(okHttpClient).newCall(requestCaptor.capture());
    Request request = requestCaptor.getValue();
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.body().contentType()).isEqualTo(MediaType.parse("application/json; charset=utf-8"));
    Buffer body = new Buffer();
    request.body().writeTo(body);
    assertThat(body.readUtf8()).isEqualTo(JSON);
    assertThat(request.url()).hasToString(TELEMETRY_URL);
  }

  @Test
  void uploadMetric() throws IOException {
    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
    settings.setProperty(SONAR_TELEMETRY_COMPRESSION.getKey(), false);
    underTest.start();

    underTest.uploadMetric(JSON);

    Mockito.verify(okHttpClient).newCall(requestCaptor.capture());
    Request request = requestCaptor.getValue();
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.body().contentType()).isEqualTo(MediaType.parse("application/json; charset=utf-8"));
    Buffer body = new Buffer();
    request.body().writeTo(body);
    assertThat(body.readUtf8()).isEqualTo(JSON);
    assertThat(request.url()).hasToString(METRICS_TELEMETRY_URL);
  }

  @Test
  void opt_out() throws IOException {
    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
    underTest.start();

    underTest.optOut(JSON);

    Mockito.verify(okHttpClient).newCall(requestCaptor.capture());
    Request request = requestCaptor.getValue();
    assertThat(request.method()).isEqualTo("DELETE");
    assertThat(request.body().contentType()).isEqualTo(MediaType.parse("application/json; charset=utf-8"));
    Buffer body = new Buffer();
    request.body().writeTo(body);
    assertThat(body.readUtf8()).isEqualTo(JSON);
    assertThat(request.url()).hasToString(TELEMETRY_URL);
  }
}
