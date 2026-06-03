/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202604;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;
import org.sonar.server.platform.db.migration.step.DropIndexChange;

import static org.sonar.db.DatabaseUtils.tableColumnExists;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class AddOrganizationUuidToScaEncounteredLicenses extends DdlChange {

  static final String TABLE_NAME = "sca_encountered_licenses";
  static final String COLUMN_NAME = "organization_uuid";
  static final String OLD_INDEX_NAME = "sca_encountered_lic_uniq";
  static final String NEW_INDEX_NAME = "sca_encountered_lic_org_uniq";

  public AddOrganizationUuidToScaEncounteredLicenses(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (!tableColumnExists(connection, TABLE_NAME, COLUMN_NAME)) {
        context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
          .addColumn(newVarcharColumnDefBuilder()
            .setColumnName(COLUMN_NAME)
            .setIsNullable(true)
            .setLimit(40)
            .build())
          .build());
      }

      new DropIndexChange(getDatabase(), OLD_INDEX_NAME, TABLE_NAME) {}.execute(context);

      if (!DatabaseUtils.indexExistsIgnoreCase(TABLE_NAME, NEW_INDEX_NAME, connection)) {
        context.execute(new CreateIndexBuilder(getDialect())
          .setTable(TABLE_NAME)
          .setName(NEW_INDEX_NAME)
          .setUnique(true)
          .addColumn(COLUMN_NAME, true)
          .addColumn("license_policy_id", false)
          .build());
      }
    }
  }

}
