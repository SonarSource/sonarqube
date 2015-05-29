/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.core.component.db;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.FilePathWithHashDto;
import org.sonar.core.component.UuidWithProjectUuidDto;

/**
 * @since 4.3
 */
public interface ComponentMapper {

  @CheckForNull
  ComponentDto selectByKey(String key);

  @CheckForNull
  ComponentDto selectById(long id);

  @CheckForNull
  ComponentDto selectByUuid(String uuid);

  /**
   * Return sub project of component keys
   */
  List<ComponentDto> selectSubProjectsByComponentUuids(@Param("uuids") Collection<String> uuids);

  List<ComponentDto> selectByKeys(@Param("keys") Collection<String> keys);

  List<ComponentDto> selectByIds(@Param("ids") Collection<Long> ids);

  List<ComponentDto> selectByUuids(@Param("uuids") Collection<String> uuids);

  List<String> selectExistingUuids(@Param("uuids") Collection<String> uuids);

  /**
   * Return all project (PRJ/TRK) uuids
   */
  List<String> selectProjectUuids();

  /**
   * Return all descendant modules (including itself) from a given component uuid and scope
   */
  List<ComponentDto> selectDescendantModules(@Param("moduleUuid") String moduleUuid, @Param(value = "scope") String scope,
    @Param(value = "excludeDisabled") boolean excludeDisabled);

  /**
   * Return all files from a given project uuid and scope
   */
  List<FilePathWithHashDto> selectEnabledFilesFromProject(@Param("projectUuid") String projectUuid);

  /**
   * Return all descendant files from a given module uuid and scope
   */
  List<FilePathWithHashDto> selectDescendantFiles(@Param("moduleUuid") String moduleUuid, @Param(value = "scope") String scope,
    @Param(value = "excludeDisabled") boolean excludeDisabled);

  /**
   * Return uuids and project uuids from list of qualifiers
   *
   * It's using a join on snapshots in order to use he indexed columns snapshots.qualifier
   */
  List<UuidWithProjectUuidDto> selectUuidsForQualifiers(@Param("qualifiers") String... qualifiers);

  /**
   * Return all components of a project
   */
  List<ComponentDto> selectComponentsFromProjectKeyAndScope(@Param("projectKey") String projectKey, @Nullable @Param("scope") String scope);

  /**
   * Return technical projects from a view or a sub-view
   */
  List<String> selectProjectsFromView(@Param("viewUuidLikeQuery") String viewUuidLikeQuery, @Param("projectViewUuid") String projectViewUuid);

  long countById(long id);

  List<ComponentDto> selectProvisionedProjects(Map<String, String> parameters, RowBounds rowBounds);

  int countProvisionedProjects(Map<String, String> parameters);

  List<ComponentDto> selectGhostProjects(Map<String, String> parameters, RowBounds rowBounds);

  long countGhostProjects(Map<String, String> parameters);

  void insert(ComponentDto componentDto);

  void update(ComponentDto componentDto);
}
