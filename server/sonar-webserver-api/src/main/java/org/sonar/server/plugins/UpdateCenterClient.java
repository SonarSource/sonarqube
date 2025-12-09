/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.SonarEdition;
import org.sonar.api.config.Configuration;
import org.sonar.api.internal.MetadataLoader;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.UriReader;
import org.sonar.process.ProcessProperties;
import org.sonar.updatecenter.common.Product;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.UpdateCenterDeserializer;
import org.sonar.updatecenter.common.UpdateCenterDeserializer.Mode;

/**
 * HTTP client to load data from the remote update center hosted at https://downloads.sonarsource.com/?prefix=sonarqube/update
 *
 * @since 2.4
 */
@Properties({
  @Property(
    key = UpdateCenterClient.URL_PROPERTY,
    defaultValue = UpdateCenterClient.URL_DEFAULT_VALUE,
    name = "Update Center URL",
    category = "Update Center",
    // hidden from UI
    global = false),
  @Property(
    key = UpdateCenterClient.CACHE_TTL_PROPERTY,
    defaultValue = UpdateCenterClient.CACHE_TTL_DEFAULT_VALUE,
    name = "Update Center cache time-to-live in milliseconds",
    category = "Update Center",
    // hidden from UI
    global = false)
})
public class UpdateCenterClient {

  private static final Logger LOG = LoggerFactory.getLogger(UpdateCenterClient.class);
  static final String URL_PROPERTY = "sonar.updatecenter.url";
  static final String URL_DEFAULT_VALUE = "https://downloads.sonarsource.com/sonarqube/update/update-center.properties";
  static final String CACHE_TTL_PROPERTY = "sonar.updatecenter.cache.ttl";
  static final String CACHE_TTL_DEFAULT_VALUE = "3600000";

  private final long periodInMilliseconds;
  private final Product product;

  private final URI uri;
  private final UriReader uriReader;
  private final boolean isActivated;
  private UpdateCenter pluginCenter = null;
  private long lastRefreshDate = 0;

  public UpdateCenterClient(UriReader uriReader, Configuration config) throws URISyntaxException {
    this.uriReader = uriReader;
    this.uri = new URI(config.get(URL_PROPERTY).get());
    this.isActivated = config.getBoolean(ProcessProperties.Property.SONAR_UPDATECENTER_ACTIVATE.getKey()).get();
    this.periodInMilliseconds = Long.parseLong(config.get(CACHE_TTL_PROPERTY).get());
    this.product = MetadataLoader.loadEdition(System2.INSTANCE) == SonarEdition.COMMUNITY ? Product.SONARQUBE_COMMUNITY_BUILD : Product.SONARQUBE_SERVER;

    LOG.atInfo()
      .addArgument(() -> uriReader.description(uri))
      .log("Update center: {}");
  }

  public Optional<UpdateCenter> getUpdateCenter() {
    return getUpdateCenter(false);
  }

  public Optional<UpdateCenter> getUpdateCenter(boolean forceRefresh) {
    if (!isActivated) {
      return Optional.empty();
    }

    if (pluginCenter == null || forceRefresh || needsRefresh()) {
      pluginCenter = init();
      lastRefreshDate = System.currentTimeMillis();
    }
    return Optional.ofNullable(pluginCenter);
  }

  public Date getLastRefreshDate() {
    return lastRefreshDate > 0 ? new Date(lastRefreshDate) : null;
  }

  private boolean needsRefresh() {
    return lastRefreshDate + periodInMilliseconds < System.currentTimeMillis();
  }

  private UpdateCenter init() {
    InputStream input = null;
    try {
      String content = uriReader.readString(uri, StandardCharsets.UTF_8);
      java.util.Properties properties = new java.util.Properties();
      input = IOUtils.toInputStream(content, StandardCharsets.UTF_8);
      properties.load(input);
      UpdateCenter updateCenter = new UpdateCenterDeserializer(Mode.PROD, true).fromProperties(properties);
      updateCenter.setInstalledSonarProduct(product);
      return updateCenter;
    } catch (Exception e) {
      LoggerFactory.getLogger(getClass()).error("Fail to connect to update center", e);
      return null;

    } finally {
      IOUtils.closeQuietly(input);
    }
  }
}
