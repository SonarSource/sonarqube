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
package org.sonar.server.platform.db.migration.version.v102;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateRulesDefaultImpactsTable extends CreateTableChange {
  static final String TABLE_NAME = "rules_default_impacts";

  public CreateRulesDefaultImpactsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName("uuid").setIsNullable(false).setLimit(VarcharColumnDef.UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("rule_uuid").setIsNullable(false).setLimit(VarcharColumnDef.UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("organization_uuid").setIsNullable(true).setLimit(VarcharColumnDef.UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("software_quality").setIsNullable(false).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("severity").setIsNullable(false).setLimit(40).build())
      .build());
  }
}
