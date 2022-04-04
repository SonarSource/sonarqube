/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84.rules.rulesparameters;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class AddIndexesToRulesParametersTable extends DdlChange {
  private static final String TABLE = "rules_parameters";
  private static final String RULE_UUID_INDEX = "rules_parameters_rule_uuid";
  private static final String RULE_UUID_NAME_UNIQUE_INDEX = "rules_parameters_unique";

  private static final VarcharColumnDef uuidColumnDefinition = newVarcharColumnDefBuilder()
    .setColumnName("rule_uuid")
    .setIsNullable(true)
    .setDefaultValue(null)
    .setLimit(VarcharColumnDef.UUID_SIZE)
    .build();

  private static final VarcharColumnDef nameColumnDefinition = newVarcharColumnDefBuilder()
    .setColumnName("name")
    .setLimit(128)
    .setIsNullable(false)
    .build();

  public AddIndexesToRulesParametersTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (indexDoesNotExist(RULE_UUID_INDEX)) {
      context.execute(new CreateIndexBuilder()
        .setTable(TABLE)
        .setName(RULE_UUID_INDEX)
        .addColumn(uuidColumnDefinition)
        .build());
    }

    if (indexDoesNotExist(RULE_UUID_NAME_UNIQUE_INDEX)) {
      context.execute(new CreateIndexBuilder()
        .setTable(TABLE)
        .setName(RULE_UUID_NAME_UNIQUE_INDEX)
        .addColumn(uuidColumnDefinition)
        .addColumn(nameColumnDefinition)
        .setUnique(true)
        .build());
    }
  }

  private boolean indexDoesNotExist(String index) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      return !DatabaseUtils.indexExistsIgnoreCase(TABLE, index, connection);
    }
  }

}
