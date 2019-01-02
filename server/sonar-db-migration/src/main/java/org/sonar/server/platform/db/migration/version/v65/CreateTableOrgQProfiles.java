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
package org.sonar.server.platform.db.migration.version.v65;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateTableOrgQProfiles extends DdlChange {

  private static final String TABLE_NAME = "org_qprofiles";
  private static final int QUALITY_PROFILE_UUID_SIZE = 255;

  public CreateTableOrgQProfiles(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    VarcharColumnDef organizationColumn = newVarcharColumnDefBuilder()
      .setColumnName("organization_uuid")
      .setLimit(UUID_SIZE)
      .setIsNullable(false)
      .setIgnoreOracleUnit(true)
      .build();
    VarcharColumnDef rulesProfileUuid = newVarcharColumnDefBuilder()
      .setColumnName("rules_profile_uuid")
      .setLimit(QUALITY_PROFILE_UUID_SIZE)
      .setIsNullable(false)
      .setIgnoreOracleUnit(true)
      .build();
    context.execute(
      new CreateTableBuilder(getDialect(), TABLE_NAME)
        .addPkColumn(newVarcharColumnDefBuilder()
          .setColumnName("uuid")
          .setLimit(QUALITY_PROFILE_UUID_SIZE)
          .setIsNullable(false)
          .build())
        .addColumn(organizationColumn)
        .addColumn(rulesProfileUuid)
        .addColumn(newVarcharColumnDefBuilder()
          .setColumnName("parent_uuid")
          .setLimit(QUALITY_PROFILE_UUID_SIZE)
          .setIsNullable(true)
          .setIgnoreOracleUnit(true)
          .build())
        .addColumn(newBigIntegerColumnDefBuilder()
          .setColumnName("last_used")
          .setIsNullable(true)
          .build())
        .addColumn(newBigIntegerColumnDefBuilder()
          .setColumnName("user_updated_at")
          .setIsNullable(true)
          .build())
        .addColumn(newBigIntegerColumnDefBuilder()
          .setColumnName("created_at")
          .setIsNullable(false)
          .build())
        .addColumn(newBigIntegerColumnDefBuilder()
          .setColumnName("updated_at")
          .setIsNullable(false)
          .build())
        .build());

    context.execute(
      new CreateIndexBuilder(getDialect())
        .setTable(TABLE_NAME)
        .setName("qprofiles_org_uuid")
        .addColumn(organizationColumn)
        .build());

    context.execute(
      new CreateIndexBuilder(getDialect())
        .setTable(TABLE_NAME)
        .setName("qprofiles_rp_uuid")
        .addColumn(rulesProfileUuid)
        .build());
  }
}
