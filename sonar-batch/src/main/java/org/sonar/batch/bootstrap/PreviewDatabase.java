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

import com.google.common.base.Throwables;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.api.utils.HttpDownloader.HttpException;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.TempFolder;

import java.io.File;
import java.net.SocketTimeoutException;

/**
 * @since 3.4
 */
public class PreviewDatabase implements BatchComponent {
  private static final Logger LOG = LoggerFactory.getLogger(PreviewDatabase.class);

  private static final String DIALECT = "h2";
  private static final String DRIVER = "org.h2.Driver";
  private static final String URL = "jdbc:h2:";
  private static final String USER = "sonar";
  private static final String PASSWORD = USER;

  private static final int DEFAULT_PREVIEW_READ_TIMEOUT_SEC = 60;

  private final Settings settings;
  private final ServerClient server;
  private final TempFolder tempUtils;
  private final AnalysisMode mode;

  public PreviewDatabase(Settings settings, ServerClient server, TempFolder tempUtils, AnalysisMode mode) {
    this.settings = settings;
    this.server = server;
    this.tempUtils = tempUtils;
    this.mode = mode;
  }

  public void start() {
    if (mode.isPreview()) {
      File databaseFile = tempUtils.newFile("preview", ".h2.db");

      int readTimeoutSec = getReadTimeout();
      downloadDatabase(databaseFile, readTimeoutSec * 1000);

      String databasePath = StringUtils.removeEnd(databaseFile.getAbsolutePath(), ".h2.db");
      replaceSettings(databasePath);
    }
  }

  // SONAR-4488 Allow to increase dryRun timeout
  private int getReadTimeout() {
    int readTimeoutSec;
    if (settings.hasKey(CoreProperties.DRY_RUN_READ_TIMEOUT_SEC)) {
      LOG.warn("Property {} is deprecated. Please use {} instead.", CoreProperties.DRY_RUN_READ_TIMEOUT_SEC, CoreProperties.PREVIEW_READ_TIMEOUT_SEC);
      readTimeoutSec = settings.getInt(CoreProperties.DRY_RUN_READ_TIMEOUT_SEC);
    } else if (settings.hasKey(CoreProperties.PREVIEW_READ_TIMEOUT_SEC)) {
      readTimeoutSec = settings.getInt(CoreProperties.PREVIEW_READ_TIMEOUT_SEC);
    } else {
      readTimeoutSec = DEFAULT_PREVIEW_READ_TIMEOUT_SEC;
    }
    return readTimeoutSec;
  }

  private void downloadDatabase(File toFile, int readTimeout) {
    String projectKey = null;
    try {
      projectKey = settings.getString(CoreProperties.PROJECT_KEY_PROPERTY);
      String branch = settings.getString(CoreProperties.PROJECT_BRANCH_PROPERTY);
      if (StringUtils.isNotBlank(branch)) {
        projectKey = String.format("%s:%s", projectKey, branch);
      }
      if (StringUtils.isBlank(projectKey)) {
        server.download("/batch_bootstrap/db", toFile, readTimeout);
      } else {
        server.download("/batch_bootstrap/db?project=" + projectKey, toFile, readTimeout);
      }
      LOG.debug("Dry Run database size: {}", FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(toFile)));
    } catch (SonarException e) {
      handleException(readTimeout, projectKey, e);
      throw e;
    }
  }

  private void handleException(int readTimeout, String projectKey, SonarException e) {
    Throwable rootCause = Throwables.getRootCause(e);
    if (rootCause instanceof SocketTimeoutException) {
      // Pico will unwrap the first runtime exception
      throw new SonarException(new SonarException(String.format("Preview database read timed out after %s ms. You can try to increase read timeout with property -D"
        + CoreProperties.PREVIEW_READ_TIMEOUT_SEC + " (in seconds)",
        readTimeout), e));
    }
    if (projectKey != null && (rootCause instanceof HttpException) && (((HttpException) rootCause).getResponseCode() == 401)) {
      // Pico will unwrap the first runtime exception
      throw new SonarException(new SonarException(String.format("You don't have access rights to project [%s]", projectKey), e));
    }
  }

  private void replaceSettings(String databasePath) {
    settings
      .setProperty(DatabaseProperties.PROP_DIALECT, DIALECT)
      .setProperty(DatabaseProperties.PROP_DRIVER, DRIVER)
      .setProperty(DatabaseProperties.PROP_USER, USER)
      .setProperty(DatabaseProperties.PROP_PASSWORD, PASSWORD)
      .setProperty(DatabaseProperties.PROP_URL, URL + databasePath);
  }
}
