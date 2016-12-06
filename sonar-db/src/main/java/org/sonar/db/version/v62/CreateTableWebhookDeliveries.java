/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.version.v62;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.version.CreateIndexBuilder;
import org.sonar.db.version.CreateTableBuilder;
import org.sonar.db.version.DdlChange;
import org.sonar.db.version.VarcharColumnDef;

import static org.sonar.db.version.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.db.version.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.db.version.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.db.version.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.db.version.VarcharColumnDef.UUID_SIZE;
import static org.sonar.db.version.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateTableWebhookDeliveries extends DdlChange {

  private static final String TABLE_NAME = "webhook_deliveries";

  public CreateTableWebhookDeliveries(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    VarcharColumnDef componentUuidColumn = newVarcharColumnDefBuilder().setColumnName("component_uuid").setLimit(UUID_SIZE).setIsNullable(false).build();
    VarcharColumnDef ceTaskUuidColumn = newVarcharColumnDefBuilder().setColumnName("ce_task_uuid").setLimit(UUID_SIZE).setIsNullable(false).build();

    context.execute(
      new CreateTableBuilder(getDialect(), TABLE_NAME)
        .addPkColumn(newVarcharColumnDefBuilder().setColumnName("uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
        .addColumn(componentUuidColumn)
        .addColumn(ceTaskUuidColumn)
        .addColumn(newVarcharColumnDefBuilder().setColumnName("name").setLimit(100).setIsNullable(false).build())
        .addColumn(newVarcharColumnDefBuilder().setColumnName("url").setLimit(2000).setIsNullable(false).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("success").setIsNullable(false).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("http_status").setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("duration_ms").setIsNullable(true).build())
        .addColumn(newClobColumnDefBuilder().setColumnName("payload").setIsNullable(false).build())
        .addColumn(newClobColumnDefBuilder().setColumnName("error_stacktrace").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .build());

    context.execute(
      new CreateIndexBuilder(getDialect())
        .setTable(TABLE_NAME)
        .setName("component_uuid")
        .addColumn(componentUuidColumn)
        .build());
    context.execute(
      new CreateIndexBuilder(getDialect())
        .setTable(TABLE_NAME)
        .setName("ce_task_uuid")
        .addColumn(ceTaskUuidColumn)
        .build());
  }
}
