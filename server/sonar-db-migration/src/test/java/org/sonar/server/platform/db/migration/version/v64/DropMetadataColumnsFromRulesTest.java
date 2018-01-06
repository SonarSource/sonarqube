/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

public class DropMetadataColumnsFromRulesTest {

  private static final String TABLE_RULES = "rules";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DropMetadataColumnsFromRulesTest.class, "rules.sql");

  private DropMetadataColumnsFromRules underTest = new DropMetadataColumnsFromRules(db.database());

  @Test
  public void execute_drops_metadata_columns_from_table_RULES() throws SQLException {
    underTest.execute();

    db.assertColumnDoesNotExist(TABLE_RULES, "note_data");
    db.assertColumnDoesNotExist(TABLE_RULES, "note_user_login");
    db.assertColumnDoesNotExist(TABLE_RULES, "note_created_at");
    db.assertColumnDoesNotExist(TABLE_RULES, "note_updated_at");
    db.assertColumnDoesNotExist(TABLE_RULES, "remediation_function");
    db.assertColumnDoesNotExist(TABLE_RULES, "remediation_gap_mult");
    db.assertColumnDoesNotExist(TABLE_RULES, "remediation_base_effort");
    db.assertColumnDoesNotExist(TABLE_RULES, "tags");
  }
}
