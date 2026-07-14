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

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateA3SContextItemsTable extends CreateTableChange {

  static final String TABLE_NAME = "a3s_context_items";
  static final String COLUMN_CONTEXT_UUID = "context_uuid";
  static final String COLUMN_ITEM_ID = "item_id";
  static final String COLUMN_SHA256 = "sha256";

  static final int UUID_SIZE = 40;
  static final int ITEM_ID_SIZE = 255;
  static final int SHA256_SIZE = 64;

  protected CreateA3SContextItemsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_CONTEXT_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ITEM_ID).setIsNullable(false).setLimit(ITEM_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SHA256).setIsNullable(false).setLimit(SHA256_SIZE).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName("idx_a3s_ctx_items_sha256")
      .setUnique(false)
      .addColumn(COLUMN_SHA256, false)
      .build());
  }
}
