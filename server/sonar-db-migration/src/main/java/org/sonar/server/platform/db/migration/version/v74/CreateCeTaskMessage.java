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
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.def.BigIntegerColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.MAX_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

@SupportsBlueGreen
public class CreateCeTaskMessage extends DdlChange {

  public static final String TABLE_NAME = "ce_task_message";
  private static final VarcharColumnDef COLUMN_UUID = newVarcharColumnDefBuilder()
    .setColumnName("uuid")
    .setIsNullable(false)
    .setLimit(UUID_SIZE)
    .build();
  private static final VarcharColumnDef COLUMN_TASK_UUID = newVarcharColumnDefBuilder()
    .setColumnName("task_uuid")
    .setIsNullable(false)
    .setLimit(UUID_SIZE)
    .build();
  private static final VarcharColumnDef COLUMN_MESSAGE = newVarcharColumnDefBuilder()
    .setColumnName("message")
    .setIsNullable(false)
    .setLimit(MAX_SIZE)
    .build();
  private static final BigIntegerColumnDef COLUMN_CREATED_AT = newBigIntegerColumnDefBuilder()
    .setColumnName("created_at")
    .setIsNullable(false)
    .build();

  public CreateCeTaskMessage(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    CreateTableBuilder createTableBuilder = new CreateTableBuilder(getDialect(), TABLE_NAME);
    if (!createTableBuilder.tableExists(getDatabase())) {
      context.execute(createTableBuilder
        .addPkColumn(COLUMN_UUID)
        .addColumn(COLUMN_TASK_UUID)
        .addColumn(COLUMN_MESSAGE)
        .addColumn(COLUMN_CREATED_AT)
        .build());

      context.execute(new CreateIndexBuilder(getDialect())
        .setTable(TABLE_NAME)
        .setName(TABLE_NAME + "_task")
        .addColumn(COLUMN_TASK_UUID)
        .setUnique(false)
      .build());
    }
  }
}
