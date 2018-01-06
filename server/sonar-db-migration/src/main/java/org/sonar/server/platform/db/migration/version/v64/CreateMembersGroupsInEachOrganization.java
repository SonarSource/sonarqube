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
import java.util.Date;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class CreateMembersGroupsInEachOrganization extends DataChange {

  private static final String GROUP_MEMBERS = "Members";

  private final System2 system2;

  public CreateMembersGroupsInEachOrganization(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    Date now = new Date(system2.now());
    if (isOrganizationEnabled(context)) {
      createMembersGroup(context, now);
      createPermissionTemplateGroups(context, now);
    }
  }

  private static void createMembersGroup(Context context, Date now) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate().rowPluralName("groups");
    massUpdate.select("SELECT o.uuid FROM organizations o " +
      "WHERE NOT EXISTS (SELECT 1 FROM groups g WHERE g.organization_uuid=o.uuid AND g.name=?)").setString(1, GROUP_MEMBERS);
    massUpdate.update("INSERT INTO groups (organization_uuid, name, description, created_at, updated_at) values (?, ?, ?, ?, ?)");
    massUpdate.execute((row, update) -> {
      update
        .setString(1, row.getString(1))
        .setString(2, GROUP_MEMBERS)
        .setString(3, "All members of the organization")
        .setDate(4, now)
        .setDate(5, now);
      return true;
    });
  }

  private static void createPermissionTemplateGroups(Context context, Date now) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate().rowPluralName("permission templates on groups");
    massUpdate.select("SELECT g.id, pt.id FROM organizations o " +
      "INNER JOIN permission_templates pt ON pt.kee=o.default_perm_template_project " +
      "INNER JOiN groups g ON g.organization_uuid=o.uuid AND g.name=? " +
      "WHERE NOT EXISTS (SELECT 1 FROM perm_templates_groups ptg WHERE ptg.group_id=g.id AND ptg.template_id=pt.id)")
      .setString(1, GROUP_MEMBERS);
    massUpdate.update("INSERT INTO perm_templates_groups (group_id, template_id, permission_reference, created_at, updated_at) values (?, ?, ?, ?, ?)");
    massUpdate.update("INSERT INTO perm_templates_groups (group_id, template_id, permission_reference, created_at, updated_at) values (?, ?, ?, ?, ?)");
    massUpdate.execute((row, update, updateIndex) -> {
      update
        .setLong(1, row.getLong(1))
        .setLong(2, row.getLong(2))
        .setString(3, updateIndex == 0 ? "user" : "codeviewer")
        .setDate(4, now)
        .setDate(5, now);
      return true;
    });
  }

  private static boolean isOrganizationEnabled(Context context) throws SQLException {
    Boolean result = context.prepareSelect("SELECT text_value FROM internal_properties WHERE kee=?")
      .setString(1, "organization.enabled")
      .get(row -> "true".equals(row.getString(1)));
    return result != null ? result : false;
  }

}
