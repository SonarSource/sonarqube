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

package org.sonar.server.db.migrations.v51;

import java.sql.SQLException;

import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.AddColumnsBuilder;
import org.sonar.server.db.migrations.DdlChange;

/**
 * Add the following columns to the issues table :
 * - issue_creation_date_ms
 * - issue_update_date_ms
 * - issue_close_date_ms
 * - tags
 * - component_uuid
 * - project_uuid
 */
public class AddIssuesColumns extends DdlChange {

  private final Database db;

  public AddIssuesColumns(Database db) {
    super(db);
    this.db = db;
  }

  @Override
  public void execute(DdlChange.Context context) throws SQLException {
    context.execute(generateSql());
  }

  private String generateSql() {
    return new AddColumnsBuilder(db.getDialect(), "issues")
      .addColumn(
        new AddColumnsBuilder.ColumnDef()
          .setName("issue_creation_date_ms")
          .setType(AddColumnsBuilder.ColumnDef.Type.BIG_INTEGER)
          .setNullable(true)
      )
      .addColumn(
        new AddColumnsBuilder.ColumnDef()
          .setName("issue_update_date_ms")
          .setType(AddColumnsBuilder.ColumnDef.Type.BIG_INTEGER)
          .setNullable(true)
      )
      .addColumn(
        new AddColumnsBuilder.ColumnDef()
          .setName("issue_close_date_ms")
          .setType(AddColumnsBuilder.ColumnDef.Type.BIG_INTEGER)
          .setNullable(true)
      )
      .addColumn(
        new AddColumnsBuilder.ColumnDef()
          .setName("tags")
          .setType(AddColumnsBuilder.ColumnDef.Type.STRING)
          .setLimit(4000)
          .setNullable(true))
      .addColumn(
        new AddColumnsBuilder.ColumnDef()
          .setName("component_uuid")
          .setType(AddColumnsBuilder.ColumnDef.Type.STRING)
          .setLimit(50)
          .setNullable(true))
      .addColumn(
        new AddColumnsBuilder.ColumnDef()
          .setName("project_uuid")
          .setType(AddColumnsBuilder.ColumnDef.Type.STRING)
          .setLimit(50)
          .setNullable(true))
      .build();
  }

}
