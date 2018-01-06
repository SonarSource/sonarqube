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
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.endsWithIgnoreCase;

class MysqlCharsetHandler extends CharsetHandler {

  private static final Logger LOGGER = Loggers.get(MysqlCharsetHandler.class);
  private static final String TYPE_LONGTEXT = "longtext";

  MysqlCharsetHandler(SqlExecutor selectExecutor) {
    super(selectExecutor);
  }

  @Override
  void handle(Connection connection, DatabaseCharsetChecker.State state) throws SQLException {
    // all the VARCHAR columns have always been created with UTF8 charset on mysql
    // (since SonarQube 2.12 to be precise). The default charset does not require
    // to be UTF8. It is not used. No need to verify it.
    // Still if a column has been accidentally created with a case-insensitive collation,
    // then we can repair it by moving to the same case-sensitive collation. That should
    // never occur.
    if (state == DatabaseCharsetChecker.State.UPGRADE) {
      repairCaseInsensitiveColumns(connection);
    }
  }

  private void repairCaseInsensitiveColumns(Connection connection) throws SQLException {
    // All VARCHAR columns are returned. No need to check database general collation.
    // Example of row:
    // issues | kee | utf8 | utf8_bin
    List<ColumnDef> columns = getSqlExecutor().select(connection,
      ColumnDef.SELECT_COLUMNS +
        "FROM INFORMATION_SCHEMA.columns " +
        "WHERE table_schema=database() and character_set_name is not null and collation_name is not null",
      ColumnDef.ColumnDefRowConverter.INSTANCE);

    List<ColumnDef> invalidColumns = columns.stream()
      .filter(ColumnDef::isInSonarQubeTable)
      .filter(column -> endsWithIgnoreCase(column.getCollation(), "_ci"))
      .collect(Collectors.toList());
    for (ColumnDef column : invalidColumns) {
      repairCaseInsensitiveColumn(connection, column);
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
    getSqlExecutor().executeDdl(connection, alterSql);
  }

  private static String toCaseSensitive(String caseInsensitiveCollation) {
    // Example: big5_chinese_ci becomes big5_bin
    // Full list of collations is available with SQL request "show collation"
    return StringUtils.substringBefore(caseInsensitiveCollation, "_") + "_bin";
  }
}
