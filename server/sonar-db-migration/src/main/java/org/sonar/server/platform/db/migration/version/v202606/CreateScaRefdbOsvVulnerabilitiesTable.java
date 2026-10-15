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
import static org.sonar.server.platform.db.migration.def.DecimalColumnDef.newDecimalColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.TimestampColumnDef.newTimestampColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.VULNERABILITY_ID_MAX_LENGTH;

public class CreateScaRefdbOsvVulnerabilitiesTable extends CreateTableChange {

  public static final String TABLE_NAME = "sca_ref_osv_vulns";

  public static final String COLUMN_UUID = "uuid";
  public static final String COLUMN_VULNERABILITY_ID = "vulnerability_id";
  public static final String COLUMN_MODIFIED = "modified";
  public static final String COLUMN_CREATED_AT = "created_at";
  public static final String COLUMN_UPDATED_AT = "updated_at";
  public static final String COLUMN_SYNC_GENERATION = "sync_generation";
  public static final String COLUMN_WITHDRAWN = "withdrawn";
  public static final String COLUMN_CVSS_SCORE = "cvss_score";
  public static final String COLUMN_CWES = "cwes";
  public static final String COLUMN_ALIASES = "aliases";
  public static final String COLUMN_UPSTREAMS = "upstreams";
  public static final String COLUMN_PUBLISHED = "published";
  public static final String COLUMN_SUMMARY = "summary";
  public static final String COLUMN_DETAILS = "details";

  public static final String INDEX_ID_UNIQ = "osv_vulns_id_uniq";
  public static final String INDEX_MODIFIED_ID = "osv_vulns_modified_id_idx";
  public static final String INDEX_SYNC_GENERATION_MODIFIED_ID = "osv_vulns_sync_gen_idx";

  public CreateScaRefdbOsvVulnerabilitiesTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_VULNERABILITY_ID).setIsNullable(false).setLimit(VULNERABILITY_ID_MAX_LENGTH).build())
      .addColumn(newTimestampColumnDefBuilder().setColumnName(COLUMN_MODIFIED).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_SYNC_GENERATION).setIsNullable(false).build())
      .addColumn(newTimestampColumnDefBuilder().setColumnName(COLUMN_WITHDRAWN).setIsNullable(true).build())
      .addColumn(newDecimalColumnDefBuilder().setColumnName(COLUMN_CVSS_SCORE).setIsNullable(true).build())
      // In sonar-sca, `cwes`, `aliases`, and `upstreams` are all of type `VARCHAR[]`. The convention for cross-dialect schemas in this repo is to use the `CLOB` type and
      // handle array serialization in application code.
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_CWES).setIsNullable(false).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_ALIASES).setIsNullable(false).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_UPSTREAMS).setIsNullable(false).build())
      .addColumn(newTimestampColumnDefBuilder().setColumnName(COLUMN_PUBLISHED).setIsNullable(true).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_SUMMARY).setIsNullable(true).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_DETAILS).setIsNullable(true).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(TABLE_NAME)
      .setName(INDEX_ID_UNIQ)
      .setUnique(true)
      .addColumn(COLUMN_VULNERABILITY_ID, false)
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(TABLE_NAME)
      .setName(INDEX_MODIFIED_ID)
      .addColumn(COLUMN_MODIFIED)
      .addColumn(COLUMN_VULNERABILITY_ID)
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(TABLE_NAME)
      .setName(INDEX_SYNC_GENERATION_MODIFIED_ID)
      .addColumn(COLUMN_SYNC_GENERATION)
      .addColumn(COLUMN_MODIFIED)
      .addColumn(COLUMN_VULNERABILITY_ID)
      .build());
  }
}
