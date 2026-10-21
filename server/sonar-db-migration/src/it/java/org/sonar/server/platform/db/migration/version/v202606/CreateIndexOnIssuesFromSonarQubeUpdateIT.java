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
import static org.sonar.db.OracleIndexTestUtils.assertIndexExistsForOracle;
import static org.sonar.server.platform.db.migration.version.v202606.CreateIndexOnIssuesFromSonarQubeUpdate.COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v202606.CreateIndexOnIssuesFromSonarQubeUpdate.INDEX_NAME;
import static org.sonar.server.platform.db.migration.version.v202606.CreateIndexOnIssuesFromSonarQubeUpdate.TABLE_NAME;

class CreateIndexOnIssuesFromSonarQubeUpdateIT {

  @RegisterExtension
  public final MigrationDbTester db = createForMigrationStep(CreateIndexOnIssuesFromSonarQubeUpdate.class);

  private final DdlChange underTest = new CreateIndexOnIssuesFromSonarQubeUpdate(db.database());

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);

    underTest.execute();
    underTest.execute();

    switch (dialectId()) {
      case H2.ID -> db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);
      case Oracle.ID -> assertIndexExistsForOracle(db, false, INDEX_NAME, TABLE_NAME);
      default -> db.assertIndex(TABLE_NAME, INDEX_NAME, COLUMN_NAME);
    }
  }

  @Test
  void execute_shouldCreatePartialIndexOnPostgres() throws SQLException {
    assumeTrue(PostgreSql.ID.equals(dialectId()));

    underTest.execute();

    List<Map<String, Object>> rows = db.select("select indexdef from pg_indexes where indexname = '" + INDEX_NAME + "'");
    assertThat(rows).hasSize(1);
    assertThat((String) rows.get(0).get("indexdef")).contains("WHERE (from_sonarqube_update = true)");
  }

  @Test
  void execute_shouldCreateFilteredIndexOnMssql() throws SQLException {
    assumeTrue(MsSql.ID.equals(dialectId()));

    underTest.execute();

    List<Map<String, Object>> rows = db.select(
      "select i.filter_definition as filter_definition from sys.indexes i " +
        "join sys.tables t on t.object_id = i.object_id " +
        "where t.name = '" + TABLE_NAME + "' and i.name = '" + INDEX_NAME + "'");
    assertThat(rows).hasSize(1);
    assertThat((String) rows.get(0).get("filter_definition")).contains(COLUMN_NAME);
  }

  private String dialectId() {
    return db.database().getDialect().getId();
  }
}
