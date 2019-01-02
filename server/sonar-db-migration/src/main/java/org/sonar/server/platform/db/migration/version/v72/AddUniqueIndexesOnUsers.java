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
package org.sonar.server.platform.db.migration.version.v72;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class AddUniqueIndexesOnUsers extends DdlChange {

  public AddUniqueIndexesOnUsers(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new CreateIndexBuilder(getDialect())
      .setTable("users")
      .setName("users_uuid")
      .setUnique(true)
      .addColumn(notNullableColumn("uuid", 255))
      .build());

    context.execute(new CreateIndexBuilder(getDialect())
      .setTable("users")
      .setName("uniq_external_id")
      .setUnique(true)
      .addColumn(notNullableColumn("external_identity_provider", 100))
      .addColumn(notNullableColumn("external_id", 255))
      .build());
  }

  private static VarcharColumnDef notNullableColumn(String columnName, int limit) {
    return newVarcharColumnDefBuilder()
      .setColumnName(columnName)
      .setLimit(limit)
      .setIsNullable(false)
      .build();
  }

}
