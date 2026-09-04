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
 * Creates the {@code a3s_purge_tasks} table backing the scheduler (db-scheduler) that runs the
 * A3S context purge inside the web process: a single recurring job that deletes
 * {@code a3s_contexts} rows past their retention, together with the {@code a3s_context_items} content
 * they were the last to reference. db-scheduler's row locking is what guarantees exactly one Data
 * Center node runs a given occurrence, so no custom locking is needed.
 *
 * <p>Shape and type rationale live in {@link AbstractCreateDbSchedulerTaskTable}. A table of its own,
 * rather than sharing {@code hunter_scheduled_tasks}, {@code agent_orch_tasks} or
 * {@code remediation_sched_tasks}, so the capabilities' schedulers never contend on one another's
 * rows — each polls only what it owns.
 */
public class CreateA3SPurgeTasksTable extends AbstractCreateDbSchedulerTaskTable {

  static final String TABLE_NAME = "a3s_purge_tasks";

  // The only value stored here is the fixed name of the single recurring purge task, so 50 is ample.
  static final int TASK_NAME_SIZE = 50;
  // db-scheduler's documented width. The purge job is a static recurring task — its instance id is the
  // library's own "recurring" constant, never a project key — so there is no reason to widen it as
  // hunter_scheduled_tasks and remediation_sched_tasks had to.
  static final int TASK_INSTANCE_SIZE = 255;

  // Both kept at or under 30 characters for the Oracle identifier limit.
  static final String INDEX_EXECUTION_TIME = "idx_a3s_purge_exec_time";
  static final String INDEX_LAST_HEARTBEAT = "idx_a3s_purge_heartbeat";

  protected CreateA3SPurgeTasksTable(Database db) {
    super(db, TABLE_NAME, TASK_NAME_SIZE, TASK_INSTANCE_SIZE, INDEX_EXECUTION_TIME, INDEX_LAST_HEARTBEAT);
  }
}
