/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.db.migrations.v52;

import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.AddColumnsBuilder;
import org.sonar.server.db.migrations.DdlChange;

import java.sql.SQLException;

/**
 * Add the following columns to the dependencies table :
 * - from_component_uuid
 * - to_component_uuid
 */
public class AddDependenciesComponentUuidColumns extends DdlChange {

  private final Database db;

  public AddDependenciesComponentUuidColumns(Database db) {
    super(db);
    this.db = db;
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(generateSql());
  }

  private String generateSql() {
    return new AddColumnsBuilder(db.getDialect(), "dependencies")
      .addColumn(
        new AddColumnsBuilder.ColumnDef()
          .setName("from_component_uuid")
          .setType(AddColumnsBuilder.ColumnDef.Type.STRING)
          .setLimit(50)
          .setNullable(true)
      )
      .addColumn(
        new AddColumnsBuilder.ColumnDef()
          .setName("to_component_uuid")
          .setType(AddColumnsBuilder.ColumnDef.Type.STRING)
          .setLimit(50)
          .setNullable(true)
      )
      .build();
  }

}
