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
package org.sonar.db.qualityprofile;

import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.sonar.db.Pagination;
import org.sonar.db.user.SearchUserMembershipDto;

public interface QProfileEditUsersMapper {

  QProfileEditUsersDto selectByQProfileAndUser(@Param("qProfileUuid") String qProfileUuid, @Param("userUuid") String userUuid);

  int countByQuery(@Param("query") SearchQualityProfilePermissionQuery query);

  List<SearchUserMembershipDto> selectByQuery(@Param("query") SearchQualityProfilePermissionQuery query, @Param("pagination") Pagination pagination);

  List<String> selectQProfileUuidsByOrganizationAndUser(@Param("organizationUuid") String organizationUuid, @Param("userUuid") String userUuid);

  void insert(@Param("dto") QProfileEditUsersDto dto, @Param("now") long now);

  int delete(@Param("qProfileUuid") String qProfileUuid, @Param("userUuid") String userUuid);

  int deleteByQProfiles(@Param("qProfileUuids") Collection<String> qProfileUuids);

  int deleteByUser(@Param("userUuid") String userUuid);

  void deleteByOrganizationAndUser(@Param("organizationUuid") String organizationUuid, @Param("userUuid") String userUuid);

  List<QProfileEditUsersDto> selectByUser(@Param("userUuid") String userUuid);
}
