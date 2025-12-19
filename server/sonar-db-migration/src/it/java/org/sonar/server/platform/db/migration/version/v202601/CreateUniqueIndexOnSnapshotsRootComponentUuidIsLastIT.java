/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202601;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.Oracle;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.sonar.db.MigrationDbTester.createForMigrationStep;

class CreateUniqueIndexOnSnapshotsRootComponentUuidIsLastIT {

  private static final String TABLE_NAME = "snapshots";
  private static final String INDEX_NAME = "uniq_snapshots_root_comp_uuid_islast";
  private static final String COLUMN_NAME = "root_component_uuid";

  @RegisterExtension
  public final MigrationDbTester db = createForMigrationStep(CreateUniqueIndexOnSnapshotsRootComponentUuidIsLast.class);

  private final DdlChange underTest = new CreateUniqueIndexOnSnapshotsRootComponentUuidIsLast(db.database());

  @Test
  void execute_shouldCreateIndex() throws SQLException {
    assumeNotH2();

    db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);
    underTest.execute();
    assertIndexCreated();
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    assumeNotH2();

    db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);
    underTest.execute();
    underTest.execute();
    assertIndexCreated();
  }

  @Test
  void index_shouldAllowDuplicateRootComponentUuidWhenIsLastIsFalse() throws SQLException {
    assumeNotH2();

    underTest.execute();

    assertThatCode(() -> insertSnapshot("snapshot-1", false)).doesNotThrowAnyException();
    assertThatCode(() -> insertSnapshot("snapshot-2", false)).doesNotThrowAnyException();
  }

  @Test
  void index_shouldPreventDuplicateRootComponentUuidWhenIsLastIsTrue() throws SQLException {
    assumeNotH2();

    underTest.execute();

    insertSnapshot("snapshot-3", true);
    assertThatThrownBy(() -> insertSnapshot("snapshot-4", true))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Fail to execute");
  }

  private void insertSnapshot(String uuid, boolean isLast) {
    db.executeInsert(TABLE_NAME,
      "uuid", uuid,
      "root_component_uuid", "root-component-1",
      "status", "P",
      "islast", isLast,
      "purged", false,
      "created_at", 1000L
    );
  }

  private void assumeNotH2() {
    assumeFalse(H2.ID.equals(db.database().getDialect().getId()), "H2 does not support partial indexes");
  }

  /**
   * For Oracle, we only verify the index exists and is unique, without checking column names.
   * Oracle creates function-based indexes with auto-generated virtual column names (like sys_nc00014$)
   * when using CASE expressions, so we can't verify against the original column name.
   */
  private void assertIndexCreated() {
    if (Oracle.ID.equals(db.database().getDialect().getId())) {
      assertUniqueIndexExistsForOracle();
    } else {
      db.assertUniqueIndex(TABLE_NAME, INDEX_NAME, COLUMN_NAME);
    }
  }

  private void assertUniqueIndexExistsForOracle() {
    try (Connection connection = db.openConnection()) {
      try (ResultSet rs = connection.getMetaData().getIndexInfo(null, null, TABLE_NAME.toUpperCase(), false, false)) {
        boolean indexFound = false;
        while (rs.next()) {
          if (INDEX_NAME.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
            indexFound = true;
            assertThat(rs.getBoolean("NON_UNIQUE")).as("Index should be unique").isFalse();
            break;
          }
        }
        assertThat(indexFound).as("Index %s should exist", INDEX_NAME).isTrue();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to check index", e);
    }
  }
}
