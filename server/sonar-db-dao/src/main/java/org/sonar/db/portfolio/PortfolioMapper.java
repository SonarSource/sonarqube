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

import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.apache.ibatis.annotations.Param;

public interface PortfolioMapper {
  @CheckForNull
  PortfolioDto selectByKey(String key);

  @CheckForNull
  PortfolioDto selectByUuid(String uuid);

  void insert(PortfolioDto portfolio);

  void deleteByUuids(String portfolioUuid);

  void deleteByUuids(@Param("uuids") Set<String> uuids);

  void deletePortfoliosByUuids(@Param("uuids") Set<String> uuids);

  void deleteReferencesByPortfolioOrReferenceUuids(@Param("uuids") Set<String> uuids);

  void deleteProjectsByPortfolioUuids(@Param("uuids") Set<String> uuids);

  void insertReference(PortfolioReferenceDto portfolioReference);

  void insertProject(PortfolioProjectDto portfolioProject);

  List<PortfolioDto> selectTree(String portfolioUuid);

  Set<String> selectReferences(String portfolioUuid);

  List<PortfolioDto> selectReferencersByKey(String referenceKey);

  Set<String> selectProjects(String portfolioUuid);

  List<ReferenceDto> selectAllReferencesToPortfolios();

  Set<String> selectAllProjectsInHierarchy(String rootUuid);

  void update(PortfolioDto portfolio);

  List<PortfolioDto> selectAllRoots();

  void deleteReferencesTo(String referenceUuid);

  void deleteProjects(String portfolioUuid);
}
