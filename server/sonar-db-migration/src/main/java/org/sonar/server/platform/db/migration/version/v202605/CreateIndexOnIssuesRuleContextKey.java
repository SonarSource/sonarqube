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
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static java.lang.String.format;

/**
 * Purging a Hunter Agent issue's write-up needs to check whether any other issue still references the
 * same (rule_uuid, rule_description_context_key) pair. The pre-existing 'issues_rule_uuid' index is
 * single-column, so that anti-join range-scans every issue of the rule instance-wide. This composite
 * index covers the lookup directly.
 */
public class CreateIndexOnIssuesRuleContextKey extends DdlChange {

  static final String TABLE_NAME = "issues";
  static final String INDEX_NAME = "issues_rule_uuid_ctx_key";
  static final String RULE_UUID_COLUMN_NAME = "rule_uuid";
  static final String RULE_DESCRIPTION_CONTEXT_KEY_COLUMN_NAME = "rule_description_context_key";

  public CreateIndexOnIssuesRuleContextKey(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      createIndex(context, connection);
    }
  }

  private void createIndex(Context context, Connection connection) {
    if (DatabaseUtils.indexExistsIgnoreCase(TABLE_NAME, INDEX_NAME, connection)) {
      return;
    }
    switch (getDialect().getId()) {
      // Partial index: rule_description_context_key is only set by Hunter Agent issues and a handful of
      // plugin-contextual scanner issues, a small fraction of the whole 'issues' table, so this keeps
      // the build cheap on PostgreSQL and SQL Server.
      case PostgreSql.ID, MsSql.ID -> context.execute(
        format("CREATE INDEX %s ON %s (%s, %s) WHERE %s IS NOT NULL",
          INDEX_NAME, TABLE_NAME, RULE_UUID_COLUMN_NAME, RULE_DESCRIPTION_CONTEXT_KEY_COLUMN_NAME,
          RULE_DESCRIPTION_CONTEXT_KEY_COLUMN_NAME));
      // Oracle has no partial-index mechanism, and its usual "all key columns NULL" exclusion doesn't
      // help here either since rule_uuid is populated on nearly every row: this is a full-table build
      // regardless. H2 is test/schema-dump only, so the same plain index is acceptable there.
      case Oracle.ID, H2.ID -> context.execute(
        format("CREATE INDEX %s ON %s (%s, %s)",
          INDEX_NAME, TABLE_NAME, RULE_UUID_COLUMN_NAME, RULE_DESCRIPTION_CONTEXT_KEY_COLUMN_NAME));
      default -> throw new IllegalArgumentException("Unsupported dialect id " + getDialect().getId());
    }
  }
}
