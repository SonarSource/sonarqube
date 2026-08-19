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
 * Creates the {@code remediation_sched_tasks} table backing the remediation orchestrator's scheduler
 * (db-scheduler). It holds two kinds of row: a single static recurring row that triggers the
 * scheduling planner cluster-wide, and one short-lived row per project that the planner fans work out
 * to. db-scheduler's row locking is what guarantees exactly one orchestrator node runs the planner per
 * interval and that each per-project work unit is claimed once.
 *
 * <p>Shape and type rationale live in {@link AbstractCreateDbSchedulerTaskTable}. A table of its own,
 * rather than sharing {@code agent_orch_tasks} or {@code hunter_scheduled_tasks}, so the capabilities'
 * schedulers never contend on one another's rows — each polls only what it owns.
 */
public class CreateRemediationSchedTasksTable extends AbstractCreateDbSchedulerTaskTable {

  static final String TABLE_NAME = "remediation_sched_tasks";

  /**
   * Capped at 50 so the composite primary key stays within SQL Server's 900-byte clustered-index key
   * limit: VarcharColumnDef emits NVARCHAR (2 bytes/char) there, so the key is
   * (TASK_NAME_SIZE + TASK_INSTANCE_SIZE) * 2 = (50 + 400) * 2 = 900 bytes — exactly the limit. 50
   * comfortably fits the two names stored here ("remediation-planner", "remediation-project-run").
   * Do not widen it without narrowing task_instance by the same amount: H2 and PostgreSQL would still
   * pass and only the MSSQL CI matrix would catch the breakage.
   */
  static final int TASK_NAME_SIZE = 50;

  /**
   * db-scheduler documents VARCHAR(255) for task_instance, but the per-project work units use the
   * SonarQube project key as their instance id, and project keys are valid up to 400 characters
   * ({@code ComponentKeys.MAX_COMPONENT_KEY_LENGTH}) — the documented width would reject legitimate
   * projects. Widened to 400 to match that limit, as {@code hunter_scheduled_tasks} does.
   */
  static final int TASK_INSTANCE_SIZE = 400;

  // Both kept at or under 30 characters for the Oracle identifier limit.
  static final String INDEX_EXECUTION_TIME = "idx_rem_sched_exec_time";
  static final String INDEX_LAST_HEARTBEAT = "idx_rem_sched_heartbeat";

  protected CreateRemediationSchedTasksTable(Database db) {
    super(db, TABLE_NAME, TASK_NAME_SIZE, TASK_INSTANCE_SIZE, INDEX_EXECUTION_TIME, INDEX_LAST_HEARTBEAT);
  }
}
