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
package org.sonar.core.util;

import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.SonarException;
import org.sonarqube.ws.client.OkHttpClientBuilder;

import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.sonar.core.util.FileUtils.deleteQuietly;

/**
 * This component downloads HTTP files
 *
 * @since 2.2
 */
public class DefaultHttpDownloader extends HttpDownloader {

  private final OkHttpClient client;

  @Inject
  public DefaultHttpDownloader(Server server, Configuration config) {
    this(server, config, null, null);
  }

  public DefaultHttpDownloader(Server server, Configuration config, @Nullable Integer connectTimeout, @Nullable Integer readTimeout) {
    client = buildHttpClient(server, config, connectTimeout, readTimeout);
  }

  private static OkHttpClient buildHttpClient(Server server, Configuration config, @Nullable Integer connectTimeout,
    @Nullable Integer readTimeout) {
    OkHttpClientBuilder clientBuilder = new OkHttpClientBuilder()
      .setFollowRedirects(true)
      .setUserAgent(getUserAgent(server, config));
    if (connectTimeout != null) {
      clientBuilder
        .setConnectTimeoutMs(connectTimeout);
    }
    if (readTimeout != null) {
      clientBuilder
        .setReadTimeoutMs(readTimeout);
    }
    return clientBuilder.build();
  }

  private static String getUserAgent(Server server, Configuration config) {
    Optional<String> serverId = config.get(CoreProperties.SERVER_ID);
    if (serverId.isEmpty()) {
      return String.format("SonarQube %s #", server.getVersion());
    }
    return String.format("SonarQube %s # %s", server.getVersion(), serverId.get());
  }

  @Override
  protected String description(URI uri) {
    return uri.toString();
  }

  @Override
  protected String[] getSupportedSchemes() {
    return new String[] {"http", "https"};
  }

  @Override
  protected byte[] readBytes(URI uri) {
    return download(uri);
  }

  @Override
  protected String readString(URI uri, Charset charset) {
    try (Response response = executeCall(uri)) {
      return IOUtils.toString(response.body().byteStream(), charset);
    } catch (IOException e) {
      throw failToDownload(uri, e);
    }
  }

  @Override
  public String downloadPlainText(URI uri, String encoding) {
    return readString(uri, Charset.forName(encoding));
  }

  @Override
  public byte[] download(URI uri) {
    try (Response response = executeCall(uri)) {
      return ByteStreams.toByteArray(response.body().byteStream());
    } catch (IOException e) {
      throw failToDownload(uri, e);
    }
  }

  @Override
  public InputStream openStream(URI uri) {
    try {
      Response response = executeCall(uri);
      return response.body().byteStream();
    } catch (IOException e) {
      throw failToDownload(uri, e);
    }
  }

  @Override
  public void download(URI uri, File toFile) {
    try (Response response = executeCall(uri)) {
      copyInputStreamToFile(response.body().byteStream(), toFile);
    } catch (IOException e) {
      deleteQuietly(toFile);
      throw failToDownload(uri, e);
    }
  }

  private Response executeCall(URI uri) throws IOException {
    Request request = new Request.Builder().url(uri.toURL()).get().build();
    return client.newCall(request).execute();
  }

  private static SonarException failToDownload(URI uri, IOException e) {
    throw new SonarException(String.format("Fail to download: %s", uri), e);
  }

}
