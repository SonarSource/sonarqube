/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.core.user;

import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerExtension;
import org.sonar.api.task.TaskExtension;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

public class PermissionDao implements TaskExtension, ServerExtension {

  private final MyBatis myBatis;

  public PermissionDao(MyBatis myBatis) {
    this.myBatis = myBatis;
  }

  public PermissionTemplateDto selectTemplateByName(String templateName) {
    SqlSession session = myBatis.openSession();
    try {
      PermissionTemplateMapper mapper = session.getMapper(PermissionTemplateMapper.class);
      return mapper.selectByName(templateName);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public PermissionTemplateDto selectPermissionTemplate(String templateName) {
    PermissionTemplateDto permissionTemplate = null;
    SqlSession session = myBatis.openSession();
    try {
      PermissionTemplateMapper mapper = session.getMapper(PermissionTemplateMapper.class);
      permissionTemplate = mapper.selectTemplateUsersPermissions(templateName);
      PermissionTemplateDto templateWithGroupsPermissions = mapper.selectTemplateGroupsPermissions(templateName);
      permissionTemplate.setGroupsByPermission(templateWithGroupsPermissions.getGroupsPermissions());
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
    return permissionTemplate;
  }

  public List<PermissionTemplateDto> selectAllPermissionTemplates() {
    SqlSession session = myBatis.openSession();
    try {
      return session.selectList("selectAllPermissionTemplates");
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public PermissionTemplateDto createPermissionTemplate(String templateName, @Nullable String description) {
    PermissionTemplateDto permissionTemplate = new PermissionTemplateDto()
      .setName(templateName)
      .setDescription(description)
      .setCreatedAt(now())
      .setUpdatedAt(now());
    SqlSession session = myBatis.openSession();
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
    SqlSession session = myBatis.openSession();
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

  public void addUserPermission(Long templateId, Long userId, String permission) {
    PermissionTemplateUserDto permissionTemplateUser = new PermissionTemplateUserDto()
      .setTemplateId(templateId)
      .setUserId(userId)
      .setPermission(permission)
      .setCreatedAt(now())
      .setUpdatedAt(now());
    SqlSession session = myBatis.openSession();
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
    SqlSession session = myBatis.openSession();
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
    SqlSession session = myBatis.openSession();
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
    SqlSession session = myBatis.openSession();
    try {
      PermissionTemplateMapper mapper = session.getMapper(PermissionTemplateMapper.class);
      mapper.deleteGroupPermission(permissionTemplateGroup);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private Date now() {
    return new Date();
  }
}
