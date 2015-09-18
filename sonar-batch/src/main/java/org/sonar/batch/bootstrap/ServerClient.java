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
package org.sonar.batch.bootstrap;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.core.util.DefaultHttpDownloader;

/**
 * Replace the deprecated org.sonar.batch.ServerMetadata
 * TODO extends Server when removing the deprecated org.sonar.batch.ServerMetadata
 *
 * @since 3.4
 */
@BatchSide
public class ServerClient {
  private static final String GET = "GET";
  private GlobalProperties props;
  private DefaultHttpDownloader.BaseHttpDownloader downloader;

  public ServerClient(GlobalProperties settings, EnvironmentInformation env) {
    this.props = settings;
    this.downloader = new DefaultHttpDownloader.BaseHttpDownloader(settings.properties(), env.toString());
  }

  public String getURL() {
    return StringUtils.removeEnd(StringUtils.defaultIfBlank(props.property("sonar.host.url"), "http://localhost:9000"), "/");
  }

  public URI getURI(String pathStartingWithSlash) {
    Preconditions.checkArgument(pathStartingWithSlash.startsWith("/"), "Path must start with slash /: " + pathStartingWithSlash);
    String path = StringEscapeUtils.escapeHtml(pathStartingWithSlash);
    return URI.create(getURL() + path);
  }

  public void download(String pathStartingWithSlash, File toFile) {
    download(pathStartingWithSlash, toFile, null, null);
  }

  public void download(String pathStartingWithSlash, File toFile, @Nullable Integer connectTimeoutMillis, @Nullable Integer readTimeoutMillis) {
    try {
      InputStream is = load(pathStartingWithSlash, GET, false, connectTimeoutMillis, readTimeoutMillis);
      Files.copy(is, toFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (HttpDownloader.HttpException he) {
      throw handleHttpException(he);
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Unable to download '%s' to: %s", pathStartingWithSlash, toFile), e);
    }
  }

  public String downloadString(String pathStartingWithSlash) {
    return downloadString(pathStartingWithSlash, GET, true, null);
  }

  public String downloadString(String pathStartingWithSlash, String requestMethod, boolean wrapHttpException, @Nullable Integer timeoutMillis) {
    InputStream is = load(pathStartingWithSlash, requestMethod, wrapHttpException, null, timeoutMillis);
    try {
      return new String(IOUtils.toByteArray(is), "UTF-8");
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Unable to request: %s", pathStartingWithSlash), e);
    }
  }

  /**
   * @throws IllegalStateException on I/O error, not limited to the network connection and if HTTP response code > 400 and wrapHttpException is true
   * @throws HttpDownloader.HttpException if HTTP response code > 400 and wrapHttpException is false
   */
  public InputStream load(String pathStartingWithSlash, String requestMethod, boolean wrapHttpException, @Nullable Integer connectTimeoutMs,
    @Nullable Integer readTimeoutMs) {
    URI uri = getURI(pathStartingWithSlash);

    try {
      if (Strings.isNullOrEmpty(getLogin())) {
        return downloader.newInputSupplier(uri, requestMethod, connectTimeoutMs, readTimeoutMs).getInput();
      } else {
        return downloader.newInputSupplier(uri, requestMethod, getLogin(), getPassword(), connectTimeoutMs, readTimeoutMs).getInput();
      }
    } catch (HttpDownloader.HttpException e) {
      if (wrapHttpException) {
        throw handleHttpException(e);
      } else {
        throw e;
      }
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Unable to request: %s", uri), e);
    }
  }

  public RuntimeException handleHttpException(HttpDownloader.HttpException he) {
    if (he.getResponseCode() == 401) {
      return new IllegalStateException(String.format(getMessageWhenNotAuthorized(), CoreProperties.LOGIN, CoreProperties.PASSWORD), he);
    }
    if (he.getResponseCode() == 403) {
      // SONAR-4397 Details are in response content
      return new IllegalStateException(tryParseAsJsonError(he.getResponseContent()), he);
    }
    return new IllegalStateException(String.format("Fail to execute request [code=%s, url=%s]", he.getResponseCode(), he.getUri()), he);
  }

  private static String tryParseAsJsonError(String responseContent) {
    try {
      JsonParser parser = new JsonParser();
      JsonObject obj = parser.parse(responseContent).getAsJsonObject();
      JsonArray errors = obj.getAsJsonArray("errors");
      List<String> errorMessages = new ArrayList<>();
      for (JsonElement e : errors) {
        errorMessages.add(e.getAsJsonObject().get("msg").getAsString());
      }
      return Joiner.on(", ").join(errorMessages);
    } catch (Exception e) {
      return responseContent;
    }
  }

  public String getMessageWhenNotAuthorized() {
    if (Strings.isNullOrEmpty(getLogin()) && Strings.isNullOrEmpty(getPassword())) {
      return "Not authorized. Analyzing this project requires to be authenticated. Please provide the values of the properties %s and %s.";
    }
    return "Not authorized. Please check the properties %s and %s.";
  }

  public String getLogin() {
    return props.property(CoreProperties.LOGIN);
  }

  public String getPassword() {
    return props.property(CoreProperties.PASSWORD);
  }
}
