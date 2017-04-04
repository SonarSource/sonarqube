/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.organization.ws;

import java.util.List;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.permission.template.PermissionTemplateGroupDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationFlags;
import org.sonar.server.user.UserSession;
import org.sonar.server.usergroups.DefaultGroupCreator;

import static java.util.Objects.requireNonNull;

public class EnableSupportAction implements OrganizationsWsAction {
  private static final String ACTION = "enable_support";

  private final UserSession userSession;
  private final DbClient dbClient;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final OrganizationFlags organizationFlags;
  private final DefaultGroupCreator defaultGroupCreator;

  public EnableSupportAction(UserSession userSession, DbClient dbClient, DefaultOrganizationProvider defaultOrganizationProvider,
    OrganizationFlags organizationFlags, DefaultGroupCreator defaultGroupCreator) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.organizationFlags = organizationFlags;
    this.defaultGroupCreator = defaultGroupCreator;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction(ACTION)
      .setPost(true)
      .setDescription("Enable support of organizations.<br />" +
        "'Administer System' permission is required. The logged-in user will be flagged as root and will be able to manage organizations and other root users.")
      .setInternal(true)
      .setPost(true)
      .setSince("6.3")
      .setChangelog(new Change("6.4", "Create default 'Members' group"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      verifySystemAdministrator();
      if (isSupportDisabled(dbSession)) {
        flagCurrentUserAsRoot(dbSession);
        createDefaultMembersGroup(dbSession);
        enableFeature(dbSession);
        dbSession.commit();
      }
    }
    response.noContent();
  }

  private void verifySystemAdministrator() {
    userSession.checkLoggedIn().checkPermission(OrganizationPermission.ADMINISTER, defaultOrganizationProvider.get().getUuid());
  }

  private boolean isSupportDisabled(DbSession dbSession) {
    return !organizationFlags.isEnabled(dbSession);
  }

  private void flagCurrentUserAsRoot(DbSession dbSession) {
    dbClient.userDao().setRoot(dbSession, requireNonNull(userSession.getLogin()), true);
  }

  private void createDefaultMembersGroup(DbSession dbSession) {
    String defaultOrganizationUuid = defaultOrganizationProvider.get().getUuid();
    int sonarUsersGroupId = dbClient.organizationDao().getDefaultGroupId(dbSession, defaultOrganizationUuid)
      .orElseThrow(() -> new IllegalStateException(String.format("Default group doesn't exist on default organization '%s'", defaultOrganizationProvider.get().getKey())));
    GroupDto members = defaultGroupCreator.create(dbSession, defaultOrganizationUuid);
    copySonarUsersGroupPermissionsToMembersGroup(dbSession, sonarUsersGroupId, members);
    copySonarUsersGroupPermissionTemplatesToMembersGroup(dbSession, sonarUsersGroupId, members);
    associateMembersOfDefaultOrganizationToGroup(dbSession, members);
  }

  private void associateMembersOfDefaultOrganizationToGroup(DbSession dbSession, GroupDto membersGroup) {
    List<Integer> organizationMembers = dbClient.organizationMemberDao().selectUserIdsByOrganizationUuid(dbSession, defaultOrganizationProvider.get().getUuid());
    organizationMembers.forEach(member -> dbClient.userGroupDao().insert(dbSession, new UserGroupDto().setGroupId(membersGroup.getId()).setUserId(member)));
  }

  private void copySonarUsersGroupPermissionsToMembersGroup(DbSession dbSession, int sonarUsersGroupId, GroupDto membersGroup) {
    String defaultOrganizationUuid = defaultOrganizationProvider.get().getUuid();
    dbClient.groupPermissionDao().selectAllPermissionsByGroupId(dbSession, defaultOrganizationUuid, sonarUsersGroupId,
      context -> {
        GroupPermissionDto groupPermissionDto = (GroupPermissionDto) context.getResultObject();
        dbClient.groupPermissionDao().insert(dbSession,
          new GroupPermissionDto().setOrganizationUuid(defaultOrganizationUuid).setGroupId(membersGroup.getId())
            .setRole(groupPermissionDto.getRole())
            .setResourceId(groupPermissionDto.getResourceId()));
      });
  }

  private void copySonarUsersGroupPermissionTemplatesToMembersGroup(DbSession dbSession, int sonarUsersGroupId, GroupDto membersGroup) {
    List<PermissionTemplateGroupDto> sonarUsersPermissionTemplates = dbClient.permissionTemplateDao().selectAllGroupPermissionTemplatesByGroupId(dbSession, sonarUsersGroupId);
    sonarUsersPermissionTemplates.forEach(permissionTemplateGroup -> dbClient.permissionTemplateDao().insertGroupPermission(dbSession,
      permissionTemplateGroup.getTemplateId(), membersGroup.getId(), permissionTemplateGroup.getPermission()));
  }

  private void enableFeature(DbSession dbSession) {
    organizationFlags.enable(dbSession);
  }

}
