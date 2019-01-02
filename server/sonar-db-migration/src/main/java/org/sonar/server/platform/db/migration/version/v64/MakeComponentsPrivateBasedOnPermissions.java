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
package org.sonar.server.platform.db.migration.version.v64;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;

/**
 * This DB migration assumes the whole PROJECTS table contains only rows with private=false
 * (set by {@link PopulateColumnProjectsPrivate}) and performs the following:
 * <ul>
 *   <li>set private=true for any tree of component which root has neither user nor codeviewer permission for group AnyOne
 *       but defines at least one permission directly to a group or user</li>
 *   <li>removes any permission to group AnyOne for root components which are made private</li>
 *   <li>ensures any group or user with direct permission to a private root component also has both permissions
 *       user and codeviewer</li>
 *   <li>deletes any permission user or codeviewer for root components which stays public</li>
 * </ul>
 * This DB migration of course works if PROJECTS table contains rows with private=true, but it will assume they have
 * been set by a previous run of itself (ie. the DB migration is reentrant).
 */
public class MakeComponentsPrivateBasedOnPermissions extends DataChange {

  private static final String SCOPE_PROJECT = "PRJ";
  private static final String QUALIFIER_PROJECT = "TRK";
  private static final String QUALIFIER_VIEW = "VW";
  private static final String PERMISSION_USER = "user";
  private static final String PERMISSION_CODEVIEWER = "codeviewer";

  public MakeComponentsPrivateBasedOnPermissions(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    makePrivateComponent(context);
    cleanPermissionsOfPublicComponents(context);
    insertUserPermissionOfPrivateRootComponent(context, PERMISSION_USER);
    insertUserPermissionOfPrivateRootComponent(context, PERMISSION_CODEVIEWER);
    insertGroupPermissionOfPrivateRootComponent(context, PERMISSION_USER);
    insertGroupPermissionOfPrivateRootComponent(context, PERMISSION_CODEVIEWER);
  }

  private static void makePrivateComponent(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select uuid, id from projects p where " +
      " p.scope = ?" +
      " and p.qualifier in (?, ?)" +
      " and p.private = ?" +
      " and (" +
      "   not exists (" +
      "     select" +
      "       1" +
      "     from group_roles gr" +
      "     where " +
      "       gr.resource_id = p.id" +
      "       and gr.group_id is null" +
      "       and gr.role = ?" +
      "   ) " +
      "   or not exists (" +
      "     select" +
      "       1" +
      "     from group_roles gr" +
      "     where " +
      "       gr.resource_id = p.id" +
      "       and gr.group_id is null" +
      "       and gr.role = ?" +
      "   )" +
      " )" +
      // trees with only permissions to group must not be made private
      " and (" +
      "   exists (" +
      "     select" +
      "       1" +
      "     from " +
      "       group_roles gr2" +
      "     where" +
      "       gr2.resource_id = p.id" +
      "       and gr2.group_id is not null" +
      "    )" +
      "    or exists (" +
      "      select" +
      "        1" +
      "      from " +
      "        user_roles ur" +
      "      where" +
      "        ur.resource_id = p.id" +
      "    )" +
      ")")
      .setString(1, SCOPE_PROJECT)
      .setString(2, QUALIFIER_PROJECT)
      .setString(3, QUALIFIER_VIEW)
      .setBoolean(4, false)
      .setString(5, PERMISSION_USER)
      .setString(6, PERMISSION_CODEVIEWER);
    massUpdate.rowPluralName("component trees to be made private");
    // make project private
    massUpdate.update("update projects set private = ? where project_uuid = ?");
    // delete any permission given to group "Anyone"
    massUpdate.update("delete from group_roles where resource_id = ? and group_id is null");
    massUpdate.execute(MakeComponentsPrivateBasedOnPermissions::handleMakePrivateComponent);
  }

  private static boolean handleMakePrivateComponent(Select.Row row, SqlStatement update, int updateIndex) throws SQLException {
    String rootUuid = row.getString(1);
    long id = row.getLong(2);
    switch (updateIndex) {
      case 0:
        update.setBoolean(1, true);
        update.setString(2, rootUuid);
        return true;
      case 1:
        update.setLong(1, id);
        return true;
      default:
        throw new IllegalArgumentException("Unsupported update index " + updateIndex);
    }
  }

  private static void cleanPermissionsOfPublicComponents(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select id from projects p where " +
      " p.scope = ?" +
      " and p.qualifier in (?, ?)" +
      " and p.private = ?" +
      " and exists (" +
      "   select" +
      "     1" +
      "   from group_roles gr" +
      "   where " +
      "     gr.resource_id = p.id" +
      "     and gr.role in (?, ?)" +
      "   union" +
      "   select" +
      "     1" +
      "   from user_roles gr" +
      "   where " +
      "     gr.resource_id = p.id" +
      "     and gr.role in (?, ?)" +
      ")")
      .setString(1, SCOPE_PROJECT)
      .setString(2, QUALIFIER_PROJECT)
      .setString(3, QUALIFIER_VIEW)
      .setBoolean(4, false)
      .setString(5, PERMISSION_USER)
      .setString(6, PERMISSION_CODEVIEWER)
      .setString(7, PERMISSION_USER)
      .setString(8, PERMISSION_CODEVIEWER);
    massUpdate.rowPluralName("public component trees to clean permissions of");
    massUpdate.update("delete from group_roles where resource_id = ? and role in ('user', 'codeviewer')");
    massUpdate.update("delete from user_roles where resource_id = ? and role in ('user', 'codeviewer')");
    massUpdate.execute(MakeComponentsPrivateBasedOnPermissions::handleCleanPermissionsOfPublicComponents);
  }

  private static boolean handleCleanPermissionsOfPublicComponents(Select.Row row, SqlStatement update, int updateIndex) throws SQLException {
    long id = row.getLong(1);
    switch (updateIndex) {
      case 0:
      case 1:
        update.setLong(1, id);
        return true;
      default:
        throw new IllegalArgumentException("Unsupported update index " + updateIndex);
    }
  }

  private static void insertUserPermissionOfPrivateRootComponent(Context context, String permission) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select" +
      " distinct r1.user_id, p.organization_uuid, p.id" +
      " from" +
      "   user_roles r1" +
      " inner join projects p on" +
      "   p.id = r1.resource_id" +
      "   and p.scope = ?" +
      "   and p.qualifier in (?, ?)" +
      "   and p.private = ?" +
      " where" +
      "   not exists (" +
      "     select" +
      "       1" +
      "     from" +
      "       user_roles r2" +
      "     where " +
      "       r2.user_id = r1.user_id" +
      "       and r2.resource_id = r1.resource_id" +
      "       and r2.role = ?" +
      ")")
      .setString(1, SCOPE_PROJECT)
      .setString(2, QUALIFIER_PROJECT)
      .setString(3, QUALIFIER_VIEW)
      .setBoolean(4, true)
      .setString(5, permission);
    massUpdate.rowPluralName("users of private component tree without " + permission + " permission");
    massUpdate.update("insert into user_roles" +
      " (organization_uuid, user_id, resource_id, role)" +
      " values" +
      " (?, ?, ?, ?)");
    massUpdate.execute((row, update) -> insertUserPermission(row, update, permission));
  }

  private static boolean insertUserPermission(Select.Row row, SqlStatement update, String permission) throws SQLException {
    int userId = row.getInt(1);
    String organizationUuid = row.getString(2);
    int resourceId = row.getInt(3);

    update.setString(1, organizationUuid);
    update.setInt(2, userId);
    update.setInt(3, resourceId);
    update.setString(4, permission);
    return true;
  }

  private static void insertGroupPermissionOfPrivateRootComponent(Context context, String permission) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select" +
      " distinct g1.group_id, p.organization_uuid, p.id" +
      " from" +
      "   group_roles g1" +
      " inner join projects p on" +
      "   p.id = g1.resource_id" +
      "   and p.scope = ?" +
      "   and p.qualifier in (?, ?)" +
      "   and p.private = ?" +
      " where" +
      "   g1.group_id is not null" +
      "   and not exists (" +
      "     select" +
      "       1" +
      "     from" +
      "       group_roles g2" +
      "     where " +
      "       g2.group_id = g1.group_id" +
      "       and g2.resource_id = g1.resource_id" +
      "       and g2.role = ?" +
      ")")
      .setString(1, SCOPE_PROJECT)
      .setString(2, QUALIFIER_PROJECT)
      .setString(3, QUALIFIER_VIEW)
      .setBoolean(4, true)
      .setString(5, permission);
    massUpdate.rowPluralName("groups of private component tree without " + permission + " permission");
    massUpdate.update("insert into group_roles" +
      " (organization_uuid, group_id, resource_id, role)" +
      " values" +
      " (?, ?, ?, ?)");
    massUpdate.execute((row, update) -> insertGroupPermission(row, update, permission));
  }

  private static boolean insertGroupPermission(Select.Row row, SqlStatement update, String permission) throws SQLException {
    int groupId = row.getInt(1);
    String organizationUuid = row.getString(2);
    int resourceId = row.getInt(3);

    update.setString(1, organizationUuid);
    update.setInt(2, groupId);
    update.setInt(3, resourceId);
    update.setString(4, permission);
    return true;
  }
}
