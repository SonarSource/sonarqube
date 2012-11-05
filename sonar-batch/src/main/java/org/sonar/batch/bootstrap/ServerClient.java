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
package org.sonar.batch.bootstrap;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.bootstrapper.EnvironmentInformation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * TODO extends Server when removing the deprecated org.sonar.batch.ServerMetadata
 *
 * @since 3.4
 */
public class ServerClient implements BatchComponent {
  private Settings settings;
  private HttpDownloader.BaseHttpDownloader downloader;

  public ServerClient(Settings settings, EnvironmentInformation env) {
    this.settings = settings;
    this.downloader = new HttpDownloader.BaseHttpDownloader(settings, env.toString());
  }

  public String getId() {
    return settings.getString(CoreProperties.SERVER_ID);
  }

  public String getVersion() {
    return settings.getString(CoreProperties.SERVER_VERSION);
  }

  public Date getStartedAt() {
    String dateString = settings.getString(CoreProperties.SERVER_STARTTIME);
    if (dateString != null) {
      try {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(dateString);

      } catch (ParseException e) {
        LoggerFactory.getLogger(getClass()).error("The property " + CoreProperties.SERVER_STARTTIME + " is badly formatted.", e);
      }
    }
    return null;
  }

  public String getURL() {
    return StringUtils.removeEnd(StringUtils.defaultIfBlank(settings.getString("sonar.host.url"), "http://localhost:9000"), "/");
  }

  public String getPermanentServerId() {
    return settings.getString(CoreProperties.PERMANENT_SERVER_ID);
  }

  public String getServerId() {
    String remoteServerInfo = request("/api/server");
    // don't use JSON utilities to extract ID from such a small string
    return extractServerId(remoteServerInfo);
  }

  @VisibleForTesting
  String extractServerId(String remoteServerInfo) {
    String partialId = StringUtils.substringAfter(remoteServerInfo, "\"id\":\"");
    return StringUtils.substringBefore(partialId, "\"");
  }

  public void download(String pathStartingWithSlash, File toFile) {
    try {
      InputSupplier<InputStream> inputSupplier = doRequest(pathStartingWithSlash);
      Files.copy(inputSupplier, toFile);
    } catch (Exception e) {
      throw new SonarException(String.format("Unable to download '%s' to: %s", pathStartingWithSlash, toFile), e);
    }
  }

  public String request(String pathStartingWithSlash) {
    InputSupplier<InputStream> inputSupplier = doRequest(pathStartingWithSlash);
    try {
      return IOUtils.toString(inputSupplier.getInput(), "UTF-8");
    } catch (IOException e) {
      throw new SonarException(String.format("Unable to request: %s", pathStartingWithSlash), e);
    }
  }

  private InputSupplier<InputStream> doRequest(String pathStartingWithSlash) {
    Preconditions.checkArgument(pathStartingWithSlash.startsWith("/"), "Path must start with slash /");

    URI uri = URI.create(getURL() + pathStartingWithSlash);
    String login = settings.getString(CoreProperties.LOGIN);

    try {
      InputSupplier<InputStream> inputSupplier;
      if (Strings.isNullOrEmpty(login)) {
        inputSupplier = downloader.newInputSupplier(uri);
      } else {
        inputSupplier = downloader.newInputSupplier(uri, login, settings.getString(CoreProperties.PASSWORD));
      }
      return inputSupplier;
    } catch (Exception e) {
      throw new SonarException(String.format("Unable to request: %s", uri), e);
    }
  }

}
