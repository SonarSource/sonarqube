/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
 * Remove the following settings from the PROPERTIES table :
 * - Settings about period 2 to 5 (sonar.timemachine.periodX + sonar.timemachine.periodX.VW + etc.)
 * - sonar.technicalDebt.hoursInDay
 * - sonar.authenticator.createUser
 */
public class DeleteUselessProperties extends DataChange {

  public DeleteUselessProperties(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate().rowPluralName("useless settings");
    massUpdate.select("SELECT id FROM properties WHERE " +
      "prop_key LIKE ? OR prop_key LIKE ? OR prop_key LIKE ? OR prop_key LIKE ? OR " +
      "prop_key IN (?, ?)")
      .setString(1, "sonar.timemachine.period2%")
      .setString(2, "sonar.timemachine.period3%")
      .setString(3, "sonar.timemachine.period4%")
      .setString(4, "sonar.timemachine.period5%")
      .setString(5, "sonar.technicalDebt.hoursInDay")
      .setString(6, "sonar.authenticator.createUser");
    massUpdate.update("DELETE FROM properties WHERE id=?");
    massUpdate.execute((row, update) -> {
      long propertyId = row.getLong(1);
      update.setLong(1, propertyId);
      return true;
    });
  }

}
