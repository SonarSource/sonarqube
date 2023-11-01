/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v104;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class DeleteRedundantFailedAlertsForApplications extends DataChange {

  private static final String SELECT_QUERY = """
    SELECT E.uuid
      FROM events E
      JOIN components C ON E.component_uuid = C.uuid
      WHERE E.name = 'Failed'
        AND E.category = 'Alert'
        AND E.event_data = '{ stillFailing: false, status: "ERROR" }'
        AND C.qualifier = 'APP'""";

  private static final String DELETE_EVENTS_STATEMENT = "DELETE FROM events WHERE uuid = ?";
  private static final String DELETE_EVENTS_COMPONENT_CHANGES_STATEMENT = "DELETE FROM event_component_changes WHERE event_uuid = ?";

  public DeleteRedundantFailedAlertsForApplications(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    var select = massUpdate.select(SELECT_QUERY);
    var deleteEventComponentChanges = massUpdate.update(DELETE_EVENTS_COMPONENT_CHANGES_STATEMENT);
    var deleteEvents = massUpdate.update(DELETE_EVENTS_STATEMENT);
    try (select; deleteEventComponentChanges; deleteEvents) {
      massUpdate.execute((row, delete, index) -> {
        // both updates use the same select, so no need to differentiate for the 2 update indexes
        String uuid = row.getString(1);
        delete.setString(1, uuid);
        return true;
      });
    }
  }
}
