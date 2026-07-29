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

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateArchOrganizationPlaceholdersTable extends CreateTableChange {

  static final String TABLE_NAME = "arch_org_placeholders";
  static final String COLUMN_ORGANIZATION_ID = "organization_id";
  static final String COLUMN_UUID = "uuid";
  static final String COLUMN_DISPLAY_NAME = "display_name";
  static final String COLUMN_TYPE = "type";
  static final String COLUMN_CREATED_AT = "created_at";
  static final String INDEX_UUID = "arch_org_placeholders_uuid";

  static final int DISPLAY_NAME_SIZE = 256;
  static final int TYPE_SIZE = 255;

  protected CreateArchOrganizationPlaceholdersTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new DynamoStyleTableBuilder(getDialect(), tableName)
      .withPartitionKey(newVarcharColumnDefBuilder().setColumnName(COLUMN_ORGANIZATION_ID).setLimit(UUID_SIZE).setIsNullable(false).build())
      .withSortKey(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setLimit(UUID_SIZE).setIsNullable(false).build())
      .withAttribute(newVarcharColumnDefBuilder().setColumnName(COLUMN_DISPLAY_NAME).setLimit(DISPLAY_NAME_SIZE).setIsNullable(false).build())
      .withAttribute(newVarcharColumnDefBuilder().setColumnName(COLUMN_TYPE).setLimit(TYPE_SIZE).setIsNullable(true).build())
      .withAttribute(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .withGlobalSecondaryIndex(INDEX_UUID, COLUMN_UUID)
      .build());
  }
}
