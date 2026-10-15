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
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.VULNERABILITY_ID_MAX_LENGTH;

public class CreateScaRefdbKevVulnerabilitiesTable extends CreateTableChange {

  public static final String TABLE_NAME = "sca_ref_kev_vulns";

  public static final String COLUMN_UUID = "uuid";
  public static final String COLUMN_VULNERABILITY_ID = "vulnerability_id";
  public static final String COLUMN_CWES = "cwes";
  public static final String COLUMN_DATE_ADDED = "date_added";
  public static final String COLUMN_DUE_DATE = "due_date";
  public static final String COLUMN_KNOWN_RANSOMWARE_CAMPAIGN_USE = "known_ransomware_campaign_use";
  public static final String COLUMN_NOTES = "notes";
  public static final String COLUMN_PRODUCT = "product";
  public static final String COLUMN_REQUIRED_ACTION = "required_action";
  public static final String COLUMN_SHORT_DESCRIPTION = "short_description";
  public static final String COLUMN_VENDOR_PROJECT = "vendor_project";
  public static final String COLUMN_VULNERABILITY_NAME = "vulnerability_name";
  public static final String COLUMN_CREATED_AT = "created_at";
  public static final String COLUMN_UPDATED_AT = "updated_at";

  public static final String INDEX_VULN_ID_UNIQ = "kev_vulns_vuln_id_uniq";

  // Stores an enum currently of max length 7, see: https://github.com/SonarSource/sonar-sca/blob/2001a7e910e8c37bc90749d41f2c8eb0888683dd/sca-service/sca-cloud-app/data-service/src/main/java/com/sonar/sca/reference/db/dtos/KnownRansomwareCampaignUse.java.
  // Migrate to a larger varchar size if enum length increases beyond 10 chars.
  public static final int KNOWN_RANSOMWARE_CAMPAIGN_USE_MAX_LENGTH = 10;
  // These are set to give fairly generous headroom based on expected sizes for their respective fields. Migrate these to larger max lengths if needed.
  public static final int PRODUCT_MAX_LENGTH = 500;
  public static final int VULNERABILITY_NAME_MAX_LENGTH = 500;
  public static final int VENDOR_PROJECT_MAX_LENGTH = 500;

  public CreateScaRefdbKevVulnerabilitiesTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_VULNERABILITY_ID).setIsNullable(false).setLimit(VULNERABILITY_ID_MAX_LENGTH).build())
      // Derived from a Postgres VARCHAR[] type column in Cloud RefDB (see https://github.com/SonarSource/sonar-sca/blob/2001a7e910e8c37bc90749d41f2c8eb0888683dd/sca-service/sca-cloud-app/data-service/src/main/resources/referencedb/migration/V20251030_2221__Add_kev_vulnerabilities.sql#L4).
      // Oracle and MSSQL don't have VARCHAR[], but the CLOB builder abstracts over equivalent types across the supported dialects.
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_CWES).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_DATE_ADDED).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_DUE_DATE).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_KNOWN_RANSOMWARE_CAMPAIGN_USE).setIsNullable(false).setLimit(KNOWN_RANSOMWARE_CAMPAIGN_USE_MAX_LENGTH).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_NOTES).setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PRODUCT).setIsNullable(false).setLimit(PRODUCT_MAX_LENGTH).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_REQUIRED_ACTION).setIsNullable(false).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_SHORT_DESCRIPTION).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_VENDOR_PROJECT).setIsNullable(false).setLimit(VENDOR_PROJECT_MAX_LENGTH).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_VULNERABILITY_NAME).setIsNullable(false).setLimit(VULNERABILITY_NAME_MAX_LENGTH).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(TABLE_NAME)
      .setName(INDEX_VULN_ID_UNIQ)
      .setUnique(true)
      .addColumn(COLUMN_VULNERABILITY_ID, false)
      .build());
  }
}
