/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v85;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.BigIntegerColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateUserDismissedMessagesTable extends DdlChange {

  private static final String TABLE_NAME = "user_dismissed_messages";

  private static final VarcharColumnDef UUID_COLUMN = newVarcharColumnDefBuilder()
    .setColumnName("uuid")
    .setIsNullable(false)
    .setLimit(UUID_SIZE)
    .build();

  private static final VarcharColumnDef USER_UUID_COLUMN = newVarcharColumnDefBuilder()
    .setColumnName("user_uuid")
    .setIsNullable(false)
    .setLimit(VarcharColumnDef.USER_UUID_SIZE)
    .build();

  private static final VarcharColumnDef PROJECT_UUID = newVarcharColumnDefBuilder()
    .setColumnName("project_uuid")
    .setIsNullable(false)
    .setLimit(UUID_SIZE)
    .build();

  private static final VarcharColumnDef MESSAGE_TYPE = newVarcharColumnDefBuilder()
    .setColumnName("message_type")
    .setIsNullable(false)
    .setLimit(255)
    .build();

  private static final BigIntegerColumnDef CREATED_AT = newBigIntegerColumnDefBuilder()
    .setColumnName("created_at")
    .setIsNullable(false)
    .build();

  public CreateUserDismissedMessagesTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), TABLE_NAME)
      .addPkColumn(UUID_COLUMN)
      .addColumn(USER_UUID_COLUMN)
      .addColumn(PROJECT_UUID)
      .addColumn(MESSAGE_TYPE)
      .addColumn(CREATED_AT)
      .build());

    context.execute(new CreateIndexBuilder()
      .setTable(TABLE_NAME)
      .setName("uniq_user_dismissed_messages")
      .setUnique(true)
      .addColumn(USER_UUID_COLUMN)
      .addColumn(PROJECT_UUID)
      .addColumn(MESSAGE_TYPE)
      .build());

    context.execute(new CreateIndexBuilder()
      .setTable(TABLE_NAME)
      .setName("udm_project_uuid")
      .addColumn(PROJECT_UUID)
      .build());

    context.execute(new CreateIndexBuilder()
      .setTable(TABLE_NAME)
      .setName("udm_message_type")
      .addColumn(MESSAGE_TYPE)
      .build());
  }
}
