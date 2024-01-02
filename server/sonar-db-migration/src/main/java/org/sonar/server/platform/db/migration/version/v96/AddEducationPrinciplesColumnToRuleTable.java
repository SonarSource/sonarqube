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
package org.sonar.server.platform.db.migration.version.v96;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class AddEducationPrinciplesColumnToRuleTable extends DdlChange {

  static final String COLUMN_EDUCATION_PRINCIPLES_KEY = "education_principles";

  static final String RULE_TABLE = "rules";

  public AddEducationPrinciplesColumnToRuleTable(Database db) {
    super(db);
  }

  @Override
  public void execute(DdlChange.Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (!DatabaseUtils.tableColumnExists(connection, RULE_TABLE, COLUMN_EDUCATION_PRINCIPLES_KEY)) {
        context.execute(new AddColumnsBuilder(getDialect(), RULE_TABLE)
          .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_EDUCATION_PRINCIPLES_KEY).setIsNullable(true).setLimit(255).build())
          .build());
      }
    }
  }

}
