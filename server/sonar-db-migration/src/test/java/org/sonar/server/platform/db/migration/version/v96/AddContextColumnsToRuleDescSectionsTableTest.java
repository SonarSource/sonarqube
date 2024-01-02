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
package org.sonar.server.platform.db.migration.version.v96;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static org.sonar.db.CoreDbTester.createForSchema;
import static org.sonar.server.platform.db.migration.version.v95.CreateRuleDescSectionsTable.RULE_DESCRIPTION_SECTIONS_TABLE;
import static org.sonar.server.platform.db.migration.version.v96.AddContextColumnsToRuleDescSectionsTable.COLUMN_CONTEXT_DISPLAY_NAME;
import static org.sonar.server.platform.db.migration.version.v96.AddContextColumnsToRuleDescSectionsTable.COLUMN_CONTEXT_KEY;

public class AddContextColumnsToRuleDescSectionsTableTest {

  @Rule
  public final CoreDbTester db = createForSchema(AddContextColumnsToRuleDescSectionsTableTest.class, "schema.sql");

  private final AddContextColumnsToRuleDescSectionsTable addContextColumnsToRuleDescSectionsTable = new AddContextColumnsToRuleDescSectionsTable(db.database());

  @Test
  public void column_context_key_should_be_added() throws SQLException {
    db.assertColumnDoesNotExist(RULE_DESCRIPTION_SECTIONS_TABLE, COLUMN_CONTEXT_KEY);

    addContextColumnsToRuleDescSectionsTable.execute();

    db.assertColumnDefinition(RULE_DESCRIPTION_SECTIONS_TABLE, COLUMN_CONTEXT_KEY, Types.VARCHAR, 50, true);
  }

  @Test
  public void column_context_display_name_should_be_added() throws SQLException {
    db.assertColumnDoesNotExist(RULE_DESCRIPTION_SECTIONS_TABLE, COLUMN_CONTEXT_DISPLAY_NAME);

    addContextColumnsToRuleDescSectionsTable.execute();

    db.assertColumnDefinition(RULE_DESCRIPTION_SECTIONS_TABLE, COLUMN_CONTEXT_DISPLAY_NAME, Types.VARCHAR, 50, true);
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    addContextColumnsToRuleDescSectionsTable.execute();

    addContextColumnsToRuleDescSectionsTable.execute();

    db.assertColumnDefinition(RULE_DESCRIPTION_SECTIONS_TABLE, COLUMN_CONTEXT_DISPLAY_NAME, Types.VARCHAR, 50, true);
    db.assertColumnDefinition(RULE_DESCRIPTION_SECTIONS_TABLE, COLUMN_CONTEXT_KEY, Types.VARCHAR, 50, true);
  }

}
