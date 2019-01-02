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
import java.util.Date;
import javax.annotation.CheckForNull;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.version.v63.DefaultOrganizationUuidProvider;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * If sonar-users doesn't exist, create it and copy permissions from existing default group
 * If it already exists, only update description if it's the wrong one
 */
public class RestoreSonarUsersGroups extends DataChange {

  private static final Logger LOG = Loggers.get(RestoreSonarUsersGroups.class);

  private static final String SONAR_USERS_NAME = "sonar-users";
  private static final String SONAR_USERS_PENDING_DESCRIPTION = "<PENDING>";
  private static final String SONAR_USERS_FINAL_DESCRIPTION = "Any new users created will automatically join this group";
  private static final String DEFAULT_GROUP_SETTING = "sonar.defaultGroup";

  private final System2 system2;
  private final DefaultOrganizationUuidProvider defaultOrganizationUuid;

  public RestoreSonarUsersGroups(Database db, System2 system2, DefaultOrganizationUuidProvider defaultOrganizationUuid) {
    super(db);
    this.system2 = system2;
    this.defaultOrganizationUuid = defaultOrganizationUuid;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    Date now = new Date(system2.now());
    Group sonarUsersGroup = selectSonarUsersGroup(context);
    Group defaultGroup = searchDefaultGroup(context);
    if (sonarUsersGroup == null) {
      createSonarUsersGroupAndCopyPermissionsFromDefaultGroup(context, defaultGroup, now);
      displayWarnLog(defaultGroup);
    } else {
      if (SONAR_USERS_PENDING_DESCRIPTION.equals(sonarUsersGroup.getDescription())) {
        copyAllPermissionsFromDefaultGroupToSonarUsers(context, sonarUsersGroup, defaultGroup, now);
      } else if (!SONAR_USERS_FINAL_DESCRIPTION.equals(sonarUsersGroup.getDescription())) {
        updateSonarUsersGroupDescription(context, SONAR_USERS_FINAL_DESCRIPTION, sonarUsersGroup.getId(), now);
      }
      if (sonarUsersGroup.getId() != defaultGroup.getId()) {
        displayWarnLog(defaultGroup);
      }
    }
  }

  private void createSonarUsersGroupAndCopyPermissionsFromDefaultGroup(Context context, Group defaultGroup, Date now) throws SQLException {
    insertSonarUsersGroupWithPendingDescription(context, defaultOrganizationUuid.get(context), now);
    Group sonarUsersGroupId = requireNonNull(selectSonarUsersGroup(context), format("Creation of '%s' group has failed", SONAR_USERS_NAME));
    copyAllPermissionsFromDefaultGroupToSonarUsers(context, sonarUsersGroupId, defaultGroup, now);
  }

  private static void copyAllPermissionsFromDefaultGroupToSonarUsers(Context context, Group sonarUsersGroup, Group defaultGroupId, Date now) throws SQLException {
    copyGlobalAndProjectPermissionsFromDefaultGroupToSonarUsers(context, defaultGroupId, sonarUsersGroup);
    copyPermissionTemplatesFromDefaultGroupToSonarUsers(context, defaultGroupId, sonarUsersGroup, now);
    updateSonarUsersGroupDescription(context, SONAR_USERS_FINAL_DESCRIPTION, sonarUsersGroup.getId(), now);
  }

  private static void copyGlobalAndProjectPermissionsFromDefaultGroupToSonarUsers(Context context, Group defaultGroupId, Group sonarUsersGroupId) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate().rowPluralName("global and project permissions");
    massUpdate.select("SELECT gr.resource_id, gr.role, gr.organization_uuid FROM group_roles gr " +
      "WHERE gr.group_id=? AND NOT EXISTS " +
      "(SELECT 1 FROM group_roles gr2 WHERE gr2.resource_id=gr.resource_id AND gr2.role=gr.role AND gr2.organization_uuid=gr.organization_uuid AND gr2.group_id=?)")
      .setLong(1, defaultGroupId.getId())
      .setLong(2, sonarUsersGroupId.getId());
    massUpdate.update("INSERT INTO group_roles (group_id, resource_id, role, organization_uuid) values (?, ?, ?, ?)");
    massUpdate.execute((row, update) -> {
      update.setLong(1, sonarUsersGroupId.getId());
      update.setLong(2, row.getNullableLong(1));
      update.setString(3, row.getString(2));
      update.setString(4, row.getString(3));
      return true;
    });
  }

  private static void copyPermissionTemplatesFromDefaultGroupToSonarUsers(Context context, Group defaultGroupId, Group sonarUsersGroupId, Date now) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate().rowPluralName("permission templates");
    massUpdate.select("SELECT ptg.template_id, ptg.permission_reference FROM perm_templates_groups ptg " +
      "WHERE ptg.group_id=? AND NOT EXISTS " +
      "(SELECT 1 FROM perm_templates_groups ptg2 WHERE ptg2.template_id=ptg.template_id AND ptg2.permission_reference=ptg.permission_reference AND ptg2.group_id=?)")
      .setLong(1, defaultGroupId.getId())
      .setLong(2, sonarUsersGroupId.getId());
    massUpdate.update("INSERT INTO perm_templates_groups (group_id, template_id, permission_reference, created_at, updated_at) values (?, ?, ?, ?, ?)");
    massUpdate.execute((row, update) -> {
      update
        .setLong(1, sonarUsersGroupId.getId())
        .setLong(2, row.getLong(1))
        .setString(3, row.getString(2))
        .setDate(4, now)
        .setDate(5, now);
      return true;
    });
  }

  private static void insertSonarUsersGroupWithPendingDescription(Context context, String organizationUuid, Date now) throws SQLException {
    context.prepareUpsert("INSERT into groups (name, description, organization_uuid, created_at, updated_at) values (?, ?, ?, ?, ?)")
      .setString(1, SONAR_USERS_NAME)
      .setString(2, SONAR_USERS_PENDING_DESCRIPTION)
      .setString(3, organizationUuid)
      .setDate(4, now)
      .setDate(5, now)
      .execute()
      .commit();
  }

  private static void updateSonarUsersGroupDescription(Context context, String description, long sonarUsersGroupId, Date now) throws SQLException {
    context.prepareUpsert("UPDATE groups SET description=?, updated_at=? WHERE id=?")
      .setString(1, description)
      .setDate(2, now)
      .setLong(3, sonarUsersGroupId)
      .execute()
      .commit();
  }

  private static Group searchDefaultGroup(Context context) throws SQLException {
    String defaultGroupName = selectDefaultGroupNameFromProperties(context);
    if (defaultGroupName == null) {
      Group sonarUsersGroup = selectSonarUsersGroup(context);
      checkState(sonarUsersGroup != null, "Default group setting %s is defined to a 'sonar-users' group but it doesn't exist.", DEFAULT_GROUP_SETTING);
      return sonarUsersGroup;
    }
    Group defaultGroup = selectGroupByName(context, defaultGroupName);
    checkState(defaultGroup != null, "Default group setting %s is defined to an unknown group.", DEFAULT_GROUP_SETTING);
    return defaultGroup;
  }

  @CheckForNull
  private static Group selectSonarUsersGroup(Context context) throws SQLException {
    return selectGroupByName(context, SONAR_USERS_NAME);
  }

  @CheckForNull
  private static String selectDefaultGroupNameFromProperties(Context context) throws SQLException {
    return context.prepareSelect("SELECT is_empty,text_value FROM properties WHERE prop_key=? AND is_empty=?")
      .setString(1, DEFAULT_GROUP_SETTING)
      .setBoolean(2, false)
      .get(row -> {
        boolean isEmpty = row.getBoolean(1);
        return isEmpty ? null : row.getString(2);
      });
  }

  @CheckForNull
  private static Group selectGroupByName(Context context, String name) throws SQLException {
    return context.prepareSelect("SELECT id, name, description FROM groups WHERE name=?")
      .setString(1, name)
      .get(Group::new);
  }

  private static void displayWarnLog(Group defaultGroup) {
    LOG.warn("The default group has been updated from '{}' to '{}'. Please verify your permission schema that everything is in order",
      defaultGroup.getName(), SONAR_USERS_NAME);
  }

  private static class Group {
    private final long id;
    private final String name;
    private final String description;

    Group(Select.Row row) throws SQLException {
      this.id = row.getLong(1);
      this.name = row.getString(2);
      this.description = row.getString(3);
    }

    long getId() {
      return id;
    }

    String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }
  }
}
