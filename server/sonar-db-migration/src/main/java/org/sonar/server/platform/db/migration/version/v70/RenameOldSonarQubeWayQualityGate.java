/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import static java.lang.String.format;

public class RenameOldSonarQubeWayQualityGate extends DataChange {

  private static final String SONARQUBE_WAY_QUALITY_GATE = "SonarQube way";
  private static final String SONARQUBE_WAY_QUALITY_GATE_OUTDATED = "Sonar way (outdated copy)";
  private final System2 system2;

  public RenameOldSonarQubeWayQualityGate(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT id FROM quality_gates WHERE name = ?")
      .setString(1, SONARQUBE_WAY_QUALITY_GATE);
    massUpdate.rowPluralName("quality gates");
    massUpdate.update("UPDATE quality_gates SET name=?, is_built_in=?, updated_at=? WHERE id=?");
    massUpdate.execute((row, update) -> {
      update.setString(1, findNewQualityGateName(context));
      update.setBoolean(2, false);
      update.setDate(3, new Date(system2.now()));
      update.setLong(4, row.getLong(1));
      return true;
    });
  }

  private String findNewQualityGateName(Context context) throws SQLException {
    if (isQualityGateNameAvailable(context, SONARQUBE_WAY_QUALITY_GATE_OUTDATED)) {
      return SONARQUBE_WAY_QUALITY_GATE_OUTDATED;
    }

    String newName = SONARQUBE_WAY_QUALITY_GATE_OUTDATED + " " + system2.now();
    if (isQualityGateNameAvailable(context, newName)) {
      return newName;
    }

    // Given up if no name available
    throw new IllegalStateException(format("There are already two quality profiles with name [%s,%s]", SONARQUBE_WAY_QUALITY_GATE_OUTDATED, newName));
  }

  private static boolean isQualityGateNameAvailable(Context context, String qualityGateName) throws SQLException {
    return context.prepareSelect(
      "SELECT COUNT(id) FROM quality_gates WHERE name = '" + qualityGateName + "'")
      .get(
        row -> row.getInt(1) == 0);
  }
}
