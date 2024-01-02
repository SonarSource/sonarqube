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
package org.sonarqube.ws.client;

import javax.net.ssl.SSLSocketFactory;
import okhttp3.OkHttpClient;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class OkHttpClientBuilderTest {


  private OkHttpClientBuilder underTest = new OkHttpClientBuilder();

  @Test
  public void build_default_instance_of_OkHttpClient() {
    OkHttpClient okHttpClient = underTest.build();

    assertThat(okHttpClient.proxy()).isNull();
    assertThat(okHttpClient.networkInterceptors()).hasSize(1);
    assertThat(okHttpClient.sslSocketFactory()).isNotNull();
    assertThat(okHttpClient.followRedirects()).isTrue();
    assertThat(okHttpClient.followSslRedirects()).isTrue();
  }

  @Test
  public void build_with_custom_sslSocketFactory() {
    SSLSocketFactory sslSocketFactory = mock(SSLSocketFactory.class);
    OkHttpClient okHttpClient = underTest
      .setSSLSocketFactory(sslSocketFactory)
      .build();

    assertThat(okHttpClient.sslSocketFactory()).isEqualTo(sslSocketFactory);
  }

  @Test
  public void build_follow_redirects() {
    OkHttpClient okHttpClientWithRedirect = underTest
      .setFollowRedirects(true)
      .build();

    assertThat(okHttpClientWithRedirect.followRedirects()).isTrue();
    assertThat(okHttpClientWithRedirect.followSslRedirects()).isTrue();

    OkHttpClient okHttpClientWithoutRedirect = underTest
      .setFollowRedirects(false)
      .build();

    assertThat(okHttpClientWithoutRedirect.followRedirects()).isFalse();
    assertThat(okHttpClientWithoutRedirect.followSslRedirects()).isFalse();
  }

  @Test
  public void build_throws_IAE_if_connect_timeout_is_negative() {
    assertThatThrownBy(() -> underTest.setConnectTimeoutMs(-10))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Connect timeout must be positive. Got -10");
  }

  @Test
  public void build_throws_IAE_if_read_timeout_is_negative() {
    assertThatThrownBy(() -> underTest.setReadTimeoutMs(-10))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Read timeout must be positive. Got -10");
  }
}
