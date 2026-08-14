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
import static org.sonar.server.platform.db.migration.def.BlobColumnDef.newBlobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.TimestampColumnDef.newTimestampColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Creates the {@code hunter_scheduled_tasks} table backing the unified Hunter agent's in-process
 * scheduler (db-scheduler, {@code com.github.kagkarlsson:db-scheduler}). The scheduler polls this
 * table and its DB-row locking guarantees that exactly one Data Center node executes each due task.
 *
 * <p><b>Type choices deliberately diverge from the rest of the unified capability tables.</b> Unlike
 * {@code findings}/{@code agent_jobs} (which store timestamps as {@code BIGINT} epoch-millis and never
 * use binary columns), db-scheduler owns the read/write of this table and requires its documented
 * schema: real {@code TIMESTAMP} columns, a binary {@code task_data} payload and a {@code BOOLEAN
 * picked} flag. db-scheduler ships per-dialect DDL for exactly the databases SonarQube supports, so we
 * follow that shape rather than the local conventions. {@link
 * org.sonar.server.platform.db.migration.def.TimestampColumnDef} is deprecated in favour of
 * {@code BIGINT} epoch storage, but is the correct (and required) choice here because db-scheduler
 * binds {@code java.time.Instant} to a JDBC {@code TIMESTAMP}.
 *
 * <p>The primary key is the composite {@code (task_name, task_instance)} mandated by db-scheduler.
 * The table is namespaced {@code hunter_scheduled_tasks} (rather than db-scheduler's default
 * {@code scheduled_tasks}) to fit the flat unified naming; the scheduler is configured with a matching
 * {@code .tableName(...)}.
 */
public class CreateHunterScheduledTasksTable extends CreateTableChange {

  static final String TABLE_NAME = "hunter_scheduled_tasks";

  static final String COLUMN_TASK_NAME = "task_name";
  static final String COLUMN_TASK_INSTANCE = "task_instance";
  static final String COLUMN_TASK_DATA = "task_data";
  static final String COLUMN_EXECUTION_TIME = "execution_time";
  static final String COLUMN_PICKED = "picked";
  static final String COLUMN_PICKED_BY = "picked_by";
  static final String COLUMN_LAST_SUCCESS = "last_success";
  static final String COLUMN_LAST_FAILURE = "last_failure";
  static final String COLUMN_CONSECUTIVE_FAILURES = "consecutive_failures";
  static final String COLUMN_LAST_HEARTBEAT = "last_heartbeat";
  static final String COLUMN_VERSION = "version";

  // Capped at 50 so the composite PK (task_name, task_instance) stays within SQL Server's 900-byte
  // clustered-index key limit: VarcharColumnDef emits NVARCHAR (2 bytes/char) on MSSQL, so the key is
  // (TASK_NAME_SIZE + TASK_INSTANCE_SIZE) * 2 = (50 + 400) * 2 = 900 bytes. 50 comfortably fits the only
  // value stored here — the fixed task name "hunter-scheduled-detection" (26 chars).
  static final int TASK_NAME_SIZE = 50;
  // db-scheduler's documented schema uses VARCHAR(255) for task_instance, but here the instance id is
  // the SonarQube project key, and project keys are valid up to 400 characters — the default width
  // would reject legitimate projects. Widen to 400 to match the key limit.
  static final int TASK_INSTANCE_SIZE = 400;
  static final int PICKED_BY_SIZE = 100;

  // db-scheduler polls for due tasks with WHERE picked = false AND execution_time <= now() ORDER BY
  // execution_time, so an index on execution_time keeps polling off a full table scan as rows accumulate
  // (one recurring row per scheduling-enabled project). Name kept <= 30 chars for the Oracle identifier limit.
  static final String INDEX_EXECUTION_TIME = "idx_hunter_sched_exec_time";

  // Cluster housekeeping (stale-executions / dead-node recovery) scans last_heartbeat to reclaim
  // executions picked by a node that died; the official db-scheduler schemas ship this index so those
  // recovery checks do not full-scan the table. Name kept <= 30 chars for the Oracle identifier limit.
  static final String INDEX_LAST_HEARTBEAT = "idx_hunter_sched_heartbeat";

  protected CreateHunterScheduledTasksTable(Database db) {
    super(db, TABLE_NAME);
  }

  // TimestampColumnDef is required by db-scheduler; see class Javadoc
  @Override
  @SuppressWarnings("deprecation")
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_TASK_NAME).setIsNullable(false).setLimit(TASK_NAME_SIZE).build())
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_TASK_INSTANCE).setIsNullable(false).setLimit(TASK_INSTANCE_SIZE).build())
      .addColumn(newBlobColumnDefBuilder().setColumnName(COLUMN_TASK_DATA).setIsNullable(true).build())
      .addColumn(newTimestampColumnDefBuilder().setColumnName(COLUMN_EXECUTION_TIME).setIsNullable(false).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName(COLUMN_PICKED).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PICKED_BY).setIsNullable(true).setLimit(PICKED_BY_SIZE).build())
      .addColumn(newTimestampColumnDefBuilder().setColumnName(COLUMN_LAST_SUCCESS).setIsNullable(true).build())
      .addColumn(newTimestampColumnDefBuilder().setColumnName(COLUMN_LAST_FAILURE).setIsNullable(true).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_CONSECUTIVE_FAILURES).setIsNullable(true).build())
      .addColumn(newTimestampColumnDefBuilder().setColumnName(COLUMN_LAST_HEARTBEAT).setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_VERSION).setIsNullable(false).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_EXECUTION_TIME)
      .setUnique(false)
      .addColumn(COLUMN_EXECUTION_TIME, false)
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_LAST_HEARTBEAT)
      .setUnique(false)
      .addColumn(COLUMN_LAST_HEARTBEAT, false)
      .build());
  }
}
