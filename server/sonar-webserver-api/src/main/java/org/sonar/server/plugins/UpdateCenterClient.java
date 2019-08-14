/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.plugins;

import com.google.common.base.Optional;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.apache.commons.io.IOUtils;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.UriReader;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.ProcessProperties;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.UpdateCenterDeserializer;
import org.sonar.updatecenter.common.UpdateCenterDeserializer.Mode;

/**
 * HTTP client to load data from the remote update center hosted at https://update.sonarsource.org.
 *
 * @since 2.4
 */
@Properties({
  @Property(
    key = UpdateCenterClient.URL_PROPERTY,
    defaultValue = "https://update.sonarsource.org/update-center.properties",
    name = "Update Center URL",
    category = "Update Center",
    project = false,
    // hidden from UI
    global = false)
})
public class UpdateCenterClient {

  public static final String URL_PROPERTY = "sonar.updatecenter.url";
  public static final int PERIOD_IN_MILLISECONDS = 60 * 60 * 1000;

  private final URI uri;
  private final UriReader uriReader;
  private final boolean isActivated;
  private UpdateCenter pluginCenter = null;
  private long lastRefreshDate = 0;

  public UpdateCenterClient(UriReader uriReader, Configuration config) throws URISyntaxException {
    this.uriReader = uriReader;
    this.uri = new URI(config.get(URL_PROPERTY).get());
    this.isActivated = config.getBoolean(ProcessProperties.Property.SONAR_UPDATECENTER_ACTIVATE.getKey()).get();
    Loggers.get(getClass()).info("Update center: " + uriReader.description(uri));
  }

  public Optional<UpdateCenter> getUpdateCenter() {
    return getUpdateCenter(false);
  }

  public Optional<UpdateCenter> getUpdateCenter(boolean forceRefresh) {
    if (!isActivated) {
      return Optional.absent();
    }

    if (pluginCenter == null || forceRefresh || needsRefresh()) {
      pluginCenter = init();
      lastRefreshDate = System.currentTimeMillis();
    }
    return Optional.fromNullable(pluginCenter);
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
      String content = uriReader.readString(uri, StandardCharsets.UTF_8);
      java.util.Properties properties = new java.util.Properties();
      input = IOUtils.toInputStream(content, StandardCharsets.UTF_8);
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
