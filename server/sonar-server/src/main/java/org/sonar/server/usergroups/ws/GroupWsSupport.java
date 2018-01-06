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
package org.sonar.server.usergroups.ws;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonarqube.ws.UserGroups;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.ws.WsUtils.checkFound;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.checkRequest;

/**
 * Factorizes code about user groups between web services
 */
public class GroupWsSupport {

  static final String PARAM_GROUP_ID = "id";
  static final String PARAM_ORGANIZATION_KEY = "organization";
  static final String PARAM_GROUP_NAME = "name";
  static final String PARAM_GROUP_DESCRIPTION = "description";
  static final String PARAM_LOGIN = "login";

  // Database column size should be 500 (since migration #353),
  // but on some instances, column size is still 200,
  // hence the validation is done with 200
  static final int DESCRIPTION_MAX_LENGTH = 200;

  private final DbClient dbClient;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final DefaultGroupFinder defaultGroupFinder;

  public GroupWsSupport(DbClient dbClient, DefaultOrganizationProvider defaultOrganizationProvider, DefaultGroupFinder defaultGroupFinder) {
    this.dbClient = dbClient;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.defaultGroupFinder = defaultGroupFinder;
  }

  /**
   * Find a group by its id (parameter {@link #PARAM_GROUP_ID}) or couple organization key/group name
   * (parameters {@link #PARAM_ORGANIZATION_KEY} and {@link #PARAM_GROUP_NAME}). The virtual
   * group "Anyone" is not supported.
   *
   * @throws NotFoundException if parameters are missing/incorrect, if the requested group does not exist
   * or if the virtual group "Anyone" is requested.
   */
  public GroupId findGroup(DbSession dbSession, Request request) {
    return GroupId.from(findGroupDto(dbSession, request));
  }

  public GroupDto findGroupDto(DbSession dbSession, Request request) {
    Integer id = request.paramAsInt(PARAM_GROUP_ID);
    String organizationKey = request.param(PARAM_ORGANIZATION_KEY);
    String name = request.param(PARAM_GROUP_NAME);
    return findGroupDto(dbSession, GroupWsRef.create(id, organizationKey, name));
  }

  public GroupDto findGroupDto(DbSession dbSession, GroupWsRef ref) {
    if (ref.hasId()) {
      GroupDto group = dbClient.groupDao().selectById(dbSession, ref.getId());
      checkFound(group, "No group with id '%s'", ref.getId());
      return group;
    }

    OrganizationDto org = findOrganizationByKey(dbSession, ref.getOrganizationKey());
    Optional<GroupDto> group = dbClient.groupDao().selectByName(dbSession, org.getUuid(), ref.getName());
    checkFoundWithOptional(group, "No group with name '%s' in organization '%s'", ref.getName(), org.getKey());
    return group.get();
  }

  public GroupIdOrAnyone findGroupOrAnyone(DbSession dbSession, GroupWsRef ref) {
    if (ref.hasId()) {
      GroupDto group = dbClient.groupDao().selectById(dbSession, ref.getId());
      checkFound(group, "No group with id '%s'", ref.getId());
      return GroupIdOrAnyone.from(group);
    }

    OrganizationDto org = findOrganizationByKey(dbSession, ref.getOrganizationKey());
    if (ref.isAnyone()) {
      return GroupIdOrAnyone.forAnyone(org.getUuid());
    }

    Optional<GroupDto> group = dbClient.groupDao().selectByName(dbSession, org.getUuid(), ref.getName());
    checkFoundWithOptional(group, "No group with name '%s' in organization '%s'", ref.getName(), org.getKey());
    return GroupIdOrAnyone.from(group.get());
  }

  /**
   * Loads organization from database by its key.
   * @param dbSession
   * @param key the organization key, or {@code null} to get the default organization
   * @return non-null organization
   * @throws NotFoundException if no organizations match the provided key
   */
  public OrganizationDto findOrganizationByKey(DbSession dbSession, @Nullable String key) {
    String effectiveKey = key;
    if (effectiveKey == null) {
      effectiveKey = defaultOrganizationProvider.get().getKey();
    }
    Optional<OrganizationDto> org = dbClient.organizationDao().selectByKey(dbSession, effectiveKey);
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
    checkArgument(!defaultGroup.getId().equals(groupDto.getId()), "Default group '%s' cannot be used to perform this action", groupDto.getName());
  }

  static UserGroups.Group.Builder toProtobuf(OrganizationDto organization, GroupDto group, int membersCount, boolean isDefault) {
    UserGroups.Group.Builder wsGroup = UserGroups.Group.newBuilder()
      .setId(group.getId())
      .setOrganization(organization.getKey())
      .setName(group.getName())
      .setMembersCount(membersCount)
      .setDefault(isDefault);
    setNullable(group.getDescription(), wsGroup::setDescription);
    return wsGroup;
  }

  static void defineGroupWsParameters(WebService.NewAction action) {
    defineGroupIdWsParameter(action);
    defineGroupNameWsParameter(action);
  }

  private static void defineGroupIdWsParameter(WebService.NewAction action) {
    action.createParam(PARAM_GROUP_ID)
      .setDescription("Group id")
      .setExampleValue("42");
  }

  private static void defineGroupNameWsParameter(WebService.NewAction action) {
    action.createParam(PARAM_ORGANIZATION_KEY)
      .setDescription("Key of organization")
      .setExampleValue("my-org")
      .setInternal(true)
      .setSince("6.2");
    action.createParam(PARAM_GROUP_NAME)
      .setDescription("Group name")
      .setExampleValue("sonar-administrators");
  }

  static WebService.NewParam defineLoginWsParameter(WebService.NewAction action) {
    return action.createParam(PARAM_LOGIN)
      .setDescription("User login")
      .setExampleValue("g.hopper");
  }
}
