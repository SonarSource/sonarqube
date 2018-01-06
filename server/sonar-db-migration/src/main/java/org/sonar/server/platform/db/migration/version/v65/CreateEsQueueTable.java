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
package org.sonar.server.platform.db.migration.version.v65;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.BigIntegerColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class CreateEsQueueTable extends DdlChange {

  private static final String TABLE_NAME = "es_queue";

  public CreateEsQueueTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), TABLE_NAME)
      .addPkColumn(VarcharColumnDef.newVarcharColumnDefBuilder()
        .setColumnName("uuid")
        .setIsNullable(false)
        .setLimit(40)
        .build())
      .addColumn(VarcharColumnDef.newVarcharColumnDefBuilder()
        .setColumnName("doc_type")
        .setIsNullable(false)
        .setLimit(40)
        .build())
      .addColumn(VarcharColumnDef.newVarcharColumnDefBuilder()
        .setColumnName("doc_id")
        .setIsNullable(false)
        .setLimit(4000)
        .build())
      .addColumn(VarcharColumnDef.newVarcharColumnDefBuilder()
        .setColumnName("doc_id_type")
        .setIsNullable(true)
        .setLimit(20)
        .build())
      .addColumn(VarcharColumnDef.newVarcharColumnDefBuilder()
        .setColumnName("doc_routing")
        .setIsNullable(true)
        .setLimit(4000)
        .build())
      .addColumn(BigIntegerColumnDef.newBigIntegerColumnDefBuilder()
        .setColumnName("created_at")
        .setIsNullable(false)
        .build())
      .build()
    );
  }
}
