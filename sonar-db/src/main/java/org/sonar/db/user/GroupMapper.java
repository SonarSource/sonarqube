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
package org.sonar.db.user;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

public interface GroupMapper {

  @CheckForNull
  GroupDto selectByKey(String name);

  @CheckForNull
  GroupDto selectById(long groupId);

  List<GroupDto> selectByUserLogin(String userLogin);

  List<GroupDto> selectByNames(@Param("organizationUuid") String organizationUuid, @Param("names") List<String> names);

  void insert(GroupDto groupDto);

  void update(GroupDto item);

  List<GroupDto> selectByQuery(@Param("organizationUuid") String organizationUuid, @Nullable @Param("query") String query, RowBounds rowBounds);

  int countByQuery(@Param("organizationUuid") String organizationUuid, @Nullable @Param("query") String query);

  void deleteById(long groupId);

  @CheckForNull
  GroupDto selectByName(@Param("organizationUuid") String organizationUuid, @Param("name") String name);

  List<GroupDto> selectByOrganizationUuid(@Param("organizationUuid") String organizationUuid);

  List<GroupDto> selectByIds(@Param("ids") List<Long> ids);
}
