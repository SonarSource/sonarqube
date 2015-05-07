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

package org.sonar.core.permission;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchSide;
import org.sonar.api.ServerSide;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.text.Normalizer;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

@BatchSide
@ServerSide
public class PermissionTemplateDao {

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
    SqlSession session = myBatis.openSession(false);
    try {
      Map<String, Object> params = newHashMap();
      params.put(QUERY_PARAMETER, query);
      params.put(TEMPLATE_ID_PARAMETER, templateId);
      return session.selectList("org.sonar.core.permission.PermissionTemplateMapper.selectUsers", params, new RowBounds(offset, limit));
    } finally {
      MyBatis.closeQuietly(session);
    }
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
  public List<GroupWithPermissionDto> selectGroups(PermissionQuery query, Long templateId) {
    SqlSession session = myBatis.openSession(false);
    try {
      Map<String, Object> params = newHashMap();
      params.put(QUERY_PARAMETER, query);
      params.put(TEMPLATE_ID_PARAMETER, templateId);
      params.put("anyoneGroup", DefaultGroups.ANYONE);
      return session.selectList("org.sonar.core.permission.PermissionTemplateMapper.selectGroups", params);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public PermissionTemplateDto selectTemplateByKey(DbSession session, String templateKey) {
    return session.getMapper(PermissionTemplateMapper.class).selectByKey(templateKey);
  }

  @CheckForNull
  public PermissionTemplateDto selectTemplateByKey(String templateKey) {
    DbSession session = myBatis.openSession(false);
    try {
      return selectTemplateByKey(session, templateKey);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public PermissionTemplateDto selectPermissionTemplate(DbSession session, String templateKey) {
    PermissionTemplateDto permissionTemplate = null;
    PermissionTemplateMapper mapper = session.getMapper(PermissionTemplateMapper.class);
    permissionTemplate = mapper.selectByKey(templateKey);
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
  public PermissionTemplateDto selectPermissionTemplate(String templateKey) {
    DbSession session = myBatis.openSession(false);
    try {
      return selectPermissionTemplate(session, templateKey);
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

  public PermissionTemplateDto createPermissionTemplate(String templateName, @Nullable String description, @Nullable String keyPattern) {
    Date creationDate = now();
    PermissionTemplateDto permissionTemplate = new PermissionTemplateDto()
      .setName(templateName)
      .setKee(generateTemplateKee(templateName, creationDate))
      .setDescription(description)
      .setKeyPattern(keyPattern)
      .setCreatedAt(creationDate)
      .setUpdatedAt(creationDate);
    SqlSession session = myBatis.openSession(false);
    try {
      PermissionTemplateMapper mapper = session.getMapper(PermissionTemplateMapper.class);
      mapper.insert(permissionTemplate);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
    return permissionTemplate;
  }

  public void deletePermissionTemplate(Long templateId) {
    SqlSession session = myBatis.openSession(false);
    try {
      PermissionTemplateMapper mapper = session.getMapper(PermissionTemplateMapper.class);
      mapper.deleteUsersPermissions(templateId);
      mapper.deleteGroupsPermissions(templateId);
      mapper.delete(templateId);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void updatePermissionTemplate(Long templateId, String templateName, @Nullable String description, @Nullable String keyPattern) {
    PermissionTemplateDto permissionTemplate = new PermissionTemplateDto()
      .setId(templateId)
      .setName(templateName)
      .setDescription(description)
      .setKeyPattern(keyPattern)
      .setUpdatedAt(now());
    SqlSession session = myBatis.openSession(false);
    try {
      PermissionTemplateMapper mapper = session.getMapper(PermissionTemplateMapper.class);
      mapper.update(permissionTemplate);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void addUserPermission(Long templateId, Long userId, String permission) {
    PermissionTemplateUserDto permissionTemplateUser = new PermissionTemplateUserDto()
      .setTemplateId(templateId)
      .setUserId(userId)
      .setPermission(permission)
      .setCreatedAt(now())
      .setUpdatedAt(now());
    SqlSession session = myBatis.openSession(false);
    try {
      PermissionTemplateMapper mapper = session.getMapper(PermissionTemplateMapper.class);
      mapper.insertUserPermission(permissionTemplateUser);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void removeUserPermission(Long templateId, Long userId, String permission) {
    PermissionTemplateUserDto permissionTemplateUser = new PermissionTemplateUserDto()
      .setTemplateId(templateId)
      .setPermission(permission)
      .setUserId(userId);
    SqlSession session = myBatis.openSession(false);
    try {
      PermissionTemplateMapper mapper = session.getMapper(PermissionTemplateMapper.class);
      mapper.deleteUserPermission(permissionTemplateUser);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void addGroupPermission(Long templateId, @Nullable Long groupId, String permission) {
    PermissionTemplateGroupDto permissionTemplateGroup = new PermissionTemplateGroupDto()
      .setTemplateId(templateId)
      .setPermission(permission)
      .setGroupId(groupId)
      .setCreatedAt(now())
      .setUpdatedAt(now());
    SqlSession session = myBatis.openSession(false);
    try {
      PermissionTemplateMapper mapper = session.getMapper(PermissionTemplateMapper.class);
      mapper.insertGroupPermission(permissionTemplateGroup);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void removeGroupPermission(Long templateId, @Nullable Long groupId, String permission) {
    PermissionTemplateGroupDto permissionTemplateGroup = new PermissionTemplateGroupDto()
      .setTemplateId(templateId)
      .setPermission(permission)
      .setGroupId(groupId);
    SqlSession session = myBatis.openSession(false);
    try {
      PermissionTemplateMapper mapper = session.getMapper(PermissionTemplateMapper.class);
      mapper.deleteGroupPermission(permissionTemplateGroup);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Remove a group from all templates (used when removing a group)
   */
  public void removeByGroup(Long groupId, SqlSession session) {
    session.getMapper(PermissionTemplateMapper.class).deleteByGroupId(groupId);
  }

  private String generateTemplateKee(String name, Date timeStamp) {
    if (PermissionTemplateDto.DEFAULT.getName().equals(name)) {
      return PermissionTemplateDto.DEFAULT.getKee();
    }
    String normalizedName = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "").replace(" ", "_");
    return normalizedName.toLowerCase() + "_" + DateFormatUtils.format(timeStamp, "yyyyMMdd_HHmmss");
  }

  private Date now() {
    return new Date(system.now());
  }
}
