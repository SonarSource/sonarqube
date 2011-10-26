/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.persistence;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Util class to create Sonar database tables
 * 
 * @since 2.12
 */
public final class DdlUtils {

  private DdlUtils() {
  }

  public static boolean supportsDialect(String dialect) {
    return "derby".equals(dialect);
  }

  /**
   * TODO to be replaced by mybatis ScriptRunner
   * The connection is commited in this method but not closed.
   */
  public static void execute(Connection connection, String dialect) {
    if (!supportsDialect(dialect)) {
      throw new IllegalArgumentException("Unsupported dialect: " + dialect);
    }
    List<String> lines = loadStatementsForDialect(dialect);
    for (String line : lines) {
      if (StringUtils.isNotBlank(line) && !StringUtils.startsWith(line, "--")) {
        try {
          Statement statement = connection.createStatement();
          statement.execute(line);
          statement.close();
        } catch (Exception e) {
          throw new IllegalStateException("Fail to execute DDL: " + line, e);
        }
      }
    }
    try {
      connection.commit();
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to commit DDL", e);
    }

  }

  private static List<String> loadStatementsForDialect(String dialect) {
    List<String> lines = new ArrayList<String>();
    lines.addAll(loadStatements("/org/sonar/persistence/schema-" + dialect + ".ddl"));
    lines.addAll(loadStatements("/org/sonar/persistence/rows-" + dialect + ".sql"));
    return lines;
  }

  private static List<String> loadStatements(String path) {
    InputStream input = DdlUtils.class.getResourceAsStream(path);
    try {
      return IOUtils.readLines(input);

    } catch (IOException e) {
      throw new IllegalStateException("Fail to load DDL file from classloader: " + path, e);
    } finally {

      IOUtils.closeQuietly(input);
    }
  }
}
