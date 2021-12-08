/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.db.portfolio;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.apache.ibatis.annotations.Param;
import org.sonar.db.project.ApplicationProjectDto;
import org.sonar.db.project.ProjectDto;

public interface PortfolioMapper {
  @CheckForNull
  PortfolioDto selectByKey(String key);

  List<PortfolioDto> selectByKeys(@Param("keys") List<String> keys);

  @CheckForNull
  PortfolioDto selectByUuid(String uuid);

  void insert(PortfolioDto portfolio);

  void deletePortfolio(@Param("uuid") String uuid);

  void deleteReferencesByPortfolioOrReferenceUuids(@Param("uuids") Set<String> uuids);

  void insertReference(PortfolioReferenceDto portfolioReference);

  void insertProject(@Param("uuid") String uuid, @Param("portfolioUuid") String portfolioUuid, @Param("projectUuid") String projectUuid, @Param("createdAt") long createdAt);

  List<PortfolioDto> selectTree(String portfolioUuid);

  Set<String> selectReferenceUuids(String portfolioUuid);

  List<PortfolioDto> selectReferencers(String referenceUuid);

  List<PortfolioProjectDto> selectPortfolioProjects(String portfolioUuid);

  PortfolioProjectDto selectPortfolioProject(@Param("portfolioUuid") String portfolioUuid, @Param("projectUuid") String projectUuid);

  List<ReferenceDto> selectAllReferencesToPortfolios();

  List<ReferenceDto> selectAllReferencesToApplications();

  List<PortfolioProjectDto> selectAllProjectsInHierarchy(String rootUuid);

  List<PortfolioDto> selectByUuids(@Param("uuids") Collection<String> uuids);

  void update(PortfolioDto portfolio);

  List<PortfolioDto> selectAllRoots();

  List<ApplicationProjectDto> selectAllApplicationProjects(String rootPortfolioUuid);

  List<PortfolioDto> selectAll();

  List<PortfolioDto> selectRootOfReferencers(String referenceUuid);

  void deleteReferencesTo(String referenceUuid);

  void deleteProjects(String portfolioUuid);

  void deleteProject(@Param("portfolioUuid") String portfolioUuid, @Param("projectUuid") String projectUuid);

  void deleteAllDescendantPortfolios(String rootUuid);

  void deleteAllReferences();

  int deleteReference(@Param("portfolioUuid") String portfolioUuid, @Param("referenceUuid") String referenceUuid);

  ReferenceDetailsDto selectReferenceToPortfolio(@Param("portfolioUuid") String portfolioUuid, @Param("referenceKey") String referenceKey);

  ReferenceDetailsDto selectReferenceToApplication(@Param("portfolioUuid") String portfolioUuid, @Param("referenceKey") String referenceKey);

  void deleteAllProjects();

  List<PortfolioProjectDto> selectAllPortfolioProjects();

  List<ReferenceDto> selectAllReferencesInHierarchy(String rootUuid);

  void deleteBranch(@Param("portfolioUuid") String portfolioUuid, @Param("projectUuid") String projectUuid, @Param("branchUuid") String branchUuid);

  void insertBranch(@Param("uuid") String uuid, @Param("portfolioProjectUuid") String portfolioProjectUuid, @Param("branchUuid") String branchUuid,
                    @Param("createdAt") long createdAt);

  List<ProjectDto> selectAllDirectChildApplications(@Param("portfolioUuid") String portfolioUuid);
}
