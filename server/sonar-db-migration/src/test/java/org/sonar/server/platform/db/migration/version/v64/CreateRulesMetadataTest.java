/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v64;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateRulesMetadataTest {
  private static final String TABLE_RULES_METADATA = "rules_metadata";

  @Rule
  public final CoreDbTester dbTester = CoreDbTester.createForSchema(CreateRulesMetadataTest.class, "empty.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CreateRulesMetadata underTest = new CreateRulesMetadata(dbTester.database());

  @Test
  public void creates_table_on_empty_db() throws SQLException {
    underTest.execute();

    assertThat(dbTester.countRowsOfTable(TABLE_RULES_METADATA)).isEqualTo(0);

    dbTester.assertColumnDefinition(TABLE_RULES_METADATA, "rule_id", Types.INTEGER, null, false);
    dbTester.assertColumnDefinition(TABLE_RULES_METADATA, "organization_uuid", Types.VARCHAR, 40, false);
    dbTester.assertColumnDefinition(TABLE_RULES_METADATA, "note_data", Types.CLOB, null, true);
    dbTester.assertColumnDefinition(TABLE_RULES_METADATA, "note_user_login", Types.VARCHAR, 255, true);
    dbTester.assertColumnDefinition(TABLE_RULES_METADATA, "note_created_at", Types.BIGINT, null, true);
    dbTester.assertColumnDefinition(TABLE_RULES_METADATA, "note_updated_at", Types.BIGINT, null, true);
    dbTester.assertColumnDefinition(TABLE_RULES_METADATA, "remediation_function", Types.VARCHAR, 20, true);
    dbTester.assertColumnDefinition(TABLE_RULES_METADATA, "remediation_gap_mult", Types.VARCHAR, 20, true);
    dbTester.assertColumnDefinition(TABLE_RULES_METADATA, "remediation_base_effort", Types.VARCHAR, 20, true);
    dbTester.assertColumnDefinition(TABLE_RULES_METADATA, "tags", Types.VARCHAR, 4000, true);
    dbTester.assertColumnDefinition(TABLE_RULES_METADATA, "created_at", Types.BIGINT, null, false);
    dbTester.assertColumnDefinition(TABLE_RULES_METADATA, "updated_at", Types.BIGINT, null, false);
    dbTester.assertPrimaryKey(TABLE_RULES_METADATA, "pk_rules_metadata", "rule_id", "organization_uuid");
  }

  @Test
  public void migration_is_not_reentrant() throws SQLException {
    underTest.execute();

    expectedException.expect(IllegalStateException.class);

    underTest.execute();
  }
}
