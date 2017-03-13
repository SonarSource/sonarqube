/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

public interface OrganizationMapper {
  void insert(@Param("organization") OrganizationDto organization);

  List<OrganizationDto> selectByQuery(@Param("query") OrganizationQuery organizationQuery,
    @Param("offset") int offset, @Param("pageSize") int pageSize);

  @CheckForNull
  OrganizationDto selectByKey(@Param("key") String key);

  @CheckForNull
  OrganizationDto selectByUuid(@Param("uuid") String uuid);

  List<OrganizationDto> selectByUuids(@Param("uuids") List<String> uuids);

  List<OrganizationDto> selectByPermission(@Param("userId") Integer userId, @Param("permission") String permission);

  /**
   * Assuming the key of the loaded template with the specified type is an organization's UUID, select all organizations
   * which does not have a row in table LOADED_TEMPLATES with the specified type.
   *
   * @param offset {@code ((#{page} - 1) * #{pageSize})}
   */
  List<OrganizationDto> selectOrganizationsWithoutLoadedTemplate(@Param("loadedTemplateType") String type,
    @Param("page") int page, @Param("pageSize") int pageSize, @Param("offset") int offset);

  DefaultTemplates selectDefaultTemplatesByUuid(@Param("uuid") String uuid);

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

  int deleteByUuid(@Param("uuid") String uuid);

  int deleteByKey(@Param("key") String key);
}
