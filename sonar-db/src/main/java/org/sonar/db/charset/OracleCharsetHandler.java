/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.charset;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Loggers;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.containsIgnoreCase;
import static org.sonar.db.charset.DatabaseCharsetChecker.Flag.ENFORCE_UTF8;

class OracleCharsetHandler extends CharsetHandler {

  protected OracleCharsetHandler(SqlExecutor selectExecutor) {
    super(selectExecutor);
  }

  @Override
  public void handle(Connection connection, Set<DatabaseCharsetChecker.Flag> flags) throws SQLException {
    // Oracle does not allow to override character set on tables. Only global charset is verified.
    if (flags.contains(ENFORCE_UTF8)) {
      Loggers.get(getClass()).info("Verify that database charset is UTF8");
      checkUtf8(connection);
    }
  }

  private void checkUtf8(Connection connection) throws SQLException {
    String charset = selectSingleString(connection, "select value from nls_database_parameters where parameter='NLS_CHARACTERSET'");
    String sort = selectSingleString(connection, "select value from nls_database_parameters where parameter='NLS_SORT'");
    if (!containsIgnoreCase(charset, UTF8) || !"BINARY".equalsIgnoreCase(sort)) {
      throw MessageException.of(format("Oracle must be have UTF8 charset and BINARY sort. NLS_CHARACTERSET is %s and NLS_SORT is %s.", charset, sort));
    }
  }
}
