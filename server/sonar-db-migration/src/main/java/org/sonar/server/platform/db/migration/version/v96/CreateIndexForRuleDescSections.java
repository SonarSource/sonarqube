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

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.version.v00.CreateInitialSchema.RULE_UUID_COL_NAME;
import static org.sonar.server.platform.db.migration.version.v95.CreateRuleDescSectionsTable.RULE_DESCRIPTION_SECTIONS_TABLE;
import static org.sonar.server.platform.db.migration.version.v96.AddContextColumnsToRuleDescSectionsTable.COLUMN_CONTEXT_KEY;

public class CreateIndexForRuleDescSections extends DdlChange {

  static final String INDEX_NAME = "uniq_rule_desc_sections";
  static final String COLUMN_KEE = "kee";

  public CreateIndexForRuleDescSections(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      createRuleDescSectionUniqueIndex(context, connection);
    }
  }

  private static void createRuleDescSectionUniqueIndex(Context context, Connection connection) {
    if (!DatabaseUtils.indexExistsIgnoreCase(RULE_DESCRIPTION_SECTIONS_TABLE, INDEX_NAME, connection)) {
      context.execute(new CreateIndexBuilder()
        .setTable(RULE_DESCRIPTION_SECTIONS_TABLE)
        .setName(INDEX_NAME)
        .addColumn(RULE_UUID_COL_NAME)
        .addColumn(COLUMN_KEE)
        .addColumn(COLUMN_CONTEXT_KEY)
        .setUnique(true)
        .build());
    }
  }

}
