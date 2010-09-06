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

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.platform.Server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

/**
 * Simple class to download a file from a HTTP repository.
 *
 * @since 2.2
 */
public class HttpDownloader implements BatchComponent, ServerComponent {

  public static final int TIMEOUT_MILLISECONDS = 20 * 1000;

  private Server server = null;

  public HttpDownloader(Server server) {
    this.server = server;
  }

  public HttpDownloader() {
  }

  public void download(URI uri, File toFile) {
    InputStream input = null;
    FileOutputStream output = null;
    try {
      HttpURLConnection connection = newHttpConnection(uri);
      output = new FileOutputStream(toFile, false);
      input = connection.getInputStream();
      IOUtils.copy(input, output);

    } catch (Exception e) {
      throw new SonarException("Fail to download the file: " + uri, e);
      
    } finally {
      IOUtils.closeQuietly(input);
      IOUtils.closeQuietly(output);
    }
  }

  public byte[] download(URI uri) {
    InputStream input = null;
    try {
      HttpURLConnection connection = newHttpConnection(uri);
      input = connection.getInputStream();
      return IOUtils.toByteArray(input);

    } catch (Exception e) {
      throw new SonarException("Fail to download the file: " + uri, e);

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  public InputStream openStream(URI uri) {
    try {
      HttpURLConnection connection = newHttpConnection(uri);
      return connection.getInputStream();

    } catch (Exception e) {
      throw new SonarException("Fail to download the file: " + uri, e);
    }
  }

  private HttpURLConnection newHttpConnection(URI uri) throws IOException {
    LoggerFactory.getLogger(getClass()).info("Download: " + uri);
    HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
    connection.setConnectTimeout(TIMEOUT_MILLISECONDS);
    connection.setReadTimeout(TIMEOUT_MILLISECONDS);
    connection.setUseCaches(true);
    connection.setRequestProperty("User-Agent", getUserAgent());
    connection.setInstanceFollowRedirects(true);
    return connection;
  }

  private String getUserAgent() {
    return (server != null ? "Sonar " + server.getVersion() : "Sonar");
  }
}
