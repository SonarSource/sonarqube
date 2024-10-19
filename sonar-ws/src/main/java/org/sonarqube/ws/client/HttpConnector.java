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

import java.io.IOException;
import java.net.Proxy;
import java.util.List;
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
import org.sonarqube.ws.client.RequestWithPayload.Part;

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
 * <p>The JVM system proxies are used.</p>
 */
public class HttpConnector implements WsConnector {

  public static final int DEFAULT_CONNECT_TIMEOUT_MILLISECONDS = 30_000;
  public static final int DEFAULT_READ_TIMEOUT_MILLISECONDS = 60_000;
  public static final int DEFAULT_RESPONSE_TIMEOUT_MILLISECONDS = 0;
  private static final String JSON = "application/json; charset=utf-8";

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
    okHttpClientBuilder.setResponseTimeoutMs(builder.responseTimeoutMs);
    okHttpClientBuilder.setReadTimeoutMs(builder.readTimeoutMs);
    okHttpClientBuilder.setSSLSocketFactory(builder.sslSocketFactory);
    okHttpClientBuilder.setTrustManager(builder.sslTrustManager);
    okHttpClientBuilder.acceptGzip(builder.acceptGzip);

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
    if (httpRequest instanceof RequestWithoutPayload httpRequestWithoutPayload) {
      return executeRequest(httpRequestWithoutPayload);
    }
    if (httpRequest instanceof RequestWithPayload httpRequestWithPayload) {
      return executeRequest(httpRequestWithPayload);
    }
    throw new IllegalArgumentException(format("Unsupported implementation: %s", httpRequest.getClass()));
  }

  private WsResponse executeRequest(RequestWithoutPayload<?> request) {
    HttpUrl.Builder urlBuilder = prepareUrlBuilder(request);
    completeUrlQueryParameters(request, urlBuilder);

    Request.Builder okRequestBuilder = prepareOkRequestBuilder(request, urlBuilder);
    okRequestBuilder = request.addVerbToBuilder().apply(okRequestBuilder);
    return new OkHttpResponse(doCall(prepareOkHttpClient(okHttpClient, request), okRequestBuilder.build()));
  }

  private WsResponse executeRequest(RequestWithPayload<?> request) {
    HttpUrl.Builder urlBuilder = prepareUrlBuilder(request);

    RequestBody body;
    Map<String, Part> parts = request.getParts();
    if (request.hasBody()) {
      MediaType contentType = MediaType.parse(request.getContentType().orElse(JSON));
      body = RequestBody.create(contentType, request.getBody());
    } else if (parts.isEmpty()) {
      // parameters are defined in the body (application/x-www-form-urlencoded)
      FormBody.Builder formBody = new FormBody.Builder();
      request.getParameters().getKeys()
        .forEach(key -> request.getParameters().getValues(key)
          .forEach(value -> formBody.add(key, value)));
      body = formBody.build();

    } else {
      // parameters are defined in the URL (as GET)
      completeUrlQueryParameters(request, urlBuilder);

      MultipartBody.Builder bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
      parts.entrySet().forEach(param -> {
        Part part = param.getValue();
        bodyBuilder.addFormDataPart(
          param.getKey(),
          part.getFile().getName(),
          RequestBody.create(MediaType.parse(part.getMediaType()), part.getFile()));
      });
      body = bodyBuilder.build();
    }
    Request.Builder okRequestBuilder = prepareOkRequestBuilder(request, urlBuilder);
    okRequestBuilder = request.addVerbToBuilder(body).apply(okRequestBuilder);
    Response response = doCall(prepareOkHttpClient(noRedirectOkHttpClient, request), okRequestBuilder.build());
    response = checkRedirect(response, request);
    return new OkHttpResponse(response);
  }

  private HttpUrl.Builder prepareUrlBuilder(WsRequest wsRequest) {
    String path = wsRequest.getPath();
    return baseUrl
      .resolve(path.startsWith("/") ? path.replaceAll("^(/)+", "") : path)
      .newBuilder();
  }

  static OkHttpClient prepareOkHttpClient(OkHttpClient okHttpClient, WsRequest wsRequest) {
    if (!wsRequest.getTimeOutInMs().isPresent() && !wsRequest.getWriteTimeOutInMs().isPresent()) {
      return okHttpClient;
    }
    OkHttpClient.Builder builder = okHttpClient.newBuilder();
    if (wsRequest.getTimeOutInMs().isPresent()) {
      builder.readTimeout(wsRequest.getTimeOutInMs().getAsInt(), TimeUnit.MILLISECONDS);
    }
    if (wsRequest.getWriteTimeOutInMs().isPresent()) {
      builder.writeTimeout(wsRequest.getWriteTimeOutInMs().getAsInt(), TimeUnit.MILLISECONDS);
    }

    return builder.build();
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
      throw new IllegalStateException("Fail to request url: " + okRequest.url(), e);
    }
  }

  private Response checkRedirect(Response response, RequestWithPayload<?> postRequest) {
    if (List.of(HTTP_MOVED_PERM, HTTP_MOVED_TEMP, HTTP_TEMP_REDIRECT, HTTP_PERM_REDIRECT).contains(response.code())) {
      // OkHttpClient does not follow the redirect with the same HTTP method. A POST is
      // redirected to a GET. Because of that the redirect must be manually implemented.
      // See:
      // https://github.com/square/okhttp/blob/07309c1c7d9e296014268ebd155ebf7ef8679f6c/okhttp/src/main/java/okhttp3/internal/http/RetryAndFollowUpInterceptor.java#L316
      // https://github.com/square/okhttp/issues/936#issuecomment-266430151
      return followPostRedirect(response, postRequest);
    } else {
      return response;
    }
  }

  private Response followPostRedirect(Response response, RequestWithPayload<?> postRequest) {
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
    private int responseTimeoutMs = DEFAULT_RESPONSE_TIMEOUT_MILLISECONDS;
    private SSLSocketFactory sslSocketFactory = null;
    private X509TrustManager sslTrustManager = null;
    private boolean acceptGzip = false;

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
     * This flag decides whether the client should accept GZIP encoding. Default is false.
     */
    public Builder acceptGzip(boolean acceptGzip) {
      this.acceptGzip = acceptGzip;
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

    /**
     * Sets the response timeout to a specified timeout, in milliseconds.
     * A timeout of zero is interpreted as an infinite timeout. Default value is {@link #DEFAULT_RESPONSE_TIMEOUT_MILLISECONDS}
     */
    public Builder responseTimeoutMilliseconds(int i) {
      this.responseTimeoutMs = i;
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
