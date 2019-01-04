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
package org.sonar.server.platform.db.migration.version.v76;

import java.sql.SQLException;
import org.sonar.api.config.Configuration;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

/**
 * Remove module and folder level measures
 */
@SupportsBlueGreen
public class DeleteModuleAndFolderMeasures extends DataChange {

  private final Configuration configuration;

  public DeleteModuleAndFolderMeasures(Database db, Configuration configuration) {
    super(db);
    this.configuration = configuration;
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (isSonarCloud(configuration)) {
      return;
    }
    MassUpdate massUpdate = context.prepareMassUpdate().rowPluralName("components");
    massUpdate.select("SELECT p.uuid FROM projects p WHERE p.qualifier in ('DIR', 'BRC') AND exists(SELECT 1 FROM project_measures m WHERE m.component_uuid = p.uuid)");
    massUpdate.update("DELETE FROM project_measures WHERE component_uuid=?")
      // important to keep the number of rows in a transaction under control. A component may have dozens/hundreds of measures to be deleted.
      .setBatchSize(10);
    massUpdate.execute((row, update) -> {
      String componentUuid = row.getString(1);
      update.setString(1, componentUuid);
      return true;
    });
  }

}
