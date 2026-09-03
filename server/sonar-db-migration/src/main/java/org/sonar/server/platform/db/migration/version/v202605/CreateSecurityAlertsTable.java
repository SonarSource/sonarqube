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
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Creates the {@code security_alerts} table: the global identity of a security alert, one row per
 * alert-worthy condition regardless of how many organizations/branches currently see it.
 *
 * <p>This table stores identifiers and lifecycle facts only, never the SCA facts (severity, CVE id,
 * package URL, status) that justified raising the alert &mdash; those are joinable from the SCA
 * schema via {@code security_alert_sca_risks.sca_issue_uuid}, so nothing here ever needs to be
 * refreshed when SCA data changes. {@code alert_type} is a plain {@code VARCHAR} validated in
 * application code rather than a native ENUM type or a {@code CHECK} constraint, consistent with
 * other enum-like columns in this schema (e.g. {@code findings.issue_status}). As elsewhere in
 * SonarQube, no physical foreign key is created and referential integrity is enforced in the
 * application layer.
 *
 * <p>The primary key column is named {@code uuid} rather than {@code id}, matching the naming
 * used for equivalent identifier columns elsewhere in this schema; future SonarQube Cloud tables
 * covering the same data should follow the same convention for column-name parity.
 */
public class CreateSecurityAlertsTable extends CreateTableChange {

  static final String TABLE_NAME = "security_alerts";

  static final String COLUMN_UUID = "uuid";
  static final String COLUMN_ALERT_TYPE = "alert_type";
  static final String COLUMN_CREATED_AT = "created_at";
  static final String COLUMN_UPDATED_AT = "updated_at";

  static final int UUID_SIZE = 40;
  static final int ALERT_TYPE_SIZE = 32;

  protected CreateSecurityAlertsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ALERT_TYPE).setIsNullable(false).setLimit(ALERT_TYPE_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .build());
  }
}
