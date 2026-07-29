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

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Creates the {@code arch_model_patterns} join table linking a Model's groups to the reusable
 * Patterns they reference. Purely a many-to-many association (no own uuid): a Model can reference
 * many Patterns, so uniqueness is expressed as a composite primary key across all three of
 * {@code organization_id}/{@code model_id}/{@code pattern_id}, which the Dynamo-style single
 * partition+sort key model cannot express. The secondary index on
 * ({@code organization_id}, {@code pattern_id}) backs the pattern-usage guard query
 * (is this pattern still referenced by any model before allowing its deletion).
 */
public class CreateArchModelPatternsTable extends CreateTableChange {

  static final String TABLE_NAME = "arch_model_patterns";
  static final String COLUMN_ORGANIZATION_ID = "organization_id";
  static final String COLUMN_MODEL_ID = "model_id";
  static final String COLUMN_PATTERN_ID = "pattern_id";
  static final String INDEX_PATTERN = "idx_arch_model_patterns_pat";

  protected CreateArchModelPatternsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ORGANIZATION_ID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_MODEL_ID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PATTERN_ID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_PATTERN)
      .setUnique(false)
      .addColumn(COLUMN_ORGANIZATION_ID, false)
      .addColumn(COLUMN_PATTERN_ID, false)
      .build());
  }
}
