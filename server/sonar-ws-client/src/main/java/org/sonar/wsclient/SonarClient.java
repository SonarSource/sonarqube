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
package org.sonar.wsclient;

import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.issue.ActionPlanClient;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.internal.DefaultActionPlanClient;
import org.sonar.wsclient.issue.internal.DefaultIssueClient;
import org.sonar.wsclient.permissions.PermissionClient;
import org.sonar.wsclient.permissions.internal.DefaultPermissionClient;
import org.sonar.wsclient.project.ProjectClient;
import org.sonar.wsclient.project.internal.DefaultProjectClient;
import org.sonar.wsclient.qprofile.QProfileClient;
import org.sonar.wsclient.qprofile.internal.DefaultQProfileClient;
import org.sonar.wsclient.qualitygate.QualityGateClient;
import org.sonar.wsclient.qualitygate.internal.DefaultQualityGateClient;
import org.sonar.wsclient.system.SystemClient;
import org.sonar.wsclient.system.internal.DefaultSystemClient;
import org.sonar.wsclient.user.UserClient;
import org.sonar.wsclient.user.internal.DefaultUserClient;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Entry point of the Java Client for Sonar Web Services. It does not support all web services yet.
 * <p/>
 * Example:
 * <pre>
 *   SonarClient client = SonarClient.create("http://localhost:9000");
 *   IssueClient issueClient = client.issueClient();
 * </pre>
 *
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
    this(new HttpRequestFactory(builder.url)
      .setLogin(builder.login)
      .setPassword(builder.password)
      .setProxyHost(builder.proxyHost)
      .setProxyPort(builder.proxyPort)
      .setProxyLogin(builder.proxyLogin)
      .setProxyPassword(builder.proxyPassword)
      .setConnectTimeoutInMilliseconds(builder.connectTimeoutMs)
      .setReadTimeoutInMilliseconds(builder.readTimeoutMs));
  }

  /**
   * Visible for testing
   */
  SonarClient(HttpRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
  }

  /**
   * New client to interact with web services related to issues
   */
  public IssueClient issueClient() {
    return new DefaultIssueClient(requestFactory);
  }

  /**
   * New client to interact with web services related to issue action plans
   */
  public ActionPlanClient actionPlanClient() {
    return new DefaultActionPlanClient(requestFactory);
  }

  /**
   * New client to interact with web services related to users
   */
  public UserClient userClient() {
    return new DefaultUserClient(requestFactory);
  }

  /**
   * New client to interact with web services related to users and groups permissions
   */
  public PermissionClient permissionClient() {
    return new DefaultPermissionClient(requestFactory);
  }

  /**
   * New client to interact with web services related to projects
   */
  public ProjectClient projectClient() {
    return new DefaultProjectClient(requestFactory);
  }

  /**
   * New client to interact with web services related to quality gates
   */
  public QualityGateClient qualityGateClient() {
    return new DefaultQualityGateClient(requestFactory);
  }

  /**
   * New client to interact with web services related to quality profiles
   */
  public QProfileClient qProfileClient() {
    return new DefaultQProfileClient(requestFactory);
  }

  public SystemClient systemClient() {
    return new DefaultSystemClient(requestFactory);
  }

  /**
   * Create a builder of {@link SonarClient}s.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Create a client with default configuration. Use {@link #builder()} to define
   * a custom configuration (credentials, HTTP proxy, HTTP timeouts).
   */
  public static SonarClient create(String serverUrl) {
    return builder().url(serverUrl).build();
  }

  /**
   * Send a POST request on the given relativeUrl, with provided parameters (can be empty).
   * The beginning slash (/) of relativeUrl is supported but not mandatory.
   * <p/>
   * Example:
   * <pre>  {@code
   *   Map<String,Object> params = new HashMap<>();
   *   params.put("name", "My Quality Gate");
   *   client.post("api/qualitygates/create", params);
   * }</pre>
   * @since 4.5
   * @return the response body
   */
  public String post(String relativeUrl, Map<String, Object> params) {
    return requestFactory.post(relativeUrl, params);
  }

  /**
   * Same as {@link #post(String, java.util.Map)} but parameters are defined as an array
   * of even number of elements (key1, value1, key, value2, ...). Keys must not be null.
   */
  public String post(String relativeUrl, Object... params) {
    return post(relativeUrl, paramsAsMap(params));
  }

  /**
   * Send a GET request on the given relativeUrl, with provided parameters (can be empty).
   * The beginning slash (/) of relativeUrl is supported but not mandatory.
   * @since 4.5
   * @return the response body
   */
  public String get(String relativeUrl, Map<String, Object> params) {
    return requestFactory.get(relativeUrl, params);
  }

  /**
   * Same as {@link #get(String, java.util.Map)} but parameters are defined as an array
   * of even number of elements (key1, value1, key, value2, ...). Keys must not be null.
   */
  public String get(String relativeUrl, Object... params) {
    return get(relativeUrl, paramsAsMap(params));
  }

  private Map<String, Object> paramsAsMap(Object[] params) {
    if (params.length % 2 == 1) {
      throw new IllegalArgumentException(String.format(
        "Expecting even number of elements. Got %s", Arrays.toString(params)));
    }
    Map<String, Object> map = new HashMap<String, Object>();
    for (int index = 0; index < params.length; index++) {
      if (params[index] == null) {
        throw new IllegalArgumentException(String.format(
          "Parameter key can't be null at index %d of %s", index, Arrays.toString(params)));
      }
      map.put(params[index].toString(), params[index + 1]);
      index++;
    }
    return map;
  }

  public static class Builder {
    private String login, password, url, proxyHost, proxyLogin, proxyPassword;
    private int proxyPort = 0;
    private int connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MILLISECONDS, readTimeoutMs = DEFAULT_READ_TIMEOUT_MILLISECONDS;

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

    /**
     * Build a new client
     */
    public SonarClient build() {
      if (url == null || "".equals(url)) {
        throw new IllegalStateException("Server URL must be set");
      }
      return new SonarClient(this);
    }
  }
}
