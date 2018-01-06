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
package org.sonar.server.platform.db.migration.version.v62;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;

import static com.google.common.base.Preconditions.checkState;

public class PopulateOrganizationUuidOfGroupRoles extends DataChange {

  private static final String INTERNAL_PROPERTY_DEFAULT_ORGANIZATION = "organization.default";

  public PopulateOrganizationUuidOfGroupRoles(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    String organizationUuid = selectDefaultOrganizationUuid(context);

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select id from group_roles where organization_uuid is null");
    massUpdate.update("update group_roles set organization_uuid=? where id=?");
    massUpdate.rowPluralName("group_roles");
    massUpdate.execute((row, update) -> {
      long id = row.getLong(1);
      update.setString(1, organizationUuid);
      update.setLong(2, id);
      return true;
    });
  }

  private static String selectDefaultOrganizationUuid(Context context) throws SQLException {
    Select select = context.prepareSelect("select text_value from internal_properties where kee=?");
    select.setString(1, INTERNAL_PROPERTY_DEFAULT_ORGANIZATION);
    String uuid = select.get(row -> row.getString(1));
    checkState(uuid != null, "Default organization uuid is missing");
    return uuid;
  }
}
