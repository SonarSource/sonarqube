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
package org.sonar.server.platform.db.migration.version.v64;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

/**
 * Set PROJECTS#COPY_COMPONENT_UUID on local views.
 *
 * Here's how local sub views are detected :
 * <ul>
 *   <li>Load all root views</li>
 *   <li>Load all sub views not having COPY_COMPONENT_UUID set</li>
 *   <li>Extract last part of the sub view key and if it matches a root view key then set the COPY_COMPONENT_UUID to the matching root view uuid</li>
 * </ul>
 */
public class SetCopyComponentUuidOnLocalViews extends DataChange {

  private static final String QUALIFIER_SUB_VIEW = "SVW";
  private static final String QUALIFIER_VIEW = "VW";
  private static final String SCOPE_PROJECT = "PRJ";

  public SetCopyComponentUuidOnLocalViews(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    Map<String, String> rootUuidsByKeys = selectRootUuidsByKeys(context);

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT p.kee, p.uuid FROM projects p " +
      "WHERE p.qualifier = ? " +
      "AND p.scope = ? " +
      "AND p.copy_component_uuid IS NULL " +
      "AND p.enabled = ? ")
      .setString(1, QUALIFIER_SUB_VIEW)
      .setString(2, SCOPE_PROJECT)
      .setBoolean(3, true);

    massUpdate.update("UPDATE projects set " +
      "copy_component_uuid=? " +
      "WHERE uuid=?");
    massUpdate.execute((row, update) -> {
      String subViewKey = row.getString(1);
      int lastViewKeyIndex = subViewKey.lastIndexOf(':');
      if (lastViewKeyIndex < 0) {
        return false;
      }
      String possibleRootViewUuid = subViewKey.substring(lastViewKeyIndex + 1);
      String rootUuid = rootUuidsByKeys.get(possibleRootViewUuid);
      if (rootUuid == null) {
        return false;
      }
      update.setString(1, rootUuid);
      update.setString(2, row.getString(2));
      return true;
    });
  }

  private static Map<String, String> selectRootUuidsByKeys(Context context) throws SQLException {
    Map<String, String> rootUuids = new HashMap<>();
    context.prepareSelect("SELECT p.kee, p.uuid FROM projects p " +
      "WHERE p.qualifier = ? " +
      "AND p.scope = ? " +
      "AND p.enabled = ? ")
      .setString(1, QUALIFIER_VIEW)
      .setString(2, SCOPE_PROJECT)
      .setBoolean(3, true)
      .scroll(row -> {
        String key = row.getString(1);
        String uuid = row.getString(2);
        rootUuids.put(key, uuid);
      });
    return rootUuids;
  }
}
