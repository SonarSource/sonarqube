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
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.db.DatabaseUtils.tableColumnExists;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Adds {@code relative_path_from_scm_root} to {@code snapshots}, storing the path of the analysed project
 * relative to the root of its SCM repository, as reported by the scanner. Component paths are stored relative
 * to the project root, so this prefix is what maps them onto repository paths for projects analysed from a
 * subdirectory of their repository. It is recorded per analysis because it changes whenever the project moves
 * within its repository, or the scanner is invoked from a different directory.
 * Nullable: null when the project is analysed from its repository root, when the SCM root cannot be determined,
 * and for analyses created before this column existed. It is sized like {@code components.path}, the column it
 * is joined with.
 */
public class AddRelativePathFromScmRootToSnapshots extends DdlChange {

  static final String TABLE_NAME = "snapshots";
  static final String COLUMN_NAME = "relative_path_from_scm_root";
  static final int COLUMN_SIZE = 2000;

  public AddRelativePathFromScmRootToSnapshots(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (!tableColumnExists(connection, TABLE_NAME, COLUMN_NAME)) {
        context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
          .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_NAME).setIsNullable(true).setLimit(COLUMN_SIZE).build())
          .build());
      }
    }
  }
}
