/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202604;

import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class AddArchitectureAdminPermissionToProjectAdmins extends DataChange {

  private static final String ADMIN_PERMISSION = "admin";
  private static final String ARCHITECTURE_ADMIN_PERMISSION = "architectureadmin";

  public AddArchitectureAdminPermissionToProjectAdmins(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    grantToUsers(context);
    grantToGroups(context);
    grantToGroupsInTemplates(context);
    grantToUsersInTemplates(context);
  }

  private static void grantToUsers(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("""
      SELECT ur.user_uuid, ur.entity_uuid FROM user_roles ur
      INNER JOIN projects p ON ur.entity_uuid = p.uuid AND p.qualifier = 'TRK'
      WHERE ur.role = ?
        AND NOT EXISTS (
          SELECT 1 FROM user_roles ur2
          WHERE ur2.user_uuid = ur.user_uuid
            AND ur2.entity_uuid = ur.entity_uuid
            AND ur2.role = ?
        )
      """)
      .setString(1, ADMIN_PERMISSION)
      .setString(2, ARCHITECTURE_ADMIN_PERMISSION);
    massUpdate.update("INSERT INTO user_roles (uuid, user_uuid, entity_uuid, role) VALUES (?, ?, ?, ?)");
    massUpdate.execute((row, update) -> {
      update
        .setString(1, UUID.randomUUID().toString())
        .setString(2, row.getString(1))
        .setString(3, row.getString(2))
        .setString(4, ARCHITECTURE_ADMIN_PERMISSION);
      return true;
    });
  }

  private static void grantToGroups(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("""
      SELECT gr.group_uuid, gr.entity_uuid FROM group_roles gr
      INNER JOIN projects p ON gr.entity_uuid = p.uuid AND p.qualifier = 'TRK'
      WHERE gr.role = ?
        AND NOT EXISTS (
          SELECT 1 FROM group_roles gr2
          WHERE gr2.group_uuid = gr.group_uuid
            AND gr2.entity_uuid = gr.entity_uuid
            AND gr2.role = ?
        )
      """)
      .setString(1, ADMIN_PERMISSION)
      .setString(2, ARCHITECTURE_ADMIN_PERMISSION);
    massUpdate.update("INSERT INTO group_roles (uuid, group_uuid, entity_uuid, role) VALUES (?, ?, ?, ?)");
    massUpdate.execute((row, update) -> {
      update
        .setString(1, UUID.randomUUID().toString())
        .setString(2, row.getString(1))
        .setString(3, row.getString(2))
        .setString(4, ARCHITECTURE_ADMIN_PERMISSION);
      return true;
    });
  }

  private static void grantToGroupsInTemplates(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("""
      SELECT template_uuid, group_uuid FROM perm_templates_groups
      WHERE permission_reference = ?
        AND NOT EXISTS (
          SELECT 1 FROM perm_templates_groups ptg2
          WHERE ptg2.template_uuid = perm_templates_groups.template_uuid
            AND ptg2.group_uuid = perm_templates_groups.group_uuid
            AND ptg2.permission_reference = ?
        )
      """)
      .setString(1, ADMIN_PERMISSION)
      .setString(2, ARCHITECTURE_ADMIN_PERMISSION);
    massUpdate.update("INSERT INTO perm_templates_groups (uuid, template_uuid, group_uuid, permission_reference, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)");
    Date now = new Date();
    massUpdate.execute((row, update) -> {
      update
        .setString(1, UUID.randomUUID().toString())
        .setString(2, row.getString(1))
        .setString(3, row.getString(2))
        .setString(4, ARCHITECTURE_ADMIN_PERMISSION)
        .setDate(5, now)
        .setDate(6, now);
      return true;
    });
  }

  private static void grantToUsersInTemplates(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("""
      SELECT template_uuid, user_uuid FROM perm_templates_users
      WHERE permission_reference = ?
        AND NOT EXISTS (
          SELECT 1 FROM perm_templates_users ptu2
          WHERE ptu2.template_uuid = perm_templates_users.template_uuid
            AND ptu2.user_uuid = perm_templates_users.user_uuid
            AND ptu2.permission_reference = ?
        )
      """)
      .setString(1, ADMIN_PERMISSION)
      .setString(2, ARCHITECTURE_ADMIN_PERMISSION);
    massUpdate.update("INSERT INTO perm_templates_users (uuid, template_uuid, user_uuid, permission_reference, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)");
    Date now = new Date();
    massUpdate.execute((row, update) -> {
      update
        .setString(1, UUID.randomUUID().toString())
        .setString(2, row.getString(1))
        .setString(3, row.getString(2))
        .setString(4, ARCHITECTURE_ADMIN_PERMISSION)
        .setDate(5, now)
        .setDate(6, now);
      return true;
    });
  }
}
