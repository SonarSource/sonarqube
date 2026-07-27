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

import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.version.v202605.AddProducerColumnToIssuesTable.COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.AddProducerColumnToIssuesTable.TABLE_NAME;

class AddProducerColumnToIssuesTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(AddProducerColumnToIssuesTable.class);

  private final AddProducerColumnToIssuesTable underTest = new AddProducerColumnToIssuesTable(db.database());

  @Test
  void execute_shouldAddNotNullableColumn() throws SQLException {
    db.assertColumnDoesNotExist(TABLE_NAME, COLUMN_NAME);

    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, tinyIntJdbcType(), null, false);
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, tinyIntJdbcType(), null, false);
  }

  private int tinyIntJdbcType() {
    String dialectId = Objects.requireNonNull(db.database().getDialect()).getId();
    if (Oracle.ID.equals(dialectId)) {
      return Types.NUMERIC;
    }
    if (PostgreSql.ID.equals(dialectId)) {
      return Types.SMALLINT;
    }
    return Types.TINYINT;
  }

  @Test
  void execute_shouldDefaultProducerToScannerOnInsert() throws SQLException {
    underTest.execute();

    insertIssue("issue-uuid");

    assertThat(selectProducer("issue-uuid")).isEqualTo(1);
  }

  private void insertIssue(String kee) {
    db.executeInsert(TABLE_NAME,
      "kee", kee,
      "manual_severity", false);
  }

  private int selectProducer(String kee) {
    var rows = db.select("SELECT producer FROM issues WHERE kee = '" + kee + "'");
    assertThat(rows).hasSize(1);
    return ((Number) rows.getFirst().get("PRODUCER")).intValue();
  }
}
