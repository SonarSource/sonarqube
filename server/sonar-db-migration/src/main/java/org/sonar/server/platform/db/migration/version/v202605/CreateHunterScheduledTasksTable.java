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

import org.sonar.db.Database;

/**
 * Creates the {@code hunter_scheduled_tasks} table backing the unified Hunter agent's in-process
 * scheduler (db-scheduler, {@code com.github.kagkarlsson:db-scheduler}). The scheduler polls this
 * table and its DB-row locking guarantees that exactly one Data Center node executes each due task.
 * Schema and column rationale shared with other db-scheduler-backed tables are documented on
 * {@link AbstractCreateDbSchedulerTaskTable}.
 *
 * <p>The table is namespaced {@code hunter_scheduled_tasks} (rather than db-scheduler's default
 * {@code scheduled_tasks}) to fit the flat unified naming; the scheduler is configured with a matching
 * {@code .tableName(...)}.
 */
public class CreateHunterScheduledTasksTable extends AbstractCreateDbSchedulerTaskTable {

  static final String TABLE_NAME = "hunter_scheduled_tasks";

  // Capped at 50 so the composite PK (task_name, task_instance) stays within SQL Server's 900-byte
  // clustered-index key limit: VarcharColumnDef emits NVARCHAR (2 bytes/char) on MSSQL, so the key is
  // (TASK_NAME_SIZE + TASK_INSTANCE_SIZE) * 2 = (50 + 400) * 2 = 900 bytes. 50 comfortably fits the only
  // value stored here — the fixed task name "hunter-scheduled-detection" (26 chars).
  static final int TASK_NAME_SIZE = 50;
  // db-scheduler's documented schema uses VARCHAR(255) for task_instance, but here the instance id is
  // the SonarQube project key, and project keys are valid up to 400 characters — the default width
  // would reject legitimate projects. Widen to 400 to match the key limit.
  static final int TASK_INSTANCE_SIZE = 400;

  static final String INDEX_EXECUTION_TIME = "idx_hunter_sched_exec_time";
  static final String INDEX_LAST_HEARTBEAT = "idx_hunter_sched_heartbeat";

  protected CreateHunterScheduledTasksTable(Database db) {
    super(db, TABLE_NAME, TASK_NAME_SIZE, TASK_INSTANCE_SIZE, INDEX_EXECUTION_TIME, INDEX_LAST_HEARTBEAT);
  }
}
