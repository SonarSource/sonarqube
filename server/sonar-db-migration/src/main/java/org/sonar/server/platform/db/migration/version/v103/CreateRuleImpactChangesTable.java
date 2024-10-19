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
package org.sonar.server.platform.db.migration.version.v103;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateRuleImpactChangesTable extends CreateTableChange {

  static final String TABLE_NAME = "rule_impact_changes";

  public CreateRuleImpactChangesTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(DdlChange.Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addColumn(newVarcharColumnDefBuilder().setColumnName("new_software_quality").setIsNullable(true).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("old_software_quality").setIsNullable(true).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("new_severity").setIsNullable(true).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("old_severity").setIsNullable(true).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("rule_change_uuid").setIsNullable(false).setLimit(40).build())
      .build());
  }
}
