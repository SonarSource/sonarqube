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
package org.sonar.server.platform.db.migration.version.v61;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class DeleteProjectDashboards extends DataChange {

  public DeleteProjectDashboards(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    deleteWidgetProperties(context);
    deleteDashboardsAndWidgets(context);
  }

  private static void deleteWidgetProperties(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT w.id " +
      "FROM widgets w " +
      " INNER JOIN dashboards d on w.dashboard_id=d.id " +
      "WHERE d.is_global=?")
      .setBoolean(1, false);
    massUpdate.update("DELETE from widget_properties WHERE widget_id=?");
    massUpdate.rowPluralName("delete widget properties of project dashboards");
    massUpdate.execute((row, update) -> {
      update.setLong(1, row.getLong(1));
      return true;
    });
  }

  private static void deleteDashboardsAndWidgets(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT id FROM dashboards d WHERE d.is_global=?").setBoolean(1, false);
    massUpdate.update("DELETE from widgets WHERE dashboard_id=?");
    massUpdate.update("DELETE from active_dashboards WHERE dashboard_id=?");
    massUpdate.update("DELETE from dashboards WHERE id=?");
    massUpdate.rowPluralName("delete project dashboards");
    massUpdate.execute((row, update, updateIndex) -> {
      long dashboardId = row.getLong(1);
      switch (updateIndex) {
        case 0:
        case 1:
        case 2:
          update.setLong(1, dashboardId);
          return true;
        default:
          throw new IllegalArgumentException("Unsupported update index " + updateIndex);
      }
    });
  }
}
