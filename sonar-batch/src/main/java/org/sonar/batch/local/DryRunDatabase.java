/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.batch.local;

import com.google.common.base.Throwables;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.bootstrap.DryRun;
import org.sonar.batch.bootstrap.ProjectReactorReady;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.bootstrap.TempDirectories;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @since 3.4
 */
public class DryRunDatabase implements BatchComponent {
  private static final Logger LOG = LoggerFactory.getLogger(DryRunDatabase.class);

  private static final String API_SYNCHRO = "/api/synchro";
  private static final String DIALECT = "h2";
  private static final String DRIVER = "org.h2.Driver";
  private static final String URL = "jdbc:h2:";
  private static final String USER = "sonar";
  private static final String PASSWORD = "sonar";

  private final DryRun dryRun;
  private final Settings settings;
  private final ServerClient server;
  private final TempDirectories tempDirectories;
  private final ProjectReactor reactor;

  public DryRunDatabase(DryRun dryRun, Settings settings, ServerClient server, TempDirectories tempDirectories, ProjectReactor reactor,
      // project reactor must be completely built
      ProjectReactorReady reactorReady) {
    this.dryRun = dryRun;
    this.settings = settings;
    this.server = server;
    this.tempDirectories = tempDirectories;
    this.reactor = reactor;
  }

  public void start() {
    if (!dryRun.isEnabled()) {
      return;
    }

    LOG.info("Install dry run database");
    File databaseFile = tempDirectories.getFile("dry_run", "db.h2.db");
    downloadDatabase(reactor.getRoot().getKey(), databaseFile);

    String databasePath = StringUtils.removeEnd(databaseFile.getAbsolutePath(), ".h2.db");
    replaceSettings(databasePath);
  }

  private void downloadDatabase(String projectKey, File toFile) {
    try {
      server.download(API_SYNCHRO + "?resource=" + projectKey, toFile);
    } catch (SonarException e) {
      Throwable rootCause = Throwables.getRootCause(e);
      if (rootCause instanceof FileNotFoundException) {
        throw new SonarException(String.format("Project [%s] doesn't exist on server", projectKey), e);
      } else if ((rootCause instanceof IOException) && (StringUtils.contains(rootCause.getMessage(), "401"))) {
        throw new SonarException(String.format("You don't have access rights to project [%s]", projectKey), e);
      }
      throw e;
    }
  }

  private void replaceSettings(String databasePath) {
    settings
        .setProperty("sonar.jdbc.schema", "")
        .setProperty(DatabaseProperties.PROP_DIALECT, DIALECT)
        .setProperty(DatabaseProperties.PROP_DRIVER, DRIVER)
        .setProperty(DatabaseProperties.PROP_USER, USER)
        .setProperty(DatabaseProperties.PROP_PASSWORD, PASSWORD)
        .setProperty(DatabaseProperties.PROP_URL, URL + databasePath);
  }
}
