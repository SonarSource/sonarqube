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
package org.sonar.server.organization;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.DefaultTemplates;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationMemberDto;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.permission.UserPermissionDto;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.DefinedQProfile;
import org.sonar.server.qualityprofile.DefinedQProfileCreation;
import org.sonar.server.qualityprofile.DefinedQProfileRepository;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupCreator;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.permission.OrganizationPermission.SCAN;
import static org.sonar.server.organization.OrganizationCreation.NewOrganization.newOrganizationBuilder;

public class OrganizationCreationImpl implements OrganizationCreation {
  private static final Logger LOGGER = Loggers.get(OrganizationCreationImpl.class);

  private final DbClient dbClient;
  private final System2 system2;
  private final UuidFactory uuidFactory;
  private final OrganizationValidation organizationValidation;
  private final Settings settings;
  private final DefinedQProfileRepository definedQProfileRepository;
  private final DefinedQProfileCreation definedQProfileCreation;
  private final DefaultGroupCreator defaultGroupCreator;
  private final ActiveRuleIndexer activeRuleIndexer;
  private final UserIndexer userIndexer;

  public OrganizationCreationImpl(DbClient dbClient, System2 system2, UuidFactory uuidFactory,
    OrganizationValidation organizationValidation, Settings settings, UserIndexer userIndexer,
    DefinedQProfileRepository definedQProfileRepository, DefinedQProfileCreation definedQProfileCreation, DefaultGroupCreator defaultGroupCreator,
    ActiveRuleIndexer activeRuleIndexer) {
    this.dbClient = dbClient;
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
    this.organizationValidation = organizationValidation;
    this.settings = settings;
    this.userIndexer = userIndexer;
    this.definedQProfileRepository = definedQProfileRepository;
    this.definedQProfileCreation = definedQProfileCreation;
    this.defaultGroupCreator = defaultGroupCreator;
    this.activeRuleIndexer = activeRuleIndexer;
  }

  @Override
  public OrganizationDto create(DbSession dbSession, UserDto userCreator, NewOrganization newOrganization) throws KeyConflictException {
    validate(newOrganization);
    String key = newOrganization.getKey();
    if (organizationKeyIsUsed(dbSession, key)) {
      throw new KeyConflictException(format("Organization key '%s' is already used", key));
    }

    OrganizationDto organization = insertOrganization(dbSession, newOrganization, dto -> {
    });
    insertOrganizationMember(dbSession, organization, userCreator.getId());
    GroupDto ownerGroup = insertOwnersGroup(dbSession, organization);
    GroupDto defaultGroup = defaultGroupCreator.create(dbSession, organization.getUuid());
    insertDefaultTemplateOnGroups(dbSession, organization, ownerGroup, defaultGroup);
    List<ActiveRuleChange> activeRuleChanges = insertQualityProfiles(dbSession, organization);
    addCurrentUserToGroup(dbSession, ownerGroup, userCreator.getId());
    addCurrentUserToGroup(dbSession, defaultGroup, userCreator.getId());

    dbSession.commit();

    // Elasticsearch is updated when DB session is committed
    userIndexer.index(userCreator.getLogin());
    activeRuleIndexer.index(activeRuleChanges);

    return organization;
  }

  @Override
  public Optional<OrganizationDto> createForUser(DbSession dbSession, UserDto newUser) {
    if (!isCreatePersonalOrgEnabled()) {
      return Optional.empty();
    }

    String nameOrLogin = nameOrLogin(newUser);
    NewOrganization newOrganization = newOrganizationBuilder()
      .setKey(organizationValidation.generateKeyFrom(newUser.getLogin()))
      .setName(toName(nameOrLogin))
      .setDescription(format(PERSONAL_ORGANIZATION_DESCRIPTION_PATTERN, nameOrLogin))
      .build();
    checkState(!organizationKeyIsUsed(dbSession, newOrganization.getKey()),
      "Can't create organization with key '%s' for new user '%s' because an organization with this key already exists",
      newOrganization.getKey(),
      newUser.getLogin());

    OrganizationDto organization = insertOrganization(dbSession, newOrganization,
      dto -> dto.setGuarded(true).setUserId(newUser.getId()));
    insertOrganizationMember(dbSession, organization, newUser.getId());
    GroupDto defaultGroup = defaultGroupCreator.create(dbSession, organization.getUuid());
    OrganizationPermission.all()
      .forEach(p -> insertUserPermissions(dbSession, newUser, organization, p));
    insertPersonalOrgDefaultTemplate(dbSession, organization, defaultGroup);
    List<ActiveRuleChange> activeRuleChanges = insertQualityProfiles(dbSession, organization);
    addCurrentUserToGroup(dbSession, defaultGroup, newUser.getId());

    dbSession.commit();

    // Elasticsearch is updated when DB session is committed
    activeRuleIndexer.index(activeRuleChanges);
    userIndexer.index(newUser.getLogin());

    return Optional.of(organization);
  }

  private static String nameOrLogin(UserDto newUser) {
    String name = newUser.getName();
    if (name == null || name.isEmpty()) {
      return newUser.getLogin();
    }
    return name;
  }

  private String toName(String login) {
    String name = login.substring(0, Math.min(login.length(), OrganizationValidation.NAME_MAX_LENGTH));
    // should not happen has login can't be less than 2 chars, but we call it for safety
    organizationValidation.checkName(name);
    return name;
  }

  private boolean isCreatePersonalOrgEnabled() {
    return settings.getBoolean(CorePropertyDefinitions.ORGANIZATIONS_CREATE_PERSONAL_ORG);
  }

  private void validate(NewOrganization newOrganization) {
    requireNonNull(newOrganization, "newOrganization can't be null");
    organizationValidation.checkName(newOrganization.getName());
    organizationValidation.checkKey(newOrganization.getKey());
    organizationValidation.checkDescription(newOrganization.getDescription());
    organizationValidation.checkUrl(newOrganization.getUrl());
    organizationValidation.checkAvatar(newOrganization.getAvatar());
  }

  private OrganizationDto insertOrganization(DbSession dbSession, NewOrganization newOrganization, Consumer<OrganizationDto> extendCreation) {
    OrganizationDto res = new OrganizationDto()
      .setUuid(uuidFactory.create())
      .setName(newOrganization.getName())
      .setKey(newOrganization.getKey())
      .setDescription(newOrganization.getDescription())
      .setUrl(newOrganization.getUrl())
      .setAvatarUrl(newOrganization.getAvatar());
    extendCreation.accept(res);
    dbClient.organizationDao().insert(dbSession, res, false);
    return res;
  }

  private boolean organizationKeyIsUsed(DbSession dbSession, String key) {
    return dbClient.organizationDao().selectByKey(dbSession, key).isPresent();
  }

  private void insertDefaultTemplateOnGroups(DbSession dbSession, OrganizationDto organizationDto, GroupDto ownerGroup, GroupDto defaultGroup) {
    Date now = new Date(system2.now());
    PermissionTemplateDto permissionTemplateDto = dbClient.permissionTemplateDao().insert(
      dbSession,
      new PermissionTemplateDto()
        .setOrganizationUuid(organizationDto.getUuid())
        .setUuid(uuidFactory.create())
        .setName(PERM_TEMPLATE_NAME)
        .setDescription(format(PERM_TEMPLATE_DESCRIPTION_PATTERN, organizationDto.getName()))
        .setCreatedAt(now)
        .setUpdatedAt(now));

    insertGroupPermission(dbSession, permissionTemplateDto, ADMIN, ownerGroup);
    insertGroupPermission(dbSession, permissionTemplateDto, ISSUE_ADMIN, ownerGroup);
    insertGroupPermission(dbSession, permissionTemplateDto, SCAN.getKey(), ownerGroup);
    insertGroupPermission(dbSession, permissionTemplateDto, USER, defaultGroup);
    insertGroupPermission(dbSession, permissionTemplateDto, CODEVIEWER, defaultGroup);

    dbClient.organizationDao().setDefaultTemplates(
      dbSession,
      organizationDto.getUuid(),
      new DefaultTemplates().setProjectUuid(permissionTemplateDto.getUuid()));
  }

  private void insertPersonalOrgDefaultTemplate(DbSession dbSession, OrganizationDto organizationDto, GroupDto defaultGroup) {
    long now = system2.now();
    Date dateNow = new Date(now);
    PermissionTemplateDto permissionTemplateDto = dbClient.permissionTemplateDao().insert(
      dbSession,
      new PermissionTemplateDto()
        .setOrganizationUuid(organizationDto.getUuid())
        .setUuid(uuidFactory.create())
        .setName("Default template")
        .setDescription(format(PERM_TEMPLATE_DESCRIPTION_PATTERN, organizationDto.getName()))
        .setCreatedAt(dateNow)
        .setUpdatedAt(dateNow));

    insertProjectCreatorPermission(dbSession, permissionTemplateDto, ADMIN, now);
    insertProjectCreatorPermission(dbSession, permissionTemplateDto, ISSUE_ADMIN, now);
    insertProjectCreatorPermission(dbSession, permissionTemplateDto, SCAN.getKey(), now);
    insertGroupPermission(dbSession, permissionTemplateDto, USER, defaultGroup);
    insertGroupPermission(dbSession, permissionTemplateDto, CODEVIEWER, defaultGroup);
    insertGroupPermission(dbSession, permissionTemplateDto, USER, null);
    insertGroupPermission(dbSession, permissionTemplateDto, CODEVIEWER, null);

    dbClient.organizationDao().setDefaultTemplates(
      dbSession,
      organizationDto.getUuid(),
      new DefaultTemplates().setProjectUuid(permissionTemplateDto.getUuid()));
  }

  private void insertProjectCreatorPermission(DbSession dbSession, PermissionTemplateDto permissionTemplateDto, String permission, long now) {
    dbClient.permissionTemplateCharacteristicDao().insert(
      dbSession,
      new PermissionTemplateCharacteristicDto()
        .setTemplateId(permissionTemplateDto.getId())
        .setWithProjectCreator(true)
        .setPermission(permission)
        .setCreatedAt(now)
        .setUpdatedAt(now));
  }

  private void insertGroupPermission(DbSession dbSession, PermissionTemplateDto template, String permission, @Nullable GroupDto group) {
    dbClient.permissionTemplateDao().insertGroupPermission(dbSession, template.getId(), group == null ? null : group.getId(), permission);
  }

  private List<ActiveRuleChange> insertQualityProfiles(DbSession dbSession, OrganizationDto organization) {
    List<ActiveRuleChange> changes = new ArrayList<>();
    definedQProfileRepository.getQProfilesByLanguage().entrySet()
      .stream()
      .flatMap(entry -> entry.getValue().stream())
      .forEach(profile -> insertQualityProfile(dbSession, profile, organization, changes));
    return changes;
  }

  private void insertQualityProfile(DbSession dbSession, DefinedQProfile profile, OrganizationDto organization, List<ActiveRuleChange> changes) {
    LOGGER.debug("Creating quality profile {} for language {} for organization {}", profile.getName(), profile.getLanguage(), organization.getKey());

    definedQProfileCreation.create(dbSession, profile, organization, changes);
  }

  /**
   * Owners group has an hard coded name, a description based on the organization's name and has all global permissions.
   */
  private GroupDto insertOwnersGroup(DbSession dbSession, OrganizationDto organization) {
    GroupDto group = dbClient.groupDao().insert(dbSession, new GroupDto()
      .setOrganizationUuid(organization.getUuid())
      .setName(OWNERS_GROUP_NAME)
      .setDescription(format(OWNERS_GROUP_DESCRIPTION_PATTERN, organization.getName())));
    OrganizationPermission.all().forEach(p -> addPermissionToGroup(dbSession, group, p));
    return group;
  }

  private void addPermissionToGroup(DbSession dbSession, GroupDto group, OrganizationPermission permission) {
    dbClient.groupPermissionDao().insert(
      dbSession,
      new GroupPermissionDto()
        .setOrganizationUuid(group.getOrganizationUuid())
        .setGroupId(group.getId())
        .setRole(permission.getKey()));
  }

  private void insertUserPermissions(DbSession dbSession, UserDto userDto, OrganizationDto organization, OrganizationPermission permission) {
    dbClient.userPermissionDao().insert(
      dbSession,
      new UserPermissionDto(organization.getUuid(), permission.getKey(), userDto.getId(), null));
  }

  private void addCurrentUserToGroup(DbSession dbSession, GroupDto group, int createUserId) {
    dbClient.userGroupDao().insert(
      dbSession,
      new UserGroupDto().setGroupId(group.getId()).setUserId(createUserId));
  }

  private void insertOrganizationMember(DbSession dbSession, OrganizationDto organizationDto, int userId) {
    dbClient.organizationMemberDao().insert(dbSession, new OrganizationMemberDto()
      .setOrganizationUuid(organizationDto.getUuid())
      .setUserId(userId));
  }
}
