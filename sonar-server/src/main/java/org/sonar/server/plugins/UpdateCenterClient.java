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
package org.sonar.server.plugins;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.Logs;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.UpdateCenterDeserializer;

import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

/**
 * HTTP client to load data from the remote update center hosted at http://update.sonarsource.org.
 * @since 2.2
 */
public class UpdateCenterClient implements ServerComponent {

  public static final String URL_PROPERTY = "sonar.updatecenter.url";
  public static final String DEFAULT_URL = "http://update.sonarsource.org/update-center.properties";
  public static final int PERIOD_IN_MILLISECONDS = 60 * 60 * 1000;

  private String url;
  private UpdateCenter center = null;
  private long downloadDate = 0;
  private HttpDownloader downloader;

  /**
   * for unit tests
   */
  UpdateCenterClient(HttpDownloader downloader, String url) {
    this.downloader = downloader;
    this.url = url;
    Logs.INFO.info("Update center: " + url);
  }

  public UpdateCenterClient(HttpDownloader downloader, Configuration configuration) {
    this(downloader, configuration.getString(URL_PROPERTY, DEFAULT_URL));
  }

  public UpdateCenter getCenter() {
    return getCenter(false);
  }

  public UpdateCenter getCenter(boolean forceRefresh) {
    if (center == null || forceRefresh || needsRefresh()) {
      center = download();
      downloadDate = System.currentTimeMillis();
    }
    return center;
  }

  private boolean needsRefresh() {
    return downloadDate + PERIOD_IN_MILLISECONDS < System.currentTimeMillis();
  }

  private UpdateCenter download() {
    InputStream input = null;
    try {
      input = downloader.openStream(new URI(url));
      if (input != null) {
        Properties properties = new Properties();
        properties.load(input);
        return UpdateCenterDeserializer.fromProperties(properties);
      }

    } catch (Exception e) {
      LoggerFactory.getLogger(getClass()).error("Fail to download data from update center", e);

    } finally {
      IOUtils.closeQuietly(input);
    }
    return null;
  }
}
