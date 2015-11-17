/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

package org.sonarqube.ws.client;

import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import javax.annotation.Nullable;

public class HttpConnector implements WsConnector {

  public static final int DEFAULT_CONNECT_TIMEOUT_MILLISECONDS = 30000;
  public static final int DEFAULT_READ_TIMEOUT_MILLISECONDS = 60000;

  /**
   * Visibility relaxed for unit tests
   */
  final HttpRequestFactory requestFactory;

  private HttpConnector(Builder builder) {
    this.requestFactory = new HttpRequestFactory(builder.url)
      .setLogin(builder.login)
      .setPassword(builder.password)
      .setProxyHost(builder.proxyHost)
      .setProxyPort(builder.proxyPort)
      .setProxyLogin(builder.proxyLogin)
      .setProxyPassword(builder.proxyPassword)
      .setConnectTimeoutInMilliseconds(builder.connectTimeoutMs)
      .setReadTimeoutInMilliseconds(builder.readTimeoutMs);
  }

  @Override
  public String execute(WsRequest wsRequest) {
    return requestFactory.execute(wsRequest);
  }

  @Override
  public <T extends Message> T execute(WsRequest wsRequest, Parser<T> protobufParser) {
    return requestFactory.execute(wsRequest, protobufParser);
  }

  /**
   * Create a builder of {@link WsClient}s.
   */
  public static Builder newHttpConnector() {
    return new Builder();
  }

  /**
   * Create a client with default configuration. Use {@link #newHttpConnector()} to define
   * a custom configuration (credentials, HTTP proxy, HTTP timeouts).
   */
  public static HttpConnector newDefaultHttpConnector(String serverUrl) {
    return newHttpConnector().url(serverUrl).build();
  }

  public static class Builder {
    private String login;
    private String password;
    private String url;
    private String proxyHost;
    private String proxyLogin;
    private String proxyPassword;
    private int proxyPort = 0;

    private int connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MILLISECONDS;
    private int readTimeoutMs = DEFAULT_READ_TIMEOUT_MILLISECONDS;

    private Builder() {
    }

    /**
     * Mandatory HTTP server URL, eg "http://localhost:9000"
     */
    public Builder url(String url) {
      this.url = url;
      return this;
    }

    /**
     * Optional login, for example "admin"
     */
    public Builder login(@Nullable String login) {
      this.login = login;
      return this;
    }

    /**
     * Optional password related to {@link #login(String)}, for example "admin"
     */
    public Builder password(@Nullable String password) {
      this.password = password;
      return this;
    }

    /**
     * Host and port of the optional HTTP proxy
     */
    public Builder proxy(@Nullable String proxyHost, int proxyPort) {
      this.proxyHost = proxyHost;
      this.proxyPort = proxyPort;
      return this;
    }

    public Builder proxyLogin(@Nullable String proxyLogin) {
      this.proxyLogin = proxyLogin;
      return this;
    }

    public Builder proxyPassword(@Nullable String proxyPassword) {
      this.proxyPassword = proxyPassword;
      return this;
    }

    /**
     * Sets a specified timeout value, in milliseconds, to be used when opening HTTP connection.
     * A timeout of zero is interpreted as an infinite timeout. Default value is {@link HttpConnector#DEFAULT_CONNECT_TIMEOUT_MILLISECONDS}
     */
    public Builder connectTimeoutMilliseconds(int i) {
      this.connectTimeoutMs = i;
      return this;
    }

    /**
     * Sets the read timeout to a specified timeout, in milliseconds.
     * A timeout of zero is interpreted as an infinite timeout. Default value is {@link HttpConnector#DEFAULT_READ_TIMEOUT_MILLISECONDS}
     */
    public Builder readTimeoutMilliseconds(int i) {
      this.readTimeoutMs = i;
      return this;
    }

    /**
     * Build a new client
     */
    public HttpConnector build() {
      if (url == null || "".equals(url)) {
        throw new IllegalStateException("Server URL must be set");
      }
      return new HttpConnector(this);
    }

  }
}
