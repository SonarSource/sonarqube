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

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.dismissmessage.MessageType;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.UserPermissionDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.provisioning.GithubOrganizationGroupDto;
import org.sonar.db.scim.ScimGroupDto;
import org.sonar.db.scim.ScimUserDto;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.user.GroupTesting.newGroupDto;

public class UserDbTester {
  private static final Set<String> PUBLIC_PERMISSIONS = Set.of(UserRole.USER, UserRole.CODEVIEWER);
  public static final String PERMISSIONS_CANT_BE_GRANTED_ON_BRANCHES = "Permissions can't be granted on branches";

  private final Random random = new SecureRandom();

  private final DbTester db;
  private final DbClient dbClient;

  public UserDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
  }

  // USERS

  public UserDto insertUserRealistic() {
    return insertUser(UserTesting.newUserDtoRealistic());
  }

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

  public ScimUserDto insertScimUser(UserDto userDto) {
    ScimUserDto scimUserDto = dbClient.scimUserDao().enableScimForUser(db.getSession(), userDto.getUuid());
    db.commit();
    return scimUserDto;
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

  public ScimUserDto enableScimForUser(UserDto userDto) {
    ScimUserDto scimUSerDto = db.getDbClient().scimUserDao().enableScimForUser(db.getSession(), userDto.getUuid());
    db.commit();
    return scimUSerDto;
  }

  public UserDto insertAdminByUserPermission() {
    UserDto user = insertUser();
    insertGlobalPermissionOnUser(user, ADMINISTER);
    return user;
  }

  public UserDto updateLastConnectionDate(UserDto user, long lastConnectionDate) {
    db.getDbClient().userDao().update(db.getSession(), user.setLastConnectionDate(lastConnectionDate));
    db.getSession().commit();
    return user;
  }

  public UserDto updateSonarLintLastConnectionDate(UserDto user, long sonarLintLastConnectionDate) {
    db.getDbClient().userDao().update(db.getSession(), user.setLastSonarlintConnectionDate(sonarLintLastConnectionDate));
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

  public Optional<UserDto> selectUserByExternalIdAndIdentityProvider(String externalId, String identityProvider) {
    return Optional.ofNullable(dbClient.userDao().selectByExternalIdAndIdentityProvider(db.getSession(), externalId, identityProvider));
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

  public void markGroupAsGithubManaged(String groupUuid) {
    db.getDbClient().externalGroupDao().insert(db.getSession(), new ExternalGroupDto(groupUuid, randomAlphanumeric(20), "github"));
    db.commit();
  }

  public GithubOrganizationGroupDto markGroupAsGithubOrganizationGroup(String groupUuid, String organizationName) {
    GithubOrganizationGroupDto githubOrganizationGroupDto = new GithubOrganizationGroupDto(groupUuid, organizationName);
    db.getDbClient().githubOrganizationGroupDao().insert(db.getSession(), githubOrganizationGroupDto);
    db.commit();
    return githubOrganizationGroupDto;
  }

  public ExternalGroupDto insertExternalGroup(ExternalGroupDto dto) {
    db.getDbClient().externalGroupDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  public ScimGroupDto insertScimGroup(GroupDto dto) {
    ScimGroupDto result = db.getDbClient().scimGroupDao().enableScimForGroup(db.getSession(), dto.getUuid());
    db.commit();
    return result;
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
    return db.getDbClient().groupDao().selectByName(db.getSession(), null, name);
  }

  public int countAllGroups() {
    return db.getDbClient().groupDao().countByQuery(db.getSession(), new GroupQuery(null, null, null));
  }

  public Optional<ExternalGroupDto> selectExternalGroupByGroupUuid(String groupUuid) {
    return db.getDbClient().externalGroupDao().selectByGroupUuid(db.getSession(), groupUuid);
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

  public List<UserDto> findMembers(GroupDto group) {
    Set<String> userUuidsInGroup = db.getDbClient().userGroupDao().selectUserUuidsInGroup(db.getSession(), group.getUuid());
    return db.getDbClient().userDao().selectByUuids(db.getSession(), userUuidsInGroup);
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

  public Set<GroupPermissionDto> insertPermissionsOnGroup(GroupDto group, String... permissions) {
    return stream(permissions)
      .map(p -> insertPermissionOnGroup(group, p))
      .collect(toSet());
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

  public GroupPermissionDto insertProjectPermissionOnAnyone(String permission, ComponentDto project) {
    checkArgument(!project.isPrivate(), "No permission to group AnyOne can be granted on a private project");
    checkArgument(!PUBLIC_PERMISSIONS.contains(permission),
      "permission %s can't be granted on a public project", permission);
    Optional<BranchDto> branchDto = db.getDbClient().branchDao().selectByUuid(db.getSession(), project.branchUuid());
    // I don't know if this check is worth it
    branchDto.ifPresent(dto -> checkArgument(dto.isMain(), PERMISSIONS_CANT_BE_GRANTED_ON_BRANCHES));
    GroupPermissionDto dto = new GroupPermissionDto()
      .setUuid(Uuids.createFast())
      .setGroupUuid(null)
      .setRole(permission)
      .setEntityUuid(project.uuid())
      .setEntityName(project.name());

    // TODO, will be removed later
    ProjectDto projectDto = new ProjectDto();
    projectDto.setQualifier(project.qualifier());
    projectDto.setKey(project.getKey());
    db.getDbClient().groupPermissionDao().insert(db.getSession(), dto, projectDto, null);
    db.commit();
    return dto;
  }

  public GroupPermissionDto insertEntityPermissionOnAnyone(String permission, EntityDto entity) {
    checkArgument(!entity.isPrivate(), "No permission to group AnyOne can be granted on a private entity");
    checkArgument(!PUBLIC_PERMISSIONS.contains(permission),
      "permission %s can't be granted on a public entity", permission);
    GroupPermissionDto dto = new GroupPermissionDto()
      .setUuid(Uuids.createFast())
      .setGroupUuid(null)
      .setRole(permission)
      .setEntityUuid(entity.getUuid())
      .setEntityName(entity.getName());
    db.getDbClient().groupPermissionDao().insert(db.getSession(), dto, entity, null);
    db.commit();
    return dto;
  }

  public void deleteProjectPermissionFromAnyone(EntityDto entity, String permission) {
    db.getDbClient().groupPermissionDao().delete(db.getSession(), permission, null, null, null, entity);
    db.commit();
  }

  public GroupPermissionDto insertProjectPermissionOnGroup(GroupDto group, String permission, ComponentDto project) {
    checkArgument(project.isPrivate() || !PUBLIC_PERMISSIONS.contains(permission),
      "%s can't be granted on a public project", permission);
    Optional<BranchDto> branchDto = db.getDbClient().branchDao().selectByUuid(db.getSession(), project.branchUuid());
    // I don't know if this check is worth it
    branchDto.ifPresent(dto -> checkArgument(dto.isMain(), PERMISSIONS_CANT_BE_GRANTED_ON_BRANCHES));
    GroupPermissionDto dto = new GroupPermissionDto()
      .setUuid(Uuids.createFast())
      .setGroupUuid(group.getUuid())
      .setGroupName(group.getName())
      .setRole(permission)
      .setEntityUuid(project.uuid())
      .setEntityName(project.name());

    // TODO, will be removed later
    ProjectDto projectDto = new ProjectDto();
    projectDto.setQualifier(project.qualifier());
    projectDto.setKey(project.getKey());
    db.getDbClient().groupPermissionDao().insert(db.getSession(), dto, projectDto, null);
    db.commit();
    return dto;
  }

  public Set<GroupPermissionDto> insertEntityPermissionsOnGroup(GroupDto group, EntityDto entity, String... permissions) {
    return stream(permissions)
      .map(permission -> insertEntityPermissionOnGroup(group, permission, entity))
      .collect(toSet());
  }

  public GroupPermissionDto insertEntityPermissionOnGroup(GroupDto group, String permission, EntityDto entity) {
    checkArgument(entity.isPrivate() || !PUBLIC_PERMISSIONS.contains(permission),
      "%s can't be granted on a public entity (project or portfolio)", permission);
    Optional<BranchDto> branchDto = db.getDbClient().branchDao().selectByUuid(db.getSession(), entity.getUuid());
    // I don't know if this check is worth it
    branchDto.ifPresent(dto -> checkArgument(dto.isMain(), PERMISSIONS_CANT_BE_GRANTED_ON_BRANCHES));
    GroupPermissionDto dto = new GroupPermissionDto()
      .setUuid(Uuids.createFast())
      .setGroupUuid(group.getUuid())
      .setGroupName(group.getName())
      .setRole(permission)
      .setEntityUuid(entity.getUuid())
      .setEntityName(entity.getName());
    db.getDbClient().groupPermissionDao().insert(db.getSession(), dto, entity, null);
    db.commit();
    return dto;
  }

  public List<String> selectGroupPermissions(GroupDto group, @Nullable EntityDto entity) {
    if (entity == null) {
      return db.getDbClient().groupPermissionDao().selectGlobalPermissionsOfGroup(db.getSession(), null, group.getUuid());
    }
    return db.getDbClient().groupPermissionDao().selectEntityPermissionsOfGroup(db.getSession(), null, group.getUuid(), entity.getUuid());
  }

  public List<String> selectAnyonePermissions(@Nullable String entityUuid) {
    if (entityUuid == null) {
      return db.getDbClient().groupPermissionDao().selectGlobalPermissionsOfGroup(db.getSession(), null, null);
    }
    return db.getDbClient().groupPermissionDao().selectEntityPermissionsOfGroup(db.getSession(), null, null, entityUuid);
  }

  // USER PERMISSIONS

  /**
   * Grant global permission
   */
  public UserPermissionDto insertGlobalPermissionOnUser(UserDto user, GlobalPermission permission) {
    return insertPermissionOnUser(user, permission.getKey());
  }

  /**
   * Grant permission
   */
  public UserPermissionDto insertPermissionOnUser(UserDto user, String permission) {
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), null, permission, user.getUuid(), null);
    db.getDbClient().userPermissionDao().insert(db.getSession(), dto, (EntityDto) null, user, null);
    db.commit();
    return dto;
  }

  public void deletePermissionFromUser(UserDto user, GlobalPermission permission) {
    db.getDbClient().userPermissionDao().deleteGlobalPermission(db.getSession(), user, permission.getKey(), null);
    db.commit();
  }

  public void deletePermissionFromUser(EntityDto project, UserDto user, String permission) {
    db.getDbClient().userPermissionDao().deleteEntityPermission(db.getSession(), user, permission, project);
    db.commit();
  }

  /**
   * Grant permission on given project
   */
  public UserPermissionDto insertProjectPermissionOnUser(UserDto user, String permission, ComponentDto project) {
    checkArgument(project.isPrivate() || !PUBLIC_PERMISSIONS.contains(permission),
      "%s can't be granted on a public project", permission);
    EntityDto entityDto;
    if (project.qualifier().equals(Qualifiers.VIEW) || project.qualifier().equals(Qualifiers.SUBVIEW)) {
      entityDto = db.getDbClient().portfolioDao().selectByUuid(db.getSession(), project.uuid())
        .orElseThrow();
    } else {
      BranchDto branchDto = db.getDbClient().branchDao().selectByUuid(db.getSession(), project.branchUuid())
        .orElseThrow();
      // I don't know if this check is worth it
      checkArgument(branchDto.isMain(), PERMISSIONS_CANT_BE_GRANTED_ON_BRANCHES);

      entityDto = dbClient.projectDao().selectByBranchUuid(db.getSession(), branchDto.getUuid())
        .orElseThrow();
    }

    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), null, permission, user.getUuid(), entityDto.getUuid());
    db.getDbClient().userPermissionDao().insert(db.getSession(), dto, entityDto, user, null);
    db.commit();
    return dto;
  }

  public UserPermissionDto insertProjectPermissionOnUser(UserDto user, String permission, EntityDto project) {
    checkArgument(project.isPrivate() || !PUBLIC_PERMISSIONS.contains(permission),
      "%s can't be granted on a public project", permission);
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), null, permission, user.getUuid(), project.getUuid());
    db.getDbClient().userPermissionDao().insert(db.getSession(), dto, project, user, null);
    db.commit();
    return dto;
  }

  public List<GlobalPermission> selectPermissionsOfUser(UserDto user) {
    return toListOfGlobalPermissions(db.getDbClient().userPermissionDao()
      .selectGlobalPermissionsOfUser(db.getSession(), null, user.getUuid()));
  }

  public List<String> selectEntityPermissionOfUser(UserDto user, String entityUuid) {
    return db.getDbClient().userPermissionDao().selectEntityPermissionsOfUser(db.getSession(), user.getUuid(), entityUuid);
  }

  private static List<GlobalPermission> toListOfGlobalPermissions(List<String> keys) {
    return keys
      .stream()
      .map(GlobalPermission::fromKey)
      .toList();
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
      .setExpirationDate(random.nextLong(Long.MAX_VALUE));
    stream(populators).forEach(p -> p.accept(dto));
    db.getDbClient().sessionTokensDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  public final UserDismissedMessageDto insertUserDismissedMessageOnProject(UserDto userDto, ProjectDto projectDto, MessageType messageType) {
    UserDismissedMessageDto dto = new UserDismissedMessageDto()
      .setUuid(Uuids.create())
      .setUserUuid(userDto.getUuid())
      .setProjectUuid(projectDto.getUuid())
      .setMessageType(messageType);
    db.getDbClient().userDismissedMessagesDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  public final UserDismissedMessageDto insertUserDismissedMessageOnInstance(UserDto userDto, MessageType messageType) {
    UserDismissedMessageDto dto = new UserDismissedMessageDto()
      .setUuid(Uuids.create())
      .setUserUuid(userDto.getUuid())
      .setMessageType(messageType);
    db.getDbClient().userDismissedMessagesDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

}
