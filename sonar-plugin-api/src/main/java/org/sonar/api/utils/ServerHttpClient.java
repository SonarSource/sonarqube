/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.utils;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchComponent;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @since 1.10
 * @deprecated use org.sonar.api.plaform.Server instead
 */
@Deprecated
public class ServerHttpClient implements BatchComponent {

  protected static final String SERVER_API_PATH = "/api/server";
  private static final String KEY_PATH = SERVER_API_PATH + "/key";
  private static final String VERSION_PATH = SERVER_API_PATH + "/version";
  protected static final String MAVEN_PATH = "/deploy/maven";
  private static final int CONNECT_TIMEOUT_MILLISECONDS = 30000;
  private static final int READ_TIMEOUT_MILLISECONDS = 60000;

  private String url;
  private Integer connectTimeoutMiliseconds = CONNECT_TIMEOUT_MILLISECONDS;
  private Integer readTimeoutMiliseconds = READ_TIMEOUT_MILLISECONDS;

  public ServerHttpClient(String remoteServerUrl) {
    this(remoteServerUrl, null, null);
  }

  public ServerHttpClient(String remoteServerUrl, Integer connectTimeoutMiliseconds, Integer readTimeoutMiliseconds) {
    this.url = StringUtils.chomp(remoteServerUrl, "/");
    if (connectTimeoutMiliseconds != null) {
      this.connectTimeoutMiliseconds = connectTimeoutMiliseconds;
    }
    if (readTimeoutMiliseconds != null) {
      this.readTimeoutMiliseconds = readTimeoutMiliseconds;
    }

  }

  public ServerHttpClient(Configuration configuration) {
    this(configuration.getString("sonar.host.url", "http://localhost:9000"),
        configuration.getInteger("sonar.host.connectTimeoutMs", CONNECT_TIMEOUT_MILLISECONDS),
        configuration.getInteger("sonar.host.readTimeoutMs", READ_TIMEOUT_MILLISECONDS));

  }

  /**
   * Throws a runtime ServerConnectionException if it fails to connect Sonar server
   */
  public void checkUp() {
    String exceptionLabel = "Sonar server at " + url +
        " is unreacheable. Either start it or setup the sonar.host.url maven setting if the URL is incorrect";
    if (getId() == null) {
      throw new ServerConnectionException(exceptionLabel);
    }
  }

  public String getId() {
    return executeAction(KEY_PATH);
  }

  public String getVersion() {
    return executeAction(VERSION_PATH);
  }

  public String getMavenRepositoryUrl() {
    return this.url + MAVEN_PATH;
  }

  protected String executeAction(String action) {
    String result = getRemoteContent(url + action);
    if (result.trim().length() == 0) {
      throw new ServerApiEmptyContentException("Empty " + action + " returned from server");
    }
    return result;
  }

  protected String getRemoteContent(String url) {
    HttpURLConnection conn = null;
    Reader reader = null;
    try {
      conn = getConnection(url, "GET");
      reader = new InputStreamReader((InputStream) conn.getContent());

      int statusCode = conn.getResponseCode();
      if (statusCode != HttpURLConnection.HTTP_OK) {
        throw new ServerConnectionException("Status returned by url : '" + url + "' is invalid : " + statusCode);
      }

      return IOUtils.toString(reader);
    } catch (IOException e) {
      throw new ServerConnectionException("url=" + url, e);

    } finally {
      IOUtils.closeQuietly(reader);
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

  public String getUrl() {
    return url;
  }

  private HttpURLConnection getConnection(String url, String method) throws IOException {
    URL page = new URL(url);
    HttpURLConnection conn = (HttpURLConnection) page.openConnection();
    conn.setConnectTimeout(connectTimeoutMiliseconds);
    conn.setReadTimeout(readTimeoutMiliseconds);

    conn.setRequestMethod(method);
    conn.connect();
    return conn;
  }

  public static class ServerApiEmptyContentException extends SonarException {

    public ServerApiEmptyContentException(String s) {
      super(s);
    }
  }

  public static class ServerConnectionException extends SonarException {

    public ServerConnectionException(String msg) {
      super(msg);
    }

    public ServerConnectionException(String msg, Throwable throwable) {
      super(msg, throwable);
    }

  }
}
