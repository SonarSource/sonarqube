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

package org.sonar.core.permission;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.api.task.TaskComponent;
import org.sonar.core.date.DateProvider;
import org.sonar.core.date.DefaultDateProvider;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.text.Normalizer;
import java.util.Date;
import java.util.List;

public class PermissionTemplateDao implements TaskComponent, ServerComponent {

  private final MyBatis myBatis;
  private final DateProvider dateProvider;

  public PermissionTemplateDao(MyBatis myBatis, DateProvider dateProvider) {
    this.myBatis = myBatis;
    this.dateProvider = dateProvider;
  }

  public PermissionTemplateDao(MyBatis myBatis) {
    this(myBatis, new DefaultDateProvider());
  }

  @CheckForNull
  public PermissionTemplateDto selectTemplateByName(String templateName) {
    SqlSession session = myBatis.openSession();
    try {
      PermissionTemplateMapper mapper = session.getMapper(PermissionTemplateMapper.class);
      return mapper.selectByName(templateName);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public PermissionTemplateDto selectTemplateByKey(String templateKey) {
    SqlSession session = myBatis.openSession();
    try {
      PermissionTemplateMapper mapper = session.getMapper(PermissionTemplateMapper.class);
      return mapper.selectByKey(templateKey);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public PermissionTemplateDto selectPermissionTemplate(String templateName) {
    PermissionTemplateDto permissionTemplate = null;
    SqlSession session = myBatis.openSession();
    try {
      PermissionTemplateMapper mapper = session.getMapper(PermissionTemplateMapper.class);
      permissionTemplate = mapper.selectByName(templateName);
      PermissionTemplateDto templateUsersPermissions = mapper.selectTemplateUsersPermissions(templateName);
      if(templateUsersPermissions != null) {
        permissionTemplate.setUsersPermissions(templateUsersPermissions.getUsersPermissions());
      }
      PermissionTemplateDto templateGroupsPermissions = mapper.selectTemplateGroupsPermissions(templateName);
      if(templateGroupsPermissions != null) {
        permissionTemplate.setGroupsByPermission(templateGroupsPermissions.getGroupsPermissions());
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
    return permissionTemplate;
  }

  @CheckForNull
  public List<PermissionTemplateDto> selectAllPermissionTemplates() {
    SqlSession session = myBatis.openSession();
    try {
      return session.selectList("selectAllPermissionTemplates");
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public PermissionTemplateDto createPermissionTemplate(String templateName, @Nullable String description) {
    Date creationDate = now();
    PermissionTemplateDto permissionTemplate = new PermissionTemplateDto()
      .setName(templateName)
      .setKee(generateTemplateKee(templateName, creationDate))
      .setDescription(description)
      .setCreatedAt(creationDate)
      .setUpdatedAt(creationDate);
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

  public void updatePermissionTemplate(Long templateId, String templateName, @Nullable String description) {
    PermissionTemplateDto permissionTemplate = new PermissionTemplateDto()
      .setId(templateId)
      .setName(templateName)
      .setDescription(description)
      .setUpdatedAt(now());
    SqlSession session = myBatis.openSession();
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

  private String generateTemplateKee(String name, Date timeStamp) {
    if(PermissionTemplateDto.DEFAULT.getName().equals(name)) {
      return PermissionTemplateDto.DEFAULT.getKee();
    }
    String normalizedName = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "").replace(" ", "_");
    return normalizedName.toLowerCase() + "_" + DateFormatUtils.format(timeStamp, "yyyyMMdd_HHmmss");
  }

  private Date now() {
    return dateProvider.now();
  }
}
