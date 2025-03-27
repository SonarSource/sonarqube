/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.permission.ws;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.Request;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserId;
import org.sonar.db.user.UserIdDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.permission.GroupUuidOrAnyone;
import org.sonar.server.permission.ws.template.WsTemplateRef;
import org.sonar.server.user.UserSession;
import org.sonar.server.usergroups.ws.GroupWsSupport;
import org.sonarqube.ws.client.permission.PermissionsWsParameters;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkProjectAdmin;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;

public class PermissionWsSupport {

  private static final String ERROR_REMOVING_OWN_BROWSE_PERMISSION = "Permission 'Browse' cannot be removed from a private project for a project administrator.";

  private final DbClient dbClient;
  private final GroupWsSupport groupWsSupport;
  private final Configuration configuration;

  public PermissionWsSupport(DbClient dbClient, Configuration configuration, GroupWsSupport groupWsSupport) {

    this.dbClient = dbClient;
    this.configuration = configuration;
    this.groupWsSupport = groupWsSupport;
  }

  public void checkPermissionManagementAccess(UserSession userSession, @Nullable EntityDto entity) {
    checkProjectAdmin(userSession, configuration, entity);
  }

  @CheckForNull
  public EntityDto findEntity(DbSession dbSession, Request request) {
    String uuid = request.param(PermissionsWsParameters.PARAM_PROJECT_ID);
    String key = request.param(PermissionsWsParameters.PARAM_PROJECT_KEY);
    if (uuid != null || key != null) {
      ProjectWsRef.validateUuidAndKeyPair(uuid, key);
      Optional<EntityDto> entityDto = uuid != null ? dbClient.entityDao().selectByUuid(dbSession, uuid) : dbClient.entityDao().selectByKey(dbSession, key);
      if (entityDto.isPresent() && !ComponentQualifiers.SUBVIEW.equals(entityDto.get().getQualifier())) {
        return entityDto.get();
      } else {
        throw new NotFoundException("Entity not found");
      }
    }
    return null;
  }

  public GroupUuidOrAnyone findGroupUuidOrAnyone(DbSession dbSession, Request request) {
    String groupName = request.mandatoryParam(PARAM_GROUP_NAME);
    return groupWsSupport.findGroupOrAnyone(dbSession, groupName);
  }

  @CheckForNull
  public GroupDto findGroupDtoOrNullIfAnyone(DbSession dbSession, Request request) {
    String groupName = request.mandatoryParam(PARAM_GROUP_NAME);
    return groupWsSupport.findGroupDtoOrNullIfAnyone(dbSession, groupName);
  }

  public UserId findUser(DbSession dbSession, String login) {
    UserDto dto = ofNullable(dbClient.userDao().selectActiveUserByLogin(dbSession, login))
      .orElseThrow(() -> new NotFoundException(format("User with login '%s' is not found'", login)));
    return new UserIdDto(dto.getUuid(), dto.getLogin());
  }

  public PermissionTemplateDto findTemplate(DbSession dbSession, WsTemplateRef ref) {
    String uuid = ref.uuid();
    String name = ref.name();
    if (uuid != null) {
      return checkFound(
        dbClient.permissionTemplateDao().selectByUuid(dbSession, uuid),
        "Permission template with id '%s' is not found", uuid);
    } else {
      checkNotNull(name);
      return checkFound(
        dbClient.permissionTemplateDao().selectByName(dbSession, name),
        "Permission template with name '%s' is not found (case insensitive)", name);
    }
  }

  public void checkRemovingOwnAdminRight(UserSession userSession, UserId user, String permission) {
    if (ADMINISTER.getKey().equals(permission) && isRemovingOwnPermission(userSession, user)) {
      throw BadRequestException.create("As an admin, you can't remove your own admin right");
    }
  }

  private static boolean isRemovingOwnPermission(UserSession userSession, UserId user) {
    return user.getLogin().equals(userSession.getLogin());
  }

  public void checkRemovingOwnBrowsePermissionOnPrivateProject(DbSession dbSession, UserSession userSession, @Nullable EntityDto entityDto, String permission,
    GroupUuidOrAnyone group) {

    if (userSession.isSystemAdministrator() || group.isAnyone() || !isUpdatingBrowsePermissionOnPrivateProject(permission, entityDto)) {
      return;
    }

    if (hasBrowsePermissionViaUser(dbSession, userSession, entityDto).contains(ProjectPermission.USER.getKey())) {
      return;
    }

    Set<String> groupUuidsWithPermission = findGroupsWithBrowsePermission(dbSession, entityDto);
    boolean isUserInAnotherGroupWithPermissionForThisProject = userSession.getGroups().stream()
      .map(GroupDto::getUuid)
      .anyMatch(groupDtoUuid -> groupUuidsWithPermission.contains(groupDtoUuid) && !groupDtoUuid.equals(group.getUuid()));

    if (!isUserInAnotherGroupWithPermissionForThisProject) {
      throw BadRequestException.create(ERROR_REMOVING_OWN_BROWSE_PERMISSION);
    }
  }

  private List<String> hasBrowsePermissionViaUser(DbSession dbSession, UserSession userSession, EntityDto entityDto) {
    String userUuid = Objects.requireNonNull(userSession.getUuid(), "The user must be authenticated");
    return dbClient.userPermissionDao().selectEntityPermissionsOfUser(dbSession, userUuid, entityDto.getUuid());
  }

  public void checkRemovingOwnBrowsePermissionOnPrivateProject(DbSession dbSession, UserSession userSession, @Nullable EntityDto entityDto, String permission, UserId user) {
    if (!user.getLogin().equals(userSession.getLogin())) {
      return;
    }
    if (userSession.isSystemAdministrator() || !isUpdatingBrowsePermissionOnPrivateProject(permission, entityDto)) {
      return;
    }
    if (userHasBrowsePermissionViaGroup(dbSession, userSession.getGroups(), entityDto)) {
      return;
    }
    throw BadRequestException.create(ERROR_REMOVING_OWN_BROWSE_PERMISSION);
  }

  public static boolean isUpdatingBrowsePermissionOnPrivateProject(String permission, @Nullable EntityDto entityDto) {
    return entityDto != null && entityDto.isPrivate() && permission.equals(ProjectPermission.USER.getKey());
  }

  private boolean userHasBrowsePermissionViaGroup(DbSession dbSession, Collection<GroupDto> groups, EntityDto entityDto) {
    Set<String> groupUuidsWithPermission = findGroupsWithBrowsePermission(dbSession, entityDto);

    return groups.stream()
      .map(GroupDto::getUuid)
      .anyMatch(groupUuidsWithPermission::contains);

  }

  private Set<String> findGroupsWithBrowsePermission(DbSession dbSession,EntityDto entityDto) {
    return dbClient.groupPermissionDao().selectGroupUuidsWithPermissionOnEntity(dbSession, entityDto.getUuid(), ProjectPermission.USER.getKey());
  }

}
