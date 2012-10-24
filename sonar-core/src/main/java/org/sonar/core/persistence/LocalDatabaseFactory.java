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
package org.sonar.core.persistence;

import com.google.common.io.Files;
import org.apache.commons.dbcp.BasicDataSource;
import org.sonar.api.ServerComponent;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.utils.SonarException;

import javax.sql.DataSource;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class LocalDatabaseFactory implements ServerComponent {
  private static final String DIALECT = "h2";
  private static final String DRIVER = "org.h2.Driver";
  private static final String URL = "jdbc:h2:";
  private static final String USER = "sonar";
  private static final String PASSWORD = "sonar";

  private final Database database;
  private final ServerFileSystem serverFileSystem;

  public LocalDatabaseFactory(Database database, ServerFileSystem serverFileSystem) {
    this.database = database;
    this.serverFileSystem = serverFileSystem;
  }

  public byte[] createDatabaseForLocalMode(int resourceId) {
    String name = serverFileSystem.getTempDir().getAbsolutePath() + "db-" + System.nanoTime();

    try {
      BasicDataSource destination = create(DIALECT, DRIVER, USER, PASSWORD, URL + name);
      copy(database.getDataSource(), destination, resourceId);
      close(destination);

      return dbFileContent(name);
    } catch (SQLException e) {
      throw new SonarException("Unable to create database for local mode", e);
    }
  }

  private void copy(DataSource source, DataSource dest, int resourceId) {
    new DbTemplate()
        .copyTable(source, dest, "PROPERTIES",
            "SELECT * FROM PROPERTIES WHERE (((USER_ID IS NULL) AND (RESOURCE_ID IS NULL)) OR (RESOURCE_ID='" + resourceId + "')) AND NOT (PROP_KEY LIKE '%.secured')")
        .copyTable(source, dest, "RULES_PROFILES", "SELECT * FROM RULES_PROFILES")
        .copyTable(source, dest, "RULES", "SELECT * FROM RULES")
        .copyTable(source, dest, "RULES_PARAMETERS", "SELECT * FROM RULES_PARAMETERS")
        .copyTable(source, dest, "ACTIVE_RULES", "SELECT * FROM ACTIVE_RULES")
        .copyTable(source, dest, "ACTIVE_RULE_PARAMETERS", "SELECT * FROM ACTIVE_RULE_PARAMETERS")
        .copyTable(source, dest, "METRICS", "SELECT * FROM METRICS")
        .copyTable(source, dest, "CHARACTERISTICS", "SELECT * FROM CHARACTERISTICS")
        .copyTable(source, dest, "CHARACTERISTIC_PROPERTIES", "SELECT * FROM CHARACTERISTIC_PROPERTIES")
        .copyTable(source, dest, "CHARACTERISTIC_EDGES", "SELECT * FROM CHARACTERISTIC_EDGES")
        .copyTable(source, dest, "QUALITY_MODELS", "SELECT * FROM QUALITY_MODELS");
  }

  private BasicDataSource create(String dialect, String driver, String user, String password, String url) {
    BasicDataSource dataSource = new DbTemplate().dataSource(driver, user, password, url);
    new DbTemplate().createSchema(dataSource, dialect);
    return dataSource;
  }

  private void close(BasicDataSource dest) throws SQLException {
    dest.close();
  }

  private byte[] dbFileContent(String name) {
    try {
      File dbFile = new File(name + ".h2.db");
      byte[] content = Files.toByteArray(dbFile);
      dbFile.delete();
      return content;
    } catch (IOException e) {
      throw new SonarException("Unable to read h2 database file", e);
    }
  }
}
