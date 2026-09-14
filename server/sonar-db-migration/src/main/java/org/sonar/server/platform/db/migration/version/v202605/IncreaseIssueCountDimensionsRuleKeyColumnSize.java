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
import org.sonar.db.ColumnMetadata;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.sql.AlterColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueCountDimensionsTable.TABLE_NAME;

public class IncreaseIssueCountDimensionsRuleKeyColumnSize extends DdlChange {

  static final int RULE_KEY_SIZE = 255 + 1 + 200;
  static final String RULE_KEY_COLUMN = "rule_key";

  public IncreaseIssueCountDimensionsRuleKeyColumnSize(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      ColumnMetadata columnMetadata = DatabaseUtils.getColumnMetadata(connection, TABLE_NAME, RULE_KEY_COLUMN);
      if (columnMetadata != null && columnMetadata.limit() < RULE_KEY_SIZE) {
        context.execute(new AlterColumnsBuilder(getDialect(), TABLE_NAME)
          .updateColumn(newVarcharColumnDefBuilder()
            .setColumnName(RULE_KEY_COLUMN)
            .setIsNullable(false)
            .setLimit(RULE_KEY_SIZE)
            .build())
          .build());
      }
    }
  }
}
