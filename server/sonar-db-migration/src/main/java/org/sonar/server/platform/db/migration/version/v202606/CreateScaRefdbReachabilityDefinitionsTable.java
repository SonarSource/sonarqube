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

public class CreateScaRefdbReachabilityDefinitionsTable extends CreateTableChange {

  public static final String TABLE_NAME = "sca_ref_reachability_defs";

  public static final String COLUMN_UUID = "uuid";
  public static final String COLUMN_LANGUAGE_KEY = "language_key";
  public static final String COLUMN_VULNERABILITY_ID = "vulnerability_id";
  public static final String COLUMN_VULNERABLE_FUNCTIONS = "vulnerable_functions";
  public static final String COLUMN_CREATED_AT = "created_at";
  public static final String COLUMN_UPDATED_AT = "updated_at";
  public static final String COLUMN_PUBLIC_CALLERS = "public_callers";
  public static final String COLUMN_ANCESTOR_CALLERS = "ancestor_callers";

  public static final String INDEX_LANGUAGE_VULNERABILITY_UNIQ = "reach_defs_lang_vuln_uniq";

  // Current supported language keys are "java", "cs", and "py".
  // See: https://github.com/SonarSource/sonar-sca/blob/5501a89091062acb2cb47d336b142d963df48492/sca-service/sca-cloud-app/sonarqube-unified-app/src/main/java/com/sonar/sca/ce/reachability/ReachabilityLanguage.java#L16-L23
  public static final int LANGUAGE_KEY_MAX_LENGTH = 10;

  public CreateScaRefdbReachabilityDefinitionsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_LANGUAGE_KEY).setIsNullable(false).setLimit(LANGUAGE_KEY_MAX_LENGTH).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_VULNERABILITY_ID).setIsNullable(false).setLimit(VULNERABILITY_ID_MAX_LENGTH).build())
      // NB: The `vulnerable_functions`, `public_callers`, and `ancestor_callers` columns are `VARCHAR[]` in sonar-sca's Postgres database. Repo convention in `sonar-enterprise`
      // is to use `CLOB` for cross-dialect compatibility.
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_VULNERABLE_FUNCTIONS).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_PUBLIC_CALLERS).setIsNullable(false).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_ANCESTOR_CALLERS).setIsNullable(false).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(TABLE_NAME)
      .setName(INDEX_LANGUAGE_VULNERABILITY_UNIQ)
      .setUnique(true)
      .addColumn(COLUMN_LANGUAGE_KEY, false)
      .addColumn(COLUMN_VULNERABILITY_ID, false)
      .build());
  }
}
