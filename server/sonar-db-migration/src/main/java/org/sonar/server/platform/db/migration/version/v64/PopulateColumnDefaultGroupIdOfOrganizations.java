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
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulateColumnDefaultGroupIdOfOrganizations extends DataChange {

  private final System2 system2;

  public PopulateColumnDefaultGroupIdOfOrganizations(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    populateDefaultGroupId(context, isOrganizationEnabled(context) ? "Members" : "sonar-users");
  }

  private void populateDefaultGroupId(Context context, String groupName) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate().rowPluralName("organizations");
    massUpdate.select("SELECT o.uuid, g.id FROM organizations o " +
      "INNER JOIN groups g ON g.organization_uuid=o.uuid AND g.name=? " +
      "WHERE o.default_group_id IS NULL")
      .setString(1, groupName);
    massUpdate.update("UPDATE organizations SET default_group_id=?,updated_at=? WHERE uuid=?");
    massUpdate.execute((row, update) -> {
      update.setLong(1, row.getLong(2));
      update.setLong(2, system2.now());
      update.setString(3, row.getString(1));
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
