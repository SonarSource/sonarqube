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
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.COLUMN_ORGANIZATION_LEGACY_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.COLUMN_PROJECT_LEGACY_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.ORGANIZATION_LEGACY_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.PROJECT_LEGACY_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.TABLE_NAME;

/**
 * Makes {@code project_legacy_id} and {@code organization_legacy_id} nullable on {@code project_configs}.
 *
 * <p>These columns are a straight port of the SonarCloud reference schema, where they carry SonarCloud
 * legacy ids into the DevOps-platform (DOP) translation path. On-prem the DevOps binding is resolved on
 * demand from the project key at dispatch time, so the columns have no consumer. Relaxing {@code NOT NULL}
 * lets the ProjectConfig API persist rows without writing placeholder values (EA-693). The columns are kept
 * (nullable) rather than dropped, in case a future SonarQube Cloud/Server unification reuses this schema.
 *
 * <p>The two columns were created without a {@code DEFAULT}, so relaxing the nullability constraint is all
 * that is required — no default needs to be dropped afterwards.
 */
public class MakeProjectConfigsLegacyIdsNullable extends DdlChange {

  public MakeProjectConfigsLegacyIdsNullable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      dropNotNull(context, connection, COLUMN_PROJECT_LEGACY_ID, PROJECT_LEGACY_ID_SIZE);
      dropNotNull(context, connection, COLUMN_ORGANIZATION_LEGACY_ID, ORGANIZATION_LEGACY_ID_SIZE);
    }
  }

  private void dropNotNull(Context context, Connection connection, String column, int size) throws SQLException {
    ColumnMetadata columnMetadata = DatabaseUtils.getColumnMetadata(connection, TABLE_NAME, column);
    if (columnMetadata == null) {
      return;
    }

    // Check the column is still NOT NULL before dropping the constraint: AlterColumnsBuilder is not
    // re-entrant on Oracle and throws ORA-01451 if the column is already nullable.
    if (!columnMetadata.nullable()) {
      context.execute(new AlterColumnsBuilder(getDialect(), TABLE_NAME)
        .updateColumn(newVarcharColumnDefBuilder()
          .setColumnName(column)
          .setIsNullable(true)
          .setLimit(size)
          .build())
        .build());
    }
  }
}
