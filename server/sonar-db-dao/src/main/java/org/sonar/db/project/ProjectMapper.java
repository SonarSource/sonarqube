/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.db.project;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.apache.ibatis.annotations.Param;

public interface ProjectMapper {

  void insert(ProjectDto project);

  @CheckForNull
  ProjectDto selectProjectByKey(String key);

  @CheckForNull
  ProjectDto selectApplicationByKey(String key);

  @CheckForNull
  ProjectDto selectProjectOrAppByKey(String key);

  List<ProjectDto> selectProjectsByKeys(@Param("kees") Collection<String> kees);

  @CheckForNull
  ProjectDto selectByUuid(String uuid);

  List<ProjectDto> selectByUuids(@Param("uuids") Collection<String> uuids);

  List<ProjectDto> selectAll();

  void updateTags(ProjectDto project);

  void update(ProjectDto project);

  List<ProjectDto> selectProjects();

  void updateVisibility(@Param("uuid") String uuid, @Param("isPrivate") boolean isPrivate,
          @Param("updatedAt") long updatedAt);

  List<ProjectDto> selectAllApplications();

  List<ProjectDto> selectApplicationsByKeys(@Param("kees") Collection<String> kees);

  List<String> selectAllProjectUuids();

  Set<String> selectProjectUuidsAssociatedToDefaultQualityProfileByLanguage(
          @Param("languageFilters") Set<String> languageFilters);

  List<ProjectDto> selectProjectsByOrganizationUuids(@Param("orgUuids") List<String> orgUuids);
}
