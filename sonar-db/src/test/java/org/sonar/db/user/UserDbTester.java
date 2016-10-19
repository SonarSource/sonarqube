/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.UserPermissionDto;

import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.db.user.UserTesting.newUserDto;

public class UserDbTester {
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

  public UserDto insertUser(UserDto userDto) {
    UserDto updatedUser = dbClient.userDao().insert(db.getSession(), userDto);
    db.commit();
    return updatedUser;
  }

  public Optional<UserDto> selectUserByLogin(String login) {
    return Optional.ofNullable(dbClient.userDao().selectByLogin(db.getSession(), login));
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

  public GroupDto insertGroup(GroupDto dto) {
    db.getDbClient().groupDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  @CheckForNull
  public GroupDto selectGroupById(long groupId) {
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

  public List<Long> selectGroupIdsOfUser(UserDto user) {
    return db.getDbClient().groupMembershipDao().selectGroupIdsByUserId(db.getSession(), user.getId());
  }

  // GROUP PERMISSIONS

  /**
   * Grant permission to virtual group "anyone" in default organization
   */
  public GroupPermissionDto insertPermissionOnAnyone(String permission) {
    return insertPermissionOnAnyone(db.getDefaultOrganization(), permission);
  }

  public GroupPermissionDto insertPermissionOnAnyone(OrganizationDto org, String permission) {
    GroupPermissionDto dto = new GroupPermissionDto()
      .setOrganizationUuid(org.getUuid())
      .setGroupId(null)
      .setRole(permission);
    db.getDbClient().groupPermissionDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
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

  public GroupPermissionDto insertProjectPermissionOnAnyone(String permission, ComponentDto project) {
    return insertProjectPermissionOnAnyone(db.getDefaultOrganization(), permission, project);
  }

  public GroupPermissionDto insertProjectPermissionOnAnyone(OrganizationDto org, String permission, ComponentDto project) {
    GroupPermissionDto dto = new GroupPermissionDto()
      .setOrganizationUuid(org.getUuid())
      .setGroupId(null)
      .setRole(permission)
      .setResourceId(project.getId());
    db.getDbClient().groupPermissionDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  public GroupPermissionDto insertProjectPermissionOnGroup(GroupDto group, String permission, ComponentDto project) {
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
    return db.getDbClient().groupPermissionDao().selectProjectPermissionsOfGroup(db.getSession(),
      org.getUuid(), null, project.getId());
  }

  // USER PERMISSIONS

  /**
   * Grant global permission on default organization
   */
  public UserPermissionDto insertPermissionOnUser(UserDto user, String permission) {
    return insertPermissionOnUser(db.getDefaultOrganization(), user, permission);
  }

  /**
   * Grant global permission
   */
  public UserPermissionDto insertPermissionOnUser(OrganizationDto org, UserDto user, String permission) {
    UserPermissionDto dto = new UserPermissionDto(org.getUuid(), permission, user.getId(), null);
    db.getDbClient().userPermissionDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  /**
   * Grant permission on given project in default organization
   */
  public UserPermissionDto insertProjectPermissionOnUser(UserDto user, String permission, ComponentDto project) {
    return insertProjectPermissionOnUser(db.getDefaultOrganization(), user, permission, project);
  }

  /**
   * Grant permission on given project
   */
  public UserPermissionDto insertProjectPermissionOnUser(OrganizationDto org, UserDto user, String permission, ComponentDto project) {
    UserPermissionDto dto = new UserPermissionDto(org.getUuid(), permission, user.getId(), project.getId());
    db.getDbClient().userPermissionDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  public List<String> selectGlobalPermissionsOfUser(UserDto user, OrganizationDto organization) {
    return db.getDbClient().userPermissionDao().selectGlobalPermissionsOfUser(db.getSession(), user.getId(), organization.getUuid());
  }

  public List<String> selectProjectPermissionsOfUser(UserDto user, ComponentDto project) {
    return db.getDbClient().userPermissionDao().selectProjectPermissionsOfUser(db.getSession(), user.getId(), project.getId());
  }
}
