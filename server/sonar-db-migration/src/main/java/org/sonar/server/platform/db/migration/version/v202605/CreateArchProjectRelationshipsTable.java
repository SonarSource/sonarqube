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
import org.sonar.server.platform.db.migration.sql.DynamoStyleTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateArchProjectRelationshipsTable extends CreateTableChange {

  static final String TABLE_NAME = "arch_proj_relations";
  static final String COLUMN_ORGANIZATION_ID = "organization_id";
  static final String COLUMN_UUID = "uuid";
  static final String COLUMN_PROJECT_ID = "project_id";
  static final String COLUMN_BOUNDARY_KEY = "boundary_key";
  static final String COLUMN_TARGET_COMPONENT_ID = "target_component_id";
  static final String COLUMN_TARGET_ENTRY_POINT_KEY = "target_entry_point_key";
  static final String INDEX_UUID = "arch_proj_relations_uuid";
  static final String INDEX_PROJECT = "arch_proj_relations_project";
  static final String INDEX_TARGET = "arch_proj_relations_target";

  static final int KEY_SIZE = 255;

  protected CreateArchProjectRelationshipsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new DynamoStyleTableBuilder(getDialect(), tableName)
      .withPartitionKey(newVarcharColumnDefBuilder().setColumnName(COLUMN_ORGANIZATION_ID).setLimit(UUID_SIZE).setIsNullable(false).build())
      .withSortKey(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setLimit(UUID_SIZE).setIsNullable(false).build())
      .withAttribute(newVarcharColumnDefBuilder().setColumnName(COLUMN_PROJECT_ID).setLimit(UUID_SIZE).setIsNullable(false).build())
      .withAttribute(newVarcharColumnDefBuilder().setColumnName(COLUMN_BOUNDARY_KEY).setLimit(KEY_SIZE).setIsNullable(true).build())
      .withAttribute(newVarcharColumnDefBuilder().setColumnName(COLUMN_TARGET_COMPONENT_ID).setLimit(UUID_SIZE).setIsNullable(true).build())
      .withAttribute(newVarcharColumnDefBuilder().setColumnName(COLUMN_TARGET_ENTRY_POINT_KEY).setLimit(KEY_SIZE).setIsNullable(true).build())
      .withGlobalSecondaryIndex(INDEX_UUID, COLUMN_UUID)
      .withGlobalSecondaryIndex(INDEX_TARGET, COLUMN_TARGET_COMPONENT_ID)
      .withLocalSecondaryIndex(INDEX_PROJECT, COLUMN_PROJECT_ID)
      .build());
  }
}
