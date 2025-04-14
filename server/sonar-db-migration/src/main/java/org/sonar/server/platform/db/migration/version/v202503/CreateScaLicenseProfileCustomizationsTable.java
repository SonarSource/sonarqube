/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v202503;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateScaLicenseProfileCustomizationsTable extends CreateTableChange {

  // abbreviated due to limits on old oracle
  private static final String TABLE_NAME = "sca_lic_prof_customs";
  private static final String COLUMN_UUID_NAME = "uuid";
  private static final String COLUMN_LICENSE_PROFILE_UUID_NAME = "sca_license_profile_uuid";
  private static final String COLUMN_LICENSE_POLICY_ID_NAME = "license_policy_id";
  private static final int COLUMN_LICENSE_POLICY_ID_SIZE = 127;
  private static final String COLUMN_STATUS_NAME = "status";
  private static final int COLUMN_STATUS_SIZE = 40;
  private static final String COLUMN_CREATED_AT_NAME = "created_at";
  private static final String COLUMN_UPDATED_AT_NAME = "updated_at";

  protected CreateScaLicenseProfileCustomizationsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID_NAME)
        .setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_LICENSE_PROFILE_UUID_NAME)
        .setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_LICENSE_POLICY_ID_NAME)
        .setIsNullable(false).setLimit(COLUMN_LICENSE_POLICY_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_STATUS_NAME)
        .setIsNullable(false).setLimit(COLUMN_STATUS_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT_NAME).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT_NAME).setIsNullable(false).build())
      .build());
  }
}
