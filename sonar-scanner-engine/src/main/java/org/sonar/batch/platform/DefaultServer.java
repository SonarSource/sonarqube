/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.platform;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;
import org.sonar.batch.bootstrap.BatchWsClient;

import static org.apache.commons.lang.StringUtils.trimToEmpty;

@BatchSide
public class DefaultServer extends Server {

  private Settings settings;
  private BatchWsClient client;

  public DefaultServer(Settings settings, BatchWsClient client) {
    this.settings = settings;
    this.client = client;
  }

  @Override
  public String getId() {
    return settings.getString(CoreProperties.SERVER_ID);
  }

  @Override
  public String getVersion() {
    return settings.getString(CoreProperties.SERVER_VERSION);
  }

  @Override
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

  @Override
  public File getRootDir() {
    return null;
  }

  @Override
  @CheckForNull
  public File getDeployDir() {
    return null;
  }

  @Override
  public String getContextPath() {
    return null;
  }

  @Override
  public String getPublicRootUrl() {
    String baseUrl = trimToEmpty(settings.getString(CoreProperties.SERVER_BASE_URL));
    if (baseUrl.isEmpty()) {
      // If server base URL was not configured in Sonar server then is is better to take URL configured on batch side
      baseUrl = client.baseUrl();
    }
    return StringUtils.removeEnd(baseUrl, "/");
  }

  @Override
  public boolean isDev() {
    return false;
  }

  @Override
  public boolean isSecured() {
    return false;
  }

  @Override
  public String getURL() {
    return StringUtils.removeEnd(client.baseUrl(), "/");
  }

  @Override
  public String getPermanentServerId() {
    return settings.getString(CoreProperties.PERMANENT_SERVER_ID);
  }
}
