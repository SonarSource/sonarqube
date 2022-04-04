/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v83.grouproles;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class AddComponentUuidColumnToGroupRoles extends DdlChange {
  private static final String TABLE = "group_roles";
  private static final String NEW_COLUMN = "component_uuid";
  private static final String INDEX1 = "group_roles_component_uuid";

  public AddComponentUuidColumnToGroupRoles(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    VarcharColumnDef column = newVarcharColumnDefBuilder()
      .setColumnName(NEW_COLUMN)
      .setLimit(VarcharColumnDef.UUID_SIZE)
      .setIsNullable(true)
      .build();
    context.execute(new AddColumnsBuilder(getDialect(), TABLE)
      .addColumn(column)
      .build());

    CreateIndexBuilder index1 = new CreateIndexBuilder()
      .setTable(TABLE)
      .addColumn(column)
      .setName(INDEX1)
      .setUnique(false);
    context.execute(index1.build());
  }
}
