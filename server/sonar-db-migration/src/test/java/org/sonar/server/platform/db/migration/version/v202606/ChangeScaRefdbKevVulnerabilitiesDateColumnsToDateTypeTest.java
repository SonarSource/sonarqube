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
package org.sonar.server.platform.db.migration.version.v202606;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.ColumnMetadata;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.Oracle;
import org.sonar.server.platform.db.migration.def.DateColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.DropColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.DropTableBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.COLUMN_DATE_ADDED;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.COLUMN_DUE_DATE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.TABLE_NAME;

class ChangeScaRefdbKevVulnerabilitiesDateColumnsToDateTypeTest {

  private static final String VULNERABILITY_UUID = "kev-vulnerability-uuid";

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(ChangeScaRefdbKevVulnerabilitiesDateColumnsToDateType.class);

  private final ChangeScaRefdbKevVulnerabilitiesDateColumnsToDateType underTest = new ChangeScaRefdbKevVulnerabilitiesDateColumnsToDateType(db.database());

  @Test
  void execute_when_table_does_not_exist_should_fail() {
    // Arrange
    new DropTableBuilder(db.database().getDialect(), TABLE_NAME).build().forEach(db::executeDdl);
    db.assertTableDoesNotExist(TABLE_NAME);

    // Act & Assert
    assertThatThrownBy(underTest::execute)
      .isInstanceOf(IllegalStateException.class);
    db.assertTableDoesNotExist(TABLE_NAME);
  }

  @Test
  void execute_when_calendar_columns_have_values_should_fail_before_changing_schema() throws Exception {
    // Arrange
    insertVulnerabilityRow(1_720_000_000L, 1_730_000_000L);

    // Act
    assertThatThrownBy(underTest::execute)
      .isInstanceOf(IllegalStateException.class);

    // Assert
    assertLegacyDateColumns();
    assertVulnerabilityRow(1_720_000_000L, 1_730_000_000L);
  }

  @Test
  void execute_when_calendar_columns_are_missing_but_table_has_rows_should_fail() throws Exception {
    insertVulnerabilityRow(1_720_000_000L, 1_730_000_000L);
    dropColumn(COLUMN_DATE_ADDED);
    dropColumn(COLUMN_DUE_DATE);
    assertThat(db.countRowsOfTable(TABLE_NAME)).isEqualTo(1);

    assertThatThrownBy(underTest::execute)
      .isInstanceOf(IllegalStateException.class);

    db.assertColumnDoesNotExist(TABLE_NAME, COLUMN_DATE_ADDED);
    db.assertColumnDoesNotExist(TABLE_NAME, COLUMN_DUE_DATE);
    assertThat(db.countRowsOfTable(TABLE_NAME)).isEqualTo(1);
  }

  @Test
  void execute_when_calendar_columns_are_empty_should_change_them_to_date() throws Exception {
    // Arrange
    assertLegacyDateColumns();

    // Act
    underTest.execute();

    // Assert
    assertDateColumn(COLUMN_DATE_ADDED, false);
    assertDateColumn(COLUMN_DUE_DATE, false);
    assertUnrelatedSchema();
  }

  @Test
  void execute_when_date_added_is_missing_should_resume_the_upgrade() throws Exception {
    // Arrange
    dropColumn(COLUMN_DATE_ADDED);
    db.assertColumnDoesNotExist(TABLE_NAME, COLUMN_DATE_ADDED);

    // Act
    underTest.execute();

    // Assert
    assertDateColumn(COLUMN_DATE_ADDED, false);
    assertDateColumn(COLUMN_DUE_DATE, false);
  }

  @Test
  void execute_when_date_added_is_already_date_should_resume_due_date() throws Exception {
    // Arrange
    replaceWithDateColumn(COLUMN_DATE_ADDED);
    assertDateColumn(COLUMN_DATE_ADDED, false);
    assertLegacyDateColumn(COLUMN_DUE_DATE);

    // Act
    underTest.execute();

    // Assert
    assertDateColumn(COLUMN_DATE_ADDED, false);
    assertDateColumn(COLUMN_DUE_DATE, false);
  }

  @Test
  void execute_when_due_date_is_missing_should_resume_the_upgrade() throws Exception {
    // Arrange
    replaceWithDateColumn(COLUMN_DATE_ADDED);
    dropColumn(COLUMN_DUE_DATE);
    assertDateColumn(COLUMN_DATE_ADDED, false);
    db.assertColumnDoesNotExist(TABLE_NAME, COLUMN_DUE_DATE);

    // Act
    underTest.execute();

    // Assert
    assertDateColumn(COLUMN_DATE_ADDED, false);
    assertDateColumn(COLUMN_DUE_DATE, false);
  }

  @Test
  void execute_when_table_is_already_converted_should_be_reentrant() throws Exception {
    // Arrange
    underTest.execute();

    // Act
    underTest.execute();

    // Assert
    assertDateColumn(COLUMN_DATE_ADDED, false);
    assertDateColumn(COLUMN_DUE_DATE, false);
  }

  @Test
  void execute_when_converted_table_has_rows_should_leave_them_unchanged() throws Exception {
    // Arrange
    underTest.execute();
    insertVulnerabilityRow(LocalDate.of(2024, 2, 29), LocalDate.of(2025, 12, 31));

    // Act
    underTest.execute();

    // Assert
    assertDateColumn(COLUMN_DATE_ADDED, false);
    assertDateColumn(COLUMN_DUE_DATE, false);
    assertVulnerabilityRow(LocalDate.of(2024, 2, 29), LocalDate.of(2025, 12, 31));
  }

  @Test
  void execute_when_date_added_is_nullable_date_should_reject_unexpected_schema() throws Exception {
    // Arrange
    replaceWithDateColumn(COLUMN_DATE_ADDED, true);

    // Act
    assertThatThrownBy(underTest::execute)
      .isInstanceOf(IllegalStateException.class);

    // Assert
    assertDateColumn(COLUMN_DATE_ADDED, true);
    assertLegacyDateColumn(COLUMN_DUE_DATE);
  }

  @Test
  void execute_when_date_added_has_unsupported_type_should_reject_unexpected_schema() throws Exception {
    // Arrange
    dropColumn(COLUMN_DATE_ADDED);
    db.executeDdl(new AddColumnsBuilder(db.database().getDialect(), TABLE_NAME)
      .addColumn(VarcharColumnDef.newVarcharColumnDefBuilder()
        .setColumnName(COLUMN_DATE_ADDED)
        .setLimit(40)
        .setIsNullable(false)
        .build())
      .build());

    // Act
    assertThatThrownBy(underTest::execute)
      .isInstanceOf(IllegalStateException.class);

    // Assert
    db.assertColumnDefinition(TABLE_NAME, COLUMN_DATE_ADDED, Types.VARCHAR, 40, false);
    assertLegacyDateColumn(COLUMN_DUE_DATE);
  }

  private void assertLegacyDateColumns() throws Exception {
    assertLegacyDateColumn(COLUMN_DATE_ADDED);
    assertLegacyDateColumn(COLUMN_DUE_DATE);
  }

  private void assertDateColumn(String columnName, boolean nullable) throws SQLException {
    if (Oracle.ID.equals(db.database().getDialect().getId())) {
      try (Connection connection = db.database().getDataSource().getConnection()) {
        ColumnMetadata metadata = DatabaseUtils.getColumnMetadata(connection, TABLE_NAME, columnName);
        assertThat(metadata).isNotNull();
        assertThat(metadata.sqlType()).isEqualTo(Types.TIMESTAMP);
        assertThat(metadata.nullable()).isEqualTo(nullable);
      }
      return;
    }
    db.assertColumnDefinition(TABLE_NAME, columnName, Types.DATE, null, nullable);
  }

  private void assertLegacyDateColumn(String columnName) throws Exception {
    if (Oracle.ID.equals(db.database().getDialect().getId())) {
      try (Connection connection = db.database().getDataSource().getConnection()) {
        ColumnMetadata metadata = DatabaseUtils.getColumnMetadata(connection, TABLE_NAME, columnName);
        assertThat(metadata).isNotNull();
        assertThat(metadata.sqlType()).isIn(Types.NUMERIC, Types.DECIMAL);
        assertThat(metadata.nullable()).isFalse();
      }
      return;
    }
    db.assertColumnDefinition(TABLE_NAME, columnName, Types.BIGINT, null, false);
  }

  private void assertVulnerabilityRow(Object dateAdded, Object dueDate) {
    Map<String, Object> row = db.selectFirst("SELECT * FROM " + TABLE_NAME +
      " WHERE uuid = '" + VULNERABILITY_UUID + "'");

    assertThat(row).containsOnlyKeys("UUID", "VULNERABILITY_ID", "CWES", "DATE_ADDED", "DUE_DATE", "KNOWN_RANSOMWARE_CAMPAIGN_USE",
      "NOTES", "PRODUCT", "REQUIRED_ACTION", "SHORT_DESCRIPTION", "VENDOR_PROJECT", "VULNERABILITY_NAME", "CREATED_AT", "UPDATED_AT");
    assertThat(row).containsEntry("UUID", VULNERABILITY_UUID);
    assertThat(row).containsEntry("VULNERABILITY_ID", "CVE-2026-0001");
    assertThat(row).containsEntry("CWES", "CWE-79");
    assertRowValue(row.get("DATE_ADDED"), dateAdded);
    assertRowValue(row.get("DUE_DATE"), dueDate);
    assertThat(row).containsEntry("KNOWN_RANSOMWARE_CAMPAIGN_USE", "UNKNOWN");
    assertThat(row).containsEntry("NOTES", "KEV source note");
    assertThat(row).containsEntry("PRODUCT", "sample-product");
    assertThat(row).containsEntry("REQUIRED_ACTION", "Apply the vendor update");
    assertThat(row).containsEntry("SHORT_DESCRIPTION", "Sample vulnerability");
    assertThat(row).containsEntry("VENDOR_PROJECT", "sample-vendor");
    assertThat(row).containsEntry("VULNERABILITY_NAME", "sample-vulnerability");
    assertThat(((Number) row.get("CREATED_AT")).longValue()).isEqualTo(1_000L);
    assertThat(((Number) row.get("UPDATED_AT")).longValue()).isEqualTo(2_000L);
  }

  private static void assertRowValue(Object actual, Object expected) {
    assertThat(actual).isNotNull();
    if (expected instanceof Number expectedNumber) {
      assertThat(actual).isInstanceOf(Number.class);
      assertThat(((Number) actual).longValue()).isEqualTo(expectedNumber.longValue());
    } else {
      assertThat(toLocalDate(actual)).isEqualTo((LocalDate) expected);
    }
  }

  private static LocalDate toLocalDate(Object value) {
    if (value instanceof java.sql.Date sqlDate) {
      return sqlDate.toLocalDate();
    }
    if (value instanceof Timestamp timestamp) {
      return timestamp.toLocalDateTime().toLocalDate();
    }
    if (value instanceof LocalDate localDate) {
      return localDate;
    }
    if (value instanceof Date date) {
      return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
    throw new AssertionError("Unexpected calendar date value type: " + value.getClass().getName());
  }

  private void assertUnrelatedSchema() {
    db.assertPrimaryKey(TABLE_NAME, "pk_sca_ref_kev_vulns", "uuid");
    db.assertUniqueIndex(TABLE_NAME, "kev_vulns_vuln_id_uniq", "vulnerability_id");
    db.assertColumnDefinition(TABLE_NAME, "uuid", Types.VARCHAR, 40, false);
    db.assertColumnDefinition(TABLE_NAME, "vulnerability_id", Types.VARCHAR, 63, false);
    db.assertColumnDefinition(TABLE_NAME, "cwes", Types.CLOB, null, false);
    db.assertColumnDefinition(TABLE_NAME, "known_ransomware_campaign_use", Types.VARCHAR, 10, false);
    db.assertColumnDefinition(TABLE_NAME, "notes", Types.CLOB, null, true);
    db.assertColumnDefinition(TABLE_NAME, "product", Types.VARCHAR, 500, false);
    db.assertColumnDefinition(TABLE_NAME, "required_action", Types.CLOB, null, false);
    db.assertColumnDefinition(TABLE_NAME, "short_description", Types.CLOB, null, false);
    db.assertColumnDefinition(TABLE_NAME, "vendor_project", Types.VARCHAR, 500, false);
    db.assertColumnDefinition(TABLE_NAME, "vulnerability_name", Types.VARCHAR, 500, false);
    db.assertColumnDefinition(TABLE_NAME, "created_at", Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, "updated_at", Types.BIGINT, null, false);
  }

  private void dropColumn(String columnName) {
    new DropColumnsBuilder(db.database().getDialect(), TABLE_NAME, columnName).build().forEach(db::executeDdl);
  }

  private void replaceWithDateColumn(String columnName) {
    replaceWithDateColumn(columnName, false);
  }

  private void replaceWithDateColumn(String columnName, boolean nullable) {
    dropColumn(columnName);
    db.executeDdl(new AddColumnsBuilder(db.database().getDialect(), TABLE_NAME)
      .addColumn(DateColumnDef.newDateColumnDefBuilder().setColumnName(columnName).setIsNullable(nullable).build())
      .build());
  }

  private void insertVulnerabilityRow(Object dateAdded, Object dueDate) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("uuid", VULNERABILITY_UUID);
    values.put("vulnerability_id", "CVE-2026-0001");
    values.put("cwes", "CWE-79");
    values.put(COLUMN_DATE_ADDED, dateAdded);
    values.put(COLUMN_DUE_DATE, dueDate);
    values.put("known_ransomware_campaign_use", "UNKNOWN");
    values.put("notes", "KEV source note");
    values.put("product", "sample-product");
    values.put("required_action", "Apply the vendor update");
    values.put("short_description", "Sample vulnerability");
    values.put("vendor_project", "sample-vendor");
    values.put("vulnerability_name", "sample-vulnerability");
    values.put("created_at", 1_000L);
    values.put("updated_at", 2_000L);
    db.executeInsert(TABLE_NAME, values);
  }
}
