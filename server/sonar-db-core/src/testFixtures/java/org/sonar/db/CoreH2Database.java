/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.db;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.io.output.NullWriter;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;

import static java.lang.String.format;

/**
 * H2 in-memory database, used for unit tests only against an empty DB or a provided H2 SQL script.
 */
public class CoreH2Database implements Database {
  private final String name;
  private BasicDataSource datasource;

  /**
   * IMPORTANT: change DB name in order to not conflict with {@link DefaultDatabaseTest}
   */
  public CoreH2Database(String name) {
    this.name = name;
  }

  @Override
  public void start() {
    startDatabase();
  }

  private void startDatabase() {
    try {
      datasource = new BasicDataSource();
      datasource.setDriverClassName("org.h2.Driver");
      datasource.setUsername("sonar");
      datasource.setPassword("sonar");
      datasource.setUrl("jdbc:h2:mem:" + name);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to start H2", e);
    }
  }

  public void executeScript(String classloaderPath) {
    try (Connection connection = datasource.getConnection()) {
      executeScript(connection, classloaderPath);
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to execute script: " + classloaderPath, e);
    }
  }

  private static void executeScript(Connection connection, String path) {
    ScriptRunner scriptRunner = newScriptRunner(connection);
    try {
      scriptRunner.runScript(Resources.getResourceAsReader(path));
      connection.commit();

    } catch (Exception e) {
      throw new IllegalStateException("Fail to restore: " + path, e);
    }
  }

  private static ScriptRunner newScriptRunner(Connection connection) {
    ScriptRunner scriptRunner = new ScriptRunner(connection);
    scriptRunner.setDelimiter(";");
    scriptRunner.setStopOnError(true);
    scriptRunner.setLogWriter(new PrintWriter(new NullWriter()));
    return scriptRunner;
  }

  @Override
  public void stop() {
    try {
      datasource.close();
    } catch (SQLException e) {
      // Ignore error
    }
  }

  public DataSource getDataSource() {
    return datasource;
  }

  public Dialect getDialect() {
    return new H2();
  }

  @Override
  public void enableSqlLogging(boolean enable) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return format("H2 Database[%s]", name);
  }
}
