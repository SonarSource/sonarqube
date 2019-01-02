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
package org.sonar.server.platform.db.migration.version.v74;

import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;

import java.sql.SQLException;
import java.util.Date;

@SupportsBlueGreen
public class CreateApplicationsAndPortfoliosCreatorPermissions extends DataChange {

  private static final Logger LOG = Loggers.get(CreateApplicationsAndPortfoliosCreatorPermissions.class);
  private static final String DEFAULT_ORGANIZATION_KEY = "default-organization";

  private final System2 system2;

  public CreateApplicationsAndPortfoliosCreatorPermissions(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    Date now = new Date(system2.now());
    Long adminGroupId = context.prepareSelect("SELECT id FROM groups WHERE name=?")
      .setString(1, DefaultGroups.ADMINISTRATORS)
      .get(row -> row.getLong(1));
    String templateKey = context.prepareSelect("SELECT default_perm_template_view FROM organizations WHERE kee=?")
      .setString(1, DEFAULT_ORGANIZATION_KEY)
      .get(row -> row.getString(1));

    if (adminGroupId == null) {
      LOG.info("Unable to find {} group. Skipping adding applications and portfolios creator permissions.", DefaultGroups.ADMINISTRATORS);
      return;
    }

    if (templateKey == null) {
      LOG.info("There is no default template for views. Skipping adding applications and portfolios creator permissions.");
    }

    Long templateId = context.prepareSelect("SELECT id FROM permission_templates WHERE kee=?")
      .setString(1, templateKey)
      .get(row -> row.getLong(1));

    if (templateId == null) {
      LOG.info("Unable to find the default template [{}] for views. Skipping adding applications and portfolios creator permissions.", templateKey);
      return;
    }

    if (isPermissionAbsent(context, adminGroupId, "applicationcreator")) {
      insertPermission(context, adminGroupId, templateId, "applicationcreator", now);
    }

    if (isPermissionAbsent(context, adminGroupId, "portfoliocreator")) {
      insertPermission(context, adminGroupId, templateId, "portfoliocreator", now);
    }
  }

  private static boolean isPermissionAbsent(Context context, Long groupId, String permission) throws SQLException {
    Long count = context.prepareSelect("SELECT count(*) FROM perm_templates_groups WHERE group_id=? AND permission_reference=?")
      .setLong(1, groupId)
      .setString(2, permission)
      .get(row -> (row.getLong(1)));

    return (count == null) || count == 0;
  }

  private static void insertPermission(Context context, Long groupId, Long templateId, String permission, Date now) throws SQLException {
    context.prepareUpsert("INSERT INTO perm_templates_groups (group_id, template_id, permission_reference, created_at, updated_at) values (?,?,?,?,?)")
      .setLong(1, groupId)
      .setLong(2, templateId)
      .setString(3, permission)
      .setDate(4, now)
      .setDate(5, now)
      .execute()
      .commit();
  }
}
