/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.MessageException;

import static java.lang.String.format;
import static org.apache.commons.lang3.Strings.CI;

class MssqlCharsetHandler extends CharsetHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(MssqlCharsetHandler.class);
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
    for (ColumnDef column : columns.stream().filter(ColumnDef::isInSonarQubeTable).toList()) {
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
    return CI.contains(collation, CASE_SENSITIVE_ACCENT_SENSITIVE)
      || CI.contains(collation, BIN)
      || CI.contains(collation, BIN2);
  }

  private void repairColumnCollation(Connection connection, ColumnDef column, String expectedCollation) throws SQLException {
    // Computed columns that reference this column block ALTER COLUMN until they are dropped.
    List<ComputedColumn> computedColumns = metadata.getDependentComputedColumns(connection, column);
    List<ColumnIndex> indices = indexesToRebuild(connection, column, computedColumns);
    // Auto-commit would persist a failure after DROP COLUMN. The next startup then skips repair
    // because the collation is already correct, and kee_lower_hash stays missing.
    boolean autoCommit = connection.getAutoCommit();
    connection.setAutoCommit(false);
    boolean committed = false;
    Exception failure = null;
    try {
      dropIndexes(connection, column.getTable(), indices);
      dropComputedColumns(connection, column.getTable(), computedColumns);
      alterColumnCollation(connection, column, expectedCollation);
      addComputedColumns(connection, column.getTable(), computedColumns);
      createIndexes(connection, column.getTable(), indices);
      connection.commit();
      committed = true;
    } catch (SQLException | RuntimeException e) {
      failure = e;
    } finally {
      if (!committed) {
        SQLException rollbackFailure = rollback(connection);
        if (rollbackFailure == null) {
          failure = restoreAutoCommit(connection, autoCommit, failure);
        } else {
          // setAutoCommit(true) runs COMMIT TRAN while a transaction is open.
          failure = attach(failure, rollbackFailure);
        }
      } else {
        failure = restoreAutoCommit(connection, autoCommit, failure);
      }
    }
    rethrow(failure);
  }

  @Nullable
  private static SQLException rollback(Connection connection) {
    try {
      connection.rollback();
      return null;
    } catch (SQLException e) {
      LOGGER.warn("Failed to roll back column collation repair", e);
      return e;
    }
  }

  @Nullable
  private static Exception restoreAutoCommit(Connection connection, boolean autoCommit, @Nullable Exception failure) {
    try {
      connection.setAutoCommit(autoCommit);
      return failure;
    } catch (SQLException e) {
      return attach(failure, e);
    }
  }

  private static Exception attach(@Nullable Exception failure, Exception extra) {
    if (failure == null) {
      return extra;
    }
    failure.addSuppressed(extra);
    return failure;
  }

  private static void rethrow(@Nullable Exception failure) throws SQLException {
    if (failure == null) {
      return;
    }
    if (failure instanceof RuntimeException runtimeException) {
      throw runtimeException;
    }
    if (failure instanceof SQLException sqlException) {
      throw sqlException;
    }
    throw new IllegalStateException(failure);
  }

  private List<ColumnIndex> indexesToRebuild(Connection connection, ColumnDef column, List<ComputedColumn> computedColumns) throws SQLException {
    List<ColumnIndex> indices = new ArrayList<>(metadata.getColumnIndices(connection, column));
    for (ComputedColumn computedColumn : computedColumns) {
      appendMissingIndexes(indices, metadata.getColumnIndices(connection, computedColumnDef(column, computedColumn)));
    }
    return indices;
  }

  private static ColumnDef computedColumnDef(ColumnDef column, ComputedColumn computedColumn) {
    return new ColumnDef(column.getTable(), computedColumn.name, column.getCharset(), column.getCollation(),
      column.getDataType(), column.getSize(), true);
  }

  private static void appendMissingIndexes(List<ColumnIndex> indices, List<ColumnIndex> extra) {
    for (ColumnIndex index : extra) {
      if (indices.stream().noneMatch(existing -> existing.name.equals(index.name))) {
        indices.add(index);
      }
    }
  }

  private void dropIndexes(Connection connection, String table, List<ColumnIndex> indices) throws SQLException {
    for (ColumnIndex index : indices) {
      getSqlExecutor().executeDdl(connection, format("DROP INDEX %s.%s", table, index.name));
    }
  }

  private void dropComputedColumns(Connection connection, String table, List<ComputedColumn> computedColumns) throws SQLException {
    for (ComputedColumn computedColumn : computedColumns) {
      getSqlExecutor().executeDdl(connection, format("ALTER TABLE %s DROP COLUMN %s", table, computedColumn.name));
    }
  }

  private void alterColumnCollation(Connection connection, ColumnDef column, String expectedCollation) throws SQLException {
    String nullability = column.isNullable() ? "NULL" : "NOT NULL";
    String size = column.getSize() >= 0 ? String.valueOf(column.getSize()) : "max";
    String alterSql = format("ALTER TABLE %s ALTER COLUMN %s %s(%s) COLLATE %s %s",
      column.getTable(), column.getColumn(), column.getDataType(), size, expectedCollation, nullability);
    LOGGER.info("Changing collation of column [{}.{}] from {} to {} | sql={}", column.getTable(), column.getColumn(), column.getCollation(), expectedCollation, alterSql);
    getSqlExecutor().executeDdl(connection, alterSql);
  }

  private void addComputedColumns(Connection connection, String table, List<ComputedColumn> computedColumns) throws SQLException {
    for (ComputedColumn computedColumn : computedColumns) {
      String persisted = computedColumn.persisted ? " PERSISTED" : "";
      getSqlExecutor().executeDdl(connection, format("ALTER TABLE %s ADD %s AS %s%s", table, computedColumn.name, computedColumn.definition, persisted));
    }
  }

  private void createIndexes(Connection connection, String table, List<ColumnIndex> indices) throws SQLException {
    for (ColumnIndex index : indices) {
      String uniqueSql = index.unique ? "UNIQUE" : "";
      String createIndexSql = format("CREATE %s INDEX %s ON %s (%s)", uniqueSql, index.name, table, index.csvColumns);
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

  static class ComputedColumn {
    private final String name;
    private final String definition;
    private final boolean persisted;

    public ComputedColumn(String name, String definition) {
      this(name, definition, false);
    }

    public ComputedColumn(String name, String definition, boolean persisted) {
      this.name = name;
      this.definition = definition;
      this.persisted = persisted;
    }

    String getName() {
      return name;
    }

    String getDefinition() {
      return definition;
    }
  }

  enum ComputedColumnConverter implements SqlExecutor.RowConverter<ComputedColumn> {
    INSTANCE;

    @Override
    public ComputedColumn convert(ResultSet rs) throws SQLException {
      return new ComputedColumn(rs.getString(1), rs.getString(2), rs.getBoolean(3));
    }
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
