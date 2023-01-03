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
package org.sonar.server.platform.db.migration.version.v84;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;

public class CreateSamlMessageIdsTable extends DdlChange {

  private static final String TABLE_NAME = "saml_message_ids";
  private static final VarcharColumnDef MESSAGE_ID_COLUMN = VarcharColumnDef.newVarcharColumnDefBuilder()
    .setColumnName("message_id")
    .setLimit(255)
    .setIsNullable(false)
    .build();

  public CreateSamlMessageIdsTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), TABLE_NAME)
      .addPkColumn(VarcharColumnDef.newVarcharColumnDefBuilder()
        .setColumnName("uuid")
        .setLimit(UUID_SIZE)
        .setIsNullable(false)
        .build())
      .addColumn(MESSAGE_ID_COLUMN)
      .addColumn(newBigIntegerColumnDefBuilder()
        .setColumnName("expiration_date")
        .setIsNullable(false)
        .build())
      .addColumn(newBigIntegerColumnDefBuilder()
        .setColumnName("created_at")
        .setIsNullable(false)
        .build())
      .build());

    context.execute(new CreateIndexBuilder()
      .setTable(TABLE_NAME)
      .setName("saml_message_ids_unique")
      .addColumn(MESSAGE_ID_COLUMN)
      .setUnique(true)
      .build());
  }
}
