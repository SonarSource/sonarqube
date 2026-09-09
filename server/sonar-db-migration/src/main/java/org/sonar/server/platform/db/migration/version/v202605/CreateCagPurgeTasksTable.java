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
 * Creates the {@code cag_purge_tasks} table backing the scheduler (db-scheduler) that runs the
 * CAG Hub impact-event purge inside the web process: a single recurring job that deletes CAG and
 * SQAA impact-event rows older than 13 months ({@code CAG-1109} / {@code CAG-1110}). db-scheduler's
 * row locking is what guarantees exactly one Data Center node runs a given occurrence, so no custom
 * locking is needed.
 *
 * <p>Shape and type rationale live in {@link AbstractCreateDbSchedulerTaskTable}. A table of its own,
 * rather than sharing {@code a3s_purge_tasks} or Hunter/Remediation scheduler tables, so the
 * capabilities' schedulers never contend on one another's rows.
 */
public class CreateCagPurgeTasksTable extends AbstractCreateDbSchedulerTaskTable {

  static final String TABLE_NAME = "cag_purge_tasks";

  static final int TASK_NAME_SIZE = 50;
  static final int TASK_INSTANCE_SIZE = 255;

  static final String INDEX_EXECUTION_TIME = "idx_cag_purge_exec_time";
  static final String INDEX_LAST_HEARTBEAT = "idx_cag_purge_heartbeat";

  protected CreateCagPurgeTasksTable(Database db) {
    super(db, TABLE_NAME, TASK_NAME_SIZE, TASK_INSTANCE_SIZE, INDEX_EXECUTION_TIME, INDEX_LAST_HEARTBEAT);
  }
}
