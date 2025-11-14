/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.step;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.ColumnMetadata;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.def.ColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.RenameColumnsBuilder;

public abstract class RenameVarcharColumnChange extends DdlChange {

  protected final String table;
  protected final String oldColumn;
  protected final String newColumn;

  protected RenameVarcharColumnChange(Database db, String table, String oldColumn, String newColumn) {
    super(db);
    this.table = table;
    this.oldColumn = oldColumn;
    this.newColumn = newColumn;
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection c = getDatabase().getDataSource().getConnection()) {
      ColumnMetadata oldColumnMetadata = DatabaseUtils.getColumnMetadata(c, table, oldColumn);
      if (!DatabaseUtils.tableColumnExists(c, table, newColumn) && oldColumnMetadata != null) {
        ColumnDef newColumnDef = new VarcharColumnDef.Builder()
          .setColumnName(newColumn)
          .setIsNullable(oldColumnMetadata.nullable())
          .setLimit(oldColumnMetadata.limit())
          .build();

        context.execute(new RenameColumnsBuilder(getDialect(), table).renameColumn(oldColumn, newColumnDef).build());
      }
    }
  }
}
