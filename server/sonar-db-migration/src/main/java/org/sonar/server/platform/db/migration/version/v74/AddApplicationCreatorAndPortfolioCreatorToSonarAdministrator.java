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

import java.sql.SQLException;
import org.sonar.api.config.Configuration;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.version.v63.DefaultOrganizationUuidProvider;

@SupportsBlueGreen
public class AddApplicationCreatorAndPortfolioCreatorToSonarAdministrator extends DataChange {

  private final Configuration configuration;
  private final DefaultOrganizationUuidProvider defaultOrganizationUuidProvider;

  public AddApplicationCreatorAndPortfolioCreatorToSonarAdministrator(Database db, Configuration configuration,
    DefaultOrganizationUuidProvider defaultOrganizationUuidProvider) {
    super(db);
    this.configuration = configuration;
    this.defaultOrganizationUuidProvider = defaultOrganizationUuidProvider;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    if (configuration.getBoolean("sonar.sonarcloud.enabled").orElse(false)) {
      // Nothing to do on SonarCloud
      return;
    }

    Integer sonarAdmGroupId = context.prepareSelect("SELECT id FROM groups WHERE name=?")
      .setString(1, "sonar-administrators")
      .get(r -> r.getInt(1));

    if (sonarAdmGroupId == null) {
      // We cannot find the default sonar-administrators groups
      return;
    }

    insertPermissionIfMissing(context, sonarAdmGroupId, "applicationcreator");
    insertPermissionIfMissing(context, sonarAdmGroupId, "portfoliocreator");
  }

  private void insertPermissionIfMissing(Context context, Integer sonarAdmGroupId, String role) throws SQLException {
    if (isPermissionMissing(context, sonarAdmGroupId, role)) {
      context.prepareUpsert("INSERT INTO group_roles(organization_uuid, group_id, role) VALUES(?, ?, ?)")
        .setString(1, defaultOrganizationUuidProvider.get(context))
        .setInt(2, sonarAdmGroupId)
        .setString(3, role)
        .execute()
        .commit();
    }
  }

  private static boolean isPermissionMissing(Context context, Integer sonarAdmGroupId, String role) throws SQLException {
    Integer count = context.prepareSelect("SELECT COUNT(id) FROM group_roles WHERE group_id=? AND role=?")
      .setInt(1, sonarAdmGroupId)
      .setString(2, role)
      .get(r -> r.getInt(1));

    return count == null || count == 0;
  }
}
