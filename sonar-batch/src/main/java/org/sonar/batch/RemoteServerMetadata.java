/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.batch;

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.util.IOUtil;
import org.sonar.api.BatchComponent;
import org.sonar.api.platform.Server;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RemoteServerMetadata implements BatchComponent {

  public static final int CONNECT_TIMEOUT_MILLISECONDS = 30000;
  public static final int READ_TIMEOUT_MILLISECONDS = 60000;

  private String serverUrl;

  public RemoteServerMetadata(Server server) {
    serverUrl = server.getURL();
    if (serverUrl.endsWith("/")) {
      serverUrl = StringUtils.chop(serverUrl);
    }
  }

  public String getServerId() throws IOException {
    String remoteServerInfo = remoteContent("/api/server");
    // don't use JSON utilities to extract ID from such a small string
    return extractId(remoteServerInfo);
  }

  protected String extractId(String remoteServerInfo) {
    String partialId = StringUtils.substringAfter(remoteServerInfo, "\"id\":\"");
    return StringUtils.substringBefore(partialId, "\"");
  }

  protected String getUrlFor(String path) {
    return serverUrl + path;
  }

  protected String remoteContent(String path) throws IOException {
    String fullUrl = getUrlFor(path);
    HttpURLConnection conn = getConnection(fullUrl, "GET");
    InputStream input = (InputStream) conn.getContent();
    try {
      int statusCode = conn.getResponseCode();
      if (statusCode != HttpURLConnection.HTTP_OK) {
        throw new IOException("Status returned by url : '" + fullUrl + "' is invalid : " + statusCode);
      }
      return IOUtil.toString(input);

    } finally {
      IOUtil.close(input);
      conn.disconnect();
    }
  }

  static HttpURLConnection getConnection(String url, String method) throws IOException {
    URL page = new URL(url);
    HttpURLConnection conn = (HttpURLConnection) page.openConnection();
    conn.setConnectTimeout(CONNECT_TIMEOUT_MILLISECONDS);
    conn.setReadTimeout(READ_TIMEOUT_MILLISECONDS);
    conn.setRequestMethod(method);
    conn.connect();
    return conn;
  }

}
