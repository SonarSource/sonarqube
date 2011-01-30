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
package org.sonar.batch.bootstrapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class BatchDownloader {

  private static final String VERSION_PATH = "/api/server/version";
  private static final String BATCH_PATH = "/batch/";

  public static final int CONNECT_TIMEOUT_MILLISECONDS = 30000;
  public static final int READ_TIMEOUT_MILLISECONDS = 60000;

  private String serverUrl;
  private String serverVersion;

  public BatchDownloader(String serverUrl) {
    if (serverUrl.endsWith("/")) {
      this.serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
    } else {
      this.serverUrl = serverUrl;
    }
  }

  /**
   * @return server url
   */
  public String getServerUrl() {
    return serverUrl;
  }

  /**
   * @return server version
   */
  public String getServerVersion() {
    if (serverVersion == null) {
      try {
        serverVersion = remoteContent(VERSION_PATH);
      } catch (IOException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
    return serverVersion;
  }

  /**
   * To use this method version of Sonar should be at least 2.6.
   * 
   * @return list of downloaded files
   */
  public List<File> downloadBatchFiles(File toDir) {
    try {
      List<File> files = new ArrayList<File>();

      String libs = remoteContent(BATCH_PATH);

      for (String lib : libs.split(",")) {
        File file = new File(toDir, lib);
        remoteContentToFile(BATCH_PATH + lib, file);
        files.add(file);
      }

      return files;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void remoteContentToFile(String path, File toFile) {
    InputStream input = null;
    FileOutputStream output = null;
    String fullUrl = serverUrl + path;
    try {
      HttpURLConnection connection = newHttpConnection(new URL(fullUrl));
      output = new FileOutputStream(toFile, false);
      input = connection.getInputStream();
      BootstrapperIOUtils.copyLarge(input, output);
    } catch (Exception e) {
      BootstrapperIOUtils.closeQuietly(output);
      BootstrapperIOUtils.deleteFileQuietly(toFile);
      throw new RuntimeException("Fail to download the file: " + fullUrl, e);
    } finally {
      BootstrapperIOUtils.closeQuietly(input);
      BootstrapperIOUtils.closeQuietly(output);
    }
  }

  String remoteContent(String path) throws IOException {
    String fullUrl = serverUrl + path;
    HttpURLConnection conn = newHttpConnection(new URL(fullUrl));
    Reader reader = new InputStreamReader((InputStream) conn.getContent());
    try {
      int statusCode = conn.getResponseCode();
      if (statusCode != HttpURLConnection.HTTP_OK) {
        throw new IOException("Status returned by url : '" + fullUrl + "' is invalid : " + statusCode);
      }
      return BootstrapperIOUtils.toString(reader);
    } finally {
      BootstrapperIOUtils.closeQuietly(reader);
      conn.disconnect();
    }
  }

  static HttpURLConnection newHttpConnection(URL url) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setConnectTimeout(CONNECT_TIMEOUT_MILLISECONDS);
    connection.setReadTimeout(READ_TIMEOUT_MILLISECONDS);
    connection.setInstanceFollowRedirects(true);
    connection.setRequestMethod("GET");
    // TODO connection.setRequestProperty("User-Agent", userAgent);
    return connection;
  }

}
