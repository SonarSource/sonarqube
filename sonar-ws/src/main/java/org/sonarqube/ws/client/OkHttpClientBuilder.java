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
package org.sonarqube.ws.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.ConnectionSpec;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.google.common.base.Strings.nullToEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

/**
 * Helper to build an instance of {@link okhttp3.OkHttpClient} that
 * correctly supports HTTPS and proxy authentication. It also handles
 * sending of User-Agent header.
 */
public class OkHttpClientBuilder {

  private static final String NONE = "NONE";
  private static final String P11KEYSTORE = "PKCS11";
  private static final String PROXY_AUTHORIZATION = "Proxy-Authorization";

  private String userAgent;
  private Proxy proxy;
  private String credentials;
  private String proxyLogin;
  private String proxyPassword;
  private long connectTimeoutMs = -1;
  private long readTimeoutMs = -1;
  private SSLSocketFactory sslSocketFactory = null;
  private X509TrustManager sslTrustManager = null;

  /**
   * Optional User-Agent. If set, then all the requests sent by the
   * {@link OkHttpClient} will include the header "User-Agent".
   */
  public OkHttpClientBuilder setUserAgent(@Nullable String s) {
    this.userAgent = s;
    return this;
  }

  /**
   * Optional SSL socket factory with which SSL sockets will be created to establish SSL connections.
   * If not set, a default SSL socket factory will be used, base d on the JVM's default key store.
   */
  public OkHttpClientBuilder setSSLSocketFactory(@Nullable SSLSocketFactory sslSocketFactory) {
    this.sslSocketFactory = sslSocketFactory;
    return this;
  }

  /**
   * Optional SSL trust manager used to validate certificates.
   * If not set, a default system trust manager will be used, based on the JVM's default truststore.
   */
  public OkHttpClientBuilder setTrustManager(@Nullable X509TrustManager sslTrustManager) {
    this.sslTrustManager = sslTrustManager;
    return this;
  }

  /**
   * Optional proxy. If set, then all the requests sent by the
   * {@link OkHttpClient} will reach the proxy. If not set,
   * then the system-wide proxy is used.
   */
  public OkHttpClientBuilder setProxy(@Nullable Proxy proxy) {
    this.proxy = proxy;
    return this;
  }

  /**
   * Login required for proxy authentication.
   */
  public OkHttpClientBuilder setProxyLogin(@Nullable String s) {
    this.proxyLogin = s;
    return this;
  }

  /**
   * Password used for proxy authentication. It is ignored if
   * proxy login is not defined (see {@link #setProxyLogin(String)}).
   * It can be null or empty when login is defined.
   */
  public OkHttpClientBuilder setProxyPassword(@Nullable String s) {
    this.proxyPassword = s;
    return this;
  }

  /**
   * Sets the default connect timeout for new connections. A value of 0 means no timeout.
   * Default is defined by OkHttp (10 seconds in OkHttp 3.3).
   */
  public OkHttpClientBuilder setConnectTimeoutMs(long l) {
    if (l < 0) {
      throw new IllegalArgumentException("Connect timeout must be positive. Got " + l);
    }
    this.connectTimeoutMs = l;
    return this;
  }

  /**
   * Set credentials that will be passed on every request
   */
  public void setCredentials(String credentials) {
    this.credentials = credentials;
  }

  /**
   * Sets the default read timeout for new connections. A value of 0 means no timeout.
   * Default is defined by OkHttp (10 seconds in OkHttp 3.3).
   */
  public OkHttpClientBuilder setReadTimeoutMs(long l) {
    if (l < 0) {
      throw new IllegalArgumentException("Read timeout must be positive. Got " + l);
    }
    this.readTimeoutMs = l;
    return this;
  }

  public OkHttpClient build() {
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    builder.proxy(proxy);
    if (connectTimeoutMs >= 0) {
      builder.connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS);
    }
    if (readTimeoutMs >= 0) {
      builder.readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS);
    }
    builder.addNetworkInterceptor(this::addHeaders);
    if (proxyLogin != null) {
      builder.proxyAuthenticator((route, response) -> {
        if (response.request().header(PROXY_AUTHORIZATION) != null) {
          // Give up, we've already attempted to authenticate.
          return null;
        }
        if (HttpURLConnection.HTTP_PROXY_AUTH == response.code()) {
          String credential = Credentials.basic(proxyLogin, nullToEmpty(proxyPassword), UTF_8);
          return response.request().newBuilder().header(PROXY_AUTHORIZATION, credential).build();
        }
        return null;
      });
    }

    ConnectionSpec tls = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
      .allEnabledTlsVersions()
      .allEnabledCipherSuites()
      .supportsTlsExtensions(true)
      .build();
    builder.connectionSpecs(asList(tls, ConnectionSpec.CLEARTEXT));

    X509TrustManager trustManager = sslTrustManager != null ? sslTrustManager : systemDefaultTrustManager();
    SSLSocketFactory sslFactory = sslSocketFactory != null ? sslSocketFactory : systemDefaultSslSocketFactory(trustManager);
    builder.sslSocketFactory(sslFactory, trustManager);

    return builder.build();
  }

  private Response addHeaders(Interceptor.Chain chain) throws IOException {
    Request.Builder newRequest = chain.request().newBuilder();
    if (userAgent != null) {
      newRequest.header("User-Agent", userAgent);
    }
    if (credentials != null) {
      newRequest.header("Authorization", credentials);
    }
    return chain.proceed(newRequest.build());
  }

  private static X509TrustManager systemDefaultTrustManager() {
    try {
      TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init((KeyStore) null);
      TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
      if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
        throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
      }
      return (X509TrustManager) trustManagers[0];
    } catch (GeneralSecurityException e) {
      // The system has no TLS. Just give up.
      throw new AssertionError(e);
    }
  }

  private static SSLSocketFactory systemDefaultSslSocketFactory(X509TrustManager trustManager) {
    KeyManager[] defaultKeyManager;
    try {
      defaultKeyManager = getDefaultKeyManager();
    } catch (Exception e) {
      throw new IllegalStateException("Unable to get default key manager", e);
    }
    try {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(defaultKeyManager, new TrustManager[] {trustManager}, null);
      return sslContext.getSocketFactory();
    } catch (GeneralSecurityException e) {
      // The system has no TLS. Just give up.
      throw new AssertionError(e);
    }
  }

  private static void logDebug(String msg) {
    boolean debugEnabled = "all".equals(System.getProperty("javax.net.debug"));
    if (debugEnabled) {
      System.out.println(msg);
    }
  }

  /**
   * Inspired from sun.security.ssl.SSLContextImpl#getDefaultKeyManager()
   */
  private static synchronized KeyManager[] getDefaultKeyManager() throws KeyStoreException, NoSuchProviderException,
    IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
    final String defaultKeyStore = System.getProperty("javax.net.ssl.keyStore", "");
    String defaultKeyStoreType = System.getProperty("javax.net.ssl.keyStoreType", KeyStore.getDefaultType());
    String defaultKeyStoreProvider = System.getProperty("javax.net.ssl.keyStoreProvider", "");

    logDebug("keyStore is : " + defaultKeyStore);
    logDebug("keyStore type is : " + defaultKeyStoreType);
    logDebug("keyStore provider is : " + defaultKeyStoreProvider);

    if (P11KEYSTORE.equals(defaultKeyStoreType) && !NONE.equals(defaultKeyStore)) {
      throw new IllegalArgumentException("if keyStoreType is " + P11KEYSTORE + ", then keyStore must be " + NONE);
    }

    KeyStore ks = null;
    String defaultKeyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword", "");
    char[] passwd = defaultKeyStorePassword.isEmpty() ? null : defaultKeyStorePassword.toCharArray();

    // Try to initialize key store.
    if (!defaultKeyStoreType.isEmpty()) {
      logDebug("init keystore");
      if (defaultKeyStoreProvider.isEmpty()) {
        ks = KeyStore.getInstance(defaultKeyStoreType);
      } else {
        ks = KeyStore.getInstance(defaultKeyStoreType, defaultKeyStoreProvider);
      }
      if (!defaultKeyStore.isEmpty() && !NONE.equals(defaultKeyStore)) {
        try (FileInputStream fs = new FileInputStream(defaultKeyStore)) {
          ks.load(fs, passwd);
        }
      } else {
        ks.load(null, passwd);
      }
    }

    // Try to initialize key manager.
    logDebug("init keymanager of type " + KeyManagerFactory.getDefaultAlgorithm());
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

    if (P11KEYSTORE.equals(defaultKeyStoreType)) {
      // do not pass key passwd if using token
      kmf.init(ks, null);
    } else {
      kmf.init(ks, passwd);
    }

    return kmf.getKeyManagers();
  }

}
