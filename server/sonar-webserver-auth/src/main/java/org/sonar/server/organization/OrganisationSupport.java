/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.organization;

import java.util.List;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.ServerSide;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.template.PermissionTemplateGroupDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.usergroups.DefaultGroupCreator;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static org.sonar.core.util.stream.MoreCollectors.toList;

@ServerSide
public class OrganisationSupport {
  private final DbClient dbClient;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final OrganizationFlags organizationFlags;
  private final DefaultGroupCreator defaultGroupCreator;
  private final DefaultGroupFinder defaultGroupFinder;
  private final RuleIndexer ruleIndexer;
  private final UuidFactory uuidFactory;

  public OrganisationSupport(DbClient dbClient, DefaultOrganizationProvider defaultOrganizationProvider,
    OrganizationFlags organizationFlags, DefaultGroupCreator defaultGroupCreator, DefaultGroupFinder defaultGroupFinder,
    RuleIndexer ruleIndexer, UuidFactory uuidFactory) {
    this.dbClient = dbClient;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.organizationFlags = organizationFlags;
    this.defaultGroupCreator = defaultGroupCreator;
    this.defaultGroupFinder = defaultGroupFinder;
    this.ruleIndexer = ruleIndexer;
    this.uuidFactory = uuidFactory;
  }

  public void enable(String login) {
    String defaultOrganizationUuid = defaultOrganizationProvider.get().getUuid();
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (!organizationFlags.isEnabled(dbSession)) {
        flagAdminUserAsRoot(dbSession, login);
        createDefaultMembersGroup(dbSession, defaultOrganizationUuid);
        List<String> disabledTemplateAndCustomRuleUuids = disableTemplateRulesAndCustomRules(dbSession);
        enableFeature(dbSession);
        ruleIndexer.commitAndIndex(dbSession, disabledTemplateAndCustomRuleUuids);
      }
    }
  }

  private void flagAdminUserAsRoot(DbSession dbSession, String login) {
    dbClient.userDao().setRoot(dbSession, login, true);
  }

  private void createDefaultMembersGroup(DbSession dbSession, String defaultOrganizationUuid) {
    GroupDto sonarUsersGroup = defaultGroupFinder.findDefaultGroup(dbSession);
    GroupDto members = defaultGroupCreator.create(dbSession);
    copySonarUsersGroupPermissionsToMembersGroup(dbSession, sonarUsersGroup, members);
    copySonarUsersGroupPermissionTemplatesToMembersGroup(dbSession, sonarUsersGroup, members);
    associateMembersOfDefaultOrganizationToGroup(dbSession, defaultOrganizationUuid, members);
  }

  private void associateMembersOfDefaultOrganizationToGroup(DbSession dbSession, String defaultOrganizationUuid, GroupDto membersGroup) {
    List<String> organizationMembers = dbClient.organizationMemberDao().selectUserUuidsByOrganizationUuid(dbSession, defaultOrganizationUuid);
    organizationMembers.forEach(member -> dbClient.userGroupDao().insert(dbSession, new UserGroupDto().setGroupUuid(membersGroup.getUuid()).setUserUuid(member)));
  }

  private void copySonarUsersGroupPermissionsToMembersGroup(DbSession dbSession, GroupDto sonarUsersGroup, GroupDto membersGroup) {
    dbClient.groupPermissionDao().selectAllPermissionsByGroupUuid(dbSession, sonarUsersGroup.getUuid(),
      context -> {
        GroupPermissionDto groupPermissionDto = (GroupPermissionDto) context.getResultObject();
        dbClient.groupPermissionDao().insert(dbSession,
          new GroupPermissionDto()
            .setUuid(uuidFactory.create())
            .setGroupUuid(membersGroup.getUuid())
            .setRole(groupPermissionDto.getRole())
            .setComponentUuid(groupPermissionDto.getComponentUuid()));
      });
  }

  private void copySonarUsersGroupPermissionTemplatesToMembersGroup(DbSession dbSession, GroupDto sonarUsersGroup, GroupDto membersGroup) {
    List<PermissionTemplateGroupDto> sonarUsersPermissionTemplates = dbClient.permissionTemplateDao().selectAllGroupPermissionTemplatesByGroupUuid(dbSession,
      sonarUsersGroup.getUuid());
    sonarUsersPermissionTemplates.forEach(permissionTemplateGroup -> dbClient.permissionTemplateDao().insertGroupPermission(dbSession,
      permissionTemplateGroup.getTemplateUuid(), membersGroup.getUuid(), permissionTemplateGroup.getPermission()));
  }

  private List<String> disableTemplateRulesAndCustomRules(DbSession dbSession) {
    List<RuleDefinitionDto> rules = dbClient.ruleDao().selectAllDefinitions(dbSession).stream()
      .filter(r -> r.isTemplate() || r.isCustomRule())
      .collect(toList());
    rules.forEach(r -> {
      r.setStatus(RuleStatus.REMOVED);
      dbClient.ruleDao().update(dbSession, r);
    });
    return rules.stream().map(RuleDefinitionDto::getUuid).collect(toList());
  }

  private void enableFeature(DbSession dbSession) {
    organizationFlags.enable(dbSession);
  }

}
