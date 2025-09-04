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
package org.sonar.db.permission.template;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.db.Pagination;
import org.sonar.db.permission.PermissionQuery;

/**
 * @since 3.7
 */
public interface PermissionTemplateMapper {

  void insert(PermissionTemplateDto permissionTemplate);

  void update(PermissionTemplateDto permissionTemplate);

  int deleteByUuid(String templateUuid);

  void deleteUserPermissionsByTemplateUuid(String templateUuid);

  void deleteUserPermissionsByOrganization(@Param("organizationUuid") String organizationUuid, @Param("userUuid") String userUuid);

  int deleteUserPermissionsByUserUuid(@Param("userUuid") String userUuid);

  int deleteUserPermission(PermissionTemplateUserDto permissionTemplateUser);

  void deleteGroupPermissionsByTemplateUuid(String templateUuid);

  int deleteGroupPermission(PermissionTemplateGroupDto permissionTemplateGroup);

  PermissionTemplateDto selectByUuid(String templateUuid);

  List<PermissionTemplateUserDto> selectUserPermissionsByTemplateUuidAndUserLogins(@Param("templateUuid") String templateUuid, @Param("logins") List<String> logins);

  List<PermissionTemplateGroupDto> selectGroupPermissionsByTemplateUuidAndGroupNames(@Param("templateUuid") String templateUuid, @Param("groups") List<String> groups);

  void insertUserPermission(PermissionTemplateUserDto permissionTemplateUser);

  void insertGroupPermission(PermissionTemplateGroupDto permissionTemplateGroup);

  int deleteByGroupUuid(String groupUuid);

  PermissionTemplateDto selectByName(@Param("organizationUuid") String organizationUuid, @Param("name") String name);

  List<String> selectUserLoginsByQueryAndTemplate(@Param("query") PermissionQuery query, @Param("templateUuid") String templateUuid, @Param("pagination") Pagination pagination);

  int countUserLoginsByQueryAndTemplate(@Param("query") PermissionQuery query, @Param("templateUuid") String templateUuid);

  List<String> selectGroupNamesByQueryAndTemplate(@Param("templateUuid") String templateUuid, @Param("query") PermissionQuery query, @Param("pagination") Pagination pagination);

  int countGroupNamesByQueryAndTemplate(@Param("organizationUuid") String organizationUuid, @Param("query") PermissionQuery query, @Param("templateUuid") String templateUuid);

  List<PermissionTemplateDto> selectAll(@Param("organizationUuid") String organizationUuid, @Nullable @Param("upperCaseNameLikeSql") String upperCaseNameLikeSql);

  void usersCountByTemplateUuidAndPermission(Map<String, Object> parameters, ResultHandler<CountByTemplateAndPermissionDto> resultHandler);

  void groupsCountByTemplateUuidAndPermission(Map<String, Object> parameters, ResultHandler<CountByTemplateAndPermissionDto> resultHandler);

  List<String> selectPotentialPermissionsByUserUuidAndTemplateUuid(@Param("userUuid") @Nullable String currentUserUuid, @Param("templateUuid") String templateUuid);

  int countGroupsWithPermission(@Param("templateUuid") String templateUuid, @Param("permission") String permission, @Nullable @Param("groupUuid") String groupUuid);

  List<PermissionTemplateGroupDto> selectAllGroupPermissionTemplatesByGroupUuid(@Param("groupUuid") String groupUuid);

  List<PermissionTemplateUserDto> selectUserPermissionsByUserUuid(@Param("userUuid") String userUuid);

}
