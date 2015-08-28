/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.db.permission;

import com.google.common.annotations.VisibleForTesting;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;

import static com.google.common.collect.Maps.newHashMap;

public class PermissionTemplateDao implements Dao {

  public static final String QUERY_PARAMETER = "query";
  public static final String TEMPLATE_ID_PARAMETER = "templateId";
  private final MyBatis myBatis;
  private final System2 system;

  public PermissionTemplateDao(MyBatis myBatis, System2 system) {
    this.myBatis = myBatis;
    this.system = system;
  }

  /**
   * @return a paginated list of users.
   */
  public List<UserWithPermissionDto> selectUsers(PermissionQuery query, Long templateId, int offset, int limit) {
    DbSession session = myBatis.openSession(false);
    try {
      return selectUsers(session, query, templateId, offset, limit);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * @return a paginated list of users.
   */
  public List<UserWithPermissionDto> selectUsers(DbSession session, PermissionQuery query, Long templateId, int offset, int limit) {
    Map<String, Object> params = newHashMap();
    params.put(QUERY_PARAMETER, query);
    params.put(TEMPLATE_ID_PARAMETER, templateId);
    return mapper(session).selectUsers(params, new RowBounds(offset, limit));
  }

  public int countUsers(DbSession session, PermissionQuery query, Long templateId) {
    Map<String, Object> params = newHashMap();
    params.put(QUERY_PARAMETER, query);
    params.put(TEMPLATE_ID_PARAMETER, templateId);
    return mapper(session).countUsers(params);
  }

  @VisibleForTesting
  List<UserWithPermissionDto> selectUsers(PermissionQuery query, Long templateId) {
    return selectUsers(query, templateId, 0, Integer.MAX_VALUE);
  }

  /**
   * 'Anyone' group is not returned when it has not the asked permission.
   * Membership parameter from query is not taking into account in order to deal more easily with the 'Anyone' group.
   * @return a non paginated list of groups.
   */
  public List<GroupWithPermissionDto> selectGroups(DbSession session, PermissionQuery query, Long templateId) {
    Map<String, Object> params = groupsParameters(query, templateId);
    return mapper(session).selectGroups(params);
  }

  public List<GroupWithPermissionDto> selectGroups(PermissionQuery query, Long templateId) {
    DbSession session = myBatis.openSession(false);
    try {
      return selectGroups(session, query, templateId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public int countGroups(DbSession session, PermissionQuery query, long templateId) {
    return countGroups(session, query, templateId, null);
  }

  private int countGroups(DbSession session, PermissionQuery query, long templateId, @Nullable String groupName) {
    Map<String, Object> parameters = groupsParameters(query, templateId);
    if (groupName != null) {
      parameters.put("groupName", groupName.toUpperCase());
    }
    return mapper(session).countGroups(parameters);
  }

  public boolean hasGroup(DbSession session, PermissionQuery query, long templateId, String groupName) {
    return countGroups(session, query, templateId, groupName) > 0;
  }

  private static Map<String, Object> groupsParameters(PermissionQuery query, Long templateId) {
    Map<String, Object> params = newHashMap();
    params.put(QUERY_PARAMETER, query);
    params.put(TEMPLATE_ID_PARAMETER, templateId);
    params.put("anyoneGroup", DefaultGroups.ANYONE);
    return params;
  }

  @CheckForNull
  public PermissionTemplateDto selectByUuid(DbSession session, String templateKey) {
    return mapper(session).selectByUuid(templateKey);
  }

  @CheckForNull
  public PermissionTemplateDto selectByUuid(String templateKey) {
    DbSession session = myBatis.openSession(false);
    try {
      return selectByUuid(session, templateKey);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public PermissionTemplateDto selectByUuidWithUserAndGroupPermissions(DbSession session, String templateKey) {
    PermissionTemplateDto permissionTemplate = null;
    PermissionTemplateMapper mapper = mapper(session);
    permissionTemplate = mapper.selectByUuid(templateKey);
    PermissionTemplateDto templateUsersPermissions = mapper.selectTemplateUsersPermissions(templateKey);
    if (templateUsersPermissions != null) {
      permissionTemplate.setUsersPermissions(templateUsersPermissions.getUsersPermissions());
    }
    PermissionTemplateDto templateGroupsPermissions = mapper.selectTemplateGroupsPermissions(templateKey);
    if (templateGroupsPermissions != null) {
      permissionTemplate.setGroupsByPermission(templateGroupsPermissions.getGroupsPermissions());
    }
    return permissionTemplate;
  }

  @CheckForNull
  public PermissionTemplateDto selectByUuidWithUserAndGroupPermissions(String templateKey) {
    DbSession session = myBatis.openSession(false);
    try {
      return selectByUuidWithUserAndGroupPermissions(session, templateKey);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<PermissionTemplateDto> selectAllPermissionTemplates(DbSession session) {
    return session.selectList("selectAllPermissionTemplates");
  }

  public List<PermissionTemplateDto> selectAllPermissionTemplates() {
    DbSession session = myBatis.openSession(false);
    try {
      return selectAllPermissionTemplates(session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public PermissionTemplateDto insert(DbSession session, PermissionTemplateDto permissionTemplate) {
    mapper(session).insert(permissionTemplate);
    session.commit();

    return permissionTemplate;
  }

  public void deleteById(DbSession session, long templateId) {
    PermissionTemplateMapper mapper = mapper(session);
    mapper.deleteUserPermissions(templateId);
    mapper.deleteGroupPermissions(templateId);
    mapper.delete(templateId);
  }

  /**
   * @deprecated since 5.2 use {@link #update(DbSession, PermissionTemplateDto)}
   */
  @Deprecated
  public void update(Long templateId, String templateName, @Nullable String description, @Nullable String projectPattern) {
    PermissionTemplateDto permissionTemplate = new PermissionTemplateDto()
      .setId(templateId)
      .setName(templateName)
      .setDescription(description)
      .setKeyPattern(projectPattern)
      .setUpdatedAt(now());

    DbSession session = myBatis.openSession(false);
    try {
      update(session, permissionTemplate);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public PermissionTemplateDto update(DbSession session, PermissionTemplateDto permissionTemplate) {
    mapper(session).update(permissionTemplate);
    session.commit();

    return permissionTemplate;
  }

  /**
   * @deprecated since 5.2 {@link #insertUserPermission(DbSession, Long, Long, String)}
   */
  @Deprecated
  public void insertUserPermission(Long templateId, Long userId, String permission) {
    DbSession session = myBatis.openSession(false);
    try {
      insertUserPermission(session, templateId, userId, permission);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insertUserPermission(DbSession session, Long templateId, Long userId, String permission) {
    PermissionTemplateUserDto permissionTemplateUser = new PermissionTemplateUserDto()
      .setTemplateId(templateId)
      .setUserId(userId)
      .setPermission(permission)
      .setCreatedAt(now())
      .setUpdatedAt(now());

    mapper(session).insertUserPermission(permissionTemplateUser);
    session.commit();
  }

  /**
   * @deprecated since 5.2 {@link #deleteUserPermission(DbSession, Long, Long, String)}
   */
  @Deprecated
  public void deleteUserPermission(Long templateId, Long userId, String permission) {
    DbSession session = myBatis.openSession(false);
    try {
      deleteUserPermission(session, templateId, userId, permission);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteUserPermission(DbSession session, Long templateId, Long userId, String permission) {
    PermissionTemplateUserDto permissionTemplateUser = new PermissionTemplateUserDto()
      .setTemplateId(templateId)
      .setPermission(permission)
      .setUserId(userId);
    mapper(session).deleteUserPermission(permissionTemplateUser);
    session.commit();
  }

  public void insertGroupPermission(Long templateId, @Nullable Long groupId, String permission) {
    DbSession session = myBatis.openSession(false);
    try {
      insertGroupPermission(session, templateId, groupId, permission);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insertGroupPermission(DbSession session, Long templateId, @Nullable Long groupId, String permission) {
    PermissionTemplateGroupDto permissionTemplateGroup = new PermissionTemplateGroupDto()
      .setTemplateId(templateId)
      .setPermission(permission)
      .setGroupId(groupId)
      .setCreatedAt(now())
      .setUpdatedAt(now());
    mapper(session).insertGroupPermission(permissionTemplateGroup);
    session.commit();
  }

  /**
   * @deprecated since 5.2 use {@link #deleteGroupPermission(DbSession, Long, Long, String)}
   */
  @Deprecated
  public void deleteGroupPermission(Long templateId, @Nullable Long groupId, String permission) {
    DbSession session = myBatis.openSession(false);
    try {
      deleteGroupPermission(session, templateId, groupId, permission);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteGroupPermission(DbSession session, Long templateId, @Nullable Long groupId, String permission) {
    PermissionTemplateGroupDto permissionTemplateGroup = new PermissionTemplateGroupDto()
      .setTemplateId(templateId)
      .setPermission(permission)
      .setGroupId(groupId);
    mapper(session).deleteGroupPermission(permissionTemplateGroup);
    session.commit();
  }

  public void deleteGroupPermissions(DbSession session, long templateId) {
    mapper(session).deleteGroupPermissions(templateId);
  }

  public void deleteUserPermissions(DbSession session, long templateId) {
    mapper(session).deleteUserPermissions(templateId);
  }

  /**
   * Load permission template and load associated collections of users and groups permissions
   */
  @VisibleForTesting
  PermissionTemplateDto selectPermissionTemplateWithPermissions(DbSession session, String templateKey) {
    PermissionTemplateDto permissionTemplateDto = selectByUuid(session, templateKey);
    if (permissionTemplateDto == null) {
      throw new IllegalArgumentException("Could not retrieve permission template with key " + templateKey);
    }
    PermissionTemplateDto templateWithPermissions = selectByUuidWithUserAndGroupPermissions(session, permissionTemplateDto.getKee());
    if (templateWithPermissions == null) {
      throw new IllegalArgumentException("Could not retrieve permissions for template with key " + templateKey);
    }
    return templateWithPermissions;
  }

  public PermissionTemplateDto selectByName(DbSession dbSession, String name) {
    return mapper(dbSession).selectByName(name.toUpperCase());
  }

  /**
   * Remove a group from all templates (used when removing a group)
   */
  public void deleteByGroup(SqlSession session, Long groupId) {
    session.getMapper(PermissionTemplateMapper.class).deleteByGroupId(groupId);
  }

  private Date now() {
    return new Date(system.now());
  }

  private PermissionTemplateMapper mapper(SqlSession session) {
    return session.getMapper(PermissionTemplateMapper.class);
  }
}
