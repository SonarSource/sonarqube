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
import java.sql.Types;
import java.util.List;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.sonar.db.ColumnMetadata;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.sql.DropTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.SmallIntColumnDef.newSmallIntColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;
import static org.sonar.server.platform.db.migration.sql.CreateTableBuilder.ColumnFlag.AUTO_INCREMENT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueCountDimensionsTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.IncreaseIssueCountDimensionsRuleKeyColumnSize.RULE_KEY_COLUMN;
import static org.sonar.server.platform.db.migration.version.v202605.IncreaseIssueCountDimensionsRuleKeyColumnSize.RULE_KEY_SIZE;

class IncreaseIssueCountDimensionsRuleKeyColumnSizeTest {

  private static final int LEGACY_RULE_KEY_SIZE = 200;

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(IncreaseIssueCountDimensionsRuleKeyColumnSize.class);

  private final DdlChange underTest = new IncreaseIssueCountDimensionsRuleKeyColumnSize(db.database());

  @Test
  void execute_widensLegacyColumnAndPreservesExistingDimensions() throws SQLException {
    makeRuleKeyLegacyVarchar();
    insertDimension("legacy-rule-key", "a".repeat(40));

    underTest.execute();
    underTest.execute();

    String canonicalRuleKey = "r".repeat(255) + ":" + "k".repeat(200);
    db.assertColumnDefinition(TABLE_NAME, RULE_KEY_COLUMN, Types.VARCHAR, RULE_KEY_SIZE, false);
    db.assertIndex(TABLE_NAME, "iss_cnt_dim_rule_key_idx", RULE_KEY_COLUMN);
    assertThat(db.selectFirst("select rule_key from issue_count_dimensions where dimension_hash = '" + "a".repeat(40) + "'"))
      .containsEntry("RULE_KEY", "legacy-rule-key");

    insertDimension(canonicalRuleKey, "b".repeat(40));

    assertThat(canonicalRuleKey).hasSize(RULE_KEY_SIZE);
    assertThat(db.selectFirst("select rule_key from issue_count_dimensions where dimension_hash = '" + "b".repeat(40) + "'"))
      .containsEntry("RULE_KEY", canonicalRuleKey);
  }

  @Test
  void execute_isNoOpWhenRuleKeyAlreadyHasCanonicalLength() throws SQLException {
    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, RULE_KEY_COLUMN, Types.VARCHAR, RULE_KEY_SIZE, false);
  }

  @ParameterizedTest
  @MethodSource("portableAlterDdl")
  void execute_generatesPortableDdlForLegacySchema(String dialectId, String alterColumnSql) throws SQLException {
    try (MigrationMocks mocks = createMigration(dialectId, LEGACY_RULE_KEY_SIZE)) {
      mocks.migration.execute(mocks.context);

      verify(mocks.context).execute(List.of(alterColumnSql));
    }
  }

  @ParameterizedTest
  @MethodSource("dialectIds")
  void execute_whenRuleKeyAlreadyHasCanonicalLength_doesNotIssueDdl(String dialectId) throws SQLException {
    try (MigrationMocks mocks = createMigration(dialectId, RULE_KEY_SIZE)) {
      mocks.migration.execute(mocks.context);

      verifyNoInteractions(mocks.context);
    }
  }

  private static Stream<Arguments> portableAlterDdl() {
    return Stream.of(
      Arguments.of(H2.ID, "ALTER TABLE issue_count_dimensions ALTER COLUMN rule_key VARCHAR (456) NOT NULL"),
      Arguments.of(PostgreSql.ID, "ALTER TABLE issue_count_dimensions ALTER COLUMN rule_key TYPE VARCHAR (456), ALTER COLUMN rule_key SET NOT NULL"),
      Arguments.of(Oracle.ID, "ALTER TABLE issue_count_dimensions MODIFY (rule_key VARCHAR2 (456 CHAR) NOT NULL)"),
      Arguments.of(MsSql.ID, "ALTER TABLE issue_count_dimensions ALTER COLUMN rule_key NVARCHAR (456) NOT NULL"));
  }

  private static Stream<String> dialectIds() {
    return Stream.of(H2.ID, PostgreSql.ID, Oracle.ID, MsSql.ID);
  }

  private static MigrationMocks createMigration(String dialectId, int ruleKeySize) throws SQLException {
    Dialect dialect = mock(Dialect.class);
    Database database = mock(Database.class);
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    DdlChange.Context context = mock(DdlChange.Context.class);
    ColumnMetadata columnMetadata = mock(ColumnMetadata.class);
    when(database.getDialect()).thenReturn(dialect);
    when(database.getDataSource()).thenReturn(dataSource);
    when(dataSource.getConnection()).thenReturn(connection);
    when(dialect.getId()).thenReturn(dialectId);
    when(columnMetadata.limit()).thenReturn(ruleKeySize);
    MockedStatic<DatabaseUtils> databaseUtils = Mockito.mockStatic(DatabaseUtils.class);
    databaseUtils.when(() -> DatabaseUtils.getColumnMetadata(connection, TABLE_NAME, RULE_KEY_COLUMN)).thenReturn(columnMetadata);
    return new MigrationMocks(new IncreaseIssueCountDimensionsRuleKeyColumnSize(database), context, databaseUtils);
  }

  private void makeRuleKeyLegacyVarchar() {
    Dialect dialect = db.database().getDialect();
    new DropTableBuilder(dialect, TABLE_NAME).build().forEach(db::executeDdl);
    new CreateTableBuilder(dialect, TABLE_NAME)
      .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
      .addColumn(newVarcharColumnDefBuilder().setColumnName("hotspot_resolution").setIsNullable(true).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("issue_code_scope").setIsNullable(false).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("issue_severity").setIsNullable(false).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("issue_status").setIsNullable(true).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("status").setIsNullable(true).setLimit(40).build())
      .addColumn(newSmallIntColumnDefBuilder().setColumnName("issue_type").setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(RULE_KEY_COLUMN).setIsNullable(false).setLimit(LEGACY_RULE_KEY_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("dimension_hash").setIsNullable(false).setLimit(40).build())
      .addColumn(newSmallIntColumnDefBuilder().setColumnName("maintainability_rating").setIsNullable(false).build())
      .addColumn(newSmallIntColumnDefBuilder().setColumnName("security_rating").setIsNullable(false).build())
      .addColumn(newSmallIntColumnDefBuilder().setColumnName("reliability_rating").setIsNullable(false).build())
      .build()
      .forEach(db::executeDdl);
    new CreateIndexBuilder(dialect)
      .setTable(TABLE_NAME)
      .setName("iss_cnt_dim_rule_key_idx")
      .addColumn(RULE_KEY_COLUMN)
      .build()
      .forEach(db::executeDdl);
    new CreateIndexBuilder(dialect)
      .setTable(TABLE_NAME)
      .setName("iss_cnt_dim_hash_uq_idx")
      .setUnique(true)
      .addColumn("dimension_hash", false)
      .build()
      .forEach(db::executeDdl);
  }

  private void insertDimension(String ruleKey, String dimensionHash) {
    db.executeInsert(TABLE_NAME,
      "hotspot_resolution", null,
      "issue_code_scope", "MAIN",
      "issue_severity", "MAJOR",
      "issue_status", "OPEN",
      "status", "OPEN",
      "issue_type", 2,
      RULE_KEY_COLUMN, ruleKey,
      "dimension_hash", dimensionHash,
      "maintainability_rating", 0,
      "security_rating", 0,
      "reliability_rating", 0);
  }

  private record MigrationMocks(IncreaseIssueCountDimensionsRuleKeyColumnSize migration, DdlChange.Context context,
                                 MockedStatic<DatabaseUtils> databaseUtils) implements AutoCloseable {
    @Override
    public void close() {
      databaseUtils.close();
    }
  }
}
