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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Loggers;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang.StringUtils.isBlank;

class PostgresCharsetHandler extends CharsetHandler {

  private final PostgresMetadataReader metadata;

  PostgresCharsetHandler(SqlExecutor selectExecutor, PostgresMetadataReader metadata) {
    super(selectExecutor);
    this.metadata = metadata;
  }

  @Override
  void handle(Connection connection, DatabaseCharsetChecker.State state) throws SQLException {
    // PostgreSQL does not have concept of case-sensitive collation. Only charset ("encoding" in postgresql terminology)
    // must be verified.
    expectUtf8AsDefault(connection);

    if (state == DatabaseCharsetChecker.State.UPGRADE || state == DatabaseCharsetChecker.State.STARTUP) {
      // no need to check columns on fresh installs... as they are not supposed to exist!
      expectUtf8Columns(connection);
    }
  }

  private void expectUtf8AsDefault(Connection connection) throws SQLException {
    Loggers.get(getClass()).info("Verify that database charset supports UTF8");
    String collation = metadata.getDefaultCharset(connection);
    if (!containsIgnoreCase(collation, UTF8)) {
      throw MessageException.of(format("Database charset is %s. It must support UTF8.", collation));
    }
  }

  private void expectUtf8Columns(Connection connection) throws SQLException {
    // Charset is defined globally and can be overridden on each column.
    // This request returns all VARCHAR columns. Charset may be empty.
    // Examples:
    // issues | key | ''
    // projects | name | utf8
    List<String[]> rows = getSqlExecutor().select(connection, "select table_name, column_name, collation_name " +
      "from information_schema.columns " +
      "where table_schema='public' " +
      "and udt_name='varchar' " +
      "order by table_name, column_name", new SqlExecutor.StringsConverter(3 /* columns returned by SELECT */));
    Set<String> errors = new LinkedHashSet<>();
    for (String[] row : rows) {
      if (!isBlank(row[2]) && !containsIgnoreCase(row[2], UTF8)) {
        errors.add(format("%s.%s", row[0], row[1]));
      }
    }

    if (!errors.isEmpty()) {
      throw MessageException.of(format("Database columns [%s] must have UTF8 charset.", Joiner.on(", ").join(errors)));
    }
  }

  @VisibleForTesting
  PostgresMetadataReader getMetadata() {
    return metadata;
  }

}
