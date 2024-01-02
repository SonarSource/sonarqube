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
package org.sonar.db.user;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeTaskMessageType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.UserPermissionDto;
import org.sonar.db.project.ProjectDto;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.stream;
import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.user.GroupTesting.newGroupDto;

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
    return insertUser(UserTesting.newUserDto());
  }

  public UserDto insertUser(String login) {
    UserDto dto = UserTesting.newUserDto().setLogin(login).setActive(true);
    return insertUser(dto);
  }

  @SafeVarargs
  public final UserDto insertUser(Consumer<UserDto>... populators) {
    UserDto dto = UserTesting.newUserDto().setActive(true);
    stream(populators).forEach(p -> p.accept(dto));
    return insertUser(dto);
  }

  @SafeVarargs
  public final UserDto insertDisabledUser(Consumer<UserDto>... populators) {
    UserDto dto = UserTesting.newDisabledUser();
    stream(populators).forEach(p -> p.accept(dto));
    return insertUser(dto);
  }

  public UserDto insertUser(UserDto userDto) {
    UserDto updatedUser = dbClient.userDao().insert(db.getSession(), userDto);
    db.commit();
    return updatedUser;
  }

  public UserDto insertAdminByUserPermission() {
    UserDto user = insertUser();
    insertPermissionOnUser(user, ADMINISTER);
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

  public Optional<UserDto> selectUserByEmail(String email) {
    List<UserDto> users = dbClient.userDao().selectByEmail(db.getSession(), email);
    if (users.size() > 1) {
      return Optional.empty();
    }
    return Optional.of(users.get(0));
  }

  public Optional<UserDto> selectUserByExternalLoginAndIdentityProvider(String login, String identityProvider) {
    return Optional.ofNullable(dbClient.userDao().selectByExternalLoginAndIdentityProvider(db.getSession(), login, identityProvider));
  }

  // GROUPS

  public GroupDto insertGroup(String name) {
    GroupDto group = newGroupDto().setName(name);
    return insertGroup(group);
  }

  @SafeVarargs
  public final GroupDto insertGroup(Consumer<GroupDto>... populators) {
    GroupDto group = newGroupDto();
    stream(populators).forEach(p -> p.accept(group));
    return insertGroup(group);
  }

  public GroupDto insertGroup(GroupDto dto) {
    db.getDbClient().groupDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  public GroupDto insertDefaultGroup() {
    GroupDto dto = db.getDbClient().groupDao().insert(db.getSession(), newGroupDto().setName(DefaultGroups.USERS).setDescription("Users"));
    db.commit();
    return dto;
  }

  @CheckForNull
  public GroupDto selectGroupByUuid(String groupUuid) {
    return db.getDbClient().groupDao().selectByUuid(db.getSession(), groupUuid);
  }

  public Optional<GroupDto> selectGroup(String name) {
    return db.getDbClient().groupDao().selectByName(db.getSession(), name);
  }

  // GROUP MEMBERSHIP

  public UserGroupDto insertMember(GroupDto group, UserDto user) {
    UserGroupDto dto = new UserGroupDto().setGroupUuid(group.getUuid()).setUserUuid(user.getUuid());
    db.getDbClient().userGroupDao().insert(db.getSession(), dto, group.getName(), user.getLogin());
    db.commit();
    return dto;
  }

  public void insertMembers(GroupDto group, UserDto... users) {
    Arrays.stream(users).forEach(user -> {
      UserGroupDto dto = new UserGroupDto().setGroupUuid(group.getUuid()).setUserUuid(user.getUuid());
      db.getDbClient().userGroupDao().insert(db.getSession(), dto, group.getName(), user.getLogin());
    });
    db.commit();
  }

  public List<String> selectGroupUuidsOfUser(UserDto user) {
    return db.getDbClient().groupMembershipDao().selectGroupUuidsByUserUuid(db.getSession(), user.getUuid());
  }

  // GROUP PERMISSIONS

  public GroupPermissionDto insertPermissionOnAnyone(String permission) {
    GroupPermissionDto dto = new GroupPermissionDto()
      .setUuid(Uuids.createFast())
      .setGroupUuid(null)
      .setRole(permission);
    db.getDbClient().groupPermissionDao().insert(db.getSession(), dto, null, null);
    db.commit();
    return dto;
  }

  public GroupPermissionDto insertPermissionOnAnyone(GlobalPermission permission) {
    return insertPermissionOnAnyone(permission.getKey());
  }

  public GroupPermissionDto insertPermissionOnGroup(GroupDto group, String permission) {
    GroupPermissionDto dto = new GroupPermissionDto()
      .setUuid(Uuids.createFast())
      .setGroupUuid(group.getUuid())
      .setRole(permission);
    db.getDbClient().groupPermissionDao().insert(db.getSession(), dto, null, null);
    db.commit();
    return dto;
  }

  public GroupPermissionDto insertPermissionOnGroup(GroupDto group, GlobalPermission permission) {
    return insertPermissionOnGroup(group, permission.getKey());
  }

  public void deletePermissionFromGroup(GroupDto group, String permission) {
    db.getDbClient().groupPermissionDao().delete(db.getSession(), permission, group.getUuid(), group.getName(), null, null);
    db.commit();
  }

  public GroupPermissionDto insertProjectPermissionOnAnyone(String permission, ComponentDto project) {
    checkArgument(!project.isPrivate(), "No permission to group AnyOne can be granted on a private project");
    checkArgument(!PUBLIC_PERMISSIONS.contains(permission),
      "permission %s can't be granted on a public project", permission);
    checkArgument(project.getMainBranchProjectUuid() == null, "Permissions can't be granted on branches");
    GroupPermissionDto dto = new GroupPermissionDto()
      .setUuid(Uuids.createFast())
      .setGroupUuid(null)
      .setRole(permission)
      .setComponentUuid(project.uuid())
      .setComponentName(project.name());
    db.getDbClient().groupPermissionDao().insert(db.getSession(), dto, project, null);
    db.commit();
    return dto;
  }

  public void deleteProjectPermissionFromAnyone(ComponentDto project, String permission) {
    db.getDbClient().groupPermissionDao().delete(db.getSession(), permission, null, null, project.uuid(), project);
    db.commit();
  }

  public GroupPermissionDto insertProjectPermissionOnGroup(GroupDto group, String permission, ComponentDto project) {
    checkArgument(project.isPrivate() || !PUBLIC_PERMISSIONS.contains(permission),
      "%s can't be granted on a public project", permission);
    checkArgument(project.getMainBranchProjectUuid() == null, "Permissions can't be granted on branches");
    GroupPermissionDto dto = new GroupPermissionDto()
      .setUuid(Uuids.createFast())
      .setGroupUuid(group.getUuid())
      .setGroupName(group.getName())
      .setRole(permission)
      .setComponentUuid(project.uuid())
      .setComponentName(project.name());
    db.getDbClient().groupPermissionDao().insert(db.getSession(), dto, project, null);
    db.commit();
    return dto;
  }

  public List<String> selectGroupPermissions(GroupDto group, @Nullable ComponentDto project) {
    if (project == null) {
      return db.getDbClient().groupPermissionDao().selectGlobalPermissionsOfGroup(db.getSession(), group.getUuid());
    }
    return db.getDbClient().groupPermissionDao().selectProjectPermissionsOfGroup(db.getSession(), group.getUuid(), project.uuid());
  }

  public List<String> selectAnyonePermissions(@Nullable ComponentDto project) {
    if (project == null) {
      return db.getDbClient().groupPermissionDao().selectGlobalPermissionsOfGroup(db.getSession(), null);
    }
    return db.getDbClient().groupPermissionDao().selectProjectPermissionsOfGroup(db.getSession(), null, project.uuid());
  }

  // USER PERMISSIONS

  /**
   * Grant permission
   */
  public UserPermissionDto insertPermissionOnUser(UserDto user, GlobalPermission permission) {
    return insertPermissionOnUser(user, permission.getKey());
  }

  /**
   * Grant global permission
   * @deprecated use {@link #insertPermissionOnUser(UserDto, GlobalPermission)}
   */
  @Deprecated
  public UserPermissionDto insertPermissionOnUser(UserDto user, String permission) {
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), permission, user.getUuid(), null);
    db.getDbClient().userPermissionDao().insert(db.getSession(), dto, null, user, null);
    db.commit();
    return dto;
  }

  public void deletePermissionFromUser(UserDto user, GlobalPermission permission) {
    db.getDbClient().userPermissionDao().deleteGlobalPermission(db.getSession(), user, permission.getKey());
    db.commit();
  }

  public void deletePermissionFromUser(ComponentDto project, UserDto user, String permission) {
    db.getDbClient().userPermissionDao().deleteProjectPermission(db.getSession(), user, permission, project);
    db.commit();
  }

  /**
   * Grant permission on given project
   */
  public UserPermissionDto insertProjectPermissionOnUser(UserDto user, String permission, ComponentDto project) {
    checkArgument(project.isPrivate() || !PUBLIC_PERMISSIONS.contains(permission),
      "%s can't be granted on a public project", permission);
    checkArgument(project.getMainBranchProjectUuid() == null, "Permissions can't be granted on branches");
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), permission, user.getUuid(), project.uuid());
    db.getDbClient().userPermissionDao().insert(db.getSession(), dto, project, user, null);
    db.commit();
    return dto;
  }

  public List<GlobalPermission> selectPermissionsOfUser(UserDto user) {
    return toListOfGlobalPermissions(db.getDbClient().userPermissionDao()
      .selectGlobalPermissionsOfUser(db.getSession(), user.getUuid()));
  }

  public List<String> selectProjectPermissionsOfUser(UserDto user, ComponentDto project) {
    return db.getDbClient().userPermissionDao().selectProjectPermissionsOfUser(db.getSession(), user.getUuid(), project.uuid());
  }

  private static List<GlobalPermission> toListOfGlobalPermissions(List<String> keys) {
    return keys
      .stream()
      .map(GlobalPermission::fromKey)
      .collect(MoreCollectors.toList());
  }

  // USER TOKEN

  @SafeVarargs
  public final UserTokenDto insertToken(UserDto user, Consumer<UserTokenDto>... populators) {
    UserTokenDto dto = UserTokenTesting.newUserToken().setUserUuid(user.getUuid());
    stream(populators).forEach(p -> p.accept(dto));
    db.getDbClient().userTokenDao().insert(db.getSession(), dto, user.getLogin());
    db.commit();
    return dto;
  }

  // PROJECT ANALYSIS TOKEN

  @SafeVarargs
  public final UserTokenDto insertProjectAnalysisToken(UserDto user, Consumer<UserTokenDto>... populators) {
    UserTokenDto dto = UserTokenTesting.newProjectAnalysisToken().setUserUuid(user.getUuid());
    stream(populators).forEach(p -> p.accept(dto));
    db.getDbClient().userTokenDao().insert(db.getSession(), dto, user.getLogin());
    db.commit();
    return dto;
  }

  // SESSION TOKENS

  @SafeVarargs
  public final SessionTokenDto insertSessionToken(UserDto user, Consumer<SessionTokenDto>... populators) {
    SessionTokenDto dto = new SessionTokenDto()
      .setUserUuid(user.getUuid())
      .setExpirationDate(nextLong());
    stream(populators).forEach(p -> p.accept(dto));
    db.getDbClient().sessionTokensDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  public final UserDismissedMessageDto insertUserDismissedMessage(UserDto userDto, ProjectDto projectDto, CeTaskMessageType messageType) {
    UserDismissedMessageDto dto = new UserDismissedMessageDto()
      .setUuid(Uuids.create())
      .setUserUuid(userDto.getUuid())
      .setProjectUuid(projectDto.getUuid())
      .setCeMessageType(messageType);
    db.getDbClient().userDismissedMessagesDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

}
