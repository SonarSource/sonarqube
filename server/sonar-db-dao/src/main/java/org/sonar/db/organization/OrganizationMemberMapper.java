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
package org.sonar.db.organization;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.annotations.Param;

public interface OrganizationMemberMapper {
  OrganizationMemberDto select(@Param("organizationUuid") String organizationUuid, @Param("userUuid") String userUuid);

  Set<String> selectOrganizationUuidsByUser(@Param("userUuid") String userUuid);

  List<OrganizationMemberDto> selectOrganizationMembersByUserUuid(@Param("userUuid") String userUuid);

  Set<String> selectOrganizationUuidsByUserUuidAndType(@Param("userUuid") String userUuid, @Param("type") String type);

  boolean isUserStandardMemberOfOrganization(@Param("userUuid") String userUuid, @Param("organizationUuid") String organizationUuid);

  List<OrganizationMemberDto> selectAllOrganizationMemberDtos(@Param("organizationUuid") String organizationUuid);

  List<String> selectUserUuids(String organizationUuid);

  List<Map<String, String>> selectForIndexing(@Param("uuids") List<String> uuids);

  List<Map<String, String>> selectAllForIndexing();

  void insert(OrganizationMemberDto organizationMember);

  void delete(@Param("organizationUuid") String organizationUuid, @Param("userUuid") String userUuid);

  void deleteByOrganization(@Param("organizationUuid") String organizationUuid);

  void deleteByUserUuid(@Param("userUuid") String userUuid);

  void updateOrgMemberType(@Param("organizationKee") String organizationKee, @Param("login") String login, @Param("type") String type) ;

}
