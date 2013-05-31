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
package org.sonar.wsclient;

import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.issue.ActionPlanClient;
import org.sonar.wsclient.issue.DefaultActionPlanClient;
import org.sonar.wsclient.issue.DefaultIssueClient;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.user.DefaultUserClient;
import org.sonar.wsclient.user.UserClient;

import javax.annotation.Nullable;

/**
 * @since 3.6
 */
public class SonarClient {

  public static final int DEFAULT_CONNECT_TIMEOUT_MILLISECONDS = 30000;
  public static final int DEFAULT_READ_TIMEOUT_MILLISECONDS = 60000;

  /**
   * Visibility relaxed for unit tests
   */
  final HttpRequestFactory requestFactory;

  private SonarClient(Builder builder) {
    requestFactory = new HttpRequestFactory(builder.url)
      .setLogin(builder.login)
      .setPassword(builder.password)
      .setProxyHost(builder.proxyHost)
      .setProxyPort(builder.proxyPort)
      .setProxyLogin(builder.proxyLogin)
      .setProxyPassword(builder.proxyPassword)
      .setConnectTimeoutInMilliseconds(builder.connectTimeoutMs)
      .setReadTimeoutInMilliseconds(builder.readTimeoutMs);
  }

  public IssueClient issueClient() {
    return new DefaultIssueClient(requestFactory);
  }

  public ActionPlanClient actionPlanClient() {
    return new DefaultActionPlanClient(requestFactory);
  }

  public UserClient userClient() {
    return new DefaultUserClient(requestFactory);
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Create a client with default configuration. Use {@link #builder()} to define a custom configuration.
   */
  public static SonarClient create(String serverUrl) {
    return builder().url(serverUrl).build();
  }

  public static class Builder {
    private String login, password, url, proxyHost, proxyLogin, proxyPassword;
    private int proxyPort = 0;
    private int connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MILLISECONDS, readTimeoutMs = DEFAULT_READ_TIMEOUT_MILLISECONDS;

    private Builder() {
    }

    public Builder url(String url) {
      this.url = url;
      return this;
    }

    public Builder login(@Nullable String login) {
      this.login = login;
      return this;
    }

    public Builder password(@Nullable String password) {
      this.password = password;
      return this;
    }

    public Builder proxy(String proxyHost, int proxyPort) {
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
     * A timeout of zero is interpreted as an infinite timeout. Default value is {@link SonarClient#DEFAULT_CONNECT_TIMEOUT_MILLISECONDS}
     */
    public Builder connectTimeoutMilliseconds(int i) {
      this.connectTimeoutMs = i;
      return this;
    }

    /**
     * Sets the read timeout to a specified timeout, in milliseconds.
     * A timeout of zero is interpreted as an infinite timeout. Default value is {@link SonarClient#DEFAULT_READ_TIMEOUT_MILLISECONDS}
     */
    public Builder readTimeoutMilliseconds(int i) {
      this.readTimeoutMs = i;
      return this;
    }

    public SonarClient build() {
      if (url == null || "".equals(url)) {
        throw new IllegalStateException("Server URL must be set");
      }
      return new SonarClient(this);
    }
  }
}
