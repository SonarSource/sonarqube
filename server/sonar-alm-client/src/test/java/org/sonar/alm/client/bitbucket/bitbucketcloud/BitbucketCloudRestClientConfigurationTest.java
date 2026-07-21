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
package org.sonar.alm.client.bitbucket.bitbucketcloud;

import java.io.IOException;
import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Test;
import org.sonar.alm.client.TimeoutConfiguration;
import org.sonar.alm.client.TimeoutConfigurationImpl;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.alm.client.TimeoutConfigurationImpl.CONNECT_TIMEOUT_PROPERTY;
import static org.sonar.alm.client.TimeoutConfigurationImpl.READ_TIMEOUT_PROPERTY;

public class BitbucketCloudRestClientConfigurationTest {

  private static final long CONNECT_TIMEOUT_VALUE = 5435L;
  private static final long READ_TIMEOUT_VALUE = 13123L;

  private final MapSettings settings = new MapSettings();
  private final TimeoutConfiguration timeoutConfiguration = new TimeoutConfigurationImpl(settings.asConfig());

  private final BitbucketCloudRestClientConfiguration underTest = new BitbucketCloudRestClientConfiguration(timeoutConfiguration, new OkHttpClient());

  public MockWebServer server = new MockWebServer();

  @Test
  public void bitBucketCloudHttpClient_returnsCorrectlyConfiguredHttpClient() throws Exception {
    settings.setProperty(CONNECT_TIMEOUT_PROPERTY, CONNECT_TIMEOUT_VALUE);
    settings.setProperty(READ_TIMEOUT_PROPERTY, READ_TIMEOUT_VALUE);

    OkHttpClient client = underTest.bitbucketCloudHttpClient();

    assertThat(client.connectTimeoutMillis()).isEqualTo(CONNECT_TIMEOUT_VALUE);
    assertThat(client.readTimeoutMillis()).isEqualTo(READ_TIMEOUT_VALUE);
    assertThat(client.proxy()).isNull();
    assertThat(client.followRedirects()).isFalse();

    RecordedRequest recordedRequest = call(client);
    assertThat(recordedRequest.getHeader("Proxy-Authorization")).isNull();
  }

  @Test
  public void bitbucketCloudHttpClient_derivesFromInjectedGlobalClient_preservingProxyConfig() {
    Authenticator proxyAuthenticator = mock(Authenticator.class);
    Interceptor interceptor = chain -> chain.proceed(chain.request());
    OkHttpClient sharedClient = new OkHttpClient.Builder()
      .proxyAuthenticator(proxyAuthenticator)
      .addNetworkInterceptor(interceptor)
      .build();

    settings.setProperty(CONNECT_TIMEOUT_PROPERTY, CONNECT_TIMEOUT_VALUE);
    settings.setProperty(READ_TIMEOUT_PROPERTY, READ_TIMEOUT_VALUE);
    BitbucketCloudRestClientConfiguration configuration = new BitbucketCloudRestClientConfiguration(timeoutConfiguration, sharedClient);

    OkHttpClient derived = configuration.bitbucketCloudHttpClient();

    assertThat(derived.proxyAuthenticator()).isSameAs(proxyAuthenticator);
    assertThat(derived.networkInterceptors()).contains(interceptor);
    assertThat(derived.connectTimeoutMillis()).isEqualTo(CONNECT_TIMEOUT_VALUE);
    assertThat(derived.readTimeoutMillis()).isEqualTo(READ_TIMEOUT_VALUE);
    assertThat(derived.followRedirects()).isFalse();
    assertThat(derived.followSslRedirects()).isFalse();

    assertThat(sharedClient.proxyAuthenticator()).isSameAs(proxyAuthenticator);
    assertThat(sharedClient.connectTimeoutMillis()).isNotEqualTo((int) CONNECT_TIMEOUT_VALUE);
  }

  private RecordedRequest call(OkHttpClient client) throws IOException, InterruptedException {
    server.enqueue(new MockResponse().setBody("pong"));
    client.newCall(new Request.Builder().url(server.url("/ping")).build()).execute();
    return server.takeRequest();
  }
}
