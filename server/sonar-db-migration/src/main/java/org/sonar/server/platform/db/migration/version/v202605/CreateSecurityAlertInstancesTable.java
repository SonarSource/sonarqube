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
 * Creates the {@code security_alert_instances} table to store data related to individual instances of a security alert. Each alert instance is unique for a given (alert,
 * organization, branch) triple. The {@code security_alert_uuid} column references {@code security_alerts.uuid}; there is a one-to-many relationship between an alert row in
 * {@code security_alerts} and the instance rows in {@code security_alert_instances} that reference it. The columns {@code branch_uuid} and {@code project_uuid} are indexed to
 * improve performance for expected data access patterns.
 *
 * <p>When an instance of an alert on a given branch is resolved (i.e., reanalysis no longer detects the issue that triggered the alert), the status of that instance is changed to
 * {@code RESOLVED}. Alert instances are cleaned up when their corresponding alert, project, or branch is deleted.
 */
public class CreateSecurityAlertInstancesTable extends CreateTableChange {

  static final String TABLE_NAME = "security_alert_instances";

  static final String COLUMN_UUID = "uuid";
  static final String COLUMN_SECURITY_ALERT_UUID = "security_alert_uuid";
  static final String COLUMN_ORGANIZATION_UUID = "organization_uuid";
  static final String COLUMN_BRANCH_UUID = "branch_uuid";
  static final String COLUMN_PROJECT_UUID = "project_uuid";
  static final String COLUMN_STATUS = "status";
  static final String COLUMN_SEVERITY = "severity";
  static final String COLUMN_CREATED_AT = "created_at";
  static final String COLUMN_UPDATED_AT = "updated_at";

  static final int UUID_SIZE = 40;
  static final int SECURITY_ALERT_UUID_SIZE = 40;
  static final int ORGANIZATION_UUID_SIZE = 40;
  static final int BRANCH_UUID_SIZE = 40;
  static final int PROJECT_UUID_SIZE = 40;
  static final int STATUS_SIZE = 32;
  static final int SEVERITY_SIZE = 32;

  static final String INDEX_UNIQ = "security_alert_inst_uniq";
  static final String INDEX_BRANCH = "security_alert_inst_branch";
  static final String INDEX_PROJECT = "security_alert_inst_project";

  protected CreateSecurityAlertInstancesTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SECURITY_ALERT_UUID).setIsNullable(false).setLimit(SECURITY_ALERT_UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ORGANIZATION_UUID).setIsNullable(false).setLimit(ORGANIZATION_UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_BRANCH_UUID).setIsNullable(false).setLimit(BRANCH_UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PROJECT_UUID).setIsNullable(false).setLimit(PROJECT_UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_STATUS).setIsNullable(false).setLimit(STATUS_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SEVERITY).setIsNullable(false).setLimit(SEVERITY_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .build());

    // Natural key: one instance per (alert, organization, branch); also the upsert lookup for raise/refresh.
    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_UNIQ)
      .setUnique(true)
      .addColumn(COLUMN_SECURITY_ALERT_UUID, false)
      .addColumn(COLUMN_ORGANIZATION_UUID, false)
      .addColumn(COLUMN_BRANCH_UUID, false)
      .build());

    // Supports the set-reconciliation clear flow (find all open instances for a branch) and purge on branch deletion.
    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_BRANCH)
      .setUnique(false)
      .addColumn(COLUMN_BRANCH_UUID, false)
      .build());

    // Bulk project deletion fires only onProjectsDeleted (not onBranchesDeleted), and does so after
    // the project's branches are already gone - so purge-on-project-deletion needs its own lookup path.
    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_PROJECT)
      .setUnique(false)
      .addColumn(COLUMN_PROJECT_UUID, false)
      .build());
  }
}
