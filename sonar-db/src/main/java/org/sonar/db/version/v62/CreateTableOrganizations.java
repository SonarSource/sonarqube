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
import org.sonar.db.version.CreateTableBuilder;
import org.sonar.db.version.DdlChange;

import static org.sonar.db.version.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.db.version.VarcharColumnDef.UUID_SIZE;
import static org.sonar.db.version.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateTableOrganizations extends DdlChange {
  public CreateTableOrganizations(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(
      new CreateTableBuilder(getDialect(), "organizations")
        .addPkColumn(newVarcharColumnDefBuilder().setColumnName("uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
        .addColumn(newVarcharColumnDefBuilder().setColumnName("kee").setLimit(32).setIsNullable(false).build())
        .addColumn(newVarcharColumnDefBuilder().setColumnName("name").setLimit(64).setIsNullable(false).build())
        .addColumn(newVarcharColumnDefBuilder().setColumnName("description").setLimit(256).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder().setColumnName("url").setLimit(256).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder().setColumnName("avatar_url").setLimit(256).setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
        .build());
  }
}
