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
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.PACKAGE_LOOKUP_KEY_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.PACKAGE_NAME_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.PACKAGE_PURL_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.VERSION_MAX_SIZE;

public class CreateScaRefdbOsvAffectedVersionsTable extends CreateTableChange {

  public static final String TABLE_NAME = "sca_ref_osv_aff_versions";

  public static final String COLUMN_UUID = "uuid";
  public static final String COLUMN_OSV_VULNERABILITY_UUID = "osv_vulnerability_uuid";
  public static final String COLUMN_ECOSYSTEM = "ecosystem";
  public static final String COLUMN_PACKAGE_NAME = "package_name";
  public static final String COLUMN_PACKAGE_PURL = "package_purl";
  public static final String COLUMN_VERSION = "version";
  public static final String COLUMN_CREATED_AT = "created_at";
  public static final String COLUMN_UPDATED_AT = "updated_at";
  public static final String COLUMN_PACKAGE_LOOKUP_KEY = "package_lookup_key";
  public static final String COLUMN_DATABASE_PREFIX = "database_prefix";

  public static final String INDEX_UNIQ = "osv_affected_versions_uniq";
  public static final String INDEX_PACKAGE_VERSION_PREFIX = "osv_aff_versions_pkg_ver_idx";

  public CreateScaRefdbOsvAffectedVersionsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_OSV_VULNERABILITY_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ECOSYSTEM).setIsNullable(false).setLimit(ECOSYSTEM_MAX_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PACKAGE_NAME).setIsNullable(false).setLimit(PACKAGE_NAME_MAX_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PACKAGE_PURL).setIsNullable(true).setLimit(PACKAGE_PURL_MAX_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_VERSION).setIsNullable(false).setLimit(VERSION_MAX_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PACKAGE_LOOKUP_KEY).setIsNullable(false).setLimit(PACKAGE_LOOKUP_KEY_MAX_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_DATABASE_PREFIX).setIsNullable(true).setLimit(DATABASE_PREFIX_MAX_SIZE).build())
      .build());

    // NB: This unique index deviates from its reference on Cloud RefDB by excluding the `package_purl` column. The information in the PURL is redundant when we already index
    // `package_name`, `ecosystem`, and `version` as their own columns. Moreover, the size of our unique indexes is constrained by MSSQL's 1700-byte limit for unclustered indexes.
    context.execute(new CreateIndexBuilder(dialect)
      .setTable(TABLE_NAME)
      .setName(INDEX_UNIQ)
      .setUnique(true)
      .addColumn(COLUMN_OSV_VULNERABILITY_UUID, false)
      .addColumn(COLUMN_PACKAGE_NAME, false)
      .addColumn(COLUMN_ECOSYSTEM, false)
      .addColumn(COLUMN_VERSION, false)
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(TABLE_NAME)
      .setName(INDEX_PACKAGE_VERSION_PREFIX)
      .addColumn(COLUMN_PACKAGE_LOOKUP_KEY)
      .addColumn(COLUMN_VERSION)
      .addColumn(COLUMN_DATABASE_PREFIX)
      .build());
  }
}
