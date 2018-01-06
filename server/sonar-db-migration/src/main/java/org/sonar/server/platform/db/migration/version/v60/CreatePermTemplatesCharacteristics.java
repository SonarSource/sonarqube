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
package org.sonar.server.platform.db.migration.version.v60;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.def.IntegerColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.server.platform.db.migration.sql.CreateTableBuilder.ColumnFlag.AUTO_INCREMENT;
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreatePermTemplatesCharacteristics extends DdlChange {

  private static final String TABLE_NAME = "perm_tpl_characteristics";

  public CreatePermTemplatesCharacteristics(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    IntegerColumnDef templateIdColumn = newIntegerColumnDefBuilder().setColumnName("template_id").setIsNullable(false).build();
    VarcharColumnDef permissionKeyColumn = newVarcharColumnDefBuilder().setColumnName("permission_key").setLimit(64).setIsNullable(false).setIgnoreOracleUnit(true).build();
    context.execute(
      new CreateTableBuilder(getDialect(), TABLE_NAME)
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(templateIdColumn)
        .addColumn(permissionKeyColumn)
        .addColumn(newBooleanColumnDefBuilder().setColumnName("with_project_creator").setIsNullable(false).setDefaultValue(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
        .build());

    context.execute(new CreateIndexBuilder(getDialect())
      .setTable(TABLE_NAME)
      .setName("uniq_perm_tpl_charac")
      .setUnique(true)
      .addColumn(templateIdColumn)
      .addColumn(permissionKeyColumn)
      .build());
  }
}
