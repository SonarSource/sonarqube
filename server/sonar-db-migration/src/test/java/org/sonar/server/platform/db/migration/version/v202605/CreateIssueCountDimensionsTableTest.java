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

import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueCountDimensionsTable.TABLE_NAME;

class CreateIssueCountDimensionsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateIssueCountDimensionsTable.class);

  private final CreateIssueCountDimensionsTable underTest = new CreateIssueCountDimensionsTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_issue_count_dimensions", "id");
    int smallIntType = Oracle.ID.equals(Objects.requireNonNull(db.database().getDialect()).getId()) ? Types.NUMERIC : Types.SMALLINT;
    db.assertColumnDefinition(TABLE_NAME, "id", Types.INTEGER, null, false);
    db.assertColumnDefinition(TABLE_NAME, "hotspot_resolution", Types.VARCHAR, 40, true);
    db.assertColumnDefinition(TABLE_NAME, "issue_code_scope", Types.VARCHAR, 40, false);
    db.assertColumnDefinition(TABLE_NAME, "issue_severity", Types.VARCHAR, 40, false);
    db.assertColumnDefinition(TABLE_NAME, "issue_status", Types.VARCHAR, 40, true);
    db.assertColumnDefinition(TABLE_NAME, "status", Types.VARCHAR, 40, true);
    db.assertColumnDefinition(TABLE_NAME, "issue_type", smallIntType, null, false);
    db.assertColumnDefinition(TABLE_NAME, "rule_key", Types.VARCHAR, 200, false);
    db.assertColumnDefinition(TABLE_NAME, "dimension_hash", Types.VARCHAR, 40, false);
    db.assertColumnDefinition(TABLE_NAME, "maintainability_rating", smallIntType, null, false);
    db.assertColumnDefinition(TABLE_NAME, "security_rating", smallIntType, null, false);
    db.assertColumnDefinition(TABLE_NAME, "reliability_rating", smallIntType, null, false);
    db.assertIndex(TABLE_NAME, "iss_cnt_dim_rule_key_idx", "rule_key");
    db.assertUniqueIndex(TABLE_NAME, "iss_cnt_dim_hash_uq_idx", "dimension_hash");
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
