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
package org.sonar.server.platform.db.migration.version.v75;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

@SupportsBlueGreen
public class AddUserExternalIdColumnInAlmAppInstall extends DdlChange {

  private static final String ALM_APP_INSTALLS_TABLE = "alm_app_installs";
  private static final String USER_EXTERNAL_ID_COLUMN = "user_external_id";

  public AddUserExternalIdColumnInAlmAppInstall(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    VarcharColumnDef userExternalIdDef = newVarcharColumnDefBuilder()
      .setColumnName(USER_EXTERNAL_ID_COLUMN)
      .setLimit(255)
      .setIsNullable(true)
      .build();
    context.execute(new AddColumnsBuilder(getDialect(), ALM_APP_INSTALLS_TABLE)
      .addColumn(userExternalIdDef).build());
    context.execute(new CreateIndexBuilder(getDialect())
      .setTable(ALM_APP_INSTALLS_TABLE)
      .addColumn(userExternalIdDef)
      .setUnique(false)
      .setName("alm_app_installs_external_id")
      .build());
  }
}
