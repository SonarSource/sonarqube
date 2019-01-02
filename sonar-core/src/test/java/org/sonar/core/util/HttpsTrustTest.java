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
package org.sonar.core.util;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.TrustManager;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpsTrustTest {
  @Test
  public void trustAllHosts() throws Exception {
    HttpsURLConnection connection = newHttpsConnection();
    HttpsTrust.INSTANCE.trust(connection);

    assertThat(connection.getHostnameVerifier()).isNotNull();
    assertThat(connection.getHostnameVerifier().verify("foo", null)).isTrue();
  }

  @Test
  public void singleHostnameVerifier() throws Exception {
    HttpsURLConnection connection1 = newHttpsConnection();
    HttpsTrust.INSTANCE.trust(connection1);
    HttpsURLConnection connection2 = newHttpsConnection();
    HttpsTrust.INSTANCE.trust(connection2);

    assertThat(connection1.getHostnameVerifier()).isSameAs(connection2.getHostnameVerifier());
  }

  @Test
  public void trustAllCerts() throws Exception {
    HttpsURLConnection connection1 = newHttpsConnection();
    HttpsTrust.INSTANCE.trust(connection1);

    assertThat(connection1.getSSLSocketFactory()).isNotNull();
    assertThat(connection1.getSSLSocketFactory().getDefaultCipherSuites()).isNotEmpty();
  }

  @Test
  public void singleSslFactory() throws Exception {
    HttpsURLConnection connection1 = newHttpsConnection();
    HttpsTrust.INSTANCE.trust(connection1);
    HttpsURLConnection connection2 = newHttpsConnection();
    HttpsTrust.INSTANCE.trust(connection2);

    assertThat(connection1.getSSLSocketFactory()).isSameAs(connection2.getSSLSocketFactory());
  }

  @Test
  public void testAlwaysTrustManager() {
    HttpsTrust.AlwaysTrustManager manager = new HttpsTrust.AlwaysTrustManager();
    assertThat(manager.getAcceptedIssuers()).isEmpty();
    // does nothing
    manager.checkClientTrusted(null, null);
    manager.checkServerTrusted(null, null);
  }

  @Test
  public void failOnError() throws Exception {
    HttpsTrust.Ssl context = mock(HttpsTrust.Ssl.class);
    KeyManagementException cause = new KeyManagementException("foo");
    when(context.newFactory(any(TrustManager.class))).thenThrow(cause);

    try {
      new HttpsTrust(context);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Fail to build SSL factory");
      assertThat(e.getCause()).isSameAs(cause);
    }
  }

  private HttpsURLConnection newHttpsConnection() throws IOException {
    return (HttpsURLConnection) new URL("https://localhost").openConnection();
  }
}
