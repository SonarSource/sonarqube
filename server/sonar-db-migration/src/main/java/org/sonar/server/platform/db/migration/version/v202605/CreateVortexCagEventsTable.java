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
package org.sonar.server.platform.db.migration.version.v202605;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Stores versioned CAG impact events separately from legacy usage rows. Searchable request context remains in typed
 * columns, while event-specific JSON uses the repository's portable CLOB representation and is validated by the writer.
 */
public class CreateVortexCagEventsTable extends CreateTableChange {

  static final String TABLE_NAME = "vortex_cag_events";

  static final String COLUMN_UUID = "uuid";
  static final String COLUMN_EVENT_TYPE = "event_type";
  static final String COLUMN_EVENT_VERSION = "event_version";
  static final String COLUMN_SUCCESS = "success";
  static final String COLUMN_TRANSPORT = "transport";
  static final String COLUMN_PROJECT_UUID = "project_uuid";
  static final String COLUMN_BRANCH_NAME = "branch_name";
  static final String COLUMN_USER_UUID = "user_uuid";
  static final String COLUMN_INVOCATION_UUID = "invocation_uuid";
  // Identifies the CAG instance: an MCP server for MCP transport, or a daemon for CLI transport.
  static final String COLUMN_CAG_INSTANCE_ID = "cag_instance_id";
  static final String COLUMN_CREATED_AT = "created_at";
  static final String COLUMN_PAYLOAD = "payload";

  static final int UUID_SIZE = 40;
  static final int EVENT_TYPE_SIZE = 40;
  static final int TRANSPORT_SIZE = 20;
  static final int BRANCH_NAME_SIZE = 255;
  static final int CAG_INSTANCE_ID_SIZE = 255;

  static final String INDEX_PROJECT_CREATED_AT = "idx_vtx_cag_proj_created";
  static final String INDEX_CREATED_AT = "idx_vtx_cag_created";

  protected CreateVortexCagEventsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_EVENT_TYPE).setIsNullable(false).setLimit(EVENT_TYPE_SIZE).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_EVENT_VERSION).setIsNullable(false).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName(COLUMN_SUCCESS).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_TRANSPORT).setIsNullable(false).setLimit(TRANSPORT_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PROJECT_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_BRANCH_NAME).setIsNullable(true).setLimit(BRANCH_NAME_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_USER_UUID).setIsNullable(true).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_INVOCATION_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_CAG_INSTANCE_ID).setIsNullable(false).setLimit(CAG_INSTANCE_ID_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_PAYLOAD).setIsNullable(false).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_PROJECT_CREATED_AT)
      .setUnique(false)
      .addColumn(COLUMN_PROJECT_UUID, false)
      .addColumn(COLUMN_CREATED_AT, false)
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_CREATED_AT)
      .setUnique(false)
      .addColumn(COLUMN_CREATED_AT, false)
      .build());
  }
}
