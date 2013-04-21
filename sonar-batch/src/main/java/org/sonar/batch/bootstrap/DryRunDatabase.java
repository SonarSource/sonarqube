/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrap;

import com.google.common.base.Throwables;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.api.utils.SonarException;

import javax.annotation.Nullable;

import java.io.File;

import static org.sonar.api.utils.HttpDownloader.HttpException;

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

  private final Settings settings;
  private final ServerClient server;
  private final TempDirectories tempDirectories;
  private ProjectReactor reactor;

  public DryRunDatabase(Settings settings, ServerClient server, TempDirectories tempDirectories, @Nullable ProjectReactor reactor) {
    this.settings = settings;
    this.server = server;
    this.tempDirectories = tempDirectories;
    this.reactor = reactor;
  }

  public DryRunDatabase(Settings settings, ServerClient server, TempDirectories tempDirectories) {
    this(settings, server, tempDirectories, null);
  }

  public void start() {
    if (settings.getBoolean(CoreProperties.DRY_RUN)) {
      LOG.info("Dry run");
      File databaseFile = tempDirectories.getFile("", "dryrun.h2.db");
      downloadDatabase(databaseFile);

      String databasePath = StringUtils.removeEnd(databaseFile.getAbsolutePath(), ".h2.db");
      replaceSettings(databasePath);
    }
  }

  private void downloadDatabase(File toFile) {
    String projectKey = null;
    try {
      if (reactor == null) {
        server.download("/batch_bootstrap/db", toFile);
      } else {
        projectKey = StringUtils.defaultString(reactor.getRoot().getKey());
        server.download("/batch_bootstrap/db?project=" + projectKey, toFile);
      }
      LOG.debug("Dry Run database size: {}", FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(toFile)));
    } catch (SonarException e) {
      Throwable rootCause = Throwables.getRootCause(e);
      if (projectKey != null && (rootCause instanceof HttpException) && (((HttpException) rootCause).getResponseCode() == 401)) {
        throw new SonarException(String.format("You don't have access rights to project [%s]", projectKey), e);
      }
      throw e;
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
