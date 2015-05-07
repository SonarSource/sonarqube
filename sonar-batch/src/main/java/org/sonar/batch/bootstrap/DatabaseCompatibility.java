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
import org.sonar.api.BatchSide;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.api.utils.MessageException;
import org.sonar.core.persistence.DatabaseVersion;
import org.sonar.core.properties.PropertiesDao;

/**
 * Detects if database is not up-to-date with the version required by the batch.
 */
@BatchSide
public class DatabaseCompatibility {

  private DatabaseVersion version;
  private Settings settings;
  private PropertiesDao propertiesDao;
  private ServerClient server;
  private DefaultAnalysisMode analysisMode;

  public DatabaseCompatibility(DatabaseVersion version, ServerClient server, Settings settings, PropertiesDao propertiesDao, DefaultAnalysisMode mode) {
    this.version = version;
    this.server = server;
    this.settings = settings;
    this.propertiesDao = propertiesDao;
    this.analysisMode = mode;
  }

  public void start() {
    if (!analysisMode.isPreview()) {
      checkCorrectServerId();
      checkDatabaseStatus();
    }
  }

  private void checkCorrectServerId() {
    if (!propertiesDao.selectGlobalProperty(CoreProperties.SERVER_ID).getValue().equals(getServerId())) {
      StringBuilder message = new StringBuilder("The current batch process and the configured remote server do not share the same DB configuration.\n");
      message.append("\t- Batch side: ");
      message.append(settings.getString(DatabaseProperties.PROP_URL));
      message.append(" (");
      String userName = settings.getString(DatabaseProperties.PROP_USER);
      message.append(userName == null ? "sonar" : userName);
      message.append(" / *****)\n\t- Server side: check the configuration at ");
      message.append(server.getURL());
      message.append("/system\n");
      throw MessageException.of(message.toString());
    }
  }

  private String getServerId() {
    String remoteServerInfo = server.request("/api/server");
    // don't use JSON utilities to extract ID from such a small string
    return extractServerId(remoteServerInfo);
  }

  @VisibleForTesting
  String extractServerId(String remoteServerInfo) {
    String partialId = StringUtils.substringAfter(remoteServerInfo, "\"id\":\"");
    return StringUtils.substringBefore(partialId, "\"");
  }

  private void checkDatabaseStatus() {
    DatabaseVersion.Status status = version.getStatus();
    if (status == DatabaseVersion.Status.REQUIRES_DOWNGRADE) {
      throw MessageException.of("Database relates to a more recent version of SonarQube. Please check your settings (JDBC settings, version of Maven plugin)");
    }
    if (status == DatabaseVersion.Status.REQUIRES_UPGRADE) {
      throw MessageException.of("Database must be upgraded. Please browse " + server.getURL() + "/setup");
    }
    if (status != DatabaseVersion.Status.UP_TO_DATE) {
      // Support other future values
      throw MessageException.of("Unknown database status: " + status);
    }
  }

}
