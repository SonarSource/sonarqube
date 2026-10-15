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
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.PACKAGE_LOOKUP_KEY_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.VERSION_MAX_SIZE;

public class CreateScaRefdbUpstreamReleasesTable extends CreateTableChange {

  public static final String TABLE_NAME = "sca_ref_upstream_releases";

  public static final String COLUMN_UUID = "uuid";
  public static final String COLUMN_PACKAGE_LOOKUP_KEY = "package_lookup_key";
  public static final String COLUMN_VERSION = "version";
  public static final String COLUMN_PUBLISHED_AT = "published_at";
  public static final String COLUMN_RAW_LICENSE = "raw_license";
  public static final String COLUMN_YANKED = "yanked";
  public static final String COLUMN_CREATED_AT = "created_at";
  public static final String COLUMN_UPDATED_AT = "updated_at";
  public static final String COLUMN_SOURCE_UPDATED_AT = "source_updated_at";

  public static final String INDEX_UNIQ = "upstream_releases_uniq";

  public CreateScaRefdbUpstreamReleasesTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PACKAGE_LOOKUP_KEY).setIsNullable(false).setLimit(PACKAGE_LOOKUP_KEY_MAX_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_VERSION).setIsNullable(false).setLimit(VERSION_MAX_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_PUBLISHED_AT).setIsNullable(true).build())
      // NB: The original type on sonar-sca is `TEXT`. `CLOB` is used here for cross-dialect SQL.
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_RAW_LICENSE).setIsNullable(true).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName(COLUMN_YANKED).setIsNullable(false).setDefaultValue(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_SOURCE_UPDATED_AT).setIsNullable(false).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(TABLE_NAME)
      .setName(INDEX_UNIQ)
      .setUnique(true)
      .addColumn(COLUMN_PACKAGE_LOOKUP_KEY, false)
      .addColumn(COLUMN_VERSION, false)
      .build());
  }
}
