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
package org.sonar.server.plugins;

import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.UriReader;
import org.sonar.api.utils.log.Loggers;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.UpdateCenterDeserializer;
import org.sonar.updatecenter.common.UpdateCenterDeserializer.Mode;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

/**
 * HTTP client to load data from the remote update center hosted at http://update.sonarsource.org.
 *
 * @since 2.4
 */
@Properties({
  @Property(
    key = UpdateCenterClient.ACTIVATION_PROPERTY,
    defaultValue = "true",
    name = "Enable Update Center",
    project = false,
    // hidden from UI
    global = false,
    category = "Update Center",
    type = PropertyType.BOOLEAN),
  @Property(
    key = UpdateCenterClient.URL_PROPERTY,
    defaultValue = "http://update.sonarsource.org/update-center.properties",
    name = "Update Center URL",
    project = false,
    // hidden from UI
    global = false,
    category = "Update Center")
})
public class UpdateCenterClient {

  public static final String URL_PROPERTY = "sonar.updatecenter.url";
  public static final String ACTIVATION_PROPERTY = "sonar.updatecenter.activate";
  public static final int PERIOD_IN_MILLISECONDS = 60 * 60 * 1000;

  private final URI uri;
  private final UriReader uriReader;
  private UpdateCenter pluginCenter = null;
  private long lastRefreshDate = 0;

  public UpdateCenterClient(UriReader uriReader, Settings settings) throws URISyntaxException {
    this.uriReader = uriReader;
    this.uri = new URI(settings.getString(URL_PROPERTY));
    Loggers.get(getClass()).info("Update center: " + uriReader.description(uri));
  }

  public UpdateCenter getUpdateCenter() {
    return getUpdateCenter(false);
  }

  public UpdateCenter getUpdateCenter(boolean forceRefresh) {
    if (pluginCenter == null || forceRefresh || needsRefresh()) {
      pluginCenter = init();
      lastRefreshDate = System.currentTimeMillis();
    }
    return pluginCenter;
  }

  public Date getLastRefreshDate() {
    return lastRefreshDate > 0 ? new Date(lastRefreshDate) : null;
  }

  private boolean needsRefresh() {
    return lastRefreshDate + PERIOD_IN_MILLISECONDS < System.currentTimeMillis();
  }

  private UpdateCenter init() {
    InputStream input = null;
    try {
      String content = uriReader.readString(uri, Charsets.UTF_8);
      java.util.Properties properties = new java.util.Properties();
      input = IOUtils.toInputStream(content, Charsets.UTF_8.name());
      properties.load(input);
      return new UpdateCenterDeserializer(Mode.PROD, true).fromProperties(properties);

    } catch (Exception e) {
      Loggers.get(getClass()).error("Fail to connect to update center", e);
      return null;

    } finally {
      IOUtils.closeQuietly(input);
    }
  }
}
