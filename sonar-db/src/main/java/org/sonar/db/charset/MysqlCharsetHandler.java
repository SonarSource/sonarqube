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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Loggers;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang.StringUtils.endsWithIgnoreCase;
import static org.sonar.db.DatabaseUtils.closeQuietly;

class MysqlCharsetHandler extends CharsetHandler {

  private final CollationEditor collationEditor;

  protected MysqlCharsetHandler(SelectExecutor selectExecutor) {
    this(selectExecutor, new CollationEditor());
  }

  @VisibleForTesting
  MysqlCharsetHandler(SelectExecutor selectExecutor, CollationEditor editor) {
    super(selectExecutor);
    this.collationEditor = editor;
  }

  @Override
  void handle(Connection connection, boolean enforceUtf8) throws SQLException {
    String message = "Verify that database collation is case-sensitive";
    if (enforceUtf8) {
      message = "Verify that database collation is UTF8";
    }
    Loggers.get(getClass()).info(message);
    checkCollation(connection, enforceUtf8);
  }

  private void checkCollation(Connection connection, boolean enforceUtf8) throws SQLException {
    // All VARCHAR columns are returned. No need to check database general collation.
    // Example of row:
    // issues | kee | utf8 | utf8_bin
    List<String[]> rows = select(connection,
      "SELECT table_name, column_name, character_set_name, collation_name " +
        "FROM INFORMATION_SCHEMA.columns " +
        "WHERE table_schema=database() and character_set_name is not null and collation_name is not null", 4 /* columns */);
    List<String> utf8Errors = new ArrayList<>();
    for (String[] row : rows) {
      String table = row[0];
      String column = row[1];
      String charset = row[2];
      String collation = row[3];
      if (enforceUtf8 && !containsIgnoreCase(charset, UTF8)) {
        utf8Errors.add(format("%s.%s", table, column));
      } else if (endsWithIgnoreCase(collation, "_ci")) {
        repairCaseInsensitiveColumn(connection, table, column, collation);
      }
    }
    if (!utf8Errors.isEmpty()) {
      throw MessageException.of(format("UTF8 case-sensitive collation is required for database columns [%s]", Joiner.on(", ").join(utf8Errors)));
    }
  }

  private void repairCaseInsensitiveColumn(Connection connection, String table, String column, String ciCollation)
    throws SQLException {
    String csCollation = toCaseSensitive(ciCollation);
    Loggers.get(getClass()).info("Changing collation of column [{}.{}] from {} to {}", table, column, ciCollation, csCollation);
    collationEditor.alter(connection, table, column, csCollation);
  }

  @VisibleForTesting
  static String toCaseSensitive(String caseInsensitiveCollation) {
    // example: big5_chinese_ci becomes big5_bin
    return StringUtils.substringBefore(caseInsensitiveCollation, "_") + "_bin";
  }

  @VisibleForTesting
  static class CollationEditor {
    void alter(Connection connection, String table, String column, String csCollation) throws SQLException {
      String charset;
      String dataType;
      boolean isNullable;
      int length;
      PreparedStatement stmt = null;
      ResultSet rs = null;
      try {
        stmt = connection.prepareStatement("SELECT character_set_name, data_type, is_nullable, character_maximum_length " +
          "FROM INFORMATION_SCHEMA.columns " +
          "WHERE table_schema=database() and table_name=? and column_name=?");
        stmt.setString(1, table);
        stmt.setString(2, column);
        rs = stmt.executeQuery();
        rs.next();
        charset = rs.getString(1);
        dataType = rs.getString(2);
        isNullable = rs.getBoolean(3);
        length = rs.getInt(4);
      } finally {
        closeQuietly(stmt);
        closeQuietly(rs);
      }

      try {
        String nullability = isNullable ? "NULL" : "NOT NULL";
        String alter = format("ALTER TABLE %s MODIFY %s %s(%d) CHARACTER SET '%s' COLLATE '%s' %s",
          table, column, dataType, length, charset, csCollation, nullability);
        stmt = connection.prepareStatement(alter);
        stmt.executeUpdate();
      } finally {
        closeQuietly(stmt);
      }
    }
  }
}
