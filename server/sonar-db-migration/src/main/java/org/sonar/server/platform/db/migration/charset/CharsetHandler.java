/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.charset;

import java.sql.Connection;
import java.sql.SQLException;

abstract class CharsetHandler {

  protected static final String UTF8 = "utf8";

  private final SqlExecutor selectExecutor;

  protected CharsetHandler(SqlExecutor selectExecutor) {
    this.selectExecutor = selectExecutor;
  }

  abstract void handle(Connection connection, DatabaseCharsetChecker.State state) throws SQLException;

  protected SqlExecutor getSqlExecutor() {
    return selectExecutor;
  }

}
