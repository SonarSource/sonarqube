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
package org.sonar.server.platform.db.migration.version.v85;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.dialect.MsSql;
import org.sonar.server.platform.db.migration.sql.DropColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;
import org.sonar.server.platform.db.migration.version.v85.util.DropMsSQLDefaultConstraintsBuilder;

public class DropUnusedVariationsInProjectMeasures extends DdlChange {
  private static final String TABLE_NAME = "project_measures";
  private static final String[] COLUMNS = {"variation_value_2", "variation_value_3", "variation_value_4", "variation_value_5"};
  private final Database db;

  public DropUnusedVariationsInProjectMeasures(Database db) {
    super(db);
    this.db = db;
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (MsSql.ID.equals(db.getDialect().getId())) {
      context.execute(new DropMsSQLDefaultConstraintsBuilder(db).setTable(TABLE_NAME).setColumns(COLUMNS).build());
    }
    context.execute(new DropColumnsBuilder(getDialect(), TABLE_NAME, COLUMNS).build());
  }
}
