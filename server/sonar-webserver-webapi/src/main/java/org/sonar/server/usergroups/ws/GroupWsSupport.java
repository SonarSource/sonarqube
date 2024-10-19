/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.usergroups.ws;

import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.permission.GroupUuid;
import org.sonar.server.permission.GroupUuidOrAnyone;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonarqube.ws.UserGroups;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Optional.ofNullable;
import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;

/**
 * Factorizes code about user groups between web services
 */
public class GroupWsSupport {

  static final String PARAM_ORGANIZATION_KEY = "organization";
  static final String PARAM_GROUP_NAME = "name";
  static final String PARAM_GROUP_CURRENT_NAME = "currentName";
  static final String PARAM_GROUP_DESCRIPTION = "description";
  static final String PARAM_LOGIN = "login";

  // Database column size should be 500 (since migration #353),
  // but on some instances, column size is still 200,
  // hence the validation is done with 200
  static final int DESCRIPTION_MAX_LENGTH = 200;

  private final DbClient dbClient;
  private final DefaultGroupFinder defaultGroupFinder;

  public GroupWsSupport(DbClient dbClient, DefaultGroupFinder defaultGroupFinder) {
    this.dbClient = dbClient;
    this.defaultGroupFinder = defaultGroupFinder;
  }

  /**
   * Find a group by its  group name (parameter {@link #PARAM_GROUP_NAME}). The virtual
   * group "Anyone" is not supported.
   *
   * @throws NotFoundException if parameters are missing/incorrect, if the requested group does not exist
   * or if the virtual group "Anyone" is requested.
   */
  public GroupUuid findGroup(DbSession dbSession, Request request) {
    return GroupUuid.from(findGroupDto(dbSession, request));
  }

  public GroupDto findGroupDto(DbSession dbSession, Request request) {
    String organizationKey = request.param(PARAM_ORGANIZATION_KEY);
    String groupName = request.mandatoryParam(PARAM_GROUP_NAME);
    return findGroupDto(dbSession, groupName);
  }

  @CheckForNull
  public GroupDto findGroupDtoOrNullIfAnyone(DbSession dbSession, String groupName) {
    if (DefaultGroups.isAnyone(groupName)) {
      return null;
    }
    return findGroupDto(dbSession, groupName);
  }

  public GroupDto findGroupDto(DbSession dbSession, String groupName) {
    OrganizationDto org = findOrganizationByKey(dbSession, ref.getOrganizationKey());
    Optional<GroupDto> group = dbClient.groupDao().selectByName(dbSession, org.getUuid(), groupName);
    checkFoundWithOptional(group, "No group with name '%s'", groupName);
    return group.get();
  }

  public GroupUuidOrAnyone findGroupOrAnyone(DbSession dbSession, String groupName) {
    OrganizationDto org = findOrganizationByKey(dbSession, ref.getOrganizationKey());

    if (DefaultGroups.isAnyone(groupName)) {
      return GroupUuidOrAnyone.forAnyone(org.getUuid());
    }

    Optional<GroupDto> group = dbClient.groupDao().selectByName(dbSession, org.getUuid(), groupName);
    checkFoundWithOptional(group, "No group with name '%s' in organization '%s'", groupName, org.getKey());
    return GroupUuidOrAnyone.from(group.get());
  }

  /**
   * Loads organization from database by its key.
   * @param dbSession
   * @param key the organization key
   * @return non-null organization
   * @throws NotFoundException if no organizations match the provided key
   */
  public OrganizationDto findOrganizationByKey(DbSession dbSession, String key) {
    Optional<OrganizationDto> org = dbClient.organizationDao().selectByKey(dbSession, key);
    checkFoundWithOptional(org, "No organization with key '%s'", key);
    return org.get();
  }

  void checkNameDoesNotExist(DbSession dbSession, String organizationUuid, String name) {
    // There is no database constraint on column groups.name
    // because MySQL cannot create a unique index
    // on a UTF-8 VARCHAR larger than 255 characters on InnoDB
    checkRequest(!dbClient.groupDao().selectByName(dbSession, organizationUuid, name).isPresent(), "Group '%s' already exists", name);
  }

  void checkGroupIsNotDefault(DbSession dbSession, GroupDto groupDto) {
    GroupDto defaultGroup = defaultGroupFinder.findDefaultGroup(dbSession, groupDto.getOrganizationUuid());
    checkArgument(!defaultGroup.getUuid().equals(groupDto.getUuid()), "Default group '%s' cannot be used to perform this action", groupDto.getName());
  }

  static UserGroups.Group.Builder toProtobuf(OrganizationDto organization, GroupDto group, int membersCount, boolean isDefault) {
    UserGroups.Group.Builder wsGroup = UserGroups.Group.newBuilder()
      .setId(group.getUuid())
      .setOrganization(organization.getKey())
      .setName(group.getName())
      .setMembersCount(membersCount)
      .setDefault(isDefault);
    ofNullable(group.getDescription()).ifPresent(wsGroup::setDescription);
    return wsGroup;
  }

  static void defineGroupWsParameters(WebService.NewAction action) {
    defineGroupNameWsParameter(action);
  }

  private static void defineGroupNameWsParameter(WebService.NewAction action) {
    action.createParam(PARAM_ORGANIZATION_KEY)
      .setDescription("Key of organization")
      .setExampleValue("my-org")
      .setInternal(true)
      .setRequired(true);
    action.createParam(PARAM_GROUP_NAME)
      .setRequired(true)
      .setDescription("Group name")
      .setExampleValue("sonar-administrators");
  }

  static WebService.NewParam defineLoginWsParameter(WebService.NewAction action) {
    return action.createParam(PARAM_LOGIN)
      .setDescription("User login")
      .setExampleValue("g.hopper");
  }
}
