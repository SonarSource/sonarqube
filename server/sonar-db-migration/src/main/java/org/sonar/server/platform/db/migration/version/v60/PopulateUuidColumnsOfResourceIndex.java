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
package org.sonar.server.platform.db.migration.version.v60;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;
import org.sonar.server.platform.db.migration.step.DataChange;

public class PopulateUuidColumnsOfResourceIndex extends DataChange {

  public PopulateUuidColumnsOfResourceIndex(Database db) {
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

  private static Map<Long, String> buildComponentUuidMap(Context context) throws SQLException {
    Map<Long, String> componentUuidById = new HashMap<>();
    context.prepareSelect("select distinct p.id, p.uuid from projects p" +
      " join resource_index ri1 on ri1.resource_id = p.id and ri1.component_uuid is null")
      .scroll(row -> componentUuidById.put(row.getLong(1), row.getString(2)));
    context.prepareSelect("select distinct p.id, p.uuid from projects p" +
      " join resource_index ri2 on ri2.root_project_id = p.id and ri2.root_component_uuid is null")
      .scroll(row -> componentUuidById.put(row.getLong(1), row.getString(2)));
    return componentUuidById;
  }

  private void populateUuidColumns(Context context, Map<Long, String> componentUuidById) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT ri.id, ri.resource_id, ri.root_project_id from resource_index ri where ri.component_uuid is null or ri.root_component_uuid is null");
    massUpdate.update("UPDATE resource_index SET component_uuid=?, root_component_uuid=? WHERE id=?");
    massUpdate.rowPluralName("resource index entries");
    massUpdate.execute((row, update) -> this.handle(componentUuidById, row, update));
  }

  public boolean handle(Map<Long, String> componentUuidById, Select.Row row, SqlStatement update) throws SQLException {
    long id = row.getLong(1);
    long componentId = row.getLong(2);
    long rootProjectId = row.getLong(3);

    String componentUuid = componentUuidById.get(componentId);
    String rootComponentUuid = componentUuidById.get(rootProjectId);

    if (componentUuid == null && rootComponentUuid == null) {
      return false;
    }

    update.setString(1, componentUuid);
    update.setString(2, rootComponentUuid);
    update.setLong(3, id);

    return true;
  }

}
