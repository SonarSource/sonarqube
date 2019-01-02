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
package org.sonar.server.platform.db.migration.version.v71;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;

public class CleanBrokenProjectToQGReferences extends DataChange {

  private static final String PROPERTY_SONAR_QUALITYGATE = "sonar.qualitygate";

  public CleanBrokenProjectToQGReferences(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    Set<String> qualityGateIds = new HashSet<>();
    context.prepareSelect("select id from quality_gates")
      .scroll(s -> qualityGateIds.add(String.valueOf(s.getInt(1))));

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select" +
      " distinct text_value" +
      " from properties" +
      " where" +
      "  prop_key=?" +
      "  and text_value is not null" +
      "  and resource_id is not null")
      .setString(1, PROPERTY_SONAR_QUALITYGATE);
    massUpdate.update("delete from properties" +
      " where" +
      "  prop_key=?" +
      "  and resource_id is not null" +
      "  and text_value=?");
    massUpdate.execute((row, update) -> handle(row, update, qualityGateIds));
  }

  private static boolean handle(Select.Row row, SqlStatement update, Set<String> qualityGateIds) throws SQLException {
    String qgId = row.getString(1);
    if (qualityGateIds.contains(qgId)) {
      return false;
    }

    update.setString(1, PROPERTY_SONAR_QUALITYGATE);
    update.setString(2, qgId);
    return true;
  }
}
