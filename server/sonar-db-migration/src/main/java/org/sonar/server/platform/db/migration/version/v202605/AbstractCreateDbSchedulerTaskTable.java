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
 * Base for migrations that create a table backing an in-process db-scheduler
 * ({@code com.github.kagkarlsson:db-scheduler}) instance. db-scheduler owns the read/write of these
 * tables and mandates their documented schema: a binary {@code task_data} payload, real
 * {@code TIMESTAMP} columns and a {@code BOOLEAN picked} flag — unlike the rest of the unified
 * capability tables, which store timestamps as {@code BIGINT} epoch-millis and never use binary
 * columns. {@link org.sonar.server.platform.db.migration.def.TimestampColumnDef} is deprecated in
 * favour of {@code BIGINT} epoch storage, but is the correct (and required) choice here because
 * db-scheduler binds {@code java.time.Instant} to a JDBC {@code TIMESTAMP}.
 *
 * <p>The primary key is the composite {@code (task_name, task_instance)} mandated by db-scheduler.
 * Column widths and index names are supplied by subclasses via the constructor since they depend on
 * each table's producer, dialect key-size limits and naming constraints.
 */
public abstract class AbstractCreateDbSchedulerTaskTable extends CreateTableChange {

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

  static final int PICKED_BY_SIZE = 100;

  private final int taskNameSize;
  private final int taskInstanceSize;
  private final String executionTimeIndexName;
  private final String lastHeartbeatIndexName;

  /**
   * @param taskNameSize max length of the {@code task_name} column of the composite primary key
   * @param taskInstanceSize max length of the {@code task_instance} column of the composite primary key
   * @param executionTimeIndexName name of the index on {@code execution_time}; must be at or under 30
   *   characters for the Oracle identifier limit (see
   *   {@link org.sonar.server.platform.db.migration.def.Validations})
   * @param lastHeartbeatIndexName name of the index on {@code last_heartbeat}; same 30-character
   *   constraint as {@code executionTimeIndexName}
   */
  protected AbstractCreateDbSchedulerTaskTable(Database db, String tableName, int taskNameSize, int taskInstanceSize,
    String executionTimeIndexName, String lastHeartbeatIndexName) {
    super(db, tableName);
    this.taskNameSize = taskNameSize;
    this.taskInstanceSize = taskInstanceSize;
    this.executionTimeIndexName = executionTimeIndexName;
    this.lastHeartbeatIndexName = lastHeartbeatIndexName;
  }

  // TimestampColumnDef is required by db-scheduler; see class Javadoc
  @Override
  @SuppressWarnings("deprecation")
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_TASK_NAME).setIsNullable(false).setLimit(taskNameSize).build())
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_TASK_INSTANCE).setIsNullable(false).setLimit(taskInstanceSize).build())
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

    // db-scheduler polls WHERE picked = false AND execution_time <= now() ORDER BY execution_time, so
    // this index keeps polling off a full table scan as rows accumulate.
    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(executionTimeIndexName)
      .setUnique(false)
      .addColumn(COLUMN_EXECUTION_TIME, false)
      .build());

    // Dead-execution detection scans last_heartbeat to recover executions picked by an instance that
    // died; the official db-scheduler schemas ship this index for exactly that reason, without which
    // every instance full-scans the table during recovery checks.
    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(lastHeartbeatIndexName)
      .setUnique(false)
      .addColumn(COLUMN_LAST_HEARTBEAT, false)
      .build());
  }
}
