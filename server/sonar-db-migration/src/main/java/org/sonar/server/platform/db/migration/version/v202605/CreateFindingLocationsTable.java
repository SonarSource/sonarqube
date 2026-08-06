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

import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Creates the {@code finding_locations} table holding the primary and secondary source locations of
 * a Hunter {@code findings} row. Each location references {@code findings.id} via {@code finding_id};
 * as elsewhere in SonarQube no physical foreign key is created, so the parent/child relationship
 * (including the cascade-on-delete behaviour described in the reference schema) is enforced in the
 * application layer.
 *
 * <p>The {@code type} column is an enum-like {@code VARCHAR} ({@code PRIMARY}/{@code SECONDARY})
 * validated in application code rather than via a native ENUM type or a {@code CHECK} constraint.
 */
public class CreateFindingLocationsTable extends CreateTableChange {

  static final String TABLE_NAME = "finding_locations";

  static final String COLUMN_ID = "id";
  static final String COLUMN_FINDING_ID = "finding_id";
  static final String COLUMN_TYPE = "type";
  static final String COLUMN_MESSAGE = "message";
  static final String COLUMN_FILE_PATH = "file_path";
  static final String COLUMN_START_LINE = "start_line";
  static final String COLUMN_START_COLUMN = "start_column";
  static final String COLUMN_END_LINE = "end_line";
  static final String COLUMN_END_COLUMN = "end_column";

  static final int ID_SIZE = 40;
  static final int FINDING_ID_SIZE = 40;
  static final int TYPE_SIZE = 20;
  static final int MESSAGE_SIZE = 4000;
  static final int FILE_PATH_SIZE = 1000;

  static final String INDEX_FINDING_ID = "idx_finding_loc_finding_id";

  protected CreateFindingLocationsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ID).setIsNullable(false).setLimit(ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_FINDING_ID).setIsNullable(false).setLimit(FINDING_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_TYPE).setIsNullable(false).setLimit(TYPE_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_MESSAGE).setIsNullable(true).setLimit(MESSAGE_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_FILE_PATH).setIsNullable(false).setLimit(FILE_PATH_SIZE).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_START_LINE).setIsNullable(false).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_START_COLUMN).setIsNullable(true).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_END_LINE).setIsNullable(false).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_END_COLUMN).setIsNullable(true).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_FINDING_ID)
      .setUnique(false)
      .addColumn(COLUMN_FINDING_ID, false)
      .build());
  }
}
