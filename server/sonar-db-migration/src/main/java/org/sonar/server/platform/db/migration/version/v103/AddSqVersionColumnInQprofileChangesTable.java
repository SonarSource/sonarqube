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
package org.sonar.server.platform.db.migration.version.v103;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.def.ColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class AddSqVersionColumnInQprofileChangesTable extends DdlChange {
  private static final String TABLE_NAME = "qprofile_changes";
  private static final String COLUMN_NAME = "sq_version";
  private static final int NEW_COLUMN_SIZE = 40;


  public AddSqVersionColumnInQprofileChangesTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (!DatabaseUtils.tableColumnExists(connection, TABLE_NAME, COLUMN_NAME)) {
        ColumnDef columnDef = VarcharColumnDef.newVarcharColumnDefBuilder()
          .setColumnName(COLUMN_NAME)
          .setLimit(NEW_COLUMN_SIZE)
          .setIsNullable(true)
          .build();
        context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME).addColumn(columnDef).build());
      }
    }
  }
}
