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
package org.sonar.server.platform.db.migration.version.v202605;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.sonar.db.ColumnMetadata;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.MsSql;
import org.sonar.server.platform.db.migration.sql.DbPrimaryKeyConstraintFinder;
import org.sonar.server.platform.db.migration.sql.DropPrimaryKeySqlGenerator;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static java.sql.Types.VARCHAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.platform.db.migration.version.v202605.IncreaseIssueStatsRuleKeyColumnSize.RULE_KEY_COLUMN;
import static org.sonar.server.platform.db.migration.version.v202605.IncreaseIssueStatsRuleKeyColumnSize.RULE_KEY_COLUMN_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.IncreaseIssueStatsRuleKeyColumnSize.TABLE_NAME;

class IncreaseIssueStatsRuleKeyColumnSizeTest {

  private static final int AGGREGATION_TYPE_COLUMN_SIZE = 20;
  private static final int AGGREGATION_ID_COLUMN_SIZE = 40;
  private static final int MSSQL_NVARCHAR_BYTES_PER_CHARACTER = 2;
  private static final int MSSQL_CLUSTERED_INDEX_MAX_BYTES = 900;
  private static final int MSSQL_NONCLUSTERED_INDEX_MAX_BYTES = 1_700;

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(IncreaseIssueStatsRuleKeyColumnSize.class);

  private final DropPrimaryKeySqlGenerator sqlGenerator =
    new DropPrimaryKeySqlGenerator(db.database(), new DbPrimaryKeyConstraintFinder(db.database()));
  private final IncreaseIssueStatsRuleKeyColumnSize underTest = new IncreaseIssueStatsRuleKeyColumnSize(db.database(), sqlGenerator,
    new DbPrimaryKeyConstraintFinder(db.database()));

  @Test
  void execute_shouldIncreaseColumnSizeAndBeReentrant() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, RULE_KEY_COLUMN, VARCHAR, 200, false);

    underTest.execute();
    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, RULE_KEY_COLUMN, VARCHAR, RULE_KEY_COLUMN_SIZE, false);
  }

  @Test
  void execute_shouldSupportMaximumCompositePrimaryKeyLengths() throws SQLException {
    String aggregationType = "t".repeat(AGGREGATION_TYPE_COLUMN_SIZE);
    String aggregationId = "i".repeat(AGGREGATION_ID_COLUMN_SIZE);
    String ruleKey = "r".repeat(RULE_KEY_COLUMN_SIZE);

    underTest.execute();
    db.executeInsert(TABLE_NAME,
      "aggregation_type", aggregationType,
      "aggregation_id", aggregationId,
      RULE_KEY_COLUMN, ruleKey,
      "issue_count", 1,
      "rating", 2,
      "mqr_rating", 3);

    assertThat(db.selectFirst("SELECT aggregation_type, aggregation_id, rule_key FROM " + TABLE_NAME))
      .containsEntry("AGGREGATION_TYPE", aggregationType)
      .containsEntry("AGGREGATION_ID", aggregationId)
      .containsEntry("RULE_KEY", ruleKey);
  }

  @Test
  void maximumCompositePrimaryKey_shouldFitMsSqlNonClusteredIndexLimit() {
    int maximumCompositePrimaryKeyBytes =
      (AGGREGATION_TYPE_COLUMN_SIZE + AGGREGATION_ID_COLUMN_SIZE + RULE_KEY_COLUMN_SIZE) * MSSQL_NVARCHAR_BYTES_PER_CHARACTER;

    assertThat(maximumCompositePrimaryKeyBytes)
      .isEqualTo(1_032)
      .isGreaterThan(MSSQL_CLUSTERED_INDEX_MAX_BYTES)
      .isLessThanOrEqualTo(MSSQL_NONCLUSTERED_INDEX_MAX_BYTES);
  }

  @Test
  void execute_whenMsSql_shouldReplacePrimaryKeyWithNonClusteredPrimaryKey() throws SQLException {
    try (var mocks = createMsSqlMigration(200, Optional.of("pk_issue_stats_by_rule_key"))) {
      List<String> dropPrimaryKeySql = List.of("DROP PRIMARY KEY");
      when(mocks.sqlGenerator.generate(TABLE_NAME,
        List.of("aggregation_type", "aggregation_id", RULE_KEY_COLUMN), false)).thenReturn(dropPrimaryKeySql);

      mocks.migration.execute(mocks.context);

      var inOrder = inOrder(mocks.context);
      inOrder.verify(mocks.context).execute(dropPrimaryKeySql);
      inOrder.verify(mocks.context).execute(
        List.of("ALTER TABLE issue_stats_by_rule_key ALTER COLUMN rule_key NVARCHAR (456) NOT NULL"));
      inOrder.verify(mocks.context).execute("ALTER TABLE issue_stats_by_rule_key ADD CONSTRAINT pk_issue_stats_by_rule_key " +
        "PRIMARY KEY NONCLUSTERED (aggregation_type,aggregation_id,rule_key)");
    }
  }

  @Test
  void execute_whenMsSqlMigrationWasInterruptedAfterDroppingPrimaryKey_shouldCompleteMigration() throws SQLException {
    try (var mocks = createMsSqlMigration(200, Optional.empty())) {
      mocks.migration.execute(mocks.context);

      verifyNoInteractions(mocks.sqlGenerator);
      var inOrder = inOrder(mocks.context);
      inOrder.verify(mocks.context).execute(
        List.of("ALTER TABLE issue_stats_by_rule_key ALTER COLUMN rule_key NVARCHAR (456) NOT NULL"));
      inOrder.verify(mocks.context).execute("ALTER TABLE issue_stats_by_rule_key ADD CONSTRAINT pk_issue_stats_by_rule_key " +
        "PRIMARY KEY NONCLUSTERED (aggregation_type,aggregation_id,rule_key)");
    }
  }

  @Test
  void execute_whenMsSqlMigrationWasInterruptedAfterIncreasingColumnSize_shouldRecreatePrimaryKey() throws SQLException {
    try (var mocks = createMsSqlMigration(RULE_KEY_COLUMN_SIZE, Optional.empty())) {
      mocks.migration.execute(mocks.context);

      verify(mocks.context).execute("ALTER TABLE issue_stats_by_rule_key ADD CONSTRAINT pk_issue_stats_by_rule_key " +
        "PRIMARY KEY NONCLUSTERED (aggregation_type,aggregation_id,rule_key)");
    }
  }

  @Test
  void execute_whenMsSqlMigrationIsComplete_shouldBeReentrant() throws SQLException {
    try (var mocks = createMsSqlMigration(RULE_KEY_COLUMN_SIZE, Optional.of("pk_issue_stats_by_rule_key"))) {
      mocks.migration.execute(mocks.context);

      verifyNoInteractions(mocks.context);
    }
  }

  @Test
  void execute_whenColumnDoesNotExist_shouldBeNoOp() throws SQLException {
    try (var mocks = createMsSqlMigration(null, Optional.empty())) {
      mocks.migration.execute(mocks.context);

      verify(mocks.primaryKeyConstraintFinder, never()).findConstraintName(TABLE_NAME);
      verifyNoInteractions(mocks.context);
    }
  }

  private static MigrationMocks createMsSqlMigration(Integer columnSize, Optional<String> primaryKey) throws SQLException {
    Dialect dialect = mock(Dialect.class);
    when(dialect.getId()).thenReturn(MsSql.ID);
    Database database = mock(Database.class, Mockito.withSettings().defaultAnswer(RETURNS_MOCKS));
    when(database.getDialect()).thenReturn(dialect);
    DropPrimaryKeySqlGenerator sqlGenerator = mock(DropPrimaryKeySqlGenerator.class);
    DbPrimaryKeyConstraintFinder primaryKeyConstraintFinder = mock(DbPrimaryKeyConstraintFinder.class);
    when(primaryKeyConstraintFinder.findConstraintName(TABLE_NAME)).thenReturn(primaryKey);
    DdlChange.Context context = mock(DdlChange.Context.class);
    IncreaseIssueStatsRuleKeyColumnSize migration =
      new IncreaseIssueStatsRuleKeyColumnSize(database, sqlGenerator, primaryKeyConstraintFinder);
    ColumnMetadata columnMetadata = columnSize == null ? null : mock(ColumnMetadata.class);
    if (columnMetadata != null) {
      when(columnMetadata.limit()).thenReturn(columnSize);
    }

    MockedStatic<DatabaseUtils> databaseUtils = Mockito.mockStatic(DatabaseUtils.class);
    databaseUtils.when(() -> DatabaseUtils.getColumnMetadata(any(Connection.class), eq(TABLE_NAME), eq(RULE_KEY_COLUMN)))
      .thenReturn(columnMetadata);
    return new MigrationMocks(migration, sqlGenerator, primaryKeyConstraintFinder, context, databaseUtils);
  }

  private record MigrationMocks(IncreaseIssueStatsRuleKeyColumnSize migration, DropPrimaryKeySqlGenerator sqlGenerator,
                                DbPrimaryKeyConstraintFinder primaryKeyConstraintFinder, DdlChange.Context context,
                                MockedStatic<DatabaseUtils> databaseUtils) implements AutoCloseable {
    @Override
    public void close() {
      databaseUtils.close();
    }
  }
}
