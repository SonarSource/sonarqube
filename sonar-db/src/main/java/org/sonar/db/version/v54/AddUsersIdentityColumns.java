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
import org.sonar.db.Database;
import org.sonar.db.version.AddColumnsBuilder;
import org.sonar.db.version.DdlChange;

import static org.sonar.db.version.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Add the following columns to the USERS table :
 * - external_identity
 * - external_identity_provider
 */
public class AddUsersIdentityColumns extends DdlChange {

  public AddUsersIdentityColumns(Database db) {
    super(db);
  }

  @Override
  public void execute(DdlChange.Context context) throws SQLException {
    context.execute(generateSql());
  }

  private String generateSql() {
    return new AddColumnsBuilder(getDialect(), "users")
      .addColumn(newVarcharColumnDefBuilder().setColumnName("external_identity").setLimit(255).setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("external_identity_provider").setLimit(100).setIsNullable(true).build())
      .build();
  }

}
