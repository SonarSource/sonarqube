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

import com.google.common.io.Closeables;
import com.google.gson.Gson;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.bootstrap.LocalMode;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.sql.SQLException;

/**
 * @since 3.4
 */
public class LocalDatabase implements BatchComponent {
  private static final Logger LOG = LoggerFactory.getLogger(LocalDatabase.class);

  public static final String API_SYNCHRO = "/api/synchro";
  private static final String DIALECT = "h2";
  private static final String DRIVER = "org.h2.Driver";
  private static final String URL = "jdbc:h2:";
  private static final String USER = "sonar";
  private static final String PASSWORD = "sonar";

  private final LocalMode localMode;
  private final Settings settings;
  private final Server server;
  private final HttpDownloader httpDownloader;
  private BasicDataSource dataSource;

  public LocalDatabase(LocalMode localMode, Settings settings, Server server, HttpDownloader httpDownloader) {
    this.localMode = localMode;
    this.settings = settings;
    this.server = server;
    this.httpDownloader = httpDownloader;
  }

  public void start() {
    if (!localMode.isEnabled()) {
      return;
    }

    LOG.info("Download database");
    Path path = downloadDatabase();

    LOG.info("Starting local database");
    replaceSettings(path);
    configureDataSource(path);
  }

  private Path downloadDatabase() {
    InputStream stream = null;
    try {
      stream = httpDownloader.openStream(URI.create(server.getURL() + API_SYNCHRO));
      return new Gson().fromJson(new InputStreamReader(stream), Path.class);
    } finally {
      Closeables.closeQuietly(stream);
    }
  }

  static class Path {
    String path;

    String getName() {
      return path.replaceAll(".h2.db", "");
    }
  }

  public void stop() {
    try {
      dataSource.close();
    } catch (SQLException e) {
      // Ignore error
    }
  }

  private void replaceSettings(Path path) {
    settings
        .setProperty(DatabaseProperties.PROP_DIALECT, DIALECT)
        .setProperty(DatabaseProperties.PROP_DRIVER, DRIVER)
        .setProperty(DatabaseProperties.PROP_USER, USER)
        .setProperty(DatabaseProperties.PROP_PASSWORD, PASSWORD)
        .setProperty(DatabaseProperties.PROP_URL, URL + path.getName());
  }

  private void configureDataSource(Path path) {
    try {
      dataSource = new BasicDataSource();
      dataSource.setDriverClassName(DRIVER);
      dataSource.setUsername(USER);
      dataSource.setPassword(PASSWORD);
      dataSource.setUrl(URL + path.getName());
    } catch (Exception e) {
      throw new SonarException("Fail to start local database", e);
    }
  }
}
