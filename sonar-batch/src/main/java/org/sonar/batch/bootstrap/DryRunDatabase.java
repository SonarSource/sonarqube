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

import java.io.File;
import java.net.SocketTimeoutException;

/**
 * @since 3.4
 */
public class DryRunDatabase implements BatchComponent {
  private static final Logger LOG = LoggerFactory.getLogger(DryRunDatabase.class);

  private static final String DIALECT = "h2";
  private static final String DRIVER = "org.h2.Driver";
  private static final String URL = "jdbc:h2:";
  private static final String USER = "sonar";
  private static final String PASSWORD = "sonar";

  private static final int DEFAULT_DRY_RUN_READ_TIMEOUT = 60 * 1000;

  private final Settings settings;
  private final ServerClient server;
  private final TempDirectories tempDirectories;

  public DryRunDatabase(Settings settings, ServerClient server, TempDirectories tempDirectories) {
    this.settings = settings;
    this.server = server;
    this.tempDirectories = tempDirectories;
  }

  public void start() {
    if (settings.getBoolean(CoreProperties.DRY_RUN)) {
      LOG.info("Dry run");
      File databaseFile = tempDirectories.getFile("", "dryrun.h2.db");

      // SONAR-4488 Allow to increase dryRun timeout
      int readTimeout = settings.getInt(CoreProperties.DRY_RUN_READ_TIMEOUT);
      readTimeout = (readTimeout == 0) ? DEFAULT_DRY_RUN_READ_TIMEOUT : readTimeout;

      downloadDatabase(databaseFile, readTimeout);

      String databasePath = StringUtils.removeEnd(databaseFile.getAbsolutePath(), ".h2.db");
      replaceSettings(databasePath);
    }
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
      throw new SonarException(new SonarException(String.format("DryRun database read timed out after %s ms. You can try to increase read timeout with property -D"
        + CoreProperties.DRY_RUN_READ_TIMEOUT,
          readTimeout), e));
    }
    if (projectKey != null && (rootCause instanceof HttpException) && (((HttpException) rootCause).getResponseCode() == 401)) {
      // Pico will unwrap the first runtime exception
      throw new SonarException(new SonarException(String.format("You don't have access rights to project [%s]", projectKey), e));
    }
  }

  private void replaceSettings(String databasePath) {
    settings
        .removeProperty("sonar.jdbc.schema")
        .setProperty(DatabaseProperties.PROP_DIALECT, DIALECT)
        .setProperty(DatabaseProperties.PROP_DRIVER, DRIVER)
        .setProperty(DatabaseProperties.PROP_USER, USER)
        .setProperty(DatabaseProperties.PROP_PASSWORD, PASSWORD)
        .setProperty(DatabaseProperties.PROP_URL, URL + databasePath);
  }
}
