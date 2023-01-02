/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.step;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.dialect.MsSql;
import org.sonar.server.platform.db.migration.sql.DropColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.DropMsSQLDefaultConstraintsBuilder;

public class DropColumnWithConstraint extends DropColumnChange {

  private final String tableName;
  private final String column;

  public DropColumnWithConstraint(Database db, String tableName, String column) {
    super(db, tableName, column);
    this.tableName = tableName;
    this.column = column;
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (!checkIfUseManagedColumnExists()) {
      return;
    }

    if (MsSql.ID.equals(getDatabase().getDialect().getId())) {
      context.execute(new DropMsSQLDefaultConstraintsBuilder(getDatabase()).setTable(tableName).setColumns(column).build());
    }
    context.execute(new DropColumnsBuilder(getDatabase().getDialect(), tableName, column).build());
  }

}
