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
package org.sonar.server.platform.db.migration.version.v74;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.sql.AlterColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

@SupportsBlueGreen
public class IncreaseOrganizationsKeeAndNameLength extends DdlChange {

  public static final String ORGANIZATIONS_TABLE = "organizations";

  public IncreaseOrganizationsKeeAndNameLength(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new AlterColumnsBuilder(getDialect(), ORGANIZATIONS_TABLE)
      .updateColumn(newVarcharColumnDefBuilder()
        .setColumnName("kee")
        .setLimit(255)
        .setIsNullable(false)
        .build())
      .build());
    context.execute(new AlterColumnsBuilder(getDialect(), ORGANIZATIONS_TABLE)
      .updateColumn(newVarcharColumnDefBuilder()
        .setColumnName("name")
        .setLimit(255)
        .setIsNullable(false)
        .build())
      .build());
  }
}
