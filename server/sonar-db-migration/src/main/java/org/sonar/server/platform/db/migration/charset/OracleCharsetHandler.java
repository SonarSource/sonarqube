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
package org.sonar.server.platform.db.migration.charset;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Loggers;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.containsIgnoreCase;

class OracleCharsetHandler extends CharsetHandler {

  OracleCharsetHandler(SqlExecutor selectExecutor) {
    super(selectExecutor);
  }

  @Override
  public void handle(Connection connection, DatabaseCharsetChecker.State state) throws SQLException {
    // Charset is a global setting on Oracle, it can't be set on a specified schema with a
    // different value. To not block users who already have a SonarQube schema, charset
    // is verified only on fresh installs but not on upgrades. Let's hope they won't face
    // any errors related to charset if they didn't follow the UTF8 requirement when creating
    // the schema in previous SonarQube versions.
    if (state == DatabaseCharsetChecker.State.FRESH_INSTALL) {
      Loggers.get(getClass()).info("Verify that database charset is UTF8");
      expectUtf8(connection);
    }
  }

  private void expectUtf8(Connection connection) throws SQLException {
    // Oracle does not allow to override character set on tables. Only global charset is verified.
    String charset = getSqlExecutor().selectSingleString(connection, "select value from nls_database_parameters where parameter='NLS_CHARACTERSET'");
    if (!containsIgnoreCase(charset, UTF8)) {
      throw MessageException.of(format("Oracle NLS_CHARACTERSET does not support UTF8: %s", charset));
    }
  }
}
