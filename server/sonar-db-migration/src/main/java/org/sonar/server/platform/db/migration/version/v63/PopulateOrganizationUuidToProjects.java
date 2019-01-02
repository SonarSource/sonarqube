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
package org.sonar.server.platform.db.migration.version.v63;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulateOrganizationUuidToProjects extends DataChange {
  private final DefaultOrganizationUuidProvider defaultOrganizationUuid;

  public PopulateOrganizationUuidToProjects(Database db, DefaultOrganizationUuidProvider defaultOrganizationUuid) {
    super(db);
    this.defaultOrganizationUuid = defaultOrganizationUuid;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    String organizationUuid = defaultOrganizationUuid.get(context);

    // update rows by component tree
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select uuid from projects where organization_uuid is null and project_uuid = uuid");
    massUpdate.update("update projects set organization_uuid=? where project_uuid=?");
    massUpdate.rowPluralName("project trees");
    massUpdate.execute((row, update) -> {
      String uuid = row.getString(1);
      update.setString(1, organizationUuid);
      update.setString(2, uuid);
      return true;
    });

    // update rows lefts without organization_uuid, just in case we have orphan rows (aka. bad data) in DB
    massUpdate = context.prepareMassUpdate();
    massUpdate.select("select uuid from projects where organization_uuid is null");
    massUpdate.update("update projects set organization_uuid=? where uuid=?");
    massUpdate.rowPluralName("components");
    massUpdate.execute((row, update) -> {
      String uuid = row.getString(1);
      update.setString(1, organizationUuid);
      update.setString(2, uuid);
      return true;
    });
  }
}
