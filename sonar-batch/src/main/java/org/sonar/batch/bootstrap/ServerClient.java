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
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchSide;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.core.util.DefaultHttpDownloader;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Replace the deprecated org.sonar.batch.ServerMetadata
 * TODO extends Server when removing the deprecated org.sonar.batch.ServerMetadata
 *
 * @since 3.4
 */
@BatchSide
public class ServerClient {

  private static final String GET = "GET";
  private BootstrapProperties props;
  private DefaultHttpDownloader.BaseHttpDownloader downloader;

  public ServerClient(BootstrapProperties settings, EnvironmentInformation env) {
    this.props = settings;
    this.downloader = new DefaultHttpDownloader.BaseHttpDownloader(settings.properties(), env.toString());
  }

  public String getURL() {
    return StringUtils.removeEnd(StringUtils.defaultIfBlank(props.property("sonar.host.url"), "http://localhost:9000"), "/");
  }

  public void download(String pathStartingWithSlash, File toFile) {
    download(pathStartingWithSlash, toFile, null);
  }

  public void download(String pathStartingWithSlash, File toFile, @Nullable Integer readTimeoutMillis) {
    try {
      InputSupplier<InputStream> inputSupplier = doRequest(pathStartingWithSlash, GET, readTimeoutMillis);
      Files.copy(inputSupplier, toFile);
    } catch (HttpDownloader.HttpException he) {
      throw handleHttpException(he);
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Unable to download '%s' to: %s", pathStartingWithSlash, toFile), e);
    }
  }

  public String request(String pathStartingWithSlash) {
    return request(pathStartingWithSlash, GET, true);
  }

  public String request(String pathStartingWithSlash, String requestMethod) {
    return request(pathStartingWithSlash, requestMethod, true);
  }

  public String request(String pathStartingWithSlash, boolean wrapHttpException) {
    return request(pathStartingWithSlash, GET, wrapHttpException, null);
  }

  public String request(String pathStartingWithSlash, String requestMethod, boolean wrapHttpException) {
    return request(pathStartingWithSlash, requestMethod, wrapHttpException, null);
  }

  public String request(String pathStartingWithSlash, String requestMethod, boolean wrapHttpException, @Nullable Integer timeoutMillis) {
    InputSupplier<InputStream> inputSupplier = doRequest(pathStartingWithSlash, requestMethod, timeoutMillis);
    try {
      return IOUtils.toString(inputSupplier.getInput(), "UTF-8");
    } catch (HttpDownloader.HttpException e) {
      throw wrapHttpException ? handleHttpException(e) : e;
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Unable to request: %s", pathStartingWithSlash), e);
    }
  }

  public InputSupplier<InputStream> doRequest(String pathStartingWithSlash, String requestMethod, @Nullable Integer timeoutMillis) {
    Preconditions.checkArgument(pathStartingWithSlash.startsWith("/"), "Path must start with slash /");
    String path = StringEscapeUtils.escapeHtml(pathStartingWithSlash);

    URI uri = URI.create(getURL() + path);
    try {
      InputSupplier<InputStream> inputSupplier;
      if (Strings.isNullOrEmpty(getLogin())) {
        inputSupplier = downloader.newInputSupplier(uri, requestMethod, timeoutMillis);
      } else {
        inputSupplier = downloader.newInputSupplier(uri, requestMethod, getLogin(), getPassword(), timeoutMillis);
      }
      return inputSupplier;
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Unable to request: %s", uri), e);
    }
  }

  public RuntimeException handleHttpException(HttpDownloader.HttpException he) {
    if (he.getResponseCode() == 401) {
      return new IllegalStateException(String.format(getMessageWhenNotAuthorized(), CoreProperties.LOGIN, CoreProperties.PASSWORD));
    }
    if (he.getResponseCode() == 403) {
      // SONAR-4397 Details are in response content
      return new IllegalStateException(tryParseAsJsonError(he.getResponseContent()));
    }
    return new IllegalStateException(String.format("Fail to execute request [code=%s, url=%s]", he.getResponseCode(), he.getUri()), he);
  }

  private String tryParseAsJsonError(String responseContent) {
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

  public static String encodeForUrl(String url) {
    try {
      return URLEncoder.encode(url, "UTF-8");

    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Encoding not supported", e);
    }
  }

}
