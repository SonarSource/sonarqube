/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v82;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.def.BigIntegerColumnDef;
import org.sonar.server.platform.db.migration.def.BooleanColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateProjectsTable extends DdlChange {

  private static final String TABLE_NAME = "projects";

  private static final VarcharColumnDef UUID_COLUMN = newVarcharColumnDefBuilder()
    .setColumnName("uuid")
    .setIsNullable(false)
    .setLimit(UUID_SIZE)
    .build();
  private static final VarcharColumnDef KEE_COLUMN = newVarcharColumnDefBuilder()
    .setColumnName("kee")
    .setIsNullable(false)
    .setLimit(400)
    .build();
  private static final VarcharColumnDef QUALIFIER_COLUMN = newVarcharColumnDefBuilder()
    .setColumnName("qualifier")
    .setIsNullable(false)
    .setLimit(10)
    .build();
  private static final VarcharColumnDef ORGANIZATION_UUID_COLUMN = newVarcharColumnDefBuilder()
    .setColumnName("organization_uuid")
    .setIsNullable(false)
    .setLimit(UUID_SIZE)
    .build();
  private static final VarcharColumnDef NAME_COLUMN = newVarcharColumnDefBuilder()
    .setColumnName("name")
    .setLimit(2000)
    .build();
  private static final VarcharColumnDef DESCRIPTION_COLUMN = newVarcharColumnDefBuilder()
    .setColumnName("description")
    .setLimit(2000)
    .build();
  private static final BooleanColumnDef PRIVATE_COLUMN = newBooleanColumnDefBuilder()
    .setColumnName("private")
    .setIsNullable(false)
    .build();
  private static final VarcharColumnDef TAGS_COLUMN = newVarcharColumnDefBuilder()
    .setColumnName("tags")
    .setLimit(500)
    .build();
  private static final BigIntegerColumnDef CREATED_AT = newBigIntegerColumnDefBuilder()
    .setColumnName("created_at")
    .setIsNullable(true)
    .build();
  private static final BigIntegerColumnDef UPDATED_AT = newBigIntegerColumnDefBuilder()
    .setColumnName("updated_at")
    .setIsNullable(false)
    .build();

  public CreateProjectsTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (tableExists()) {
      return;
    }
    context.execute(new CreateTableBuilder(getDialect(), TABLE_NAME)
      .withPkConstraintName("pk_new_projects")
      .addPkColumn(UUID_COLUMN)
      .addColumn(KEE_COLUMN)
      .addColumn(QUALIFIER_COLUMN)
      .addColumn(ORGANIZATION_UUID_COLUMN)
      .addColumn(NAME_COLUMN)
      .addColumn(DESCRIPTION_COLUMN)
      .addColumn(PRIVATE_COLUMN)
      .addColumn(TAGS_COLUMN)
      .addColumn(CREATED_AT)
      .addColumn(UPDATED_AT)
      .build());

    context.execute(new CreateIndexBuilder()
      .setTable(TABLE_NAME)
      .setName("uniq_projects_kee")
      .setUnique(true)
      .addColumn(KEE_COLUMN)
      .build());

    context.execute(new CreateIndexBuilder()
      .setTable(TABLE_NAME)
      .setName("idx_qualifier")
      .addColumn(QUALIFIER_COLUMN)
      .build());
  }

  private boolean tableExists() throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      return DatabaseUtils.tableExists(TABLE_NAME, connection);
    }
  }
}
