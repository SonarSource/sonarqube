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
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Loggers;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.containsIgnoreCase;

class PostgresCharsetHandler extends CharsetHandler {

  protected PostgresCharsetHandler(SqlExecutor selectExecutor) {
    super(selectExecutor);
  }

  @Override
  void handle(Connection connection, boolean enforceUtf8) throws SQLException {
    // PostgreSQL does not support case-insensitive collations. Only charset must be verified.
    if (enforceUtf8) {
      Loggers.get(getClass()).info("Verify that database collation supports UTF8");
      checkUtf8(connection);
    }
  }

  private void checkUtf8(Connection connection) throws SQLException {
    // Character set is defined globally and can be overridden on each column.
    // This request returns all VARCHAR columns. Collation may be empty.
    // Examples:
    // issues | key | ''
    // projects | name | utf8
    List<String[]> rows = select(connection, "select table_name, column_name, collation_name " +
      "from information_schema.columns " +
      "where table_schema='public' " +
      "and udt_name='varchar' " +
      "order by table_name, column_name", new SqlExecutor.StringsConverter(3 /* columns returned by SELECT */));
    boolean mustCheckGlobalCollation = false;
    List<String> errors = new ArrayList<>();
    for (String[] row : rows) {
      if (StringUtils.isBlank(row[2])) {
        mustCheckGlobalCollation = true;
      } else if (!containsIgnoreCase(row[2], UTF8)) {
        errors.add(format("%s.%s", row[0], row[1]));
      }
    }

    if (mustCheckGlobalCollation) {
      String charset = selectSingleString(connection, "SELECT pg_encoding_to_char(encoding) FROM pg_database WHERE datname = current_database()");
      if (!containsIgnoreCase(charset, UTF8)) {
        throw MessageException.of(format("Database collation is %s. It must support UTF8.", charset));
      }
    }
    if (!errors.isEmpty()) {
      throw MessageException.of(format("Database columns [%s] must support UTF8 collation.", Joiner.on(", ").join(errors)));
    }
  }
}
