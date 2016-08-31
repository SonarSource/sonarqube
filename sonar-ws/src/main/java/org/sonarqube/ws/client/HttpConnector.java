/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import java.net.Proxy;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.Call;
import okhttp3.ConnectionSpec;
import okhttp3.Credentials;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static java.lang.String.format;
import static java.util.Arrays.asList;

/**
 * Connect to any SonarQube server available through HTTP or HTTPS.
 * <p>TLS 1.0, 1.1 and 1.2 are supported on both Java 7 and 8. SSLv3 is not supported.</p>
 * <p>The JVM system proxies are used.</p>
 */
public class HttpConnector implements WsConnector {

  public static final int DEFAULT_CONNECT_TIMEOUT_MILLISECONDS = 30_000;
  public static final int DEFAULT_READ_TIMEOUT_MILLISECONDS = 60_000;
  private static final String NONE = "NONE";
  private static final String P11KEYSTORE = "PKCS11";

  /**
   * Base URL with trailing slash, for instance "https://localhost/sonarqube/".
   * It is required for further usage of {@link HttpUrl#resolve(String)}.
   */
  private final HttpUrl baseUrl;
  private final String userAgent;
  private final String credentials;
  private final String proxyCredentials;
  private final OkHttpClient okHttpClient;

  private HttpConnector(Builder builder) {
    this.baseUrl = HttpUrl.parse(builder.url.endsWith("/") ? builder.url : format("%s/", builder.url));
    checkArgument(this.baseUrl != null, "Malformed URL: '%s'", builder.url);
    this.userAgent = builder.userAgent;

    if (isNullOrEmpty(builder.login)) {
      // no login nor access token
      this.credentials = null;
    } else {
      // password is null when login represents an access token. In this case
      // the Basic credentials consider an empty password.
      this.credentials = Credentials.basic(builder.login, nullToEmpty(builder.password));
    }
    // proxy credentials can be used on system-wide proxies, so even if builder.proxy is null
    if (isNullOrEmpty(builder.proxyLogin)) {
      this.proxyCredentials = null;
    } else {
      this.proxyCredentials = Credentials.basic(builder.proxyLogin, nullToEmpty(builder.proxyPassword));
    }
    this.okHttpClient = buildClient(builder);
  }

  private static OkHttpClient buildClient(Builder builder) {
    OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
    if (builder.proxy != null) {
      okHttpClientBuilder.proxy(builder.proxy);
    }

    okHttpClientBuilder.connectTimeout(builder.connectTimeoutMs, TimeUnit.MILLISECONDS);
    okHttpClientBuilder.readTimeout(builder.readTimeoutMs, TimeUnit.MILLISECONDS);

    ConnectionSpec tls = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
      .allEnabledTlsVersions()
      .allEnabledCipherSuites()
      .supportsTlsExtensions(true)
      .build();
    okHttpClientBuilder.connectionSpecs(asList(tls, ConnectionSpec.CLEARTEXT));
    X509TrustManager systemDefaultTrustManager = systemDefaultTrustManager();
    okHttpClientBuilder.sslSocketFactory(systemDefaultSslSocketFactory(systemDefaultTrustManager), systemDefaultTrustManager);

    return okHttpClientBuilder.build();
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
  private static synchronized KeyManager[] getDefaultKeyManager() throws Exception {

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

    /**
     * Try to initialize key store.
     */
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

    /*
     * Try to initialize key manager.
     */
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

  @Override
  public String baseUrl() {
    return baseUrl.url().toExternalForm();
  }

  @CheckForNull
  public String userAgent() {
    return userAgent;
  }

  public OkHttpClient okHttpClient() {
    return okHttpClient;
  }

  @Override
  public WsResponse call(WsRequest httpRequest) {
    if (httpRequest instanceof GetRequest) {
      return get((GetRequest) httpRequest);
    }
    if (httpRequest instanceof PostRequest) {
      return post((PostRequest) httpRequest);
    }
    throw new IllegalArgumentException(format("Unsupported implementation: %s", httpRequest.getClass()));
  }

  private WsResponse get(GetRequest getRequest) {
    HttpUrl.Builder urlBuilder = prepareUrlBuilder(getRequest);
    Request.Builder okRequestBuilder = prepareOkRequestBuilder(getRequest, urlBuilder).get();
    return doCall(okRequestBuilder.build());
  }

  private WsResponse post(PostRequest postRequest) {
    HttpUrl.Builder urlBuilder = prepareUrlBuilder(postRequest);
    Request.Builder okRequestBuilder = prepareOkRequestBuilder(postRequest, urlBuilder);

    Map<String, PostRequest.Part> parts = postRequest.getParts();
    if (parts.isEmpty()) {
      okRequestBuilder.post(RequestBody.create(null, ""));
    } else {
      MultipartBody.Builder bodyBuilder = new MultipartBody.Builder();
      bodyBuilder.setType(MultipartBody.FORM);
      for (Map.Entry<String, PostRequest.Part> param : parts.entrySet()) {
        PostRequest.Part part = param.getValue();
        bodyBuilder.addPart(
          Headers.of("Content-Disposition", format("form-data; name=\"%s\"", param.getKey())),
          RequestBody.create(MediaType.parse(part.getMediaType()), part.getFile()));
      }
      okRequestBuilder.post(bodyBuilder.build());
    }

    return doCall(okRequestBuilder.build());
  }

  private HttpUrl.Builder prepareUrlBuilder(WsRequest wsRequest) {
    String path = wsRequest.getPath();
    HttpUrl.Builder urlBuilder = baseUrl
      .resolve(path.startsWith("/") ? path.replaceAll("^(/)+", "") : path)
      .newBuilder();
    wsRequest.getParameters().getKeys()
      .forEach(key -> wsRequest.getParameters().getValues(key)
        .forEach(value -> urlBuilder.addQueryParameter(key, value)));

    return urlBuilder;
  }

  private Request.Builder prepareOkRequestBuilder(WsRequest getRequest, HttpUrl.Builder urlBuilder) {
    Request.Builder okHttpRequestBuilder = new Request.Builder()
      .url(urlBuilder.build())
      .addHeader("Accept", getRequest.getMediaType())
      .addHeader("Accept-Charset", "UTF-8");
    if (credentials != null) {
      okHttpRequestBuilder.header("Authorization", credentials);
    }
    if (proxyCredentials != null) {
      okHttpRequestBuilder.header("Proxy-Authorization", proxyCredentials);
    }
    if (userAgent != null) {
      okHttpRequestBuilder.addHeader("User-Agent", userAgent);
    }
    return okHttpRequestBuilder;
  }

  private OkHttpResponse doCall(Request okRequest) {
    Call call = okHttpClient.newCall(okRequest);
    try {
      Response okResponse = call.execute();
      return new OkHttpResponse(okResponse);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to request " + okRequest.url(), e);
    }
  }

  /**
   * @since 5.5
   */
  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private String url;
    private String userAgent;
    private String login;
    private String password;
    private Proxy proxy;
    private String proxyLogin;
    private String proxyPassword;
    private int connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MILLISECONDS;
    private int readTimeoutMs = DEFAULT_READ_TIMEOUT_MILLISECONDS;

    /**
     * Private since 5.5.
     * @see HttpConnector#newBuilder()
     */
    private Builder() {
    }

    /**
     * Optional User  Agent
     */
    public Builder userAgent(@Nullable String userAgent) {
      this.userAgent = userAgent;
      return this;
    }

    /**
     * Mandatory HTTP server URL, eg "http://localhost:9000"
     */
    public Builder url(String url) {
      this.url = url;
      return this;
    }

    /**
     * Optional login/password, for example "admin"
     */
    public Builder credentials(@Nullable String login, @Nullable String password) {
      this.login = login;
      this.password = password;
      return this;
    }

    /**
     * Optional access token, for example {@code "ABCDE"}. Alternative to {@link #credentials(String, String)}
     */
    public Builder token(@Nullable String token) {
      this.login = token;
      this.password = null;
      return this;
    }

    /**
     * Sets a specified timeout value, in milliseconds, to be used when opening HTTP connection.
     * A timeout of zero is interpreted as an infinite timeout. Default value is {@link #DEFAULT_CONNECT_TIMEOUT_MILLISECONDS}
     */
    public Builder connectTimeoutMilliseconds(int i) {
      this.connectTimeoutMs = i;
      return this;
    }

    /**
     * Sets the read timeout to a specified timeout, in milliseconds.
     * A timeout of zero is interpreted as an infinite timeout. Default value is {@link #DEFAULT_READ_TIMEOUT_MILLISECONDS}
     */
    public Builder readTimeoutMilliseconds(int i) {
      this.readTimeoutMs = i;
      return this;
    }

    public Builder proxy(@Nullable Proxy proxy) {
      this.proxy = proxy;
      return this;
    }

    public Builder proxyCredentials(@Nullable String proxyLogin, @Nullable String proxyPassword) {
      this.proxyLogin = proxyLogin;
      this.proxyPassword = proxyPassword;
      return this;
    }

    public HttpConnector build() {
      checkArgument(!isNullOrEmpty(url), "Server URL is not defined");
      return new HttpConnector(this);
    }
  }

}
