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
import org.sonar.db.user.SearchGroupMembershipDto;

public interface QProfileEditGroupsMapper {

  List<QProfileEditGroupsDto> selectByQProfileAndGroups(@Param("qProfileUuid") String qProfileUuid, @Param("groupUuids") List<String> groupUuids);

  int countByQuery(@Param("query") SearchQualityProfilePermissionQuery query);

  List<SearchGroupMembershipDto> selectByQuery(@Param("query") SearchQualityProfilePermissionQuery query, @Param("pagination") Pagination pagination);

  List<String> selectQProfileUuidsByGroups(@Param("groupUuids") List<String> groupUuids);

  void insert(@Param("dto") QProfileEditGroupsDto dto, @Param("now") long now);

  int delete(@Param("qProfileUuid") String qProfileUuid, @Param("groupUuid") String groupUuid);

  int deleteByQProfiles(@Param("qProfileUuids") Collection<String> qProfileUuids);

  int deleteByGroup(@Param("groupUuid") String groupUuid);

}
