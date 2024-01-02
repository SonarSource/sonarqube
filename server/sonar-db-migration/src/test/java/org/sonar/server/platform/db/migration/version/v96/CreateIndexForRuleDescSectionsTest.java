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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static org.sonar.server.platform.db.migration.version.v00.CreateInitialSchema.RULE_UUID_COL_NAME;
import static org.sonar.server.platform.db.migration.version.v95.CreateRuleDescSectionsTable.RULE_DESCRIPTION_SECTIONS_TABLE;
import static org.sonar.server.platform.db.migration.version.v96.AddContextColumnsToRuleDescSectionsTable.COLUMN_CONTEXT_KEY;
import static org.sonar.server.platform.db.migration.version.v96.CreateIndexForRuleDescSections.COLUMN_KEE;
import static org.sonar.server.platform.db.migration.version.v96.CreateIndexForRuleDescSections.INDEX_NAME;

public class CreateIndexForRuleDescSectionsTest {

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(CreateIndexForRuleDescSectionsTest.class, "schema.sql");

  private final CreateIndexForRuleDescSections createIndexForRuleDescSections = new CreateIndexForRuleDescSections(db.database());

  @Test
  public void migration_should_create_index() throws SQLException {
    db.assertIndexDoesNotExist(RULE_DESCRIPTION_SECTIONS_TABLE, INDEX_NAME);

    createIndexForRuleDescSections.execute();

    db.assertUniqueIndex(RULE_DESCRIPTION_SECTIONS_TABLE, INDEX_NAME, RULE_UUID_COL_NAME, COLUMN_KEE, COLUMN_CONTEXT_KEY);
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    createIndexForRuleDescSections.execute();

    createIndexForRuleDescSections.execute();

    db.assertUniqueIndex(RULE_DESCRIPTION_SECTIONS_TABLE, INDEX_NAME, RULE_UUID_COL_NAME, COLUMN_KEE, COLUMN_CONTEXT_KEY);
  }

}
