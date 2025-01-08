/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v108;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.ColumnMetadata;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.def.BooleanColumnDef;
import org.sonar.server.platform.db.migration.def.ColumnDef;
import org.sonar.server.platform.db.migration.sql.RenameColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class RenameAiCodeAssuranceColumnInProjects extends DdlChange {

  private static final String TABLE_NAME = "projects";
  private static final String OLD_COLUMN_NAME = "ai_code_assurance";
  private static final String NEW_COLUMN_NAME = "contains_ai_code";

  public RenameAiCodeAssuranceColumnInProjects(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (
      Connection c = getDatabase().getDataSource().getConnection()) {
      ColumnMetadata oldColumnMetadata = DatabaseUtils.getColumnMetadata(c, TABLE_NAME, OLD_COLUMN_NAME);
      if (!DatabaseUtils.tableColumnExists(c, TABLE_NAME, NEW_COLUMN_NAME) && oldColumnMetadata != null) {
        ColumnDef newColumnDef = BooleanColumnDef.newBooleanColumnDefBuilder()
          .setColumnName(NEW_COLUMN_NAME)
          .build();

        context.execute(new RenameColumnsBuilder(getDialect(), TABLE_NAME).renameColumn(OLD_COLUMN_NAME, newColumnDef).build());
      }
    }
  }
}
