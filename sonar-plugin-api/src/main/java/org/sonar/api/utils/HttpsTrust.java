/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.utils;

import javax.net.ssl.*;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * @since 4.0
 */
class HttpsTrust {

  static HttpsTrust INSTANCE = new HttpsTrust(new SslContext());

  static class SslContext {
    SSLSocketFactory newFactory(TrustManager... managers) throws NoSuchAlgorithmException, KeyManagementException {
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(null, managers, new SecureRandom());
      return context.getSocketFactory();
    }
  }

  private final SSLSocketFactory socketFactory;
  private final HostnameVerifier hostnameVerifier;

  HttpsTrust(SslContext context) {
    this.socketFactory = createSocketFactory(context);
    this.hostnameVerifier = createHostnameVerifier();
  }

  void trust(HttpURLConnection connection) {
    if (connection instanceof HttpsURLConnection) {
      HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
      httpsConnection.setSSLSocketFactory(socketFactory);
      httpsConnection.setHostnameVerifier(hostnameVerifier);
    }
  }

  /**
   * Trust all certificates
   */
  private SSLSocketFactory createSocketFactory(SslContext context) {
    try {
      return context.newFactory(new AlwaysTrustManager());
    } catch (Exception e) {
      throw new IllegalStateException("Fail to build SSL factory", e);
    }
  }

  /**
   * Trust all hosts
   */
  private HostnameVerifier createHostnameVerifier() {
    return new HostnameVerifier() {
      public boolean verify(String hostname, SSLSession session) {
        return true;
      }
    };
  }

  static class AlwaysTrustManager implements X509TrustManager {
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType) {
      // Do not check
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) {
      // Do not check
    }
  }
}
