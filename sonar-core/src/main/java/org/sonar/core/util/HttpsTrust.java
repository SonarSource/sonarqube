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
package org.sonar.core.util;

import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @since 4.0
 */
class HttpsTrust {

  static final HttpsTrust INSTANCE = new HttpsTrust(new Ssl());

  static class Ssl {
    SSLSocketFactory newFactory(TrustManager... managers) throws NoSuchAlgorithmException, KeyManagementException {
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(null, managers, new SecureRandom());
      return context.getSocketFactory();
    }
  }

  private final SSLSocketFactory socketFactory;
  private final HostnameVerifier hostnameVerifier;

  HttpsTrust(Ssl context) {
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
  private static SSLSocketFactory createSocketFactory(Ssl context) {
    try {
      return context.newFactory(new AlwaysTrustManager());
    } catch (Exception e) {
      throw new IllegalStateException("Fail to build SSL factory", e);
    }
  }

  /**
   * Trust all hosts
   */
  private static HostnameVerifier createHostnameVerifier() {
    return new HostnameVerifier() {
      @Override
      public boolean verify(String hostname, SSLSession session) {
        return true;
      }
    };
  }

  static class AlwaysTrustManager implements X509TrustManager {
    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
      // Do not check
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) {
      // Do not check
    }
  }
}
