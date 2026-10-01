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
package org.sonar.server.platform.db.migration.version.v202606;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Mirrors sonar-sca's {@code sca_available_fixes} table (cloud migration
 * {@code V20260921_1500__Create_sca_available_fixes.sql}) column-for-column, so shared write-path
 * code in {@code sonarqube-unified-app} can run unmodified on-prem. No FK on {@code sca_issue_uuid}:
 * no table in this schema uses real FK constraints.
 */
public class CreateScaAvailableFixesTable extends CreateTableChange {

  static final String TABLE_NAME = "sca_available_fixes";
  static final String COLUMN_UUID = "uuid";
  static final String COLUMN_SCA_ISSUE_UUID = "sca_issue_uuid";
  static final String COLUMN_VERSION_IN_USE = "version_in_use";
  static final String COLUMN_BASELINE_CLASSIFICATION = "baseline_classification";
  static final String COLUMN_BASELINE_NEAREST_FIX_VERSION = "baseline_nearest_fix_version";
  static final String COLUMN_BASELINE_NEAREST_IS_COMPLETE_FIX = "baseline_nearest_is_complete_fix";
  static final String COLUMN_BASELINE_LATEST_FIX_VERSION = "baseline_latest_fix_version";
  static final String COLUMN_LIGHTWELL_CLASSIFICATION = "lightwell_classification";
  static final String COLUMN_LIGHTWELL_NEAREST_FIX_VERSION = "lightwell_nearest_fix_version";
  static final String COLUMN_LIGHTWELL_NEAREST_IS_COMPLETE_FIX = "lightwell_nearest_is_complete_fix";
  static final String COLUMN_LIGHTWELL_LATEST_FIX_VERSION = "lightwell_latest_fix_version";
  static final String COLUMN_CREATED_AT = "created_at";
  static final String COLUMN_UPDATED_AT = "updated_at";
  static final String INDEX_UNIQUE = "sca_available_fixes_unique";

  static final int VERSION_SIZE = 400;
  static final int CLASSIFICATION_SIZE = 40;
  static final String DEFAULT_CLASSIFICATION_UNKNOWN = "UNKNOWN";

  protected CreateScaAvailableFixesTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SCA_ISSUE_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_VERSION_IN_USE).setIsNullable(false).setLimit(VERSION_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_BASELINE_CLASSIFICATION).setIsNullable(false).setLimit(CLASSIFICATION_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_BASELINE_NEAREST_FIX_VERSION).setIsNullable(true).setLimit(VERSION_SIZE).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName(COLUMN_BASELINE_NEAREST_IS_COMPLETE_FIX).setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_BASELINE_LATEST_FIX_VERSION).setIsNullable(true).setLimit(VERSION_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_LIGHTWELL_CLASSIFICATION).setIsNullable(false).setLimit(CLASSIFICATION_SIZE)
        .setDefaultValue(DEFAULT_CLASSIFICATION_UNKNOWN).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_LIGHTWELL_NEAREST_FIX_VERSION).setIsNullable(true).setLimit(VERSION_SIZE).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName(COLUMN_LIGHTWELL_NEAREST_IS_COMPLETE_FIX).setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_LIGHTWELL_LATEST_FIX_VERSION).setIsNullable(true).setLimit(VERSION_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_UNIQUE)
      .setUnique(true)
      .addColumn(COLUMN_SCA_ISSUE_UUID, false)
      .addColumn(COLUMN_VERSION_IN_USE, false)
      .build());
  }
}
