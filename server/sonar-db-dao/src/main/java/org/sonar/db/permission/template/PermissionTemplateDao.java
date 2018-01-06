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
import org.apache.ibatis.session.RowBounds;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.permission.CountPerProjectPermission;
import org.sonar.db.permission.PermissionQuery;

import static java.lang.String.format;
import static org.sonar.api.security.DefaultGroups.ANYONE;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeInputsWithoutOutput;

public class PermissionTemplateDao implements Dao {

  private static final String ANYONE_GROUP_PARAMETER = "anyoneGroup";

  private final System2 system;

  public PermissionTemplateDao(System2 system) {
    this.system = system;
  }

  /**
   * @return a paginated list of user logins.
   */
  public List<String> selectUserLoginsByQueryAndTemplate(DbSession session, PermissionQuery query, long templateId) {
    return mapper(session).selectUserLoginsByQueryAndTemplate(query, templateId, new RowBounds(query.getPageOffset(), query.getPageSize()));
  }

  public int countUserLoginsByQueryAndTemplate(DbSession session, PermissionQuery query, long templateId) {
    return mapper(session).countUserLoginsByQueryAndTemplate(query, templateId);
  }

  public List<PermissionTemplateUserDto> selectUserPermissionsByTemplateIdAndUserLogins(DbSession dbSession, long templateId, List<String> logins) {
    return executeLargeInputs(logins, l -> mapper(dbSession).selectUserPermissionsByTemplateIdAndUserLogins(templateId, l));
  }

  public List<PermissionTemplateUserDto> selectUserPermissionsByTemplateId(DbSession dbSession, long templateId) {
    return mapper(dbSession).selectUserPermissionsByTemplateIdAndUserLogins(templateId, Collections.emptyList());
  }

  public List<String> selectGroupNamesByQueryAndTemplate(DbSession session, PermissionQuery query, long templateId) {
    return mapper(session).selectGroupNamesByQueryAndTemplate(templateId, query, new RowBounds(query.getPageOffset(), query.getPageSize()));
  }

  public int countGroupNamesByQueryAndTemplate(DbSession session, PermissionQuery query, String organizationUuid, long templateId) {
    return mapper(session).countGroupNamesByQueryAndTemplate(organizationUuid, query, templateId);
  }

  public List<PermissionTemplateGroupDto> selectGroupPermissionsByTemplateIdAndGroupNames(DbSession dbSession, long templateId, List<String> groups) {
    return executeLargeInputs(groups, g -> mapper(dbSession).selectGroupPermissionsByTemplateIdAndGroupNames(templateId, g));
  }

  public List<PermissionTemplateGroupDto> selectGroupPermissionsByTemplateId(DbSession dbSession, long templateId) {
    return mapper(dbSession).selectGroupPermissionsByTemplateIdAndGroupNames(templateId, Collections.emptyList());
  }

  /**
   * @return {@code true} if template contains groups that are granted with {@code permission}, else {@code false}
   */
  public boolean hasGroupsWithPermission(DbSession dbSession, long templateId, String permission, @Nullable Integer groupId) {
    return mapper(dbSession).countGroupsWithPermission(templateId, permission, groupId) > 0;
  }

  @CheckForNull
  public PermissionTemplateDto selectByUuid(DbSession session, String templateUuid) {
    return mapper(session).selectByUuid(templateUuid);
  }

  public List<PermissionTemplateDto> selectAll(DbSession session, String organizationUuid, @Nullable String nameMatch) {
    String upperCaseNameLikeSql = nameMatch != null ? toUppercaseSqlQuery(nameMatch) : null;
    return mapper(session).selectAll(organizationUuid, upperCaseNameLikeSql);
  }

  private static String toUppercaseSqlQuery(String nameMatch) {
    String wildcard = "%";
    return format("%s%s%s", wildcard, nameMatch.toUpperCase(Locale.ENGLISH), wildcard);
  }

  public PermissionTemplateDto insert(DbSession session, PermissionTemplateDto dto) {
    mapper(session).insert(dto);
    return dto;
  }

  /**
   * Each row returns a #{@link CountPerProjectPermission}
   */
  public void usersCountByTemplateIdAndPermission(DbSession dbSession, List<Long> templateIds, ResultHandler<CountByTemplateAndPermissionDto> resultHandler) {
    Map<String, Object> parameters = new HashMap<>(1);

    executeLargeInputsWithoutOutput(
      templateIds,
      partitionedTemplateIds -> {
        parameters.put("templateIds", partitionedTemplateIds);
        mapper(dbSession).usersCountByTemplateIdAndPermission(parameters, resultHandler);
      });
  }

  /**
   * Each row returns a #{@link CountPerProjectPermission}
   */
  public void groupsCountByTemplateIdAndPermission(DbSession dbSession, List<Long> templateIds, ResultHandler<CountByTemplateAndPermissionDto> resultHandler) {
    Map<String, Object> parameters = new HashMap<>(2);
    parameters.put(ANYONE_GROUP_PARAMETER, ANYONE);

    executeLargeInputsWithoutOutput(
      templateIds,
      partitionedTemplateIds -> {
        parameters.put("templateIds", partitionedTemplateIds);
        mapper(dbSession).groupsCountByTemplateIdAndPermission(parameters, resultHandler);
      });
  }

  public List<PermissionTemplateGroupDto> selectAllGroupPermissionTemplatesByGroupId(DbSession dbSession, long groupId) {
    return mapper(dbSession).selectAllGroupPermissionTemplatesByGroupId(groupId);
  }

  public void deleteById(DbSession session, long templateId) {
    PermissionTemplateMapper mapper = mapper(session);
    mapper.deleteUserPermissionsByTemplateId(templateId);
    mapper.deleteGroupPermissionsByTemplateId(templateId);
    session.getMapper(PermissionTemplateCharacteristicMapper.class).deleteByTemplateId(templateId);
    mapper.deleteById(templateId);
  }

  public PermissionTemplateDto update(DbSession session, PermissionTemplateDto permissionTemplate) {
    mapper(session).update(permissionTemplate);
    return permissionTemplate;
  }

  public void insertUserPermission(DbSession session, Long templateId, Integer userId, String permission) {
    PermissionTemplateUserDto permissionTemplateUser = new PermissionTemplateUserDto()
      .setTemplateId(templateId)
      .setUserId(userId)
      .setPermission(permission)
      .setCreatedAt(now())
      .setUpdatedAt(now());

    mapper(session).insertUserPermission(permissionTemplateUser);
    session.commit();
  }

  public void deleteUserPermission(DbSession session, Long templateId, Integer userId, String permission) {
    PermissionTemplateUserDto permissionTemplateUser = new PermissionTemplateUserDto()
      .setTemplateId(templateId)
      .setPermission(permission)
      .setUserId(userId);
    mapper(session).deleteUserPermission(permissionTemplateUser);
    session.commit();
  }

  public void deleteUserPermissionsByOrganization(DbSession dbSession, String organizationUuid, int userId) {
    mapper(dbSession).deleteUserPermissionsByOrganization(organizationUuid, userId);
  }

  public void deleteUserPermissionsByUserId(DbSession dbSession, int userId) {
    mapper(dbSession).deleteUserPermissionsByUserId(userId);
  }

  public void insertGroupPermission(DbSession session, long templateId, @Nullable Integer groupId, String permission) {
    PermissionTemplateGroupDto permissionTemplateGroup = new PermissionTemplateGroupDto()
      .setTemplateId(templateId)
      .setPermission(permission)
      .setGroupId(groupId)
      .setCreatedAt(now())
      .setUpdatedAt(now());
    mapper(session).insertGroupPermission(permissionTemplateGroup);
  }

  public void insertGroupPermission(DbSession session, PermissionTemplateGroupDto permissionTemplateGroup) {
    mapper(session).insertGroupPermission(permissionTemplateGroup);
  }

  public void deleteGroupPermission(DbSession session, Long templateId, @Nullable Integer groupId, String permission) {
    PermissionTemplateGroupDto permissionTemplateGroup = new PermissionTemplateGroupDto()
      .setTemplateId(templateId)
      .setPermission(permission)
      .setGroupId(groupId);
    mapper(session).deleteGroupPermission(permissionTemplateGroup);
    session.commit();
  }

  public PermissionTemplateDto selectByName(DbSession dbSession, String organizationUuid, String name) {
    return mapper(dbSession).selectByName(organizationUuid, name.toUpperCase(Locale.ENGLISH));
  }

  public List<String> selectPotentialPermissionsByUserIdAndTemplateId(DbSession dbSession, @Nullable Integer currentUserId, long templateId) {
    return mapper(dbSession).selectPotentialPermissionsByUserIdAndTemplateId(currentUserId, templateId);
  }

  /**
   * Remove a group from all templates (used when removing a group)
   */
  public void deleteByGroup(DbSession session, int groupId) {
    session.getMapper(PermissionTemplateMapper.class).deleteByGroupId(groupId);
  }

  private Date now() {
    return new Date(system.now());
  }

  private static PermissionTemplateMapper mapper(DbSession session) {
    return session.getMapper(PermissionTemplateMapper.class);
  }

  public void deleteByOrganization(DbSession dbSession, String organizationUuid) {
    PermissionTemplateMapper templateMapper = mapper(dbSession);
    PermissionTemplateCharacteristicMapper templateCharacteristicMapper = dbSession.getMapper(PermissionTemplateCharacteristicMapper.class);
    List<Long> templateIds = templateMapper.selectTemplateIdsByOrganization(organizationUuid);
    executeLargeInputsWithoutOutput(templateIds, subList -> {
      templateCharacteristicMapper.deleteByTemplateIds(subList);
      templateMapper.deleteGroupPermissionsByTemplateIds(subList);
      templateMapper.deleteUserPermissionsByTemplateIds(subList);
      templateMapper.deleteByIds(subList);
    });
  }
}
