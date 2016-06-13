/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.version.v60;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.sonar.db.Database;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.MassUpdate;
import org.sonar.db.version.Select;
import org.sonar.db.version.SqlStatement;

public class PopulateComponentUuidOfMeasures extends BaseDataChange {

  public PopulateComponentUuidOfMeasures(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    Map<Long, String> componentUuidById = buildComponentUuidMap(context);
    if (componentUuidById.isEmpty()) {
      return;
    }

    populateUuidColumns(context, componentUuidById);
  }

  private Map<Long, String> buildComponentUuidMap(Context context) throws SQLException {
    Map<Long, String> componentUuidById = new HashMap<>();
    context.prepareSelect("select distinct p.id, p.uuid from projects p" +
      " join project_measures pm on pm.project_id = p.id and pm.component_uuid is null")
      .scroll(row -> componentUuidById.put(row.getLong(1), row.getString(2)));
    return componentUuidById;
  }

  private void populateUuidColumns(Context context, Map<Long, String> componentUuidById) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT pm.id, pm.project_id from project_measures pm where pm.component_uuid is null");
    massUpdate.update("UPDATE project_measures SET component_uuid=? WHERE id=?");
    massUpdate.rowPluralName("measures");
    massUpdate.execute((row, update) -> this.handle(componentUuidById, row, update));
  }

  public boolean handle(Map<Long, String> componentUuidById, Select.Row row, SqlStatement update) throws SQLException {
    long id = row.getLong(1);
    long componentId = row.getLong(2);

    String componentUuid = componentUuidById.get(componentId);

    if (componentUuid == null) {
      return false;
    }

    update.setString(1, componentUuid);
    update.setLong(2, id);

    return true;
  }

}
