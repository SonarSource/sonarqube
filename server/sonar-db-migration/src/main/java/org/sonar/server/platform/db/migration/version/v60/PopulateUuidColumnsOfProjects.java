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
package org.sonar.server.platform.db.migration.version.v60;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;
import org.sonar.server.platform.db.migration.step.DataChange;

public class PopulateUuidColumnsOfProjects extends DataChange {
  private static final Logger LOG = Loggers.get(PopulateUuidColumnsOfProjects.class);

  public PopulateUuidColumnsOfProjects(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {

    Map<Long, String> componentUuidById = buildComponentUuidMap(context);
    if (componentUuidById.isEmpty()) {
      return;
    }

    populateRootUuidColumnForRoots(context);
    populateRootUuidColumnForSubnodes(context, componentUuidById);
    populateCopyComponentUuidColumn(context, componentUuidById);
    populatePersonUuidColumn(context, componentUuidById);
  }

  private static Map<Long, String> buildComponentUuidMap(Context context) throws SQLException {
    Map<Long, String> componentUuidById = new HashMap<>();
    // rootId for root nodes (ie. column root_id is null)
    context.prepareSelect("select distinct p1.id, p1.uuid from projects p1" +
      " where p1.root_id is null and p1.root_uuid is null")
      .scroll(row -> componentUuidById.put(row.getLong(1), row.getString(2)));
    // rootId for other nodes (ie. column root_id is not null)
    context.prepareSelect("select distinct p1.id, p1.uuid from projects p1" +
      " join projects p2 on p1.id = p2.root_id" +
      " where p2.root_uuid is null")
      .scroll(row -> componentUuidById.put(row.getLong(1), row.getString(2)));
    // copyResourceId
    context.prepareSelect("select distinct p1.id, p1.uuid from projects p1" +
      " join projects p2 on p1.id = p2.copy_resource_id" +
      " where p2.copy_resource_id is not null and p2.copy_component_uuid is null")
      .scroll(row -> componentUuidById.put(row.getLong(1), row.getString(2)));
    // person_id
    context.prepareSelect("select distinct p1.id, p1.uuid from projects p1" +
      " join projects p2 on p1.id = p2.person_id" +
      " where p2.person_id is not null and p2.developer_uuid is null")
      .scroll(row -> componentUuidById.put(row.getLong(1), row.getString(2)));
    return componentUuidById;
  }

  private static void populateRootUuidColumnForRoots(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT distinct p.id from projects p where p.root_id is null and p.root_uuid is null");
    massUpdate.update("UPDATE projects SET root_uuid=uuid WHERE id=? and root_id is null and root_uuid is null");
    massUpdate.rowPluralName("root uuid of root components");
    massUpdate.execute(PopulateUuidColumnsOfProjects::handleRootIdUpdateForRootNodes);
  }

  private static boolean handleRootIdUpdateForRootNodes(Select.Row row, SqlStatement update) throws SQLException {
    long rootId = row.getLong(1);

    update.setLong(1, rootId);

    return true;
  }

  private static void populateRootUuidColumnForSubnodes(Context context, Map<Long, String> componentUuidById) throws SQLException {
    // update all rows with specific root_id which have no root_uuid yet in a single update
    // this will be efficient as root_id is indexed
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT distinct p.root_id from projects p where p.root_id is not null and p.root_uuid is null");
    massUpdate.update("UPDATE projects SET root_uuid=? WHERE root_id=? and root_uuid is null");
    massUpdate.rowPluralName("root uuid of non-root components");
    massUpdate.execute((row, update) -> handleRootIdUpdateForSubNodes(componentUuidById, row, update));
  }

  private static boolean handleRootIdUpdateForSubNodes(Map<Long, String> componentUuidById, Select.Row row, SqlStatement update) throws SQLException {
    long rootId = row.getLong(1);
    String rootUuid = componentUuidById.get(rootId);

    if (rootUuid == null) {
      LOG.trace("No UUID found for rootId={}", rootUuid);
      return false;
    }

    update.setString(1, rootUuid);
    update.setLong(2, rootId);

    return true;
  }

  private static void populateCopyComponentUuidColumn(Context context, Map<Long, String> componentUuidById) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT p.id, p.copy_resource_id from projects p where p.copy_resource_id is not null and p.copy_component_uuid is null");
    massUpdate.update("UPDATE projects SET copy_component_uuid=? WHERE id=?");
    massUpdate.rowPluralName("copy component uuid of components");
    massUpdate.execute((row, update) -> handleCopyComponentUuidUpdate(componentUuidById, row, update));
  }

  private static boolean handleCopyComponentUuidUpdate(Map<Long, String> componentUuidById, Select.Row row, SqlStatement update) throws SQLException {
    long id = row.getLong(1);
    long copyResourceId = row.getLong(2);

    String copyComponentUuid = componentUuidById.get(copyResourceId);
    if (copyComponentUuid == null) {
      LOG.trace("No UUID found for copyResourceId={}", copyResourceId);
      return false;
    }

    update.setString(1, copyComponentUuid);
    update.setLong(2, id);

    return true;
  }

  private static void populatePersonUuidColumn(Context context, Map<Long, String> componentUuidById) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT p.id, p.person_id from projects p where p.person_id is not null and p.developer_uuid is null");
    massUpdate.update("UPDATE projects SET developer_uuid=? WHERE id=?");
    massUpdate.rowPluralName("person uuid of components");
    massUpdate.execute((row, update) -> handleDeveloperUuuidUpdate(componentUuidById, row, update));
  }

  private static boolean handleDeveloperUuuidUpdate(Map<Long, String> componentUuidById, Select.Row row, SqlStatement update) throws SQLException {
    long id = row.getLong(1);
    long personId = row.getLong(2);

    String developerUuid = componentUuidById.get(personId);
    if (developerUuid == null) {
      LOG.trace("No UUID found for personId={}", personId);
      return false;
    }

    update.setString(1, developerUuid);
    update.setLong(2, id);

    return true;
  }

}
