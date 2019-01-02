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
package org.sonar.db.qualityprofile;

import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.sonar.db.Pagination;

public interface QProfileEditUsersMapper {

  QProfileEditUsersDto selectByQProfileAndUser(@Param("qProfileUuid") String qProfileUuid, @Param("userId") int userId);

  int countByQuery(@Param("query") SearchUsersQuery query);

  List<UserMembershipDto> selectByQuery(@Param("query") SearchUsersQuery query, @Param("pagination") Pagination pagination);

  List<String> selectQProfileUuidsByOrganizationAndUser(@Param("organizationUuid") String organizationUuid, @Param("userId") int userId);

  void insert(@Param("dto") QProfileEditUsersDto dto, @Param("now") long now);

  void delete(@Param("qProfileUuid") String qProfileUuid, @Param("userId") int userId);

  void deleteByQProfiles(@Param("qProfileUuids") Collection<String> qProfileUuids);

  void deleteByUser(@Param("userId") int userId);

  void deleteByOrganizationAndUser(@Param("organizationUuid") String organizationUuid, @Param("userId") int userId);
}
