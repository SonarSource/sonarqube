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
package org.sonar.server.util;

import java.io.IOException;
import java.util.Base64;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.SonarQubeVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.config.ProxyProperties.HTTP_PROXY_PASSWORD;
import static org.sonar.core.config.ProxyProperties.HTTP_PROXY_USER;

public class OkHttpClientProviderTest {

  private final MapSettings settings = new MapSettings();
  private final SonarQubeVersion sonarQubeVersion = new SonarQubeVersion(Version.parse("6.2"));
  private final OkHttpClientProvider underTest = new OkHttpClientProvider();

  @Rule
  public MockWebServer server = new MockWebServer();

  @Test
  public void get_returns_a_OkHttpClient_with_default_configuration() throws Exception {
    OkHttpClient client = underTest.provide(settings.asConfig(), sonarQubeVersion);

    assertThat(client.connectTimeoutMillis()).isEqualTo(10_000);
    assertThat(client.readTimeoutMillis()).isEqualTo(10_000);
    assertThat(client.proxy()).isNull();

    RecordedRequest recordedRequest = call(client);
    assertThat(recordedRequest.getHeader("User-Agent")).isEqualTo("SonarQube/6.2");
    assertThat(recordedRequest.getHeader("Proxy-Authorization")).isNull();
  }

  @Test
  public void get_returns_a_OkHttpClient_with_proxy_authentication() throws Exception {
    settings.setProperty(HTTP_PROXY_USER, "the-login");
    settings.setProperty(HTTP_PROXY_PASSWORD, "the-password");

    OkHttpClient client = underTest.provide(settings.asConfig(), sonarQubeVersion);
    Response response = new Response.Builder().protocol(Protocol.HTTP_1_1).request(new Request.Builder().url("http://foo").build()).code(407)
      .message("").build();
    Request request = client.proxyAuthenticator().authenticate(null, response);

    assertThat(request.header("Proxy-Authorization")).isEqualTo("Basic " + Base64.getEncoder().encodeToString("the-login:the-password".getBytes()));
  }

  private RecordedRequest call(OkHttpClient client) throws IOException, InterruptedException {
    server.enqueue(new MockResponse().setBody("pong"));
    client.newCall(new Request.Builder().url(server.url("/ping")).build()).execute();

    return server.takeRequest();
  }
}
