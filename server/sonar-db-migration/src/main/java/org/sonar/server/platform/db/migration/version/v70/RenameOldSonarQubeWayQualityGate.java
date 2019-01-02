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
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class RenameOldSonarQubeWayQualityGate extends DataChange {

  private static final Logger LOG = Loggers.get(RenameOldSonarQubeWayQualityGate.class);
  private static final String SONARQUBE_WAY_QUALITY_GATE = "SonarQube way";
  private static final String SONARQUBE_WAY_QUALITY_GATE_OUTDATED = "Sonar way (outdated copy)";
  private final System2 system2;

  public RenameOldSonarQubeWayQualityGate(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    try {
      MassUpdate massUpdate = context.prepareMassUpdate();
      massUpdate.select("SELECT id FROM quality_gates WHERE name = ?")
        .setString(1, SONARQUBE_WAY_QUALITY_GATE);
      massUpdate.rowPluralName("quality gates");
      massUpdate.update("UPDATE quality_gates SET name=?, is_built_in=?, updated_at=? WHERE id=?");
      massUpdate.execute((row, update) -> {
        update.setString(1, SONARQUBE_WAY_QUALITY_GATE_OUTDATED);
        update.setBoolean(2, false);
        update.setDate(3, new Date(system2.now()));
        update.setLong(4, row.getLong(1));
        return true;
      });
    } catch(Exception ex) {
      LOG.error("There is already a quality profile with name [{}]", SONARQUBE_WAY_QUALITY_GATE_OUTDATED);
      throw ex;
    }
  }
}
