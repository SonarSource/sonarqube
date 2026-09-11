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
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.sonar.db.MigrationDbTester.createForMigrationStep;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIndexOnIssuesHotspotMigration.INDEX_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIndexOnIssuesHotspotMigration.TABLE_NAME;

class CreateIndexOnIssuesHotspotMigrationIT {
  @RegisterExtension
  public final MigrationDbTester db = createForMigrationStep(CreateIndexOnIssuesHotspotMigration.class);
  private final DdlChange underTest = new CreateIndexOnIssuesHotspotMigration(db.database());

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);

    underTest.execute();
    underTest.execute();

    if (!supportsPartialIndex()) {
      // issue_type leads here: it stands in for the partial predicate the dialect cannot express.
      db.assertIndex(TABLE_NAME, INDEX_NAME, "issue_type", "project_uuid", "kee", "rule_uuid");
    } else if (PostgreSql.ID.equals(db.database().getDialect().getId())) {
      // Unlike the other supported drivers, PostgreSQL's JDBC getIndexInfo() reports the INCLUDE-d columns of a
      // covering index alongside the key columns.
      db.assertIndex(TABLE_NAME, INDEX_NAME, "project_uuid", "kee", "rule_uuid");
    } else {
      db.assertIndex(TABLE_NAME, INDEX_NAME, "project_uuid", "kee");
    }
  }

  @Test
  void execute_shouldCreatePartialIndexOnPostgres() throws SQLException {
    assumeTrue(PostgreSql.ID.equals(db.database().getDialect().getId()));

    underTest.execute();

    List<Map<String, Object>> rows = db.select("select indexdef from pg_indexes where indexname = '" + INDEX_NAME + "'");
    assertThat(rows).hasSize(1);
    assertThat((String) rows.get(0).get("indexdef"))
      // (project_uuid, kee) in this order is what lets the index serve the keyset seek and the ORDER BY, not just
      // the filter - a different order would silently reintroduce a sort.
      .contains("(project_uuid, kee)")
      // rule_uuid must be INCLUDE-d rather than a key column: the rules semi-join reads it, but as a key it would
      // take part in the keyset seek. Without it the scan cannot be index-only.
      .contains("INCLUDE (rule_uuid)")
      .contains("WHERE (issue_type = 4)");
  }

  @Test
  void execute_shouldCreateFilteredIndexOnMssql() throws SQLException {
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
    assertThat((String) row.get("filter_definition")).contains("issue_type").contains("4");
    assertThat((String) row.get("included_columns")).isEqualTo("rule_uuid");
  }

  @Test
  void execute_shouldCreateFullIndexOnOracleAndH2() throws SQLException {
    assumeTrue(!supportsPartialIndex());

    underTest.execute();

    // Neither dialect supports a partial predicate or INCLUDE, so every column the query needs is a key column,
    // with issue_type leading so the equality filter is still seekable. Column order is the point of this
    // assertion: project_uuid before kee is what serves the keyset seek and the ORDER BY without a sort, and
    // rule_uuid must stay last so it never participates in either.
    db.assertIndex(TABLE_NAME, INDEX_NAME, "issue_type", "project_uuid", "kee", "rule_uuid");
  }

  private boolean supportsPartialIndex() {
    String dialect = db.database().getDialect().getId();
    return !H2.ID.equals(dialect) && !Oracle.ID.equals(dialect);
  }
}
