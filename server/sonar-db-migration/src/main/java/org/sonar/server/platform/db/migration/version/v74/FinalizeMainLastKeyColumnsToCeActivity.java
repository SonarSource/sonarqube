/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.server.platform.db.migration.def.BooleanColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.AlterColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.DropColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.DropIndexBuilder;
import org.sonar.server.platform.db.migration.sql.RenameColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class FinalizeMainLastKeyColumnsToCeActivity extends DdlChange {
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
  private static final VarcharColumnDef COLUMN_STATUS = newVarcharColumnDefBuilder()
    .setColumnName("status")
    .setLimit(15)
    .setIsNullable(false)
    .build();

  public FinalizeMainLastKeyColumnsToCeActivity(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    // drop index on existing columns
    context.execute(new DropIndexBuilder(getDialect())
      .setTable(TABLE_NAME)
      .setName("ce_activity_islastkey")
      .build());
    context.execute(new DropIndexBuilder(getDialect())
      .setTable(TABLE_NAME)
      .setName("ce_activity_islast_status")
      .build());

    // drop existing columns
    context.execute(new DropColumnsBuilder(getDialect(), TABLE_NAME, COLUMN_IS_LAST.getName(), COLUMN_IS_LAST_KEY.getName())
      .build());

    // drop index on tmp columns
    context.execute(new DropIndexBuilder(getDialect())
      .setTable(TABLE_NAME)
      .setName("ce_activity_t_islast_key")
      .build());
    context.execute(new DropIndexBuilder(getDialect())
      .setTable(TABLE_NAME)
      .setName("ce_activity_t_islast")
      .build());
    context.execute(new DropIndexBuilder(getDialect())
      .setTable(TABLE_NAME)
      .setName("ce_activity_t_main_islast_key")
      .build());
    context.execute(new DropIndexBuilder(getDialect())
      .setTable(TABLE_NAME)
      .setName("ce_activity_t_main_islast")
      .build());

    // rename tmp columns
    context.execute(new RenameColumnsBuilder(getDialect(), TABLE_NAME)
      .renameColumn("tmp_is_last", COLUMN_IS_LAST)
      .renameColumn("tmp_is_last_key", COLUMN_IS_LAST_KEY)
      .renameColumn("tmp_main_is_last", COLUMN_MAIN_IS_LAST)
      .renameColumn("tmp_main_is_last_key", COLUMN_MAIN_IS_LAST_KEY)
      .build());

    // make not nullable
    context.execute(new AlterColumnsBuilder(getDialect(), TABLE_NAME)
      .updateColumn(COLUMN_IS_LAST)
      .updateColumn(COLUMN_IS_LAST_KEY)
      .updateColumn(COLUMN_MAIN_IS_LAST)
      .updateColumn(COLUMN_MAIN_IS_LAST_KEY)
      .build());

    // create indexes
    context.execute(new CreateIndexBuilder(getDialect())
      .setTable(TABLE_NAME)
      .setName(TABLE_NAME + "_islast_key")
      .addColumn(COLUMN_IS_LAST_KEY)
      .setUnique(false)
      .build());
    context.execute(new CreateIndexBuilder(getDialect())
      .setTable(TABLE_NAME)
      .setName(TABLE_NAME + "_islast")
      .addColumn(COLUMN_IS_LAST)
      .addColumn(COLUMN_STATUS)
      .setUnique(false)
      .build());
    context.execute(new CreateIndexBuilder(getDialect())
      .setTable(TABLE_NAME)
      .setName(TABLE_NAME + "_main_islast_key")
      .addColumn(COLUMN_MAIN_IS_LAST_KEY)
      .setUnique(false)
      .build());
    context.execute(new CreateIndexBuilder(getDialect())
      .setTable(TABLE_NAME)
      .setName(TABLE_NAME + "_main_islast")
      .addColumn(COLUMN_MAIN_IS_LAST)
      .addColumn(COLUMN_STATUS)
      .setUnique(false)
      .build());
  }
}
