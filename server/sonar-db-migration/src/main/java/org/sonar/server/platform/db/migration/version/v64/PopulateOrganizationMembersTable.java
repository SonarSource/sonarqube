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
import org.sonar.server.platform.db.migration.version.v63.DefaultOrganizationUuidProvider;

public class PopulateOrganizationMembersTable extends DataChange {

  private static final String INSERT_ORGANIZATION_MEMBERS_SQL = "INSERT INTO organization_members (user_id, organization_uuid) VALUES (?, ?)";

  private final DefaultOrganizationUuidProvider defaultOrganizationUuid;

  public PopulateOrganizationMembersTable(Database db, DefaultOrganizationUuidProvider defaultOrganizationUuid) {
    super(db);
    this.defaultOrganizationUuid = defaultOrganizationUuid;
  }

  @Override
  public void execute(Context context) throws SQLException {
    associateUsersToDefaultOrganization(context);
    associateUsersToOrganizationBasedOnPermission(context);
  }

  private void associateUsersToDefaultOrganization(Context context) throws SQLException {
    String organizationUuid = defaultOrganizationUuid.getAndCheck(context);
    MassUpdate massUpdate = context.prepareMassUpdate().rowPluralName("default organization members");
    massUpdate.select(
      "SELECT u.id FROM users u " +
        "WHERE u.active=? AND " +
        "NOT EXISTS (SELECT 1 FROM organization_members om WHERE om.user_id=u.id AND om.organization_uuid=?) ")
      .setBoolean(1, true)
      .setString(2, organizationUuid);
    massUpdate.update(INSERT_ORGANIZATION_MEMBERS_SQL);
    massUpdate.execute((row, update) -> {
      update.setInt(1, row.getInt(1));
      update.setString(2, organizationUuid);
      return true;
    });
  }

  private static void associateUsersToOrganizationBasedOnPermission(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate().rowPluralName("non default organization members");
    massUpdate.select(
      "SELECT distinct ur.organization_uuid, ur.user_id FROM user_roles ur " +
        "INNER JOIN users u ON u.id=ur.user_id AND u.active=? " +
        "WHERE NOT EXISTS (SELECT 1 FROM organization_members om WHERE om.user_id=ur.user_id AND om.organization_uuid=ur.organization_uuid) " +
        "UNION " +
        "SELECT distinct g.organization_uuid, gu.user_id FROM groups_users gu " +
        "INNER JOIN users u ON u.id=gu.user_id AND u.active=? " +
        "INNER JOIN groups g ON g.id=gu.group_id " +
        "WHERE NOT EXISTS (SELECT 1 FROM organization_members om WHERE om.user_id=gu.user_id AND om.organization_uuid=g.organization_uuid)")
      .setBoolean(1, true)
      .setBoolean(2, true);
    massUpdate.update(INSERT_ORGANIZATION_MEMBERS_SQL);
    massUpdate.execute((row, update) -> {
      update.setInt(1, row.getInt(2));
      update.setString(2, row.getString(1));
      return true;
    });
  }
}
