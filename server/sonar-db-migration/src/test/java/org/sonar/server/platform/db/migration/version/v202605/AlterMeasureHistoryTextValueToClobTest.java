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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.sql.DropColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.DropTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.OracleIndexTestUtils.assertIndexExistsForOracle;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.SmallIntColumnDef.newSmallIntColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;
import static org.sonar.server.platform.db.migration.version.v202605.AlterMeasureHistoryTextValueToClob.LEGACY_TEXT_VALUE_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.AlterMeasureHistoryTextValueToClob.TEMP_COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateMeasureHistoryTable.ENTITY_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateMeasureHistoryTable.ENTITY_TYPE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateMeasureHistoryTable.METRIC_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateMeasureHistoryTable.MSR_HIST_ENTITY_METRIC_IDX;
import static org.sonar.server.platform.db.migration.version.v202605.CreateMeasureHistoryTable.MSR_HIST_EPOCH_UQ_IDX_2;
import static org.sonar.server.platform.db.migration.version.v202605.CreateMeasureHistoryTable.RECORDED_AT_EPOCH;
import static org.sonar.server.platform.db.migration.version.v202605.CreateMeasureHistoryTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateMeasureHistoryTable.TEXT_VALUE;

class AlterMeasureHistoryTextValueToClobTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(AlterMeasureHistoryTextValueToClob.class);

  private final DdlChange underTest = new AlterMeasureHistoryTextValueToClob(db.database());

  @Test
  void execute_convertsLegacyColumnWithoutLosingHistoryRowsIndexesOrCurrentMeasures() throws SQLException {
    makeTextValueLegacyVarchar();
    String historyValue = "h".repeat(LEGACY_TEXT_VALUE_SIZE);
    String currentMeasuresJson = "{\"ncloc\":42.0}";
    insertHistoryValue("history-value", historyValue);
    insertHistoryValue("history-null", null);
    db.executeInsert("measures",
      "component_uuid", "current-measures",
      "branch_uuid", "current-measures",
      "json_value", currentMeasuresJson,
      "json_value_hash", 0L,
      "created_at", 1L,
      "updated_at", 1L);

    underTest.execute();
    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, TEXT_VALUE, Types.CLOB, null, true);
    db.assertColumnDoesNotExist(TABLE_NAME, TEMP_COLUMN_NAME);
    assertEpochUniqueIndex();
    db.assertIndex(TABLE_NAME, MSR_HIST_ENTITY_METRIC_IDX, ENTITY_ID, ENTITY_TYPE, METRIC_ID);
    assertThat(db.selectFirst("select text_value from measure_history where entity_id = 'history-value'"))
      .containsEntry("TEXT_VALUE", historyValue);
    assertThat(db.selectFirst("select text_value from measure_history where entity_id = 'history-null'"))
      .containsEntry("TEXT_VALUE", null);
    assertThat(db.selectFirst("select json_value from measures where component_uuid = 'current-measures'"))
      .containsEntry("JSON_VALUE", currentMeasuresJson);
  }

  @Test
  void execute_isNoOpWhenTextValueIsAlreadyClob() throws SQLException {
    insertHistoryValue("already-clob", "history value");

    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, TEXT_VALUE, Types.CLOB, null, true);
    db.assertColumnDoesNotExist(TABLE_NAME, TEMP_COLUMN_NAME);
    assertThat(db.selectFirst("select text_value from measure_history where entity_id = 'already-clob'"))
      .containsEntry("TEXT_VALUE", "history value");
  }

  @ParameterizedTest
  @MethodSource("dialectIds")
  void execute_doesNotTreatClobWithLegacySizeAsLegacyColumn(String dialectId) throws SQLException {
    try (MigrationMocks mocks = createMigration(dialectId, LEGACY_TEXT_VALUE_SIZE, true, false, Types.CLOB)) {
      mocks.migration.execute(mocks.context);

      verifyNoInteractions(mocks.context);
    }
  }

  @ParameterizedTest
  @MethodSource("portableClobDdl")
  void execute_resumesWithClobAndTemporaryColumnByCopyingBackAndDroppingTemporaryColumn(String dialectId,
    String addTemporaryColumnSql, String dropColumnSql) throws SQLException {
    try (MigrationMocks mocks = createMigration(dialectId, LEGACY_TEXT_VALUE_SIZE, true, true, Types.CLOB)) {
      mocks.migration.execute(mocks.context);

      var inOrder = inOrder(mocks.context);
      inOrder.verify(mocks.context).execute("UPDATE measure_history SET text_value = text_value_tmp");
      inOrder.verify(mocks.context).execute(List.of(dropColumnSql.replace(TEXT_VALUE, TEMP_COLUMN_NAME)));
      inOrder.verifyNoMoreInteractions();
    }
  }

  @Test
  void execute_resumesAfterAddingTemporaryColumnBeforeCopyingData() throws SQLException {
    makeTextValueLegacyVarchar();
    insertHistoryValue("temporary-added", "history value");
    addClobColumn(TEMP_COLUMN_NAME);

    underTest.execute();

    assertConvertedHistoryValue("temporary-added", "history value");
  }

  @Test
  void execute_resumesAfterDroppingOriginalColumn() throws SQLException {
    makeTextValueLegacyVarchar();
    insertHistoryValue("original-dropped", "history value");
    addClobColumn(TEMP_COLUMN_NAME);
    db.executeUpdateSql("UPDATE " + TABLE_NAME + " SET " + TEMP_COLUMN_NAME + " = " + TEXT_VALUE);
    dropColumn(TEXT_VALUE);

    underTest.execute();

    assertConvertedHistoryValue("original-dropped", "history value");
  }

  @Test
  void execute_resumesAfterRestoringOriginalClobBeforeCopyingDataBack() throws SQLException {
    makeTextValueLegacyVarchar();
    insertHistoryValue("original-restored", "history value");
    addClobColumn(TEMP_COLUMN_NAME);
    db.executeUpdateSql("UPDATE " + TABLE_NAME + " SET " + TEMP_COLUMN_NAME + " = " + TEXT_VALUE);
    dropColumn(TEXT_VALUE);
    addClobColumn(TEXT_VALUE);

    underTest.execute();

    assertConvertedHistoryValue("original-restored", "history value");
  }

  @ParameterizedTest
  @MethodSource("portableClobDdl")
  void execute_generatesPortableStagingDdlForLegacySchema(String dialectId, String addTemporaryColumnSql, String dropColumnSql) throws SQLException {
    try (MigrationMocks mocks = createMigration(dialectId, LEGACY_TEXT_VALUE_SIZE, true, false)) {
      mocks.migration.execute(mocks.context);

      var inOrder = inOrder(mocks.context);
      inOrder.verify(mocks.context).execute(addTemporaryColumnSql);
      inOrder.verify(mocks.context).execute("UPDATE measure_history SET text_value_tmp = text_value");
      inOrder.verify(mocks.context).execute(List.of(dropColumnSql));
      inOrder.verify(mocks.context).execute(addTemporaryColumnSql.replace(TEMP_COLUMN_NAME, TEXT_VALUE));
      inOrder.verify(mocks.context).execute("UPDATE measure_history SET text_value = text_value_tmp");
      inOrder.verify(mocks.context).execute(List.of(dropColumnSql.replace(TEXT_VALUE, TEMP_COLUMN_NAME)));
      inOrder.verifyNoMoreInteractions();
    }
  }

  @ParameterizedTest
  @MethodSource("dialectIds")
  void execute_whenTextValueIsAlreadyLargeText_doesNotIssueDdl(String dialectId) throws SQLException {
    try (MigrationMocks mocks = createMigration(dialectId, Integer.MAX_VALUE, true, false)) {
      mocks.migration.execute(mocks.context);

      verifyNoInteractions(mocks.context);
    }
  }

  private static Stream<Arguments> portableClobDdl() {
    return Stream.of(
      Arguments.of(H2.ID, "ALTER TABLE measure_history ADD (text_value_tmp CLOB NULL)", "ALTER TABLE measure_history DROP COLUMN text_value"),
      Arguments.of(PostgreSql.ID, "ALTER TABLE measure_history ADD COLUMN text_value_tmp TEXT NULL", "ALTER TABLE measure_history DROP COLUMN text_value"),
      Arguments.of(Oracle.ID, "ALTER TABLE measure_history ADD (text_value_tmp CLOB NULL)", "ALTER TABLE measure_history SET UNUSED (text_value)"),
      Arguments.of(MsSql.ID, "ALTER TABLE measure_history ADD text_value_tmp NVARCHAR (MAX) NULL", "ALTER TABLE measure_history DROP COLUMN text_value"));
  }

  private static Stream<String> dialectIds() {
    return Stream.of(H2.ID, PostgreSql.ID, Oracle.ID, MsSql.ID);
  }

  private static MigrationMocks createMigration(String dialectId, int textValueSize, boolean hasTextValue, boolean hasTemporaryTextValue) throws SQLException {
    return createMigration(dialectId, textValueSize, hasTextValue, hasTemporaryTextValue, Types.VARCHAR);
  }

  private static MigrationMocks createMigration(String dialectId, int textValueSize, boolean hasTextValue, boolean hasTemporaryTextValue, int textValueType) throws SQLException {
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
    when(columnMetadata.limit()).thenReturn(textValueSize);
    when(columnMetadata.sqlType()).thenReturn(textValueType);
    MockedStatic<DatabaseUtils> databaseUtils = Mockito.mockStatic(DatabaseUtils.class);
    databaseUtils.when(() -> DatabaseUtils.tableColumnExists(connection, TABLE_NAME, TEXT_VALUE)).thenReturn(hasTextValue);
    databaseUtils.when(() -> DatabaseUtils.tableColumnExists(connection, TABLE_NAME, TEMP_COLUMN_NAME)).thenReturn(hasTemporaryTextValue);
    databaseUtils.when(() -> DatabaseUtils.getColumnMetadata(connection, TABLE_NAME, TEXT_VALUE)).thenReturn(columnMetadata);
    return new MigrationMocks(new AlterMeasureHistoryTextValueToClob(database), context, databaseUtils);
  }

  private void assertEpochUniqueIndex() {
    if (Oracle.ID.equals(db.database().getDialect().getId())) {
      assertIndexExistsForOracle(db, true, MSR_HIST_EPOCH_UQ_IDX_2, TABLE_NAME);
    } else {
      db.assertUniqueIndex(TABLE_NAME, MSR_HIST_EPOCH_UQ_IDX_2, ENTITY_ID, ENTITY_TYPE, METRIC_ID, RECORDED_AT_EPOCH);
    }
  }

  private void makeTextValueLegacyVarchar() {
    Dialect dialect = db.database().getDialect();
    new DropTableBuilder(dialect, TABLE_NAME).build().forEach(db::executeDdl);
    new CreateTableBuilder(dialect, TABLE_NAME)
      .addColumn(newSmallIntColumnDefBuilder().setColumnName(METRIC_ID).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(ENTITY_ID).setIsNullable(false).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(ENTITY_TYPE).setIsNullable(false).setLimit(40).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(RECORDED_AT_EPOCH).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(TEXT_VALUE).setIsNullable(true).setLimit(LEGACY_TEXT_VALUE_SIZE).build())
      .build()
      .forEach(db::executeDdl);
    new CreateIndexBuilder(dialect)
      .setTable(TABLE_NAME)
      .setName(MSR_HIST_EPOCH_UQ_IDX_2)
      .setUnique(true)
      .addColumn(ENTITY_ID, false)
      .addColumn(ENTITY_TYPE, false)
      .addColumn(METRIC_ID, false)
      .addColumn(RECORDED_AT_EPOCH, false, true)
      .build()
      .forEach(db::executeDdl);
    new CreateIndexBuilder(dialect)
      .setTable(TABLE_NAME)
      .setName(MSR_HIST_ENTITY_METRIC_IDX)
      .addColumn(ENTITY_ID)
      .addColumn(ENTITY_TYPE)
      .addColumn(METRIC_ID)
      .build()
      .forEach(db::executeDdl);
  }

  private void insertHistoryValue(String entityId, String textValue) {
    db.executeInsert(TABLE_NAME,
      METRIC_ID, 1,
      ENTITY_ID, entityId,
      ENTITY_TYPE, "PROJECT_BRANCH",
      RECORDED_AT_EPOCH, 1L,
      TEXT_VALUE, textValue);
  }

  private void addClobColumn(String columnName) {
    db.executeDdl(new AddColumnsBuilder(db.database().getDialect(), TABLE_NAME)
      .addColumn(newClobColumnDefBuilder().setColumnName(columnName).setIsNullable(true).build())
      .build());
  }

  private void dropColumn(String columnName) {
    new DropColumnsBuilder(db.database().getDialect(), TABLE_NAME, columnName).build()
      .forEach(db::executeDdl);
  }

  private void assertConvertedHistoryValue(String entityId, String textValue) throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, TEXT_VALUE, Types.CLOB, null, true);
    db.assertColumnDoesNotExist(TABLE_NAME, TEMP_COLUMN_NAME);
    try (Connection connection = db.openConnection();
      PreparedStatement statement = connection.prepareStatement("select text_value from measure_history where entity_id = ?")) {
      statement.setString(1, entityId);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        assertThat(HistoryBackfillSupport.readLargeText(resultSet, 1)).isEqualTo(textValue);
      }
    }
  }

  private record MigrationMocks(AlterMeasureHistoryTextValueToClob migration, DdlChange.Context context,
                                 MockedStatic<DatabaseUtils> databaseUtils) implements AutoCloseable {
    @Override
    public void close() {
      databaseUtils.close();
    }
  }
}
