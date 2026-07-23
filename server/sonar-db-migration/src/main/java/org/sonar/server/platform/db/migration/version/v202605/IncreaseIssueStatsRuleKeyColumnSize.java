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
import java.util.List;
import org.sonar.db.ColumnMetadata;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.dialect.MsSql;
import org.sonar.server.platform.db.migration.sql.AlterColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.DbPrimaryKeyConstraintFinder;
import org.sonar.server.platform.db.migration.sql.DropPrimaryKeySqlGenerator;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class IncreaseIssueStatsRuleKeyColumnSize extends DdlChange {
  static final String TABLE_NAME = "issue_stats_by_rule_key";
  static final String AGGREGATION_TYPE_COLUMN = "aggregation_type";
  static final String AGGREGATION_ID_COLUMN = "aggregation_id";
  static final String RULE_KEY_COLUMN = "rule_key";
  static final int RULE_KEY_COLUMN_SIZE = 456;

  private final DropPrimaryKeySqlGenerator dropPrimaryKeySqlGenerator;
  private final DbPrimaryKeyConstraintFinder primaryKeyConstraintFinder;

  public IncreaseIssueStatsRuleKeyColumnSize(Database db, DropPrimaryKeySqlGenerator dropPrimaryKeySqlGenerator,
    DbPrimaryKeyConstraintFinder primaryKeyConstraintFinder) {
    super(db);
    this.dropPrimaryKeySqlGenerator = dropPrimaryKeySqlGenerator;
    this.primaryKeyConstraintFinder = primaryKeyConstraintFinder;
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (var connection = getDatabase().getDataSource().getConnection()) {
      ColumnMetadata columnMetadata = DatabaseUtils.getColumnMetadata(connection, TABLE_NAME, RULE_KEY_COLUMN);
      if (columnMetadata == null) {
        return;
      }

      boolean shouldIncreaseColumnSize = columnMetadata.limit() < RULE_KEY_COLUMN_SIZE;
      if (MsSql.ID.equals(getDialect().getId())) {
        executeForMsSql(context, shouldIncreaseColumnSize);
      } else if (shouldIncreaseColumnSize) {
        increaseColumnSize(context);
      }
    }
  }

  private void executeForMsSql(Context context, boolean shouldIncreaseColumnSize) throws SQLException {
    boolean hasPrimaryKey = primaryKeyConstraintFinder.findConstraintName(TABLE_NAME).isPresent();
    if (!shouldIncreaseColumnSize && hasPrimaryKey) {
      return;
    }

    if (hasPrimaryKey) {
      // The 1,032-byte widened Unicode composite key exceeds SQL Server's 900-byte clustered index limit,
      // but fits within its 1,700-byte nonclustered index limit.
      context.execute(dropPrimaryKeySqlGenerator.generate(TABLE_NAME,
        List.of(AGGREGATION_TYPE_COLUMN, AGGREGATION_ID_COLUMN, RULE_KEY_COLUMN), false));
    }

    if (shouldIncreaseColumnSize) {
      increaseColumnSize(context);
    }

    context.execute("ALTER TABLE issue_stats_by_rule_key ADD CONSTRAINT pk_issue_stats_by_rule_key " +
      "PRIMARY KEY NONCLUSTERED (aggregation_type,aggregation_id,rule_key)");
  }

  private void increaseColumnSize(Context context) {
    context.execute(new AlterColumnsBuilder(getDialect(), TABLE_NAME)
      .updateColumn(newVarcharColumnDefBuilder()
        .setColumnName(RULE_KEY_COLUMN)
        .setIsNullable(false)
        .setLimit(RULE_KEY_COLUMN_SIZE)
        .build())
      .build());
  }
}
