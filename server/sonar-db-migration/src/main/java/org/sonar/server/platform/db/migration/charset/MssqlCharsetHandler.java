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

import com.google.common.annotations.VisibleForTesting;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.containsIgnoreCase;

class MssqlCharsetHandler extends CharsetHandler {

  private static final Logger LOGGER = Loggers.get(MssqlCharsetHandler.class);
  private static final String CASE_SENSITIVE_ACCENT_SENSITIVE = "_CS_AS";
  private static final String CASE_INSENSITIVE_ACCENT_INSENSITIVE = "_CI_AI";
  private static final String CASE_INSENSITIVE_ACCENT_SENSITIVE = "_CI_AS";
  private static final String CASE_SENSITIVE_ACCENT_INSENSITIVE = "_CS_AI";
  private static final String BIN = "BIN";
  private static final String BIN2 = "BIN2";

  private final MssqlMetadataReader metadata;

  MssqlCharsetHandler(SqlExecutor selectExecutor, MssqlMetadataReader metadataReader) {
    super(selectExecutor);
    this.metadata = metadataReader;
  }

  @Override
  void handle(Connection connection, DatabaseCharsetChecker.State state) throws SQLException {
    expectCaseSensitiveDefaultCollation(connection);
    if (state == DatabaseCharsetChecker.State.UPGRADE || state == DatabaseCharsetChecker.State.STARTUP) {
      repairColumns(connection);
    }
  }

  private void expectCaseSensitiveDefaultCollation(Connection connection) throws SQLException {
    LOGGER.info("Verify that database collation is case-sensitive and accent-sensitive");
    String defaultCollation = metadata.getDefaultCollation(connection);

    if (!isCollationCorrect(defaultCollation)) {
      String fixedCollation = toCaseSensitive(defaultCollation);
      throw MessageException.of(format(
        "Database collation must be case-sensitive and accent-sensitive. It is %s but should be %s.", defaultCollation, fixedCollation));
    }
  }

  private void repairColumns(Connection connection) throws SQLException {
    String defaultCollation = metadata.getDefaultCollation(connection);

    // All VARCHAR columns are returned. No need to check database general collation.
    // Example of row:
    // issues | kee | Latin1_General_CS_AS or Latin1_General_100_CI_AS_KS_WS
    List<ColumnDef> columns = metadata.getColumnDefs(connection);
    for (ColumnDef column : columns.stream().filter(ColumnDef::isInSonarQubeTable).collect(Collectors.toList())) {
      String collation = column.getCollation();
      if (!isCollationCorrect(collation)) {
        repairColumnCollation(connection, column, toCaseSensitive(collation));
      } else if ("Latin1_General_CS_AS".equals(collation) && !collation.equals(defaultCollation)) {
        repairColumnCollation(connection, column, defaultCollation);
      }
    }
  }

  /**
   * Collation is correct if contains {@link #CASE_SENSITIVE_ACCENT_SENSITIVE} or {@link #BIN} or {@link #BIN2}.
   */
  private static boolean isCollationCorrect(String collation) {
    return containsIgnoreCase(collation, CASE_SENSITIVE_ACCENT_SENSITIVE)
      || containsIgnoreCase(collation, BIN)
      || containsIgnoreCase(collation, BIN2);
  }

  private void repairColumnCollation(Connection connection, ColumnDef column, String expectedCollation) throws SQLException {
    // 1. select the indices defined on this column
    List<ColumnIndex> indices = metadata.getColumnIndices(connection, column);

    // 2. drop indices
    for (ColumnIndex index : indices) {
      getSqlExecutor().executeDdl(connection, format("DROP INDEX %s.%s", column.getTable(), index.name));
    }

    // 3. alter collation of column
    String nullability = column.isNullable() ? "NULL" : "NOT NULL";
    String size = column.getSize() >= 0 ? String.valueOf(column.getSize()) : "max";
    String alterSql = format("ALTER TABLE %s ALTER COLUMN %s %s(%s) COLLATE %s %s",
      column.getTable(), column.getColumn(), column.getDataType(), size, expectedCollation, nullability);
    LOGGER.info("Changing collation of column [{}.{}] from {} to {} | sql=", column.getTable(), column.getColumn(), column.getCollation(), expectedCollation, alterSql);
    getSqlExecutor().executeDdl(connection, alterSql);

    // 4. re-create indices
    for (ColumnIndex index : indices) {
      String uniqueSql = index.unique ? "UNIQUE" : "";
      String createIndexSql = format("CREATE %s INDEX %s ON %s (%s)", uniqueSql, index.name, column.getTable(), index.csvColumns);
      getSqlExecutor().executeDdl(connection, createIndexSql);
    }
  }

  @VisibleForTesting
  static String toCaseSensitive(String collation) {
    // Example: Latin1_General_CI_AI --> Latin1_General_CS_AS or Latin1_General_100_CI_AS_KS_WS --> Latin1_General_100_CS_AS_KS_WS
    return collation
      .replace(CASE_INSENSITIVE_ACCENT_INSENSITIVE, CASE_SENSITIVE_ACCENT_SENSITIVE)
      .replace(CASE_INSENSITIVE_ACCENT_SENSITIVE, CASE_SENSITIVE_ACCENT_SENSITIVE)
      .replace(CASE_SENSITIVE_ACCENT_INSENSITIVE, CASE_SENSITIVE_ACCENT_SENSITIVE);
  }

  @VisibleForTesting
  static class ColumnIndex {
    private final String name;
    private final boolean unique;
    private final String csvColumns;

    public ColumnIndex(String name, boolean unique, String csvColumns) {
      this.name = name;
      this.unique = unique;
      this.csvColumns = csvColumns;
    }
  }

  @VisibleForTesting
  enum ColumnIndexConverter implements SqlExecutor.RowConverter<ColumnIndex> {
    INSTANCE;
    @Override
    public ColumnIndex convert(ResultSet rs) throws SQLException {
      return new ColumnIndex(rs.getString(1), rs.getBoolean(2), rs.getString(3));
    }
  }

}
