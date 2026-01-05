/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.permission.template;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.PermissionTemplateNewValue;
import org.sonar.db.permission.CountPerEntityPermission;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.ProjectPermission;

import static java.lang.String.format;
import static org.sonar.api.security.DefaultGroups.ANYONE;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeInputsWithoutOutput;

public class PermissionTemplateDao implements Dao {

  private static final String ANYONE_GROUP_PARAMETER = "anyoneGroup";

  private final System2 system;
  private final UuidFactory uuidFactory;
  private final AuditPersister auditPersister;

  public PermissionTemplateDao(UuidFactory uuidFactory, System2 system, AuditPersister auditPersister) {
    this.uuidFactory = uuidFactory;
    this.system = system;
    this.auditPersister = auditPersister;
  }

  /**
   * @return a paginated list of user logins.
   */
  public List<String> selectUserLoginsByQueryAndTemplate(DbSession session, PermissionQuery query, String templateUuid) {
    return mapper(session).selectUserLoginsByQueryAndTemplate(query, templateUuid, Pagination.forPage(query.getPageIndex()).andSize(query.getPageSize()));
  }

  public int countUserLoginsByQueryAndTemplate(DbSession session, PermissionQuery query, String templateUuid) {
    return mapper(session).countUserLoginsByQueryAndTemplate(query, templateUuid);
  }

  public List<PermissionTemplateUserDto> selectUserPermissionsByTemplateIdAndUserLogins(DbSession dbSession, String templateUuid, List<String> logins) {
    return executeLargeInputs(logins, l -> mapper(dbSession).selectUserPermissionsByTemplateUuidAndUserLogins(templateUuid, l));
  }

  public List<PermissionTemplateUserDto> selectUserPermissionsByTemplateId(DbSession dbSession, String templateUuid) {
    return mapper(dbSession).selectUserPermissionsByTemplateUuidAndUserLogins(templateUuid, Collections.emptyList());
  }

  public List<String> selectGroupNamesByQueryAndTemplate(DbSession session, PermissionQuery query, String templateUuid) {
    return mapper(session).selectGroupNamesByQueryAndTemplate(templateUuid, query, Pagination.forPage(query.getPageIndex()).andSize(query.getPageSize()));
  }

  public int countGroupNamesByQueryAndTemplate(DbSession session, PermissionQuery query, String templateUuid) {
    return mapper(session).countGroupNamesByQueryAndTemplate(query, templateUuid);
  }

  public List<PermissionTemplateGroupDto> selectGroupPermissionsByTemplateIdAndGroupNames(DbSession dbSession, String templateUuid, List<String> groups) {
    return executeLargeInputs(groups, g -> mapper(dbSession).selectGroupPermissionsByTemplateUuidAndGroupNames(templateUuid, g));
  }

  public List<PermissionTemplateGroupDto> selectGroupPermissionsByTemplateUuid(DbSession dbSession, String templateUuid) {
    return mapper(dbSession).selectGroupPermissionsByTemplateUuidAndGroupNames(templateUuid, Collections.emptyList());
  }

  /**
   * @return {@code true} if template contains groups that are granted with {@code permission}, else {@code false}
   */
  public boolean hasGroupsWithPermission(DbSession dbSession, String templateUuid, String permission, @Nullable String groupUuid) {
    return mapper(dbSession).countGroupsWithPermission(templateUuid, permission, groupUuid) > 0;
  }

  @CheckForNull
  public PermissionTemplateDto selectByUuid(DbSession session, String templateUuid) {
    return mapper(session).selectByUuid(templateUuid);
  }

  public List<PermissionTemplateDto> selectAll(DbSession session, @Nullable String nameMatch) {
    String upperCaseNameLikeSql = nameMatch != null ? toUppercaseSqlQuery(nameMatch) : null;
    return mapper(session).selectAll(upperCaseNameLikeSql);
  }

  private static String toUppercaseSqlQuery(String nameMatch) {
    String wildcard = "%";
    return format("%s%s%s", wildcard, nameMatch.toUpperCase(Locale.ENGLISH), wildcard);
  }

  public PermissionTemplateDto insert(DbSession session, PermissionTemplateDto dto) {
    if (dto.getUuid() == null) {
      dto.setUuid(uuidFactory.create());
    }
    mapper(session).insert(dto);
    auditPersister.addPermissionTemplate(session, new PermissionTemplateNewValue(dto.getUuid(), dto.getName()));

    return dto;
  }

  /**
   * Each row returns a #{@link CountPerEntityPermission}
   */
  public void usersCountByTemplateUuidAndPermission(DbSession dbSession, List<String> templateUuids, ResultHandler<CountByTemplateAndPermissionDto> resultHandler) {
    Map<String, Object> parameters = HashMap.newHashMap(1);

    executeLargeInputsWithoutOutput(
      templateUuids,
      partitionedTemplateUuids -> {
        parameters.put("templateUuids", partitionedTemplateUuids);
        mapper(dbSession).usersCountByTemplateUuidAndPermission(parameters, resultHandler);
      });
  }

  /**
   * Each row returns a #{@link CountPerEntityPermission}
   */
  public void groupsCountByTemplateUuidAndPermission(DbSession dbSession, List<String> templateUuids, ResultHandler<CountByTemplateAndPermissionDto> resultHandler) {
    Map<String, Object> parameters = HashMap.newHashMap(2);
    parameters.put(ANYONE_GROUP_PARAMETER, ANYONE);

    executeLargeInputsWithoutOutput(
      templateUuids,
      partitionedTemplateUuids -> {
        parameters.put("templateUuids", partitionedTemplateUuids);
        mapper(dbSession).groupsCountByTemplateUuidAndPermission(parameters, resultHandler);
      });
  }

  public List<PermissionTemplateGroupDto> selectAllGroupPermissionTemplatesByGroupUuid(DbSession dbSession, String groupUuid) {
    return mapper(dbSession).selectAllGroupPermissionTemplatesByGroupUuid(groupUuid);
  }

  public void deleteByUuid(DbSession session, String templateUuid, String templateName) {
    PermissionTemplateMapper mapper = mapper(session);
    mapper.deleteUserPermissionsByTemplateUuid(templateUuid);
    mapper.deleteGroupPermissionsByTemplateUuid(templateUuid);
    session.getMapper(PermissionTemplateCharacteristicMapper.class).deleteByTemplateUuid(templateUuid);
    int deletedRows = mapper.deleteByUuid(templateUuid);

    if (deletedRows > 0) {
      auditPersister.deletePermissionTemplate(session, new PermissionTemplateNewValue(templateUuid, templateName));
    }
  }

  public PermissionTemplateDto update(DbSession session, PermissionTemplateDto permissionTemplate) {
    mapper(session).update(permissionTemplate);
    auditPersister.updatePermissionTemplate(session, new PermissionTemplateNewValue(permissionTemplate));
    return permissionTemplate;
  }

  public void insertUserPermission(DbSession session, String templateUuid, String userUuid, ProjectPermission permission,
    String templateName, String userLogin) {
    insertUserPermission(session, templateUuid, userUuid, permission.getKey(), templateName, userLogin);
  }

  public void insertUserPermission(DbSession session, String templateUuid, String userUuid, String permission,
    String templateName, String userLogin) {
    PermissionTemplateUserDto permissionTemplateUser = new PermissionTemplateUserDto()
      .setUuid(uuidFactory.create())
      .setTemplateUuid(templateUuid)
      .setUserUuid(userUuid)
      .setPermission(permission)
      .setCreatedAt(now())
      .setUpdatedAt(now());

    mapper(session).insertUserPermission(permissionTemplateUser);

    auditPersister.addUserToPermissionTemplate(session, new PermissionTemplateNewValue(templateUuid, templateName, permission, userUuid, userLogin, null, null));

    session.commit();
  }

  public void deleteUserPermission(DbSession session, String templateUuid, String userUuid, ProjectPermission permission,
    String templateName, String userLogin) {
    deleteUserPermission(session, templateUuid, userUuid, permission.getKey(), templateName, userLogin);
  }

  public void deleteUserPermission(DbSession session, String templateUuid, String userUuid, String permission,
    String templateName, String userLogin) {
    PermissionTemplateUserDto permissionTemplateUser = new PermissionTemplateUserDto()
      .setTemplateUuid(templateUuid)
      .setPermission(permission)
      .setUserUuid(userUuid);
    int deletedRows = mapper(session).deleteUserPermission(permissionTemplateUser);

    if (deletedRows > 0) {
      auditPersister.deleteUserFromPermissionTemplate(session, new PermissionTemplateNewValue(templateUuid, templateName, permission, userUuid, userLogin, null, null));
    }

    session.commit();
  }

  public void deleteUserPermissionsByUserUuid(DbSession dbSession, String userUuid, String userLogin) {
    int deletedRows = mapper(dbSession).deleteUserPermissionsByUserUuid(userUuid);

    if (deletedRows > 0) {
      auditPersister.deleteUserFromPermissionTemplate(dbSession, new PermissionTemplateNewValue(null, null, null, userUuid, userLogin, null, null));
    }
  }

  public void insertGroupPermission(DbSession session, String templateUuid, @Nullable String groupUuid, ProjectPermission permission,
    String templateName, @Nullable String groupName) {
    insertGroupPermission(session, templateUuid, groupUuid, permission.getKey(), templateName, groupName);
  }

  public void insertGroupPermission(DbSession session, String templateUuid, @Nullable String groupUuid, String permission,
    String templateName, @Nullable String groupName) {
    PermissionTemplateGroupDto permissionTemplateGroup = new PermissionTemplateGroupDto()
      .setUuid(uuidFactory.create())
      .setTemplateUuid(templateUuid)
      .setPermission(permission)
      .setGroupUuid(groupUuid)
      .setCreatedAt(now())
      .setUpdatedAt(now());
    mapper(session).insertGroupPermission(permissionTemplateGroup);

    auditPersister.addGroupToPermissionTemplate(session, new PermissionTemplateNewValue(templateUuid, templateName, permission, null, null, groupUuid, groupName));
  }

  public void insertGroupPermission(DbSession session, PermissionTemplateGroupDto permissionTemplateGroup, String templateName) {
    mapper(session).insertGroupPermission(permissionTemplateGroup);

    auditPersister.addGroupToPermissionTemplate(session, new PermissionTemplateNewValue(permissionTemplateGroup.getTemplateUuid(), templateName,
      permissionTemplateGroup.getPermission(), null, null, permissionTemplateGroup.getGroupUuid(), permissionTemplateGroup.getGroupName()));
  }

  public void deleteGroupPermission(DbSession session, String templateUuid, @Nullable String groupUuid, ProjectPermission permission, String templateName,
    @Nullable String groupName) {
    deleteGroupPermission(session, templateUuid, groupUuid, permission.getKey(), templateName, groupName);
  }

  public void deleteGroupPermission(DbSession session, String templateUuid, @Nullable String groupUuid, String permission, String templateName,
    @Nullable String groupName) {
    PermissionTemplateGroupDto permissionTemplateGroup = new PermissionTemplateGroupDto()
      .setTemplateUuid(templateUuid)
      .setPermission(permission)
      .setGroupUuid(groupUuid);
    int deletedRows = mapper(session).deleteGroupPermission(permissionTemplateGroup);

    if (deletedRows > 0) {
      auditPersister.deleteGroupFromPermissionTemplate(session, new PermissionTemplateNewValue(permissionTemplateGroup.getTemplateUuid(), templateName,
        permissionTemplateGroup.getPermission(), null, null, permissionTemplateGroup.getGroupUuid(), groupName));
    }

    session.commit();
  }

  public PermissionTemplateDto selectByName(DbSession dbSession, String name) {
    return mapper(dbSession).selectByName(name.toUpperCase(Locale.ENGLISH));
  }

  public List<String> selectPotentialPermissionsByUserUuidAndTemplateUuid(DbSession dbSession, @Nullable String currentUserUuid, String templateUuid) {
    return mapper(dbSession).selectPotentialPermissionsByUserUuidAndTemplateUuid(currentUserUuid, templateUuid);
  }

  /**
   * Remove a group from all templates (used when removing a group)
   */
  public void deleteByGroup(DbSession session, String groupUuid, String groupName) {
    int deletedRows = session.getMapper(PermissionTemplateMapper.class).deleteByGroupUuid(groupUuid);

    if (deletedRows > 0) {
      auditPersister.deleteGroupFromPermissionTemplate(session, new PermissionTemplateNewValue(null, null, null, null, null, groupUuid, groupName));
    }
  }

  private Date now() {
    return new Date(system.now());
  }

  private static PermissionTemplateMapper mapper(DbSession session) {
    return session.getMapper(PermissionTemplateMapper.class);
  }
}
