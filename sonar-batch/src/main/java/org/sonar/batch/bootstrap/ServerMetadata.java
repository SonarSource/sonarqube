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

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @deprecated in 3.4. Replaced by {@link org.sonar.batch.bootstrap.ServerClient}
 */
@Deprecated
public class ServerMetadata extends Server implements BatchComponent {

  private Settings settings;
  private ServerClient client;

  public ServerMetadata(Settings settings, ServerClient client) {
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
  public String getURL() {
    return client.getURL();
  }

  @Override
  public String getPermanentServerId() {
    return settings.getString(CoreProperties.PERMANENT_SERVER_ID);
  }

  public String getServerId() {
    String remoteServerInfo = client.request("/api/server");
    // don't use JSON utilities to extract ID from such a small string
    return extractServerId(remoteServerInfo);
  }

  @VisibleForTesting
  String extractServerId(String remoteServerInfo) {
    String partialId = StringUtils.substringAfter(remoteServerInfo, "\"id\":\"");
    return StringUtils.substringBefore(partialId, "\"");
  }
}
