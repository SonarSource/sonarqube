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

import com.google.common.base.Joiner;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Loggers;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.endsWithIgnoreCase;

class MssqlCharsetHandler extends CharsetHandler {

  protected MssqlCharsetHandler(SelectExecutor selectExecutor) {
    super(selectExecutor);
  }

  @Override
  void handle(Connection connection, boolean enforceUtf8) throws SQLException {
    Loggers.get(getClass()).info("Verify that database collation is case-sensitive and accent-sensitive");
    checkCollation(connection);
  }

  private void checkCollation(Connection connection) throws SQLException {
    // All VARCHAR columns are returned. No need to check database general collation.
    // Example of row:
    // issues | kee | Latin1_General_CS_AS
    List<String[]> rows = select(connection,
      "SELECT table_name, column_name, collation_name " +
        "FROM [INFORMATION_SCHEMA].[COLUMNS] " +
        "WHERE collation_name is not null " +
        "ORDER BY table_name,column_name", 3 /* columns */);
    List<String> errors = new ArrayList<>();
    for (String[] row : rows) {
      if (!endsWithIgnoreCase(row[2], "_CS_AS")) {
        errors.add(row[0] + "." + row[1]);
      }
    }
    if (!errors.isEmpty()) {
      throw MessageException.of(format("Case-sensitive and accent-sensitive collation (CS_AS) is required for database columns [%s]", Joiner.on(", ").join(errors)));
    }
  }
}
