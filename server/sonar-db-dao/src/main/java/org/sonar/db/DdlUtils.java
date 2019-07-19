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

import java.sql.Connection;

import static org.sonar.db.CoreDdlUtils.executeScript;

public class DdlUtils {

  private DdlUtils() {
    // prevents instantiation
  }

  /**
   * The connection is commited in this method but not closed.
   */
  public static void createSchema(Connection connection, String dialect, boolean createSchemaMigrations) {
    if (createSchemaMigrations) {
      executeScript(connection, "org/sonar/db/schema_migrations-" + dialect + ".ddl");
    }
    executeScript(connection, "org/sonar/db/schema-" + dialect + ".ddl");
    executeScript(connection, "org/sonar/db/rows-" + dialect + ".sql");
  }
}
