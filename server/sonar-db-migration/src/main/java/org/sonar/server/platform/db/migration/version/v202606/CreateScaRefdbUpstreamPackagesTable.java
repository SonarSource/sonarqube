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
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.ECOSYSTEM_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.PACKAGE_NAME_MAX_SIZE;

public class CreateScaRefdbUpstreamPackagesTable extends CreateTableChange {

  public static final String TABLE_NAME = "sca_ref_upstream_pkgs";

  public static final String COLUMN_UUID = "uuid";
  public static final String COLUMN_PURL_TYPE = "purl_type";
  public static final String COLUMN_CANONICAL_NAME = "canonical_name";
  public static final String COLUMN_PACKAGE_LOOKUP_KEY = "package_lookup_key";
  public static final String COLUMN_DISPLAY_NAME = "display_name";
  public static final String COLUMN_REPOSITORY_URL = "repository_url";
  public static final String COLUMN_HOMEPAGE_URL = "homepage_url";
  public static final String COLUMN_DESCRIPTION = "description";
  public static final String COLUMN_RENAMED_TO = "renamed_to";
  public static final String COLUMN_CREATED_AT = "created_at";
  public static final String COLUMN_UPDATED_AT = "updated_at";
  public static final String COLUMN_SOURCE_UPDATED_AT = "source_updated_at";

  public static final String INDEX_PACKAGE_LOOKUP_KEY_UNIQ = "upstream_pkgs_lookup_uniq";

  // RFC 9110 recommends a minimum supported URI length of 8000 octets, but the SQL dialect limits in this project put a hard limit at 4000 bytes.
  // See: https://www.rfc-editor.org/info/rfc9110/#section-4.1-5
  public static final int URL_MAX_SIZE = 4_000;

  public CreateScaRefdbUpstreamPackagesTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PURL_TYPE).setIsNullable(false).setLimit(ECOSYSTEM_MAX_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_CANONICAL_NAME).setIsNullable(false).setLimit(PACKAGE_NAME_MAX_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PACKAGE_LOOKUP_KEY).setIsNullable(false).setLimit(PACKAGE_LOOKUP_KEY_MAX_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_DISPLAY_NAME).setIsNullable(false).setLimit(PACKAGE_NAME_MAX_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_REPOSITORY_URL).setIsNullable(true).setLimit(URL_MAX_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_HOMEPAGE_URL).setIsNullable(true).setLimit(URL_MAX_SIZE).build())
      // NB: The original type on sonar-sca is `TEXT`. `CLOB` is used here for cross-dialect SQL.
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_DESCRIPTION).setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_RENAMED_TO).setIsNullable(true).setLimit(PACKAGE_NAME_MAX_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_SOURCE_UPDATED_AT).setIsNullable(false).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(TABLE_NAME)
      .setName(INDEX_PACKAGE_LOOKUP_KEY_UNIQ)
      .setUnique(true)
      .addColumn(COLUMN_PACKAGE_LOOKUP_KEY, false)
      .build());
  }
}
