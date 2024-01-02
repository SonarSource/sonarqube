/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v95;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static java.sql.Types.BIGINT;
import static java.sql.Types.CLOB;
import static java.sql.Types.TINYINT;
import static java.sql.Types.VARCHAR;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.USER_UUID_SIZE;

public class AddRulesMetadataColumnsToRulesTableTest {

  private final String TABLE = "rules";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddRulesMetadataColumnsToRulesTableTest.class, "schema.sql");

  private final DdlChange underTest = new AddRulesMetadataColumnsToRulesTable(db.database());

  @Test
  public void columns_are_created() throws SQLException {
    underTest.execute();

    verifyColumns();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    verifyColumns();
  }

  private void verifyColumns() {
    db.assertColumnDefinition(TABLE, "note_data", CLOB, null, true);
    db.assertColumnDefinition(TABLE, "note_user_uuid", VARCHAR, USER_UUID_SIZE, true);
    db.assertColumnDefinition(TABLE, "note_created_at", BIGINT, null, true);
    db.assertColumnDefinition(TABLE, "note_updated_at", BIGINT, null, true);
    db.assertColumnDefinition(TABLE, "remediation_function", VARCHAR, 20, true);
    db.assertColumnDefinition(TABLE, "remediation_gap_mult", VARCHAR, 20, true);
    db.assertColumnDefinition(TABLE, "remediation_base_effort", VARCHAR, 20, true);
    db.assertColumnDefinition(TABLE, "tags", VARCHAR, 4000, true);
    db.assertColumnDefinition(TABLE, "ad_hoc_name", VARCHAR, 200, true);
    db.assertColumnDefinition(TABLE, "ad_hoc_description", CLOB, null, true);
    db.assertColumnDefinition(TABLE, "ad_hoc_severity", VARCHAR, 10, true);
    db.assertColumnDefinition(TABLE, "ad_hoc_type", TINYINT, null, true);
  }

}