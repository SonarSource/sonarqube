/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.Buffer;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_URL;

public class TelemetryClientTest {

  private static final String JSON = "{\"key\":\"value\"}";
  private static final String TELEMETRY_URL = "https://telemetry.com/url";

  private OkHttpClient okHttpClient = mock(OkHttpClient.class, RETURNS_DEEP_STUBS);
  private MapSettings settings = new MapSettings();

  private TelemetryClient underTest = new TelemetryClient(okHttpClient, settings.asConfig());

  @Test
  public void upload() throws IOException {
    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
    settings.setProperty(SONAR_TELEMETRY_URL.getKey(), TELEMETRY_URL);
    underTest.start();

    underTest.upload(JSON);

    verify(okHttpClient).newCall(requestCaptor.capture());
    Request request = requestCaptor.getValue();
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.body().contentType()).isEqualTo(MediaType.parse("application/json; charset=utf-8"));
    Buffer body = new Buffer();
    request.body().writeTo(body);
    assertThat(body.readUtf8()).isEqualTo(JSON);
    assertThat(request.url().toString()).isEqualTo(TELEMETRY_URL);
  }

  @Test
  public void opt_out() throws IOException {
    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
    settings.setProperty(SONAR_TELEMETRY_URL.getKey(), TELEMETRY_URL);
    underTest.start();

    underTest.optOut(JSON);

    verify(okHttpClient).newCall(requestCaptor.capture());
    Request request = requestCaptor.getValue();
    assertThat(request.method()).isEqualTo("DELETE");
    assertThat(request.body().contentType()).isEqualTo(MediaType.parse("application/json; charset=utf-8"));
    Buffer body = new Buffer();
    request.body().writeTo(body);
    assertThat(body.readUtf8()).isEqualTo(JSON);
    assertThat(request.url().toString()).isEqualTo(TELEMETRY_URL);
  }
}
