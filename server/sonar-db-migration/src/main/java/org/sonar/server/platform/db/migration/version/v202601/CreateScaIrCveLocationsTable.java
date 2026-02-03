/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202601;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.IntegerColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateScaIrCveLocationsTable extends CreateTableChange {

  static final String TABLE_NAME = "sca_ir_cve_locations";
  static final String COLUMN_UUID = "uuid";
  static final String COLUMN_SCA_ISSUES_RELEASES_UUID = "sca_issues_releases_uuid";
  static final String COLUMN_CVE_ID = "cve_id";
  static final String COLUMN_SIGNATURE = "signature";
  static final String COLUMN_FILE_PATH = "file_path";
  static final String COLUMN_START_LINE = "start_line";
  static final String COLUMN_START_LINE_OFFSET = "start_line_offset";
  static final String COLUMN_END_LINE = "end_line";
  static final String COLUMN_END_LINE_OFFSET = "end_line_offset";
  static final String COLUMN_CREATED_AT = "created_at";
  static final String COLUMN_UPDATED_AT = "updated_at";

  static final int CVE_ID_SIZE = 63;
  static final int SIGNATURE_SIZE = 1000;
  static final int FILE_PATH_SIZE = 1000;

  protected CreateScaIrCveLocationsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SCA_ISSUES_RELEASES_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_CVE_ID).setIsNullable(false).setLimit(CVE_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SIGNATURE).setIsNullable(false).setLimit(SIGNATURE_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_FILE_PATH).setIsNullable(false).setLimit(FILE_PATH_SIZE).build())
      .addColumn(IntegerColumnDef.newIntegerColumnDefBuilder().setColumnName(COLUMN_START_LINE).setIsNullable(false).build())
      .addColumn(IntegerColumnDef.newIntegerColumnDefBuilder().setColumnName(COLUMN_START_LINE_OFFSET).setIsNullable(false).build())
      .addColumn(IntegerColumnDef.newIntegerColumnDefBuilder().setColumnName(COLUMN_END_LINE).setIsNullable(false).build())
      .addColumn(IntegerColumnDef.newIntegerColumnDefBuilder().setColumnName(COLUMN_END_LINE_OFFSET).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName("sca_ir_cve_loc_ir_uuid")
      .addColumn(COLUMN_SCA_ISSUES_RELEASES_UUID)
      .build());
  }

}
