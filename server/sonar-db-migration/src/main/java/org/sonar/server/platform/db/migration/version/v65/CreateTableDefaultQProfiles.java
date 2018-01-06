/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

public class CreateTableDefaultQProfiles extends DdlChange {
  private static final int QUALITY_PROFILE_UUID_SIZE = 255;

  public CreateTableDefaultQProfiles(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    VarcharColumnDef profileUuidColumn = newVarcharColumnDefBuilder()
      .setColumnName("qprofile_uuid")
      .setLimit(QUALITY_PROFILE_UUID_SIZE)
      .setIsNullable(false)
      .setIgnoreOracleUnit(true)
      .build();
    context.execute(
      new CreateTableBuilder(getDialect(), "default_qprofiles")
        .addPkColumn(newVarcharColumnDefBuilder()
          .setColumnName("organization_uuid")
          .setLimit(UUID_SIZE)
          .setIsNullable(false)
          .setIgnoreOracleUnit(true)
          .build())
        .addPkColumn(newVarcharColumnDefBuilder()
          .setColumnName("language")
          .setLimit(20)
          .setIsNullable(false)
          .setIgnoreOracleUnit(true)
          .build())
        .addColumn(profileUuidColumn)
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
        .setTable("default_qprofiles")
        .setName("uniq_default_qprofiles_uuid")
        .addColumn(profileUuidColumn)
        .setUnique(true)
        .build());
  }
}
