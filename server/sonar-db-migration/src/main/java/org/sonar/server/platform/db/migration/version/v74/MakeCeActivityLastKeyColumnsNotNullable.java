/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v74;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.def.BooleanColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.AlterColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

@SupportsBlueGreen
public class MakeCeActivityLastKeyColumnsNotNullable extends DdlChange {
  private static final String TABLE_NAME = "ce_activity";
  private static final int TASK_TYPE_COLUMN_SIZE = 15;
  private static final BooleanColumnDef COLUMN_IS_LAST = newBooleanColumnDefBuilder()
    .setColumnName("is_last")
    .setIsNullable(false)
    .build();
  private static final VarcharColumnDef COLUMN_IS_LAST_KEY = newVarcharColumnDefBuilder()
    .setColumnName("is_last_key")
    .setLimit(UUID_SIZE + TASK_TYPE_COLUMN_SIZE)
    .setIsNullable(false)
    .build();
  private static final BooleanColumnDef COLUMN_MAIN_IS_LAST = newBooleanColumnDefBuilder()
    .setColumnName("main_is_last")
    .setIsNullable(false)
    .build();
  private static final VarcharColumnDef COLUMN_MAIN_IS_LAST_KEY = newVarcharColumnDefBuilder()
    .setColumnName("main_is_last_key")
    .setLimit(UUID_SIZE + TASK_TYPE_COLUMN_SIZE)
    .setIsNullable(false)
    .build();

  public MakeCeActivityLastKeyColumnsNotNullable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new AlterColumnsBuilder(getDialect(), TABLE_NAME)
      .updateColumn(COLUMN_IS_LAST)
      .updateColumn(COLUMN_IS_LAST_KEY)
      .updateColumn(COLUMN_MAIN_IS_LAST)
      .updateColumn(COLUMN_MAIN_IS_LAST_KEY)
      .build());
  }
}
