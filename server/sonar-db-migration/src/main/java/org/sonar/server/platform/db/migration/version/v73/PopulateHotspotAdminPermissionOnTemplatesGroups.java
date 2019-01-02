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
package org.sonar.server.platform.db.migration.version.v73;

import java.sql.SQLException;
import java.util.Date;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;

@SupportsBlueGreen
public class PopulateHotspotAdminPermissionOnTemplatesGroups extends DataChange {
  private static final String ISSUE_ADMIN_ROLE = "issueadmin";
  private static final String HOTSPOT_ADMIN_ROLE = "securityhotspotadmin";
  private final System2 system2;

  public PopulateHotspotAdminPermissionOnTemplatesGroups(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    Date now = new Date(system2.now());
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT group_id, template_id" +
      "  FROM perm_templates_groups gr1" +
      "  WHERE permission_reference = ?" +
      "  AND NOT EXISTS (" +
      "    SELECT id" +
      "    FROM perm_templates_groups gr2" +
      "    WHERE permission_reference = ?" +
      "    AND gr1.group_id = gr2.group_id" +
      "    AND gr1.template_id = gr2.template_id)")
      .setString(1, ISSUE_ADMIN_ROLE)
      .setString(2, HOTSPOT_ADMIN_ROLE);
    massUpdate.update("INSERT INTO perm_templates_groups (group_id, template_id, permission_reference, created_at, updated_at) values (?,?,?,?,?)");
    massUpdate.rowPluralName("permission templates groups roles");
    massUpdate.execute((row, update) -> handle(row, update, now));
  }

  private static boolean handle(Select.Row row, SqlStatement update, Date now) throws SQLException {
    int groupId = row.getInt(1);
    int templateId = row.getInt(2);

    update.setInt(1, groupId);
    update.setInt(2, templateId);
    update.setString(3, HOTSPOT_ADMIN_ROLE);
    update.setDate(4, now);
    update.setDate(5, now);
    return true;
  }
}
