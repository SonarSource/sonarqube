/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.user;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

public interface GroupMembershipMapper {

  List<GroupMembershipDto> selectGroups(Map<String, Object> params, RowBounds rowBounds);

  int countGroups(Map<String, Object> params);

  List<UserMembershipDto> selectMembers(Map<String, Object> params, RowBounds rowBounds);

  int countMembers(Map<String, Object> params);

  List<GroupUserCount> countUsersByGroup(@Param("groupIds") List<Integer> groupIds);

  List<LoginGroup> selectGroupsByLogins(@Param("logins") List<String> logins);

  List<LoginGroup> selectGroupsByLoginsAndOrganization(@Param("logins") List<String> logins, @Param("organizationUuid") String organizationUuid);

  List<Integer> selectGroupIdsByUserId(@Param("userId") int userId);
}
