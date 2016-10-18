/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.sonar.db.permission.PermissionQuery;

/**
 * @since 3.7
 */
public interface PermissionTemplateMapper {

  void insert(PermissionTemplateDto permissionTemplate);

  void update(PermissionTemplateDto permissionTemplate);

  void deleteById(long templateId);

  void deleteByIds(@Param("templateIds") List<Long> templateIds);

  void deleteUserPermissionsByTemplateId(long templateId);

  void deleteUserPermissionsByTemplateIds(@Param("templateIds") List<Long> templateIds);

  void deleteUserPermission(PermissionTemplateUserDto permissionTemplateUser);

  void deleteGroupPermissionsByTemplateId(long templateId);
  
  void deleteGroupPermissionsByTemplateIds(@Param("templateIds") List<Long> templateIds);

  void deleteGroupPermission(PermissionTemplateGroupDto permissionTemplateGroup);

  PermissionTemplateDto selectByUuid(String templateUuid);

  List<PermissionTemplateUserDto> selectUserPermissionsByTemplateIdAndUserLogins(@Param("templateId") long templateId, @Param("logins") List<String> logins);

  List<PermissionTemplateGroupDto> selectGroupPermissionsByTemplateIdAndGroupNames(@Param("templateId") long templateId, @Param("groups") List<String> groups);

  void insertUserPermission(PermissionTemplateUserDto permissionTemplateUser);

  void insertGroupPermission(PermissionTemplateGroupDto permissionTemplateGroup);

  void deleteByGroupId(long groupId);

  PermissionTemplateDto selectByName(@Param("organizationUuid") String organizationUuid, @Param("name") String name);

  List<String> selectUserLoginsByQueryAndTemplate(@Param("query") PermissionQuery query, @Param("templateId") long templateId, RowBounds rowBounds);

  int countUserLoginsByQueryAndTemplate(@Param("query") PermissionQuery query, @Param("templateId") long templateId);

  List<String> selectGroupNamesByQueryAndTemplate(@Param("query") PermissionQuery query, @Param("templateId") long templateId, RowBounds rowBounds);

  int countGroupNamesByQueryAndTemplate(@Param("query") PermissionQuery query, @Param("templateId") long templateId);

  List<PermissionTemplateDto> selectAll(@Nullable @Param("nameMatch") String nameMatch);

  void usersCountByTemplateIdAndPermission(Map<String, Object> parameters, ResultHandler resultHandler);

  void groupsCountByTemplateIdAndPermission(Map<String, Object> parameters, ResultHandler resultHandler);

  List<String> selectPotentialPermissionsByUserIdAndTemplateId(@Param("userId") @Nullable Long currentUserId, @Param("templateId") long templateId);

  int countGroupsWithPermission(@Param("templateId") long templateId, @Param("permission") String permission, @Nullable @Param("groupId") Long groupId);

  List<Long> selectTemplateIdsByOrganization(@Param("organizationUuid") String organizationUuid);

}
