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
package org.sonar.batch.bootstrap;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.bootstrapper.EnvironmentInformation;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Replace the deprecated org.sonar.batch.ServerMetadata
 * TODO extends Server when removing the deprecated org.sonar.batch.ServerMetadata
 *
 * @since 3.4
 */
public class ServerClient implements BatchComponent {

  private BootstrapSettings settings;
  private HttpDownloader.BaseHttpDownloader downloader;

  public ServerClient(BootstrapSettings settings, EnvironmentInformation env) {
    this.settings = settings;
    this.downloader = new HttpDownloader.BaseHttpDownloader(settings.properties(), env.toString());
  }

  public String getURL() {
    return StringUtils.removeEnd(settings.property("sonar.host.url", "http://localhost:9000"), "/");
  }

  public void download(String pathStartingWithSlash, File toFile) {
    download(pathStartingWithSlash, toFile, null);
  }

  public void download(String pathStartingWithSlash, File toFile, @Nullable Integer readTimeoutMillis) {
    try {
      InputSupplier<InputStream> inputSupplier = doRequest(pathStartingWithSlash, readTimeoutMillis);
      Files.copy(inputSupplier, toFile);
    } catch (HttpDownloader.HttpException he) {
      throw handleHttpException(he);
    } catch (IOException e) {
      throw new SonarException(String.format("Unable to download '%s' to: %s", pathStartingWithSlash, toFile), e);
    }
  }

  public String request(String pathStartingWithSlash) {
    return request(pathStartingWithSlash, true);
  }

  public String request(String pathStartingWithSlash, boolean wrapHttpException) {
    return request(pathStartingWithSlash, wrapHttpException, null);
  }

  public String request(String pathStartingWithSlash, boolean wrapHttpException, Integer timeoutMillis) {
    InputSupplier<InputStream> inputSupplier = doRequest(pathStartingWithSlash, timeoutMillis);
    try {
      return IOUtils.toString(inputSupplier.getInput(), "UTF-8");
    } catch (HttpDownloader.HttpException e) {
      throw (wrapHttpException ? handleHttpException(e) : e);
    } catch (IOException e) {
      throw new SonarException(String.format("Unable to request: %s", pathStartingWithSlash), e);
    }
  }

  private InputSupplier<InputStream> doRequest(String pathStartingWithSlash, @Nullable Integer timeoutMillis) {
    Preconditions.checkArgument(pathStartingWithSlash.startsWith("/"), "Path must start with slash /");
    String path = StringEscapeUtils.escapeHtml(pathStartingWithSlash);

    URI uri = URI.create(getURL() + path);
    try {
      InputSupplier<InputStream> inputSupplier;
      if (Strings.isNullOrEmpty(getLogin())) {
        inputSupplier = downloader.newInputSupplier(uri, timeoutMillis);
      } else {
        inputSupplier = downloader.newInputSupplier(uri, getLogin(), getPassword(), timeoutMillis);
      }
      return inputSupplier;
    } catch (Exception e) {
      throw new SonarException(String.format("Unable to request: %s", uri), e);
    }
  }

  private RuntimeException handleHttpException(HttpDownloader.HttpException he) {
    if (he.getResponseCode() == 401) {
      return new SonarException(String.format(getMessageWhenNotAuthorized(), CoreProperties.LOGIN, CoreProperties.PASSWORD));
    }
    if (he.getResponseCode() == 403) {
      // SONAR-4397 Details are in response content
      return new SonarException(he.getResponseContent());
    }
    return new SonarException(String.format("Fail to execute request [code=%s, url=%s]", he.getResponseCode(), he.getUri()), he);
  }

  private String getMessageWhenNotAuthorized() {
    if (Strings.isNullOrEmpty(getLogin()) && Strings.isNullOrEmpty(getPassword())) {
      return "Not authorized. Analyzing this project requires to be authenticated. Please provide the values of the properties %s and %s.";
    }
    return "Not authorized. Please check the properties %s and %s.";
  }

  private String getLogin() {
    return settings.property(CoreProperties.LOGIN);
  }

  private String getPassword() {
    return settings.property(CoreProperties.PASSWORD);
  }
}
