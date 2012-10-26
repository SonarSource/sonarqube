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

import org.sonar.api.BatchComponent;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.batch.bootstrap.DryRun;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.bootstrap.TempDirectories;

import java.io.File;

/**
 * @since 3.4
 */
public class LocalDatabase implements BatchComponent {
  private static final String API_SYNCHRO = "/api/synchro";
  private static final String DIALECT = "h2";
  private static final String DRIVER = "org.h2.Driver";
  private static final String URL = "jdbc:h2:";
  private static final String USER = "sonar";
  private static final String PASSWORD = "sonar";

  private final DryRun dryRun;
  private final Settings settings;
  private final ServerClient server;
  private ProjectReactor reactor;
  private final TempDirectories tempDirectories;

  public LocalDatabase(DryRun dryRun, Settings settings, ServerClient server, TempDirectories tempDirectories, ProjectReactor reactor) {
    this.dryRun = dryRun;
    this.settings = settings;
    this.server = server;
    this.reactor = reactor;
    this.tempDirectories = tempDirectories;
  }

  public void start() {
    if (!dryRun.isEnabled()) {
      return;
    }

    File file = tempDirectories.getFile("local", "db.h2.db");
    String h2DatabasePath = file.getAbsolutePath().replaceAll(".h2.db", "");

    downloadDatabase(reactor.getRoot().getKey(), file);
    replaceSettings(h2DatabasePath);
  }

  private void downloadDatabase(String projectKey, File toFile) {
    server.download(API_SYNCHRO + "?resource=" + projectKey, toFile);
  }

  private void replaceSettings(String h2DatabasePath) {
    settings
      .setProperty(DatabaseProperties.PROP_DIALECT, DIALECT)
      .setProperty(DatabaseProperties.PROP_DRIVER, DRIVER)
      .setProperty(DatabaseProperties.PROP_USER, USER)
      .setProperty(DatabaseProperties.PROP_PASSWORD, PASSWORD)
      .setProperty(DatabaseProperties.PROP_URL, URL + h2DatabasePath);
  }
}
