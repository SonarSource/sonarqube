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
package org.sonar.server.platform.db.migration.version.v70;

import java.sql.SQLException;
import java.util.Date;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulateQualityGatesIsBuiltIn extends DataChange {

  private static final String SONARQUBE_WAY_QUALITY_GATE = "SonarQube way";

  private final System2 system2;

  public PopulateQualityGatesIsBuiltIn(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select id, name from quality_gates where is_built_in is null");
    massUpdate.rowPluralName("quality_gates");
    massUpdate.update("update quality_gates set is_built_in=?, updated_at=? where id=?");
    massUpdate.execute((row, update) -> {
      String name = row.getString(2);
      update.setBoolean(1, SONARQUBE_WAY_QUALITY_GATE.equals(name));
      update.setDate(2, new Date(system2.now()));
      update.setLong(3, row.getLong(1));
      return true;
    });
  }

}
