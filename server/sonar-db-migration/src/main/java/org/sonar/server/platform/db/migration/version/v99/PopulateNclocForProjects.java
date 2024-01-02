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
package org.sonar.server.platform.db.migration.version.v99;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulateNclocForProjects extends DataChange {

  private static final String SELECT_QUERY = """
    SELECT b.project_uuid AS projectUuid, max(lm.value) AS maxncloc
    FROM live_measures lm
    INNER JOIN metrics m ON m.uuid = lm.metric_uuid
    INNER JOIN project_branches b ON b.uuid = lm.component_uuid
    INNER JOIN projects p on p.uuid = b.project_uuid and p.qualifier = 'TRK'
    WHERE m.name  = 'ncloc'
    GROUP BY b.project_uuid
    """;

  public PopulateNclocForProjects(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(SELECT_QUERY);
    massUpdate.update("update projects set ncloc = ? where uuid = ?");
    massUpdate.execute((row, update) -> {
      String uuid = row.getString(1);
      Long ncloc = row.getLong(2);
      update.setLong(1, ncloc);
      update.setString(2, uuid);
      return true;
    });
  }
}
