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
import org.sonar.server.platform.db.migration.sql.AlterColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.db.DatabaseUtils.indexExistsIgnoreCase;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class ResizeScaTtrEntityIdAndEntityType extends DdlChange {

  static final String SCA_TTR_HISTORY_TABLE = "sca_ttr_history";
  static final String COLUMN_ENTITY_ID = "entity_id";
  static final String COLUMN_ENTITY_TYPE = "entity_type";
  static final String COLUMN_RECORDED_AT_EPOCH = "recorded_at_epoch";
  static final String COLUMN_SCA_DIMENSION_ID = "sca_dimension_id";
  static final String INDEX_UNIQUE = "sca_ttr_history_uq_idx";
  static final String INDEX_ENTITY_TYPE_RECORDED_AT = "sca_ttr_history_ent_type_epoch";

  static final int ENTITY_ID_SIZE = 40;
  static final int ENTITY_TYPE_SIZE = 40;

  public ResizeScaTtrEntityIdAndEntityType(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    var dialect = getDialect();

    // 1 - Resize the columns
    context.execute(new AlterColumnsBuilder(dialect, SCA_TTR_HISTORY_TABLE)
      .updateColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ENTITY_ID).setIsNullable(false).setLimit(ENTITY_ID_SIZE).build())
      .updateColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ENTITY_TYPE).setIsNullable(false).setLimit(ENTITY_TYPE_SIZE).build())
      .build());

    // 2 - Recreate the dropped indices
    try (var connection = getDatabase().getDataSource().getConnection()) {
      if (!indexExistsIgnoreCase(SCA_TTR_HISTORY_TABLE, INDEX_UNIQUE, connection)) {
        context.execute(new CreateIndexBuilder(dialect)
          .setTable(SCA_TTR_HISTORY_TABLE)
          .setName(INDEX_UNIQUE)
          .setUnique(true)
          .addColumn(COLUMN_ENTITY_ID, false)
          .addColumn(COLUMN_ENTITY_TYPE, false)
          .addColumn(COLUMN_SCA_DIMENSION_ID, false)
          .addColumn(COLUMN_RECORDED_AT_EPOCH, false, true)
          .build());
      }

      if (!indexExistsIgnoreCase(SCA_TTR_HISTORY_TABLE, INDEX_ENTITY_TYPE_RECORDED_AT, connection)) {
        context.execute(new CreateIndexBuilder(dialect)
          .setTable(SCA_TTR_HISTORY_TABLE)
          .setName(INDEX_ENTITY_TYPE_RECORDED_AT)
          .addColumn(COLUMN_ENTITY_ID)
          .addColumn(COLUMN_ENTITY_TYPE)
          .addColumn(COLUMN_RECORDED_AT_EPOCH)
          .build());
      }
    }
  }
}
