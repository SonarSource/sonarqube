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
package org.sonar.server.platform.db.migration.version.v61;

import java.sql.SQLException;
import java.util.List;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.sql.CreateTableBuilder.ColumnFlag.AUTO_INCREMENT;
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.MAX_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateTableProperties2 extends DdlChange {

  private static final String TABLE_NAME = "properties2";

  public CreateTableProperties2(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    VarcharColumnDef propKey = newVarcharColumnDefBuilder().setColumnName("prop_key").setLimit(512).setIsNullable(false).setIgnoreOracleUnit(true).build();
    List<String> stmts = new CreateTableBuilder(getDialect(), TABLE_NAME)
      .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
      .addColumn(propKey)
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("resource_id").setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("user_id").setIsNullable(true).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName("is_empty").setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("text_value").setLimit(MAX_SIZE).setIgnoreOracleUnit(true).build())
      .addColumn(newClobColumnDefBuilder().setColumnName("clob_value").setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      // table with be renamed to properties in following migration, use final constraint name right away
      .withPkConstraintName("pk_properties")
      .build();
    context.execute(stmts);

    context.execute(new CreateIndexBuilder(getDialect())
      .setTable(TABLE_NAME)
      .setName("properties2_key")
      .addColumn(propKey)
      .build());
  }
}
