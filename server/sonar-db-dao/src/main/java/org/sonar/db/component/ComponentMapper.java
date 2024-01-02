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
package org.sonar.db.component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

public interface ComponentMapper {
  @CheckForNull
  ComponentDto selectByKeyCaseInsensitive(@Param("key") String key);

  @CheckForNull
  ComponentDto selectByKeyAndBranchOrPr(@Param("key") String key, @Nullable @Param("branch") String branch, @Nullable @Param("pullRequest") String pullRequest);

  @CheckForNull
  ComponentDto selectByUuid(@Param("uuid") String uuid);

  /**
   * Return sub project of component keys
   */
  List<ComponentDto> selectSubProjectsByComponentUuids(@Param("uuids") Collection<String> uuids);

  List<ComponentDto> selectByKeysAndBranchOrPr(@Param("keys") Collection<String> keys,
    @Nullable @Param("branch") String branch, @Nullable @Param("pullRequest") String pullRequest);

  List<ComponentDto> selectByUuids(@Param("uuids") Collection<String> uuids);

  List<ComponentDto> selectByBranchUuid(@Param("branchUuid") String branchUuid);

  List<String> selectExistingUuids(@Param("uuids") Collection<String> uuids);

  List<ComponentDto> selectComponentsByQualifiers(@Param("qualifiers") Collection<String> qualifiers);

  int countEnabledModulesByBranchUuid(@Param("branchUuid") String branchUuid);

  List<ComponentDto> selectByQuery(@Param("query") ComponentQuery query, RowBounds rowBounds);

  int countByQuery(@Param("query") ComponentQuery query);

  List<ComponentDto> selectDescendants(@Param("query") ComponentTreeQuery query, @Param("baseUuid") String baseUuid, @Param("baseUuidPath") String baseUuidPath);

  List<ComponentDto> selectChildren(@Param("branchUuid") String branchUuid, @Param("uuidPaths") Set<String> uuidPaths);

  /**
   * Returns all enabled projects (Scope {@link org.sonar.api.resources.Scopes#PROJECT} and qualifier
   * {@link org.sonar.api.resources.Qualifiers#PROJECT}) no matter if they are ghost project, provisioned projects or
   * regular ones.
   */
  List<ComponentDto> selectProjects();

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
  List<UuidWithBranchUuidDto> selectUuidsForQualifiers(@Param("qualifiers") String... qualifiers);

  /**
   * Return components of a given scope of a project
   *
   * @param scope scope of components to return. If null, all components are returned
   */
  List<ComponentDto> selectComponentsFromProjectKeyAndScope(@Param("projectKey") String projectKey, @Nullable @Param("scope") String scope,
    @Param(value = "excludeDisabled") boolean excludeDisabled, @Nullable @Param("branch") String branch, @Nullable @Param("pullRequest") String pullRequest);

  /**
   * Return keys and UUIDs of all components belonging to a project
   */
  List<KeyWithUuidDto> selectUuidsByKeyFromProjectKeyAndBranchOrPr(@Param("projectKey") String projectKey,
    @Nullable @Param("branch") String branch, @Nullable @Param("pullRequest") String pullRequest);

  Set<String> selectViewKeysWithEnabledCopyOfProject(@Param("projectUuids") Collection<String> projectUuids);

  /**
   * Return technical projects from a view or a sub-view
   */
  List<String> selectProjectsFromView(@Param("viewUuidLikeQuery") String viewUuidLikeQuery, @Param("projectViewUuid") String projectViewUuid);

  void scrollForIndexing(@Param("projectUuid") @Nullable String projectUuid, ResultHandler<ComponentDto> handler);

  void scrollAllFilesForFileMove(@Param("branchUuid") String branchUuid, ResultHandler<FileMoveRowDto> handler);

  void insert(ComponentDto componentDto);

  void update(ComponentUpdateDto component);

  void updateBEnabledToFalse(@Param("uuids") List<String> uuids);

  void applyBChangesForRootComponentUuid(@Param("branchUuid") String branchUuid);

  void resetBChangedForRootComponentUuid(@Param("branchUuid") String branchUuid);

  void setPrivateForRootComponentUuid(@Param("branchUuid") String branchUuid, @Param("isPrivate") boolean isPrivate);

  List<KeyWithUuidDto> selectComponentsFromPullRequestsTargetingCurrentBranchThatHaveOpenIssues(@Param("referenceBranchUuid") String referenceBranchUuid,
    @Param("currentBranchUuid") String currentBranchUuid);

  List<KeyWithUuidDto> selectComponentsFromBranchesThatHaveOpenIssues(@Param("branchUuids") List<String> branchUuids);

  List<ProjectNclocDistributionDto> selectPrivateProjectsWithNcloc();

  List<ComponentWithModuleUuidDto> selectEnabledComponentsWithModuleUuidFromProjectKey(String projectKey);

  short checkIfAnyOfComponentsWithQualifiers(@Param("componentKeys") Collection<String> componentKeys, @Param("qualifiers") Set<String> qualifiers);
}
