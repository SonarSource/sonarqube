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
package org.sonar.updatecenter.server;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class HttpDownloader {
  private static Logger LOG = LoggerFactory.getLogger(HttpDownloader.class);

  private File outputDir;

  public HttpDownloader(File outputDir) {
    this.outputDir = outputDir;
  }

  public File download(String url, boolean force) throws IOException, URISyntaxException {
    return download(url, force, null, null);
  }
  public File download(String url, boolean force, String login, String password) throws IOException, URISyntaxException {
    FileUtils.forceMkdir(outputDir);

    String filename = StringUtils.substringAfterLast(url, "/");
    File output = new File(outputDir, filename);
    if (force || !output.exists() || output.length() <= 0) {
      downloadFile(new URI(url), output, login, password);
    } else {
      LOG.info("Already downloaded: " + url);
    }
    return output;
  }

  File downloadFile(URI fileURI, File toFile, String login, String password) {
    LOG.info("Download " + fileURI + " in " + toFile);
    DefaultHttpClient client = new DefaultHttpClient();
    try {
      if (StringUtils.isNotBlank(login)) {
        client.getCredentialsProvider().setCredentials(
            new AuthScope(fileURI.getHost(), fileURI.getPort()),
            new UsernamePasswordCredentials(login, password));
      }
      HttpGet httpget = new HttpGet(fileURI);
      byte[] data = client.execute(httpget, new ByteResponseHandler());
      if (data != null) {
        FileUtils.writeByteArrayToFile(toFile, data);
      }

    } catch (Exception e) {
      LOG.error("Fail to download " + fileURI + " to " + toFile, e);
      FileUtils.deleteQuietly(toFile);

    } finally {
      client.getConnectionManager().shutdown();
    }
    return toFile;
  }

  static class ByteResponseHandler implements ResponseHandler<byte[]> {
    public byte[] handleResponse(HttpResponse response) throws IOException {
      HttpEntity entity = response.getEntity();
      if (response.getStatusLine().getStatusCode()!=200) {
        throw new RuntimeException("Unvalid HTTP response: " + response.getStatusLine());
      }
      if (entity != null) {
        return EntityUtils.toByteArray(entity);
      }
      return null;
    }
  }
}
