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
package org.sonar.server.platform.db.migration.version.v202603;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

abstract class AddGitlabRolePermissionMapping extends DataChange {

  private static final Logger LOGGER = LoggerFactory.getLogger(AddGitlabRolePermissionMapping.class);

  private static final String GITLAB = "gitlab";

  protected AddGitlabRolePermissionMapping(Database db) {
    super(db);
  }

  abstract String getGitlabRoleName();

  abstract List<String> getSonarqubePermissions();

  @Override
  protected void execute(Context context) throws SQLException {
    String roleName = getGitlabRoleName();

    if (doesRoleAlreadyHaveMapping(context, roleName)) {
      LOGGER.info("Skipping adding of permission mapping for '{}' GitLab role since there is already existing permission mapping in the database.", roleName);
      return;
    }

    if (isGitlabProvisioningEnabled(context)) {
      LOGGER.info("Skipping adding of permission mapping for '{}' GitLab role since GitLab provisioning is already enabled.", roleName);
      return;
    }

    try (var upsert = context.prepareUpsert(
      "insert into devops_perms_mapping (uuid, devops_platform, devops_platform_role, sonarqube_permission) values (?, ?, ?, ?)")) {
      for (String permission : getSonarqubePermissions()) {
        upsert
          .setString(1, UUID.randomUUID().toString())
          .setString(2, GITLAB)
          .setString(3, roleName)
          .setString(4, permission)
          .execute();
      }
      upsert.commit();
    }
  }

  private static boolean doesRoleAlreadyHaveMapping(Context context, String roleName) throws SQLException {
    return context.prepareSelect(
        "select count(*) from devops_perms_mapping where devops_platform = ? and lower(devops_platform_role) = lower(?)")
      .setString(1, GITLAB)
      .setString(2, roleName)
      .get(r -> r.getLong(1)) > 0;
  }

  private static boolean isGitlabProvisioningEnabled(Context context) throws SQLException {
    return context.prepareSelect(
        "select count(*) from properties where prop_key = 'provisioning.gitlab.enabled' and text_value = 'true'")
      .get(r -> r.getLong(1)) > 0;
  }
}
