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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.version.v63.DefaultOrganizationUuidProvider;

import static com.google.common.base.Preconditions.checkState;

public class SupportPrivateProjectInDefaultPermissionTemplate extends DataChange {
  private static final String PERMISSION_USER = "user";
  private static final String PERMISSION_CODEVIEWER = "codeviewer";

  private DefaultOrganizationUuidProvider defaultOrganizationUuidProvider;

  public SupportPrivateProjectInDefaultPermissionTemplate(Database db, DefaultOrganizationUuidProvider defaultOrganizationUuidProvider) {
    super(db);
    this.defaultOrganizationUuidProvider = defaultOrganizationUuidProvider;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    String defaultOrganizationUuid = this.defaultOrganizationUuidProvider.get(context);

    ResolvedOrganizationProperties organizationProperties = readOrganizationProperties(context, defaultOrganizationUuid);

    int defaultGroupId = organizationProperties.defaultGroupId;
    for (Integer groupId : Arrays.asList(organizationProperties.getProjectId(), organizationProperties.getViewId())) {
      if (groupId != null) {
        insertGroupPermissionIfNotPresent(context, defaultGroupId, groupId, PERMISSION_USER);
        insertGroupPermissionIfNotPresent(context, defaultGroupId, groupId, PERMISSION_CODEVIEWER);
      }
    }
  }

  private static void insertGroupPermissionIfNotPresent(Context context, int groupId, int templateId, String permission) throws SQLException {
    if (!groupHasPermissionInTemplate(context, templateId, groupId, permission)) {
      insertGroupPermission(context, templateId, groupId, permission);
    }
  }

  private static boolean groupHasPermissionInTemplate(Context context, int templateId, int groupId, String permission) throws SQLException {
    List<Integer> rows = context.prepareSelect("select 1 from perm_templates_groups where" +
      " template_id = ?" +
      " and group_id=?" +
      " and permission_reference=?")
      .setInt(1, templateId)
      .setInt(2, groupId)
      .setString(3, permission)
      .list(row -> row.getInt(1));
    return !rows.isEmpty();
  }

  private static void insertGroupPermission(Context context, int templateId, int groupId, String permission) throws SQLException {
    Date now = new Date();
    context.prepareUpsert("insert into perm_templates_groups (group_id, template_id, permission_reference, created_at, updated_at) values (?,?,?,?,?)")
      .setInt(1, groupId)
      .setInt(2, templateId)
      .setString(3, permission)
      .setDate(4, now)
      .setDate(5, now)
      .execute()
      .commit();
  }

  private static ResolvedOrganizationProperties readOrganizationProperties(Context context, String defaultOrganizationUuid) throws SQLException {
    Select select = context.prepareSelect("select default_group_id, default_perm_template_project, default_perm_template_view from organizations where uuid=?")
      .setString(1, defaultOrganizationUuid);
    List<OrganizationProperties> rows = select
      .list(row -> new OrganizationProperties(row.getNullableInt(1), row.getNullableString(2), row.getNullableString(3)));
    checkState(!rows.isEmpty(), "Default organization with uuid '%s' does not exist in table ORGANIZATIONS", defaultOrganizationUuid);
    OrganizationProperties rawProperties = rows.iterator().next();
    checkState(rawProperties.defaultGroupId != null, "No default group id is defined for default organization (uuid=%s)", defaultOrganizationUuid);
    checkState(rawProperties.projectUuid != null || rawProperties.viewUuid == null,
      "Inconsistent state for default organization (uuid=%s): no project default template is defined but view default template is", defaultOrganizationUuid);
    Integer projectTemplateId = getPermTemplateId(context, rawProperties.projectUuid);
    Integer viewTemplateId = getViewTemplateIdOrClearReference(context, rawProperties.viewUuid, defaultOrganizationUuid);
    return new ResolvedOrganizationProperties(
      rawProperties.defaultGroupId,
      projectTemplateId,
      viewTemplateId);
  }

  @CheckForNull
  private static Integer getPermTemplateId(Context context, @Nullable String permissionTemplateUuid) throws SQLException {
    if (permissionTemplateUuid == null) {
      return null;
    }
    List<Integer> ids = getTemplateIds(context, permissionTemplateUuid);
    checkState(!ids.isEmpty(), "Permission template with uuid %s not found", permissionTemplateUuid);
    checkState(ids.size() == 1, "Multiple permission templates found with uuid %s", permissionTemplateUuid);
    return ids.iterator().next();
  }

  @CheckForNull
  private static Integer getViewTemplateIdOrClearReference(Context context, @Nullable String permissionTemplateUuid,
    String defaultOrganizationUuid) throws SQLException {
    if (permissionTemplateUuid == null) {
      return null;
    }
    List<Integer> ids = getTemplateIds(context, permissionTemplateUuid);
    if (ids.isEmpty()) {
      clearViewTemplateReference(context, defaultOrganizationUuid);
      return null;
    }
    checkState(ids.size() == 1, "Multiple permission templates found with uuid %s", permissionTemplateUuid);
    return ids.iterator().next();
  }

  private static void clearViewTemplateReference(Context context, String defaultOrganizationUuid) throws SQLException {
    context.prepareUpsert("update organizations set default_perm_template_view = null where uuid=?")
      .setString(1, defaultOrganizationUuid)
      .execute()
      .commit();
    Loggers.get(SupportPrivateProjectInDefaultPermissionTemplate.class)
      .info("Permission template with uuid %s referenced as default permission template for view does not exist. Reference cleared.");
  }

  private static List<Integer> getTemplateIds(Context context, @Nullable String permissionTemplateUuid) throws SQLException {
    return context.prepareSelect("select id from permission_templates where kee=?")
      .setString(1, permissionTemplateUuid)
      .list(row -> row.getInt(1));
  }

  private static final class OrganizationProperties {
    private final Integer defaultGroupId;
    private final String projectUuid;
    private final String viewUuid;

    private OrganizationProperties(@Nullable Integer defaultGroupId, @Nullable String projectUuid, @Nullable String viewUuid) {
      this.defaultGroupId = defaultGroupId;
      this.projectUuid = projectUuid;
      this.viewUuid = viewUuid;
    }
  }

  private static final class ResolvedOrganizationProperties {
    private final int defaultGroupId;
    private final Integer projectId;
    private final Integer viewId;

    private ResolvedOrganizationProperties(int defaultGroupId, @Nullable Integer projectId, @Nullable Integer viewId) {
      this.defaultGroupId = defaultGroupId;
      this.projectId = projectId;
      this.viewId = viewId;
    }

    int getDefaultGroupId() {
      return defaultGroupId;
    }

    Integer getProjectId() {
      return projectId;
    }

    @CheckForNull

    Integer getViewId() {
      return viewId;
    }
  }
}
