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

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

/**
 * Purging a Hunter Agent issue looks up its write-up in 'rule_desc_sections' by (rule_uuid, context_key).
 * The only pre-existing index, 'uniq_rule_desc_sections', has context_key as its third column behind
 * 'kee', so it cannot serve that lookup: the database range-scans every section of the rule instead.
 * This composite index covers the lookup directly.
 */
public class CreateIndexOnRuleDescSectionsRuleContext extends DdlChange {

  static final String TABLE_NAME = "rule_desc_sections";
  static final String INDEX_NAME = "rule_desc_sections_rule_ctx";
  static final String RULE_UUID_COLUMN_NAME = "rule_uuid";
  static final String CONTEXT_KEY_COLUMN_NAME = "context_key";

  public CreateIndexOnRuleDescSectionsRuleContext(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      createIndex(context, connection);
    }
  }

  private void createIndex(Context context, Connection connection) {
    if (!DatabaseUtils.indexExistsIgnoreCase(TABLE_NAME, INDEX_NAME, connection)) {
      context.execute(new CreateIndexBuilder(getDialect())
        .setTable(TABLE_NAME)
        .setName(INDEX_NAME)
        .addColumn(RULE_UUID_COLUMN_NAME, false)
        .addColumn(CONTEXT_KEY_COLUMN_NAME, true)
        .build());
    }
  }
}
