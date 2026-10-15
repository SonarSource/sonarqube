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
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.PACKAGE_LOOKUP_KEY_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.VERSION_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.VERSION_TYPE_MAX_SIZE;

public class CreateScaRefdbLicensingInfosTable extends CreateTableChange {

  public static final String TABLE_NAME = "sca_ref_licensing_infos";

  public static final String COLUMN_UUID = "uuid";
  public static final String COLUMN_PACKAGE_LOOKUP_KEY = "package_lookup_key";
  public static final String COLUMN_VERSION_TYPE = "version_type";
  public static final String COLUMN_VERSION_RANGE_START = "version_range_start";
  public static final String COLUMN_VERSION_RANGE_END = "version_range_end";
  public static final String COLUMN_CATEGORIZED_SOURCE = "categorized_source";
  public static final String COLUMN_LICENSE_EXPRESSION = "license_expression";
  public static final String COLUMN_ORIGINAL_LICENSE = "original_license";
  public static final String COLUMN_ORIGINAL_LICENSE_SOURCE = "original_license_source";
  public static final String COLUMN_REPOSITORY_LICENSE = "repository_license";
  public static final String COLUMN_CREATED_AT = "created_at";
  public static final String COLUMN_UPDATED_AT = "updated_at";

  public static final String INDEX_PKG_LOOKUP_KEY = "lic_infos_pkg_lookup_key_idx";

  // Values for categorized_source come from an enum currently under 30 characters in length.
  // See: https://github.com/SonarSource/sonar-sca/blob/2001a7e910e8c37bc90749d41f2c8eb0888683dd/sca-service/sca-cloud-app/data-service/src/main/java/com/sonar/sca/reference/db/dtos/LicensingCategorizedSource.java
  public static final int CATEGORIZED_SOURCE_MAX_SIZE = 30;
  // Values for original_license_source come from an enum currently under 20 characters in length.
  // See: https://github.com/SonarSource/sonar-sca/blob/2001a7e910e8c37bc90749d41f2c8eb0888683dd/sca-service/sca-cloud-app/data-service/src/main/java/com/sonar/sca/reference/db/dtos/LicensingOriginalLicenseSource.java
  public static final int ORIGINAL_LICENSE_SOURCE_MAX_SIZE = 20;
  public CreateScaRefdbLicensingInfosTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PACKAGE_LOOKUP_KEY).setIsNullable(false).setLimit(PACKAGE_LOOKUP_KEY_MAX_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_VERSION_TYPE).setIsNullable(false).setLimit(VERSION_TYPE_MAX_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_VERSION_RANGE_START).setIsNullable(false).setLimit(VERSION_MAX_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_VERSION_RANGE_END).setIsNullable(true).setLimit(VERSION_MAX_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_CATEGORIZED_SOURCE).setIsNullable(true).setLimit(CATEGORIZED_SOURCE_MAX_SIZE).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_LICENSE_EXPRESSION).setIsNullable(false).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_ORIGINAL_LICENSE).setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ORIGINAL_LICENSE_SOURCE).setIsNullable(true).setLimit(ORIGINAL_LICENSE_SOURCE_MAX_SIZE).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_REPOSITORY_LICENSE).setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .build()
    );

    // NB: The original table definition in sonar-sca, from which this table definition is derived, uses `autovacuum_enabled = false`. This is a Postgres-specific table setting,
    // and is omitted from the multi-dialect table definition here. Add a Postgres-specific `ALTER TABLE` command to this definition if needed for this table on Postgres instances.

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(TABLE_NAME)
      .setName(INDEX_PKG_LOOKUP_KEY)
      .addColumn(COLUMN_PACKAGE_LOOKUP_KEY)
      .build());
  }
}
