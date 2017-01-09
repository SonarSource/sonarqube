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
package org.sonar.db.component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

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

  List<ComponentDto> selectByProjectUuid(@Param("projectUuid") String projectUuid);

  List<String> selectExistingUuids(@Param("uuids") Collection<String> uuids);

  List<ComponentDto> selectComponentsByQualifiers(@Param("qualifiers") Collection<String> qualifiers);

  List<ComponentDto> selectByQuery(@Param("query") ComponentQuery query, RowBounds rowBounds);

  int countByQuery(@Param("query") ComponentQuery query);

  List<ComponentDto> selectAncestors(@Param("query") ComponentTreeQuery query, @Param("baseUuidPathLike") String baseUuidPathLike);

  List<ComponentDto> selectDescendants(@Param("query") ComponentTreeQuery query, @Param("baseUuid") String baseUuid, @Param("baseUuidPath") String baseUuidPath);

  /**
   * Return all project (PRJ/TRK) uuids
   */
  List<String> selectProjectUuids();

  /**
   * Returns all enabled projects (Scope {@link org.sonar.api.resources.Scopes#PROJECT} and qualifier
   * {@link org.sonar.api.resources.Qualifiers#PROJECT}) no matter if they are ghost project, provisioned projects or
   * regular ones.
   */
  List<ComponentDto> selectProjects();

  List<ComponentDto> selectComponents(Map<String, Object> parameters, RowBounds rowBounds);

  int countRootComponents(Map<String, Object> parameters);

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
   * <p/>
   * It's using a join on snapshots in order to use he indexed columns snapshots.qualifier
   */
  List<UuidWithProjectUuidDto> selectUuidsForQualifiers(@Param("qualifiers") String... qualifiers);

  /**
   * Return components of a given scope of a project
   *
   * @param scope scope of components to return. If null, all components are returned
   */
  List<ComponentDto> selectComponentsFromProjectKeyAndScope(@Param("projectKey") String projectKey, @Nullable @Param("scope") String scope,
    @Param(value = "excludeDisabled") boolean excludeDisabled);

  /**
   * Return technical projects from a view or a sub-view
   */
  List<String> selectProjectsFromView(@Param("viewUuidLikeQuery") String viewUuidLikeQuery, @Param("projectViewUuid") String projectViewUuid);

  long countById(long id);

  List<ComponentDto> selectProvisionedProjects(Map<String, Object> parameters, RowBounds rowBounds);

  int countProvisionedProjects(Map<String, Object> parameters);

  List<ComponentDto> selectGhostProjects(Map<String, Object> parameters, RowBounds rowBounds);

  List<ComponentDto> selectComponentsHavingSameKeyOrderedById(String key);

  long countGhostProjects(Map<String, Object> parameters);

  void insert(ComponentDto componentDto);

  void insertBatch(ComponentDto componentDto);

  void update(ComponentUpdateDto component);

  void updateBEnabledToFalse(@Param("uuids") List<String> uuids);

  void applyBChangesForRootComponentUuid(@Param("projectUuid") String projectUuid);

  void resetBChangedForRootComponentUuid(@Param("projectUuid") String projectUuid);

  void delete(long componentId);
}
