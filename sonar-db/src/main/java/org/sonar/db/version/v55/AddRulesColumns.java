/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.version.v55;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.version.AddColumnsBuilder;
import org.sonar.db.version.DdlChange;
import org.sonar.db.version.TinyIntColumnDef;

import static org.sonar.db.version.BigDecimalColumnDef.newBigDecimalColumnDefBuilder;

/**
 * Add the following columns to the rules table :
 * - created_at_ms
 * - updated_at_ms
 * - rule_type
 */
public class AddRulesColumns extends DdlChange {

  private final Database db;

  public AddRulesColumns(Database db) {
    super(db);
    this.db = db;
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(generateSql());
  }

  private String generateSql() {
    return new AddColumnsBuilder(db.getDialect(), "rules")
      .addColumn(newBigDecimalColumnDefBuilder().setColumnName("created_at_ms").setIsNullable(true).build())
      .addColumn(newBigDecimalColumnDefBuilder().setColumnName("updated_at_ms").setIsNullable(true).build())
      .addColumn(new TinyIntColumnDef.Builder().setColumnName("rule_type").setIsNullable(true).build())
      .build();
  }

}
