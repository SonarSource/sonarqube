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

import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Creates the {@code project_configs} table holding the unified Hunter agent's per-project
 * enablement and scheduling configuration. There is at most one row per project, so
 * {@code project_id} is backed by a unique index; a separate non-unique index supports lookups by
 * {@code organization_id}.
 *
 * <p>The {@code scheduling_*} / {@code cron} columns overlap conceptually with the orchestrator's
 * re-modeled {@code agent_schedules} table; they are carried here to stay faithful to the SonarCloud
 * reference schema, and reconciling the two scheduling models is a separate design decision.
 *
 * <p>Boolean columns are rendered per dialect by the framework ({@code BOOLEAN} on PostgreSQL/H2,
 * {@code NUMBER(1)} on Oracle, {@code BIT} on MS SQL Server); uuids are stored as {@code VARCHAR(40)}.
 */
public class CreateProjectConfigsTable extends CreateTableChange {

  static final String TABLE_NAME = "project_configs";

  static final String COLUMN_ID = "id";
  static final String COLUMN_ORGANIZATION_ID = "organization_id";
  static final String COLUMN_PROJECT_ID = "project_id";
  static final String COLUMN_IS_ENABLED = "is_enabled";
  static final String COLUMN_CRON = "cron";
  static final String COLUMN_PROJECT_LEGACY_ID = "project_legacy_id";
  static final String COLUMN_ORGANIZATION_LEGACY_ID = "organization_legacy_id";
  static final String COLUMN_IS_SCHEDULING_ENABLED = "is_scheduling_enabled";
  static final String COLUMN_SCHEDULING_BRANCH_ID = "scheduling_branch_id";
  static final String COLUMN_SCHEDULING_TIMEZONE = "scheduling_timezone";

  static final int ID_SIZE = 40;
  static final int ORGANIZATION_ID_SIZE = 40;
  static final int PROJECT_ID_SIZE = 40;
  static final int CRON_SIZE = 100;
  static final int PROJECT_LEGACY_ID_SIZE = 40;
  static final int ORGANIZATION_LEGACY_ID_SIZE = 40;
  static final int SCHEDULING_BRANCH_ID_SIZE = 40;
  static final int SCHEDULING_TIMEZONE_SIZE = 50;

  static final String INDEX_PROJECT_ID = "uq_project_configs_project_id";
  static final String INDEX_ORGANIZATION_ID = "idx_proj_configs_org_id";

  protected CreateProjectConfigsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ID).setIsNullable(false).setLimit(ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ORGANIZATION_ID).setIsNullable(false).setLimit(ORGANIZATION_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PROJECT_ID).setIsNullable(false).setLimit(PROJECT_ID_SIZE).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName(COLUMN_IS_ENABLED).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_CRON).setIsNullable(false).setLimit(CRON_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PROJECT_LEGACY_ID).setIsNullable(false).setLimit(PROJECT_LEGACY_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ORGANIZATION_LEGACY_ID).setIsNullable(false).setLimit(ORGANIZATION_LEGACY_ID_SIZE).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName(COLUMN_IS_SCHEDULING_ENABLED).setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SCHEDULING_BRANCH_ID).setIsNullable(true).setLimit(SCHEDULING_BRANCH_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SCHEDULING_TIMEZONE).setIsNullable(true).setLimit(SCHEDULING_TIMEZONE_SIZE).build())
      .build());

    // Unique index: at most one config row per project. Also serves lookups by project_id.
    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_PROJECT_ID)
      .setUnique(true)
      .addColumn(COLUMN_PROJECT_ID, false)
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_ORGANIZATION_ID)
      .setUnique(false)
      .addColumn(COLUMN_ORGANIZATION_ID, false)
      .build());
  }
}
