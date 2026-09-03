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
package org.sonar.server.platform.db.migration.version.v202605;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Stores additional details specific to SCA dependency risk alert types. The {@code security_alert_uuid} column references {@code security_alerts.uuid}. There is a one-to-one
 * relationship between rows on the {@code security_alert_sca_risks} table and the corresponding referenced rows on the {@code security_alerts} table, but the inverse is not true:
 * not every row on {@code security_alerts} has a corresponding row on {@code security_alert_sca_risks}.
 *
 * <p>The column {@code sca_issue_uuid} references the identifier on {@code sca_issues}. When an issue on {@code sca_issues} is deleted, it is expected that the corresponding rows
 * on {@code security_alert_sca_risks} are cleaned up to avoid dangling references.
 */
public class CreateSecurityAlertScaRisksTable extends CreateTableChange {

  static final String TABLE_NAME = "security_alert_sca_risks";

  static final String COLUMN_SECURITY_ALERT_UUID = "security_alert_uuid";
  static final String COLUMN_SCA_ISSUE_UUID = "sca_issue_uuid";
  static final String COLUMN_ISSUE_DESCRIPTION = "issue_description";
  static final String COLUMN_CREATED_AT = "created_at";
  static final String COLUMN_UPDATED_AT = "updated_at";

  static final int SECURITY_ALERT_UUID_SIZE = 40;
  static final int SCA_ISSUE_UUID_SIZE = 40;
  static final int ISSUE_DESCRIPTION_SIZE = 4000;

  static final String INDEX_SCA_ISSUE_UUID = "security_alert_sca_issue_uniq";

  protected CreateSecurityAlertScaRisksTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SECURITY_ALERT_UUID).setIsNullable(false).setLimit(SECURITY_ALERT_UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SCA_ISSUE_UUID).setIsNullable(false).setLimit(SCA_ISSUE_UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ISSUE_DESCRIPTION).setIsNullable(true).setLimit(ISSUE_DESCRIPTION_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .build());

    // Enforces "update, don't duplicate": at most one alert per SCA dependency risk.
    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_SCA_ISSUE_UUID)
      .setUnique(true)
      .addColumn(COLUMN_SCA_ISSUE_UUID, false)
      .build());
  }
}
