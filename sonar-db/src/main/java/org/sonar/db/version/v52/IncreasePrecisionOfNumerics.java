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
package org.sonar.db.version.v52;

import java.sql.SQLException;
import java.util.List;
import org.sonar.db.Database;
import org.sonar.db.version.AlterColumnsBuilder;
import org.sonar.db.version.DdlChange;

import static org.sonar.db.version.DecimalColumnDef.newDecimalColumnDefBuilder;

/**
 * Update numeric type from numeric(30,20) to numeric(38,20) :
 * <ul>
 *  <li>metrics.worst_value</li>
 *  <li>metrics.best_value</li>
 *  <li>project_measures.value</li>
 *  <li>project_measures.variation_value_1</li>
 *  <li>project_measures.variation_value_2</li>
 *  <li>project_measures.variation_value_3</li>
 *  <li>project_measures.variation_value_4</li>
 *  <li>project_measures.variation_value_5</li>
 *  <li>manual_measures.value</li>
 * </ul>
 *
 * The goal is to be able to manage DATE (DATE is using 13 digits) in these columns
 */
public class IncreasePrecisionOfNumerics extends DdlChange {

  public IncreasePrecisionOfNumerics(Database db) {
    super(db);
  }

  @Override
  public void execute(DdlChange.Context context) throws SQLException {
    executeSql(context, "metrics", "worst_value", "best_value");
    executeSql(context, "project_measures", "value", "variation_value_1", "variation_value_2", "variation_value_3", "variation_value_4", "variation_value_5");
    executeSql(context, "manual_measures", "value");
  }

  private void executeSql(DdlChange.Context context, String table, String... columns) throws SQLException {
    for (String sql : generateSql(table, columns)) {
      context.execute(sql);
    }
  }

  private List<String> generateSql(String table, String... columns) {
    AlterColumnsBuilder columnsBuilder = new AlterColumnsBuilder(getDialect(), table);
    for (String column : columns) {
      columnsBuilder.updateColumn(newDecimalColumnDefBuilder().setColumnName(column).build());
    }
    return columnsBuilder.build();
  }

}
