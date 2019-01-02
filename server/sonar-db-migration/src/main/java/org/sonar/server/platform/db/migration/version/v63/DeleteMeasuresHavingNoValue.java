/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v63;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

/**
 * Delete measures having no value in order to remove measures related to periods 2 to 5.
 * <br/>
 * Here are some example of measures to remove :
 * <ul>
 * <li>VALUE=[null], TEXT_VALUE=[null], MEASURE_DATA=[null], VARIATION_VALUE_1=[null], VARIATION_VALUE_2=[null], VARIATION_VALUE_3=[null]</li>
 * <li>VALUE=[null], TEXT_VALUE=[null], MEASURE_DATA=[null], VARIATION_VALUE_1=[null], VARIATION_VALUE_2=20, VARIATION_VALUE_3=[null]</li>
 * <li>VALUE=[null], TEXT_VALUE=[null], MEASURE_DATA=[null], VARIATION_VALUE_1=[null], VARIATION_VALUE_2=10, VARIATION_VALUE_3=15</li>
 * </ul>
 */
public class DeleteMeasuresHavingNoValue extends DataChange {

  public DeleteMeasuresHavingNoValue(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT pm.id FROM project_measures pm " +
      "WHERE pm.value IS NULL AND pm.text_value IS NULL AND pm.variation_value_1 IS NULL AND pm.measure_data IS NULL ");
    massUpdate.update("DELETE FROM project_measures WHERE id=?");
    massUpdate.rowPluralName("measures");
    massUpdate.execute((row, update) -> {
      update.setLong(1, row.getLong(1));
      return true;
    });
  }

}
