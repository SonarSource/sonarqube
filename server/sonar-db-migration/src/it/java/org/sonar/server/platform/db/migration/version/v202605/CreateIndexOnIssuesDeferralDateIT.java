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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.sonar.db.MigrationDbTester.createForMigrationStep;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIndexOnIssuesDeferralDate.COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIndexOnIssuesDeferralDate.INDEX_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIndexOnIssuesDeferralDate.TABLE_NAME;

class CreateIndexOnIssuesDeferralDateIT {
  @RegisterExtension
  public final MigrationDbTester db = createForMigrationStep(CreateIndexOnIssuesDeferralDate.class);
  private final DdlChange underTest = new CreateIndexOnIssuesDeferralDate(db.database());

  @Test
  void execute_shouldCreateIndex() throws SQLException {
    db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);
    underTest.execute();
    assertIndexColumns();
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);
    underTest.execute();
    underTest.execute();
    assertIndexColumns();
  }

  private void assertIndexColumns() {
    if (PostgreSql.ID.equals(db.database().getDialect().getId())) {
      // Unlike the other supported drivers, PostgreSQL's JDBC getIndexInfo() reports the
      // INCLUDE-d columns of a covering index alongside the key column.
      db.assertIndex(TABLE_NAME, INDEX_NAME, COLUMN_NAME, "project_uuid", "kee", "status");
    } else {
      db.assertIndex(TABLE_NAME, INDEX_NAME, COLUMN_NAME);
    }
  }

  @Test
  void execute_shouldCreatePartialCoveringIndexOnPostgres() throws SQLException {
    assumeTrue(PostgreSql.ID.equals(db.database().getDialect().getId()));

    underTest.execute();

    List<Map<String, Object>> rows = db.select("select indexdef from pg_indexes where indexname = '" + INDEX_NAME + "'");
    assertThat(rows).hasSize(1);
    String indexDef = (String) rows.get(0).get("indexdef");
    assertThat(indexDef)
      .contains("INCLUDE (project_uuid, kee, status)")
      .contains("WHERE (" + COLUMN_NAME + " IS NOT NULL)");
  }

  @Test
  void execute_shouldCreatePartialCoveringIndexOnMssql() throws SQLException {
    assumeTrue(MsSql.ID.equals(db.database().getDialect().getId()));

    underTest.execute();

    List<Map<String, Object>> rows = db.select(
      "select i.filter_definition as filter_definition, " +
        "stuff((select ',' + c.name from sys.index_columns ic " +
        "join sys.columns c on c.object_id = ic.object_id and c.column_id = ic.column_id " +
        "where ic.object_id = i.object_id and ic.index_id = i.index_id and ic.is_included_column = 1 " +
        "order by ic.index_column_id for xml path('')), 1, 1, '') as included_columns " +
        "from sys.indexes i " +
        "join sys.tables t on t.object_id = i.object_id " +
        "where t.name = '" + TABLE_NAME + "' and i.name = '" + INDEX_NAME + "'");
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.get(0);
    assertThat((String) row.get("filter_definition"))
      .contains(COLUMN_NAME)
      .contains("IS NOT NULL");
    assertThat((String) row.get("included_columns")).isEqualTo("project_uuid,kee,status");
  }
}
