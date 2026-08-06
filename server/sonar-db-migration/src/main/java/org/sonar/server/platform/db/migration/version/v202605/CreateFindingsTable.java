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
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Creates the {@code findings} table backing the unified Hunter agent's analysis-finding store.
 * Rows are written idempotently by the capability: the consumer derives a deterministic {@code id}
 * from the job id and finding identity, so an at-least-once event re-delivery re-inserts the same
 * primary key rather than creating a duplicate.
 *
 * <p>Portable type choices, consistent with the other unified capability tables ({@code agent_jobs},
 * {@code properties}, {@code agent_schedules}): uuids are stored as {@code VARCHAR(40)};
 * enum-like columns ({@code issue_status}) are plain {@code VARCHAR} validated in application code
 * rather than a native ENUM type or a {@code CHECK} constraint; the JSON payloads {@code context}
 * and {@code agent_context} are stored as large text (CLOB on Oracle/H2, {@code NVARCHAR(MAX)} on
 * MS SQL Server, {@code TEXT} on PostgreSQL); timestamps are epoch millis stored as {@code BIGINT}.
 *
 * <p>The sibling {@code finding_locations} table references {@code findings.id}; as elsewhere in
 * SonarQube, no physical foreign key is created and referential integrity is enforced in the
 * application layer.
 */
public class CreateFindingsTable extends CreateTableChange {

  static final String TABLE_NAME = "findings";

  static final String COLUMN_ID = "id";
  static final String COLUMN_JOB_ID = "job_id";
  static final String COLUMN_ORG_ID = "org_id";
  static final String COLUMN_PROJECT_ID = "project_id";
  static final String COLUMN_BRANCH_ID = "branch_id";
  static final String COLUMN_PLAYBOOK_ID = "playbook_id";
  static final String COLUMN_PLAYBOOK_KEY = "playbook_key";
  static final String COLUMN_RULE_ID = "rule_id";
  static final String COLUMN_EFFORT_MINUTES = "effort_minutes";
  static final String COLUMN_ISSUE_STATUS = "issue_status";
  static final String COLUMN_CONTEXT = "context";
  static final String COLUMN_AGENT_CONTEXT = "agent_context";
  static final String COLUMN_CREATED_AT = "created_at";
  static final String COLUMN_UPDATED_AT = "updated_at";

  static final int ID_SIZE = 40;
  static final int JOB_ID_SIZE = 40;
  static final int ORG_ID_SIZE = 40;
  static final int PROJECT_ID_SIZE = 40;
  static final int BRANCH_ID_SIZE = 40;
  static final int PLAYBOOK_ID_SIZE = 40;
  static final int PLAYBOOK_KEY_SIZE = 100;
  static final int RULE_ID_SIZE = 255;
  static final int ISSUE_STATUS_SIZE = 20;

  static final String INDEX_ORG_PROJECT_BRANCH = "idx_findings_org_proj_branch";

  protected CreateFindingsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ID).setIsNullable(false).setLimit(ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_JOB_ID).setIsNullable(false).setLimit(JOB_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ORG_ID).setIsNullable(false).setLimit(ORG_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PROJECT_ID).setIsNullable(false).setLimit(PROJECT_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_BRANCH_ID).setIsNullable(false).setLimit(BRANCH_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PLAYBOOK_ID).setIsNullable(false).setLimit(PLAYBOOK_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PLAYBOOK_KEY).setIsNullable(false).setLimit(PLAYBOOK_KEY_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_RULE_ID).setIsNullable(false).setLimit(RULE_ID_SIZE).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_EFFORT_MINUTES).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ISSUE_STATUS).setIsNullable(false).setLimit(ISSUE_STATUS_SIZE).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_CONTEXT).setIsNullable(false).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_AGENT_CONTEXT).setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_ORG_PROJECT_BRANCH)
      .setUnique(false)
      .addColumn(COLUMN_ORG_ID, false)
      .addColumn(COLUMN_PROJECT_ID, false)
      .addColumn(COLUMN_BRANCH_ID, false)
      .build());
  }
}
