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
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.DATABASE_PREFIX_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.ECOSYSTEM_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.PACKAGE_NAME_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.PACKAGE_PURL_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.PACKAGE_LOOKUP_KEY_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.VERSION_MAX_SIZE;

public class CreateScaRefdbOsvAffectedRangesTable extends CreateTableChange {

  public static final String TABLE_NAME = "sca_ref_osv_aff_ranges";

  public static final String COLUMN_UUID = "uuid";
  public static final String COLUMN_OSV_VULNERABILITY_UUID = "osv_vulnerability_uuid";
  public static final String COLUMN_ECOSYSTEM = "ecosystem";
  public static final String COLUMN_PACKAGE_NAME = "package_name";
  public static final String COLUMN_PACKAGE_PURL = "package_purl";
  public static final String COLUMN_INTRODUCED_VERSION = "introduced_version";
  public static final String COLUMN_LIMIT_VERSION = "limit_version";
  public static final String COLUMN_FIXED_VERSION = "fixed_version";
  public static final String COLUMN_LAST_AFFECTED_VERSION = "last_affected_version";
  public static final String COLUMN_CREATED_AT = "created_at";
  public static final String COLUMN_UPDATED_AT = "updated_at";
  public static final String COLUMN_RANGE_TYPE = "range_type";
  public static final String COLUMN_PACKAGE_LOOKUP_KEY = "package_lookup_key";
  public static final String COLUMN_DATABASE_PREFIX = "database_prefix";

  public static final String INDEX_OSV_AFF_RANGES_PKG_PREFIX = "osv_aff_ranges_pkg_prefix_idx";

  // The values in range_type are derived from an enum with a current max key size of <10 characters.
  // See: https://github.com/SonarSource/sonar-sca/blob/a15b95f1e421795e1a960016941a2d640cedc6ad/sca-service/sca-cloud-app/data-service/src/main/java/com/sonar/sca/reference/db/dtos/OsvAffectedRangeType.java#L4
  public static final int RANGE_TYPE_MAX_SIZE = 10;

  protected CreateScaRefdbOsvAffectedRangesTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      // The column `osv_vulnerability_uuid` references the primary key column on the `osv_vulnerabilities` table.
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_OSV_VULNERABILITY_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ECOSYSTEM).setIsNullable(false).setLimit(ECOSYSTEM_MAX_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PACKAGE_NAME).setIsNullable(false).setLimit(PACKAGE_NAME_MAX_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PACKAGE_PURL).setIsNullable(true).setLimit(PACKAGE_PURL_MAX_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_INTRODUCED_VERSION).setIsNullable(false).setLimit(VERSION_MAX_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_LIMIT_VERSION).setIsNullable(true).setLimit(VERSION_MAX_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_FIXED_VERSION).setIsNullable(true).setLimit(VERSION_MAX_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_LAST_AFFECTED_VERSION).setIsNullable(true).setLimit(VERSION_MAX_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_RANGE_TYPE).setIsNullable(false).setLimit(RANGE_TYPE_MAX_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PACKAGE_LOOKUP_KEY).setIsNullable(false).setLimit(PACKAGE_LOOKUP_KEY_MAX_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_DATABASE_PREFIX).setIsNullable(true).setLimit(DATABASE_PREFIX_MAX_SIZE).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(TABLE_NAME)
      .setName(INDEX_OSV_AFF_RANGES_PKG_PREFIX)
      .addColumn(COLUMN_PACKAGE_LOOKUP_KEY)
      .addColumn(COLUMN_DATABASE_PREFIX)
      .build());
  }
}
