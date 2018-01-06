/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import javax.annotation.CheckForNull;
import org.apache.ibatis.annotations.Param;
import org.sonar.db.Pagination;

public interface OrganizationMapper {
  void insert(@Param("organization") OrganizationDto organization, @Param("newProjectPrivate") boolean newProjectPrivate);

  int countByQuery(@Param("query") OrganizationQuery organizationQuery);

  List<OrganizationDto> selectByQuery(@Param("query") OrganizationQuery organizationQuery,
    @Param("pagination") Pagination pagination);

  @CheckForNull
  OrganizationDto selectByKey(@Param("key") String key);

  @CheckForNull
  OrganizationDto selectByUuid(@Param("uuid") String uuid);

  List<OrganizationDto> selectByUuids(@Param("uuids") List<String> uuids);

  List<OrganizationDto> selectByPermission(@Param("userId") Integer userId, @Param("permission") String permission);

  List<String> selectAllUuids();

  DefaultTemplates selectDefaultTemplatesByUuid(@Param("uuid") String uuid);

  Integer selectDefaultGroupIdByUuid(@Param("uuid") String uuid);

  boolean selectNewProjectPrivateByUuid(@Param("uuid") String uuid);

  /**
   * Update the organization with UUID specified by {@link OrganizationDto#getUuid()}.
   * <p>
   * This method ignores {@link OrganizationDto#getCreatedAt()} and {@link OrganizationDto#getKey()}
   * (they are not updated).
   * </p>
   */
  int update(@Param("organization") OrganizationDto organization);

  void updateDefaultTemplates(@Param("organizationUuid") String organizationUuid,
    @Param("defaultTemplates") DefaultTemplates defaultTemplates, @Param("now") long now);

  void updateDefaultGroupId(@Param("organizationUuid") String organizationUuid,
    @Param("defaultGroupId") int defaultGroupId, @Param("now") long now);

  void updateDefaultQualityGate(@Param("organizationUuid") String organizationUuid,
    @Param("defaultQualityGateUuid") String defaultQualityGateUuid, @Param("now") long now);

  void updateNewProjectPrivate(@Param("organizationUuid") String organizationUuid, @Param("newProjectPrivate") boolean newProjectPrivate, @Param("now") long now);

  int deleteByUuid(@Param("uuid") String uuid);
}
