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
package org.sonar.db.qualitygate;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.sonar.db.Pagination;
import org.sonar.db.user.SearchPermissionQuery;
import org.sonar.db.user.SearchUserMembershipDto;

public interface QualityGateUserPermissionsMapper {

  QualityGateUserPermissionsDto selectByQualityGateAndUser(@Param("qualityGateUuid") String qualityGateUuid, @Param("userUuid") String userUuid);

  void insert(@Param("dto") QualityGateUserPermissionsDto dto, @Param("now") long now);

  List<SearchUserMembershipDto> selectByQuery(@Param("query") SearchPermissionQuery query, @Param("pagination") Pagination pagination);

  int countByQuery(@Param("query") SearchPermissionQuery query);

  int delete(@Param("qualityGateUuid") String qualityGateUuid, @Param("userUuid") String userUuid);

  int deleteByUser(@Param("userUuid") String userUuid);

  int deleteByQualityGate(@Param("qualityGateUuid") String qualityGateUuid);

}
