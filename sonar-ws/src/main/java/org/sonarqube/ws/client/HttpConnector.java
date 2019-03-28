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
package org.sonarqube.ws.client;

import java.io.IOException;
import java.net.Proxy;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.Call;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.nio.charset.StandardCharsets.UTF_8;
import static okhttp3.internal.http.StatusLine.HTTP_PERM_REDIRECT;
import static okhttp3.internal.http.StatusLine.HTTP_TEMP_REDIRECT;
import static org.sonarqube.ws.WsUtils.checkArgument;
import static org.sonarqube.ws.WsUtils.isNullOrEmpty;
import static org.sonarqube.ws.WsUtils.nullToEmpty;

/**
 * Connect to any SonarQube server available through HTTP or HTTPS.
 * <p>TLS 1.0, 1.1 and 1.2 are supported on both Java 7 and 8. SSLv3 is not supported.</p>
 * <p>The JVM system proxies are used.</p>
 */
public class HttpConnector implements WsConnector {

  public static final int DEFAULT_CONNECT_TIMEOUT_MILLISECONDS = 30_000;
  public static final int DEFAULT_READ_TIMEOUT_MILLISECONDS = 60_000;

  /**
   * Base URL with trailing slash, for instance "https://localhost/sonarqube/".
   * It is required for further usage of {@link HttpUrl#resolve(String)}.
   */
  private final HttpUrl baseUrl;
  private final String systemPassCode;
  private final OkHttpClient okHttpClient;
  private final OkHttpClient noRedirectOkHttpClient;

  private HttpConnector(Builder builder) {
    this.baseUrl = HttpUrl.parse(builder.url.endsWith("/") ? builder.url : format("%s/", builder.url));
    checkArgument(this.baseUrl != null, "Malformed URL: '%s'", builder.url);

    OkHttpClientBuilder okHttpClientBuilder = new OkHttpClientBuilder();
    okHttpClientBuilder.setUserAgent(builder.userAgent);

    if (!isNullOrEmpty(builder.login)) {
      // password is null when login represents an access token. In this case
      // the Basic credentials consider an empty password.
      okHttpClientBuilder.setCredentials(Credentials.basic(builder.login, nullToEmpty(builder.password), UTF_8));
    }
    this.systemPassCode = builder.systemPassCode;
    okHttpClientBuilder.setProxy(builder.proxy);
    okHttpClientBuilder.setProxyLogin(builder.proxyLogin);
    okHttpClientBuilder.setProxyPassword(builder.proxyPassword);
    okHttpClientBuilder.setConnectTimeoutMs(builder.connectTimeoutMs);
    okHttpClientBuilder.setReadTimeoutMs(builder.readTimeoutMs);
    okHttpClientBuilder.setSSLSocketFactory(builder.sslSocketFactory);
    okHttpClientBuilder.setTrustManager(builder.sslTrustManager);
    this.okHttpClient = okHttpClientBuilder.build();
    this.noRedirectOkHttpClient = newClientWithoutRedirect(this.okHttpClient);
  }

  private static OkHttpClient newClientWithoutRedirect(OkHttpClient client) {
    return client.newBuilder()
      .followRedirects(false)
      .followSslRedirects(false)
      .build();
  }

  @Override
  public String baseUrl() {
    return baseUrl.url().toExternalForm();
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
    completeUrlQueryParameters(getRequest, urlBuilder);

    Request.Builder okRequestBuilder = prepareOkRequestBuilder(getRequest, urlBuilder).get();
    return new OkHttpResponse(doCall(prepareOkHttpClient(okHttpClient, getRequest), okRequestBuilder.build()));
  }

  private WsResponse post(PostRequest postRequest) {
    HttpUrl.Builder urlBuilder = prepareUrlBuilder(postRequest);

    RequestBody body;
    Map<String, PostRequest.Part> parts = postRequest.getParts();
    if (parts.isEmpty()) {
      // parameters are defined in the body (application/x-www-form-urlencoded)
      FormBody.Builder formBody = new FormBody.Builder();
      postRequest.getParameters().getKeys()
        .forEach(key -> postRequest.getParameters().getValues(key)
          .forEach(value -> formBody.add(key, value)));
      body = formBody.build();

    } else {
      // parameters are defined in the URL (as GET)
      completeUrlQueryParameters(postRequest, urlBuilder);

      MultipartBody.Builder bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
      parts.entrySet().forEach(param -> {
        PostRequest.Part part = param.getValue();
        bodyBuilder.addFormDataPart(
          param.getKey(),
          part.getFile().getName(),
          RequestBody.create(MediaType.parse(part.getMediaType()), part.getFile()));
      });
      body = bodyBuilder.build();
    }
    Request.Builder okRequestBuilder = prepareOkRequestBuilder(postRequest, urlBuilder).post(body);
    Response response = doCall(prepareOkHttpClient(noRedirectOkHttpClient, postRequest), okRequestBuilder.build());
    response = checkRedirect(response, postRequest);
    return new OkHttpResponse(response);
  }

  private HttpUrl.Builder prepareUrlBuilder(WsRequest wsRequest) {
    String path = wsRequest.getPath();
    return baseUrl
      .resolve(path.startsWith("/") ? path.replaceAll("^(/)+", "") : path)
      .newBuilder();
  }

  private static OkHttpClient prepareOkHttpClient(OkHttpClient okHttpClient, WsRequest wsRequest) {
    if (!wsRequest.getTimeOutInMs().isPresent()) {
      return okHttpClient;
    }

    return okHttpClient.newBuilder()
      .readTimeout(wsRequest.getTimeOutInMs().getAsInt(), TimeUnit.MILLISECONDS)
      .build();
  }

  private static void completeUrlQueryParameters(BaseRequest<?> request, HttpUrl.Builder urlBuilder) {
    request.getParameters().getKeys()
      .forEach(key -> request.getParameters().getValues(key)
        .forEach(value -> urlBuilder.addQueryParameter(key, value)));
  }

  private Request.Builder prepareOkRequestBuilder(WsRequest getRequest, HttpUrl.Builder urlBuilder) {
    Request.Builder okHttpRequestBuilder = new Request.Builder()
      .url(urlBuilder.build())
      .header("Accept", getRequest.getMediaType())
      .header("Accept-Charset", "UTF-8");
    if (systemPassCode != null) {
      okHttpRequestBuilder.header("X-Sonar-Passcode", systemPassCode);
    }
    getRequest.getHeaders().getNames().forEach(name -> okHttpRequestBuilder.header(name, getRequest.getHeaders().getValue(name).get()));
    return okHttpRequestBuilder;
  }

  private static Response doCall(OkHttpClient client, Request okRequest) {
    Call call = client.newCall(okRequest);
    try {
      return call.execute();
    } catch (IOException e) {
      throw new IllegalStateException("Fail to request " + okRequest.url(), e);
    }
  }

  private Response checkRedirect(Response response, PostRequest postRequest) {
    switch (response.code()) {
      case HTTP_MOVED_PERM:
      case HTTP_MOVED_TEMP:
      case HTTP_TEMP_REDIRECT:
      case HTTP_PERM_REDIRECT:
        // OkHttpClient does not follow the redirect with the same HTTP method. A POST is
        // redirected to a GET. Because of that the redirect must be manually implemented.
        // See:
        // https://github.com/square/okhttp/blob/07309c1c7d9e296014268ebd155ebf7ef8679f6c/okhttp/src/main/java/okhttp3/internal/http/RetryAndFollowUpInterceptor.java#L316
        // https://github.com/square/okhttp/issues/936#issuecomment-266430151
        return followPostRedirect(response, postRequest);
      default:
        return response;
    }
  }

  private Response followPostRedirect(Response response, PostRequest postRequest) {
    String location = response.header("Location");
    if (location == null) {
      throw new IllegalStateException(format("Missing HTTP header 'Location' in redirect of %s", response.request().url()));
    }
    HttpUrl url = response.request().url().resolve(location);

    // Don't follow redirects to unsupported protocols.
    if (url == null) {
      throw new IllegalStateException(format("Unsupported protocol in redirect of %s to %s", response.request().url(), location));
    }

    Request.Builder redirectRequest = response.request().newBuilder();
    redirectRequest.post(response.request().body());
    response.body().close();
    return doCall(prepareOkHttpClient(noRedirectOkHttpClient, postRequest), redirectRequest.url(url).build());
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
    private String systemPassCode;
    private int connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MILLISECONDS;
    private int readTimeoutMs = DEFAULT_READ_TIMEOUT_MILLISECONDS;
    private SSLSocketFactory sslSocketFactory = null;
    private X509TrustManager sslTrustManager = null;

    /**
     * Private since 5.5.
     *
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
     * Optional SSL socket factory with which SSL sockets will be created to establish SSL connections.
     * If not set, a default SSL socket factory will be used, base d on the JVM's default key store.
     */
    public Builder setSSLSocketFactory(@Nullable SSLSocketFactory sslSocketFactory) {
      this.sslSocketFactory = sslSocketFactory;
      return this;
    }

    /**
     * Optional SSL trust manager used to validate certificates.
     * If not set, a default system trust manager will be used, based on the JVM's default truststore.
     */
    public Builder setTrustManager(@Nullable X509TrustManager sslTrustManager) {
      this.sslTrustManager = sslTrustManager;
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

    public Builder systemPassCode(@Nullable String systemPassCode) {
      this.systemPassCode = systemPassCode;
      return this;
    }

    public HttpConnector build() {
      checkArgument(!isNullOrEmpty(url), "Server URL is not defined");
      return new HttpConnector(this);
    }
  }

}
