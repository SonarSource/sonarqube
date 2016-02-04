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
package org.sonar.db.version.v54;

import java.sql.SQLException;
import java.util.List;
import org.sonar.db.Database;
import org.sonar.db.version.AlterColumnsTypeBuilder;
import org.sonar.db.version.DdlChange;

import static org.sonar.db.version.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Update the following columns to the PROJECTS table :
 * - name to 2000 characters
 * - long_name to 2000 characters
 */
public class IncreaseProjectsNameColumnsSize extends DdlChange {

  private final Database db;

  public IncreaseProjectsNameColumnsSize(Database db) {
    super(db);
    this.db = db;
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(generateSql());
  }

  private List<String> generateSql() {
    return new AlterColumnsTypeBuilder(db.getDialect(), "projects")
      .updateColumn(newVarcharColumnDefBuilder().setColumnName("name").setLimit(2000).setIsNullable(true).build())
      .updateColumn(newVarcharColumnDefBuilder().setColumnName("long_name").setLimit(2000).setIsNullable(true).build())
      .build();
  }

}
