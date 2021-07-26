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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.System2;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.Uuids;
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
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.qualityprofile.DefaultQProfileDto;
import org.sonar.db.qualityprofile.OrgQProfileDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.qualityprofile.BuiltInQProfile;
import org.sonar.server.qualityprofile.BuiltInQProfileRepository;
import org.sonar.server.qualityprofile.QProfileName;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupCreator;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.SECURITYHOTSPOT_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.db.organization.OrganizationDto.Subscription.FREE;
import static org.sonar.db.permission.OrganizationPermission.SCAN;
import static org.sonar.server.organization.OrganizationUpdater.NewOrganization.newOrganizationBuilder;

public class OrganizationUpdaterImpl implements OrganizationUpdater {

  private final DbClient dbClient;
  private final System2 system2;
  private final UuidFactory uuidFactory;
  private final OrganizationValidation organizationValidation;
  private final Configuration config;
  private final BuiltInQProfileRepository builtInQProfileRepository;
  private final DefaultGroupCreator defaultGroupCreator;
  private final UserIndexer userIndexer;
  private final PermissionService permissionService;

  public OrganizationUpdaterImpl(DbClient dbClient, System2 system2, UuidFactory uuidFactory,
    OrganizationValidation organizationValidation, Configuration config, UserIndexer userIndexer,
    BuiltInQProfileRepository builtInQProfileRepository, DefaultGroupCreator defaultGroupCreator, PermissionService permissionService) {
    this.dbClient = dbClient;
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
    this.organizationValidation = organizationValidation;
    this.config = config;
    this.userIndexer = userIndexer;
    this.builtInQProfileRepository = builtInQProfileRepository;
    this.defaultGroupCreator = defaultGroupCreator;
    this.permissionService = permissionService;
  }

  @Override
  public OrganizationDto create(DbSession dbSession, UserDto userCreator, NewOrganization newOrganization, Consumer<OrganizationDto> beforeCommit) throws KeyConflictException {
    validate(newOrganization);
    String key = newOrganization.getKey();
    if (organizationKeyIsUsed(dbSession, key)) {
      throw new KeyConflictException(format("Organization key '%s' is already used", key));
    }

    QualityGateDto builtInQualityGate = dbClient.qualityGateDao().selectBuiltIn(dbSession);
    OrganizationDto organization = insertOrganization(dbSession, newOrganization, builtInQualityGate);
    beforeCommit.accept(organization);
    insertOrganizationMember(dbSession, organization, userCreator.getUuid());
    dbClient.qualityGateDao().associate(dbSession, uuidFactory.create(), organization, builtInQualityGate);
    GroupDto ownerGroup = insertOwnersGroup(dbSession, organization);
    GroupDto defaultGroup = defaultGroupCreator.create(dbSession, organization.getUuid());
    insertDefaultTemplateOnGroups(dbSession, organization, ownerGroup, defaultGroup);
    addCurrentUserToGroup(dbSession, ownerGroup, userCreator.getUuid());
    addCurrentUserToGroup(dbSession, defaultGroup, userCreator.getUuid());
    try (DbSession batchDbSession = dbClient.openSession(true)) {
      insertQualityProfiles(dbSession, batchDbSession, organization);
      batchDbSession.commit();

      // Elasticsearch is updated when DB session is committed
      userIndexer.commitAndIndex(dbSession, userCreator);

      return organization;
    }
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
    checkKey(dbSession, newOrganization.getKey());

    QualityGateDto builtInQualityGate = dbClient.qualityGateDao().selectBuiltIn(dbSession);
    OrganizationDto organization = insertOrganization(dbSession, newOrganization, builtInQualityGate);
    // dbClient.userDao().update(dbSession, newUser.setOrganizationUuid(organization.getUuid()));
    insertOrganizationMember(dbSession, organization, newUser.getUuid());
    GroupDto defaultGroup = defaultGroupCreator.create(dbSession, organization.getUuid());
    dbClient.qualityGateDao().associate(dbSession, uuidFactory.create(), organization, builtInQualityGate);
    permissionService.getAllOrganizationPermissions()
            .forEach(p -> insertUserPermissions(dbSession, newUser, organization, p));
    insertPersonalOrgDefaultTemplate(dbSession, organization, defaultGroup);
    try (DbSession batchDbSession = dbClient.openSession(true)) {
      insertQualityProfiles(dbSession, batchDbSession, organization);
      addCurrentUserToGroup(dbSession, defaultGroup, newUser.getUuid());

      batchDbSession.commit();

      // Elasticsearch is updated when DB session is committed
      userIndexer.commitAndIndex(dbSession, newUser);

      return Optional.of(organization);
    }
  }

  @Override
  public void updateOrganizationKey(DbSession dbSession, OrganizationDto organization, String newKey) {
    String sanitizedKey = organizationValidation.generateKeyFrom(newKey);
    if (organization.getKey().equals(sanitizedKey)) {
      return;
    }
    checkKey(dbSession, sanitizedKey);
    dbClient.organizationDao().update(dbSession, organization.setKey(sanitizedKey));
  }

  private void checkKey(DbSession dbSession, String key) {
    checkState(!organizationKeyIsUsed(dbSession, key),
      "Can't create organization with key '%s' because an organization with this key already exists", key);
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
    return config.getBoolean(CorePropertyDefinitions.ORGANIZATIONS_CREATE_PERSONAL_ORG).orElse(false);
  }

  private void validate(NewOrganization newOrganization) {
    requireNonNull(newOrganization, "newOrganization can't be null");
    organizationValidation.checkName(newOrganization.getName());
    organizationValidation.checkKey(newOrganization.getKey());
    organizationValidation.checkDescription(newOrganization.getDescription());
    organizationValidation.checkUrl(newOrganization.getUrl());
    organizationValidation.checkAvatar(newOrganization.getAvatar());
  }

  private OrganizationDto insertOrganization(DbSession dbSession, NewOrganization newOrganization, QualityGateDto builtInQualityGate, Consumer<OrganizationDto>... extendCreation) {
    OrganizationDto res = new OrganizationDto()
      .setUuid(uuidFactory.create())
      .setName(newOrganization.getName())
      .setKey(newOrganization.getKey())
      .setDescription(newOrganization.getDescription())
      .setUrl(newOrganization.getUrl())
      .setDefaultQualityGateUuid(builtInQualityGate.getUuid())
      .setAvatarUrl(newOrganization.getAvatar())
      .setSubscription(FREE);
    Arrays.stream(extendCreation).forEach(c -> c.accept(res));
    dbClient.organizationDao().insert(dbSession, res, false);
    
    Optional<Boolean> publicVisibility = config.getBoolean(CorePropertyDefinitions.ORGANIZATIONS_DEFAULT_PUBLIC_VISIBILITY);
    if ( publicVisibility.isPresent() && publicVisibility.get() == false ) {
    	dbClient.organizationDao().setNewProjectPrivate(dbSession, res, true);
    }
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
    insertGroupPermission(dbSession, permissionTemplateDto, SCAN.getKey(), ownerGroup);
    insertGroupPermission(dbSession, permissionTemplateDto, USER, defaultGroup);
    insertGroupPermission(dbSession, permissionTemplateDto, CODEVIEWER, defaultGroup);
    insertGroupPermission(dbSession, permissionTemplateDto, ISSUE_ADMIN, defaultGroup);
    insertGroupPermission(dbSession, permissionTemplateDto, SECURITYHOTSPOT_ADMIN, defaultGroup);

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
    insertProjectCreatorPermission(dbSession, permissionTemplateDto, SECURITYHOTSPOT_ADMIN, now);
    insertProjectCreatorPermission(dbSession, permissionTemplateDto, SCAN.getKey(), now);
    insertGroupPermission(dbSession, permissionTemplateDto, USER, defaultGroup);
    insertGroupPermission(dbSession, permissionTemplateDto, CODEVIEWER, defaultGroup);

    dbClient.organizationDao().setDefaultTemplates(
            dbSession,
            organizationDto.getUuid(),
            new DefaultTemplates().setProjectUuid(permissionTemplateDto.getUuid()));
  }

  private void insertProjectCreatorPermission(DbSession dbSession, PermissionTemplateDto permissionTemplateDto, String permission, long now) {
    dbClient.permissionTemplateCharacteristicDao().insert(
            dbSession,
            new PermissionTemplateCharacteristicDto()
                    .setUuid(Uuids.create())
                    .setTemplateUuid(permissionTemplateDto.getUuid())
                    .setWithProjectCreator(true)
                    .setPermission(permission)
                    .setCreatedAt(now)
                    .setUpdatedAt(now));
  }

  private void insertGroupPermission(DbSession dbSession, PermissionTemplateDto template, String permission, @Nullable GroupDto group) {
    dbClient.permissionTemplateDao().insertGroupPermission(dbSession, template.getUuid(), group == null ? null : group.getUuid(), permission);
  }

  private void insertQualityProfiles(DbSession dbSession, DbSession batchDbSession, OrganizationDto organization) {
    Map<QProfileName, BuiltInQProfile> builtInsPerName = builtInQProfileRepository.get().stream()
      .collect(uniqueIndex(BuiltInQProfile::getQProfileName));

    List<DefaultQProfileDto> defaults = new ArrayList<>();
    dbClient.qualityProfileDao().selectBuiltInRuleProfiles(dbSession).forEach(rulesProfile -> {
      OrgQProfileDto dto = new OrgQProfileDto()
        .setOrganizationUuid(organization.getUuid())
        .setRulesProfileUuid(rulesProfile.getUuid())
        .setUuid(uuidFactory.create());

      QProfileName name = new QProfileName(rulesProfile.getLanguage(), rulesProfile.getName());
      BuiltInQProfile builtIn = builtInsPerName.get(name);
      if (builtIn == null || builtIn.isDefault()) {
        // If builtIn == null, the plugin has been removed
        // rows of table default_qprofiles must be inserted after org_qprofiles
        // in order to benefit from batch SQL inserts
        defaults.add(new DefaultQProfileDto()
          .setQProfileUuid(dto.getUuid())
          .setOrganizationUuid(organization.getUuid())
          .setLanguage(rulesProfile.getLanguage()));
      }

      dbClient.qualityProfileDao().insert(batchDbSession, dto);
    });

    defaults.forEach(defaultQProfileDto -> dbClient.defaultQProfileDao().insertOrUpdate(dbSession, defaultQProfileDto));
  }

  /**
   * Owners group has an hard coded name, a description based on the organization's name and has all global permissions.
   */
  private GroupDto insertOwnersGroup(DbSession dbSession, OrganizationDto organization) {
    GroupDto group = dbClient.groupDao().insert(dbSession, new GroupDto()
      .setUuid(uuidFactory.create())
      .setOrganizationUuid(organization.getUuid())
      .setName(OWNERS_GROUP_NAME)
      .setDescription(OWNERS_GROUP_DESCRIPTION));
    permissionService.getAllOrganizationPermissions().forEach(p -> addPermissionToGroup(dbSession, group, p));
    return group;
  }

  private void addPermissionToGroup(DbSession dbSession, GroupDto group, OrganizationPermission permission) {
    dbClient.groupPermissionDao().insert(
      dbSession,
      new GroupPermissionDto()
        .setUuid(uuidFactory.create())
        .setOrganizationUuid(group.getOrganizationUuid())
        .setGroupUuid(group.getUuid())
        .setRole(permission.getKey()));
  }

  private void insertUserPermissions(DbSession dbSession, UserDto userDto, OrganizationDto organization, OrganizationPermission permission) {
    dbClient.userPermissionDao().insert(
            dbSession,
            new UserPermissionDto(Uuids.create(), organization.getUuid(), permission.getKey(), userDto.getUuid(), null));
  }

  private void addCurrentUserToGroup(DbSession dbSession, GroupDto group, String createUserUuid) {
    dbClient.userGroupDao().insert(
      dbSession,
      new UserGroupDto().setGroupUuid(group.getUuid()).setUserUuid(createUserUuid));
  }

  private void insertOrganizationMember(DbSession dbSession, OrganizationDto organizationDto, String userUuid) {
    dbClient.organizationMemberDao().insert(dbSession, new OrganizationMemberDto()
      .setOrganizationUuid(organizationDto.getUuid())
      .setUserUuid(userUuid));
  }
}
