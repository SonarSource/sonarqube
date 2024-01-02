/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v104;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateRuleTagsTable extends CreateTableChange {

  static final String RULE_TAGS_TABLE_NAME = "rule_tags";

  static final String UUID_COLUMN_NAME = "uuid";
  static final String VALUE_COLUMN_NAME = "value";
  static final String IS_SYSTEM_TAG_COLUMN_NAME = "is_system_tag";
  static final String RULE_UUID_COLUMN_NAME = "rule_uuid";
  static final int VALUE_COLUMN_SIZE = 40;

  public CreateRuleTagsTable(Database db) {
    super(db, RULE_TAGS_TABLE_NAME);
  }

  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(VALUE_COLUMN_NAME).setIsNullable(false).setLimit(VALUE_COLUMN_SIZE).build())
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(RULE_UUID_COLUMN_NAME).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName(IS_SYSTEM_TAG_COLUMN_NAME).setIsNullable(false).build())
      .build());
  }
}
