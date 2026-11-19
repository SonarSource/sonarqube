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
package org.sonar.server.platform.db.migration.version.v202606;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * The newest issue-count row of every (entity, entity type, dimension). The history library keeps it in step with
 * {@code issue_count_history}, so the current count of a dimension is one primary-key row instead of a walk through its history.
 */
public class CreateIssueCountLatestTable extends CreateTableChange {

  static final String TABLE_NAME = "issue_count_latest";
  static final String COLUMN_ENTITY_ID = "entity_id";
  static final String COLUMN_ENTITY_TYPE = "entity_type";
  static final String COLUMN_DIMENSION_ID = "dimension_id";
  static final String COLUMN_RECORDED_AT_EPOCH = "recorded_at_epoch";
  static final String COLUMN_ISSUE_COUNT = "issue_count";
  static final String PK_CONSTRAINT = "pk_issue_count_latest";

  static final int ENTITY_ID_SIZE = 40;
  static final int ENTITY_TYPE_SIZE = 40;

  protected CreateIssueCountLatestTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ENTITY_ID).setIsNullable(false).setLimit(ENTITY_ID_SIZE).build())
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ENTITY_TYPE).setIsNullable(false).setLimit(ENTITY_TYPE_SIZE).build())
      .addPkColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_DIMENSION_ID).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_RECORDED_AT_EPOCH).setIsNullable(false).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_ISSUE_COUNT).setIsNullable(false).build())
      .withPkConstraintName(PK_CONSTRAINT)
      .build());
  }
}
