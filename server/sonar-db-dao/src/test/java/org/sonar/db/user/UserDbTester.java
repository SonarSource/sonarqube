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
package org.sonar.db.user;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.permission.UserPermissionDto;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.db.user.UserTesting.newDisabledUser;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.db.user.UserTesting.newUserSettingDto;
import static org.sonar.db.user.UserTokenTesting.newUserToken;

public class UserDbTester {
  private static final Set<String> PUBLIC_PERMISSIONS = ImmutableSet.of(UserRole.USER, UserRole.CODEVIEWER); // FIXME to check with Simon

  private final DbTester db;
  private final DbClient dbClient;

  public UserDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
  }

  // USERS

  public UserDto insertUser() {
    return insertUser(newUserDto());
  }

  public UserDto insertUser(String login) {
    UserDto dto = newUserDto().setLogin(login).setActive(true);
    return insertUser(dto);
  }

  @SafeVarargs
  public final UserDto insertUser(Consumer<UserDto>... populators) {
    UserDto dto = newUserDto().setActive(true);
    stream(populators).forEach(p -> p.accept(dto));
    return insertUser(dto);
  }

  @SafeVarargs
  public final UserDto insertDisabledUser(Consumer<UserDto>... populators) {
    UserDto dto = newDisabledUser();
    stream(populators).forEach(p -> p.accept(dto));
    return insertUser(dto);
  }

  public UserDto insertUser(UserDto userDto) {
    UserDto updatedUser = dbClient.userDao().insert(db.getSession(), userDto);
    db.commit();
    return updatedUser;
  }

  public UserDto makeRoot(UserDto userDto) {
    dbClient.userDao().setRoot(db.getSession(), userDto.getLogin(), true);
    db.commit();
    return dbClient.userDao().selectByLogin(db.getSession(), userDto.getLogin());
  }

  public UserDto makeNotRoot(UserDto userDto) {
    dbClient.userDao().setRoot(db.getSession(), userDto.getLogin(), false);
    db.commit();
    return dbClient.userDao().selectByLogin(db.getSession(), userDto.getLogin());
  }

  public UserDto insertAdminByUserPermission(OrganizationDto org) {
    UserDto user = insertUser();
    insertPermissionOnUser(org, user, ADMINISTER);
    return user;
  }

  public UserDto updateLastConnectionDate(UserDto user, long lastConnectionDate) {
    db.getDbClient().userDao().update(db.getSession(), user.setLastConnectionDate(lastConnectionDate));
    db.getSession().commit();
    return user;
  }

  public Optional<UserDto> selectUserByLogin(String login) {
    return Optional.ofNullable(dbClient.userDao().selectByLogin(db.getSession(), login));
  }

  // USER SETTINGS

  @SafeVarargs
  public final UserPropertyDto insertUserSetting(UserDto user, Consumer<UserPropertyDto>... populators) {
    UserPropertyDto dto = newUserSettingDto(user);
    stream(populators).forEach(p -> p.accept(dto));
    dbClient.userPropertiesDao().insertOrUpdate(db.getSession(), dto);
    db.commit();
    return dto;
  }

  // GROUPS

  public GroupDto insertGroup(OrganizationDto organization, String name) {
    GroupDto group = newGroupDto().setName(name).setOrganizationUuid(organization.getUuid());
    return insertGroup(group);
  }

  /**
   * Create group in default organization
   */
  public GroupDto insertGroup() {
    GroupDto group = newGroupDto().setOrganizationUuid(db.getDefaultOrganization().getUuid());
    return insertGroup(group);
  }

  /**
   * Create group in specified organization
   */
  public GroupDto insertGroup(OrganizationDto organizationDto) {
    GroupDto group = newGroupDto().setOrganizationUuid(organizationDto.getUuid());
    return insertGroup(group);
  }

  public GroupDto insertGroup(GroupDto dto) {
    db.getDbClient().groupDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  public GroupDto insertDefaultGroup(GroupDto dto) {
    String organizationUuid = dto.getOrganizationUuid();
    db.getDbClient().organizationDao().getDefaultGroupId(db.getSession(), organizationUuid)
      .ifPresent(groupId -> {
        throw new IllegalArgumentException(format("Organization '%s' has already a default group", organizationUuid));
      });
    db.getDbClient().groupDao().insert(db.getSession(), dto);
    db.getDbClient().organizationDao().setDefaultGroupId(db.getSession(), organizationUuid, dto);
    db.commit();
    return dto;
  }

  public GroupDto insertDefaultGroup(OrganizationDto organization, String name) {
    return insertDefaultGroup(newGroupDto().setName(name).setOrganizationUuid(organization.getUuid()));
  }

  public GroupDto insertDefaultGroup(OrganizationDto organization) {
    return insertDefaultGroup(newGroupDto().setOrganizationUuid(organization.getUuid()));
  }

  @CheckForNull
  public GroupDto selectGroupById(int groupId) {
    return db.getDbClient().groupDao().selectById(db.getSession(), groupId);
  }

  public Optional<GroupDto> selectGroup(OrganizationDto org, String name) {
    return db.getDbClient().groupDao().selectByName(db.getSession(), org.getUuid(), name);
  }

  public List<GroupDto> selectGroups(OrganizationDto org) {
    return db.getDbClient().groupDao().selectByOrganizationUuid(db.getSession(), org.getUuid());
  }

  // GROUP MEMBERSHIP

  public UserGroupDto insertMember(GroupDto group, UserDto user) {
    UserGroupDto dto = new UserGroupDto().setGroupId(group.getId()).setUserId(user.getId());
    db.getDbClient().userGroupDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  public void insertMembers(GroupDto group, UserDto... users) {
    Arrays.stream(users).forEach(user -> {
      UserGroupDto dto = new UserGroupDto().setGroupId(group.getId()).setUserId(user.getId());
      db.getDbClient().userGroupDao().insert(db.getSession(), dto);
    });
    db.commit();
  }

  public List<Integer> selectGroupIdsOfUser(UserDto user) {
    return db.getDbClient().groupMembershipDao().selectGroupIdsByUserId(db.getSession(), user.getId());
  }

  // GROUP PERMISSIONS

  public GroupPermissionDto insertPermissionOnAnyone(OrganizationDto org, String permission) {
    GroupPermissionDto dto = new GroupPermissionDto()
      .setOrganizationUuid(org.getUuid())
      .setGroupId(null)
      .setRole(permission);
    db.getDbClient().groupPermissionDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  public GroupPermissionDto insertPermissionOnAnyone(OrganizationDto org, OrganizationPermission permission) {
    return insertPermissionOnAnyone(org, permission.getKey());
  }

  public GroupPermissionDto insertPermissionOnGroup(GroupDto group, String permission) {
    GroupPermissionDto dto = new GroupPermissionDto()
      .setOrganizationUuid(group.getOrganizationUuid())
      .setGroupId(group.getId())
      .setRole(permission);
    db.getDbClient().groupPermissionDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  public GroupPermissionDto insertPermissionOnGroup(GroupDto group, OrganizationPermission permission) {
    return insertPermissionOnGroup(group, permission.getKey());
  }

  public void deletePermissionFromGroup(GroupDto group, String permission) {
    db.getDbClient().groupPermissionDao().delete(db.getSession(), permission, group.getOrganizationUuid(), group.getId(), null);
    db.commit();
  }

  public GroupPermissionDto insertProjectPermissionOnAnyone(String permission, ComponentDto project) {
    checkArgument(!project.isPrivate(), "No permission to group AnyOne can be granted on a private project");
    checkArgument(!PUBLIC_PERMISSIONS.contains(permission),
      "permission %s can't be granted on a public project", permission);
    checkArgument(project.getMainBranchProjectUuid() == null, "Permissions can't be granted on branches");
    GroupPermissionDto dto = new GroupPermissionDto()
      .setOrganizationUuid(project.getOrganizationUuid())
      .setGroupId(null)
      .setRole(permission)
      .setResourceId(project.getId());
    db.getDbClient().groupPermissionDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  public void deleteProjectPermissionFromAnyone(ComponentDto project, String permission) {
    db.getDbClient().groupPermissionDao().delete(db.getSession(), permission, project.getOrganizationUuid(), null, project.getId());
    db.commit();
  }

  public GroupPermissionDto insertProjectPermissionOnGroup(GroupDto group, String permission, ComponentDto project) {
    checkArgument(group.getOrganizationUuid().equals(project.getOrganizationUuid()), "Different organizations");
    checkArgument(project.isPrivate() || !PUBLIC_PERMISSIONS.contains(permission),
      "%s can't be granted on a public project", permission);
    checkArgument(project.getMainBranchProjectUuid() == null, "Permissions can't be granted on branches");
    GroupPermissionDto dto = new GroupPermissionDto()
      .setOrganizationUuid(group.getOrganizationUuid())
      .setGroupId(group.getId())
      .setRole(permission)
      .setResourceId(project.getId());
    db.getDbClient().groupPermissionDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  public List<String> selectGroupPermissions(GroupDto group, @Nullable ComponentDto project) {
    if (project == null) {
      return db.getDbClient().groupPermissionDao().selectGlobalPermissionsOfGroup(db.getSession(),
        group.getOrganizationUuid(), group.getId());
    }
    return db.getDbClient().groupPermissionDao().selectProjectPermissionsOfGroup(db.getSession(),
      group.getOrganizationUuid(), group.getId(), project.getId());
  }

  public List<String> selectAnyonePermissions(OrganizationDto org, @Nullable ComponentDto project) {
    if (project == null) {
      return db.getDbClient().groupPermissionDao().selectGlobalPermissionsOfGroup(db.getSession(),
        org.getUuid(), null);
    }
    checkArgument(org.getUuid().equals(project.getOrganizationUuid()), "Different organizations");
    return db.getDbClient().groupPermissionDao().selectProjectPermissionsOfGroup(db.getSession(), org.getUuid(), null, project.getId());
  }

  // USER PERMISSIONS

  /**
   * Grant permission on default organization
   */
  public UserPermissionDto insertPermissionOnUser(UserDto user, OrganizationPermission permission) {
    return insertPermissionOnUser(db.getDefaultOrganization(), user, permission);
  }

  /**
   * Grant global permission
   * @deprecated use {@link #insertPermissionOnUser(OrganizationDto, UserDto, OrganizationPermission)}
   */
  @Deprecated
  public UserPermissionDto insertPermissionOnUser(OrganizationDto org, UserDto user, String permission) {
    UserPermissionDto dto = new UserPermissionDto(org.getUuid(), permission, user.getId(), null);
    db.getDbClient().userPermissionDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  /**
   * Grant organization permission to user
   */
  public UserPermissionDto insertPermissionOnUser(OrganizationDto org, UserDto user, OrganizationPermission permission) {
    return insertPermissionOnUser(org, user, permission.getKey());
  }

  public void deletePermissionFromUser(OrganizationDto org, UserDto user, OrganizationPermission permission) {
    db.getDbClient().userPermissionDao().deleteGlobalPermission(db.getSession(), user.getId(), permission.getKey(), org.getUuid());
    db.commit();
  }

  public void deletePermissionFromUser(ComponentDto project, UserDto user, String permission) {
    db.getDbClient().userPermissionDao().deleteProjectPermission(db.getSession(), user.getId(), permission, project.getId());
    db.commit();
  }

  /**
   * Grant permission on given project
   */
  public UserPermissionDto insertProjectPermissionOnUser(UserDto user, String permission, ComponentDto project) {
    checkArgument(project.isPrivate() || !PUBLIC_PERMISSIONS.contains(permission),
      "%s can't be granted on a public project", permission);
    checkArgument(project.getMainBranchProjectUuid() == null, "Permissions can't be granted on branches");
    UserPermissionDto dto = new UserPermissionDto(project.getOrganizationUuid(), permission, user.getId(), project.getId());
    db.getDbClient().userPermissionDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  public List<OrganizationPermission> selectPermissionsOfUser(UserDto user, OrganizationDto organization) {
    return toListOfOrganizationPermissions(db.getDbClient().userPermissionDao()
      .selectGlobalPermissionsOfUser(db.getSession(), user.getId(), organization.getUuid()));
  }

  public List<String> selectProjectPermissionsOfUser(UserDto user, ComponentDto project) {
    return db.getDbClient().userPermissionDao().selectProjectPermissionsOfUser(db.getSession(), user.getId(), project.getId());
  }

  private static List<OrganizationPermission> toListOfOrganizationPermissions(List<String> keys) {
    return keys
      .stream()
      .map(OrganizationPermission::fromKey)
      .collect(MoreCollectors.toList());
  }

  @SafeVarargs
  public final UserTokenDto insertToken(UserDto user, Consumer<UserTokenDto>... populators) {
    UserTokenDto dto = newUserToken().setUserUuid(user.getUuid());
    stream(populators).forEach(p -> p.accept(dto));
    db.getDbClient().userTokenDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

}
