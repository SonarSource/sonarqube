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
import org.apache.commons.io.output.NullWriter;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;

/**
 * Util class to create Sonar database tables
 *
 * @since 2.12
 */
public final class DdlUtils {

  private DdlUtils() {
  }

  public static boolean supportsDialect(String dialect) {
    return "h2".equals(dialect);
  }

  /**
   * The connection is commited in this method but not closed.
   */
  public static void createSchema(Connection connection, String dialect, boolean createSchemaMigrations) {
    if (createSchemaMigrations) {
      executeScript(connection, "org/sonar/db/version/schema_migrations-" + dialect + ".ddl");
    }
    executeScript(connection, "org/sonar/db/version/schema-" + dialect + ".ddl");
    executeScript(connection, "org/sonar/db/version/rows-" + dialect + ".sql");
  }

  public static void executeScript(Connection connection, String path) {
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
}
