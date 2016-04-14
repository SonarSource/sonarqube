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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang.StringUtils.endsWithIgnoreCase;

class MysqlCharsetHandler extends CharsetHandler {

  private static final Logger LOGGER = Loggers.get(MysqlCharsetHandler.class);
  private static final String TYPE_LONGTEXT = "longtext";

  protected MysqlCharsetHandler(SqlExecutor selectExecutor) {
    super(selectExecutor);
  }

  @Override
  void handle(Connection connection, boolean enforceUtf8) throws SQLException {
    logInit(enforceUtf8);
    checkCollation(connection, enforceUtf8);
  }

  private static void logInit(boolean enforceUtf8) {
    String message = "Verify that database collation is case-sensitive";
    if (enforceUtf8) {
      message = "Verify that database collation is UTF8";
    }
    LOGGER.info(message);
  }

  private void checkCollation(Connection connection, boolean enforceUtf8) throws SQLException {
    // All VARCHAR columns are returned. No need to check database general collation.
    // Example of row:
    // issues | kee | utf8 | utf8_bin
    List<ColumnDef> columns = select(connection,
      ColumnDef.SELECT_COLUMNS +
        "FROM INFORMATION_SCHEMA.columns " +
        "WHERE table_schema=database() and character_set_name is not null and collation_name is not null", ColumnDef.ColumnDefRowConverter.INSTANCE);
    List<String> utf8Errors = new ArrayList<>();
    for (ColumnDef column : columns) {
      if (enforceUtf8 && !containsIgnoreCase(column.getCharset(), UTF8)) {
        utf8Errors.add(format("%s.%s", column.getTable(), column.getColumn()));
      } else if (endsWithIgnoreCase(column.getCollation(), "_ci")) {
        repairCaseInsensitiveColumn(connection, column);
      }
    }
    if (!utf8Errors.isEmpty()) {
      throw MessageException.of(format("UTF8 case-sensitive collation is required for database columns [%s]", Joiner.on(", ").join(utf8Errors)));
    }
  }

  private void repairCaseInsensitiveColumn(Connection connection, ColumnDef column)
    throws SQLException {
    String csCollation = toCaseSensitive(column.getCollation());

    String nullability = column.isNullable() ? "NULL" : "NOT NULL";
    String type = column.getDataType().equalsIgnoreCase(TYPE_LONGTEXT) ? TYPE_LONGTEXT : format("%s(%d)", column.getDataType(), column.getSize());
    String alterSql = format("ALTER TABLE %s MODIFY %s %s CHARACTER SET '%s' COLLATE '%s' %s",
      column.getTable(), column.getColumn(), type, column.getCharset(), csCollation, nullability);
    LOGGER.info("Changing collation of column [{}.{}] from {} to {} | sql={}", column.getTable(), column.getColumn(), column.getCollation(), csCollation, alterSql);
    getSqlExecutor().executeUpdate(connection, alterSql);
  }

  @VisibleForTesting
  static String toCaseSensitive(String caseInsensitiveCollation) {
    // Example: big5_chinese_ci becomes big5_bin
    // Full list of collations is available with SQL request "show collation"
    return StringUtils.substringBefore(caseInsensitiveCollation, "_") + "_bin";
  }
}
