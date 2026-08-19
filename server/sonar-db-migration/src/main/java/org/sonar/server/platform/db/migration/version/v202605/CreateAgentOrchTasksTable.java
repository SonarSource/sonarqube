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
 * Creates the {@code agent_orch_tasks} table backing the in-process scheduler (db-scheduler,
 * {@code com.github.kagkarlsson:db-scheduler}) that replaces the generic {@code
 * agent_orchestrator_queue} claim queue for {@code JobDispatcher}. Schema and column rationale shared
 * with other db-scheduler-backed tables are documented on {@link AbstractCreateDbSchedulerTaskTable}.
 *
 * <p>The optional {@code priority} column is omitted since db-scheduler's priority feature is not
 * enabled here.
 */
public class CreateAgentOrchTasksTable extends AbstractCreateDbSchedulerTaskTable {

  static final String TABLE_NAME = "agent_orch_tasks";

  static final int TASK_NAME_SIZE = 255;
  // task_instance holds a job id (agent_jobs.id, VARCHAR(40)) — narrower than db-scheduler's
  // documented default VARCHAR(255), but wide enough for this table's only producer.
  static final int TASK_INSTANCE_SIZE = 40;

  static final String INDEX_EXECUTION_TIME = "idx_agent_orch_exec_time";
  static final String INDEX_LAST_HEARTBEAT = "idx_agent_orch_heartbeat";

  protected CreateAgentOrchTasksTable(Database db) {
    super(db, TABLE_NAME, TASK_NAME_SIZE, TASK_INSTANCE_SIZE, INDEX_EXECUTION_TIME, INDEX_LAST_HEARTBEAT);
  }
}
