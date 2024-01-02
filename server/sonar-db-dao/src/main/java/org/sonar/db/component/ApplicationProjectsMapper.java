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
import java.util.Set;
import org.apache.ibatis.annotations.Param;
import org.sonar.db.project.ProjectDto;

public interface ApplicationProjectsMapper {
  void addProject(
    @Param("uuid") String uuid,
    @Param("applicationUuid") String applicationUuid,
    @Param("projectUuid") String projectUuid,
    @Param("now") long now);

  void removeApplicationBranchProjectBranchesByApplicationAndProject(
    @Param("applicationUuid") String applicationUuid,
    @Param("projectUuid") String projectUuid);

  void removeApplicationProjectsByApplicationAndProject(
    @Param("applicationUuid") String applicationUuid,
    @Param("projectUuid") String projectUuid);

  Set<ProjectDto> selectProjects(@Param("applicationUuid") String applicationUuid);

  void removeApplicationProjectsByApplication(String applicationUuid);

  void removeApplicationBranchProjectBranchesByApplication(String applicationUuid);

  void addProjectBranchToAppBranch(
    @Param("uuid") String uuid,
    @Param("applicationUuid") String applicationUuid,
    @Param("applicationBranchUuid") String applicationBranchUuid,
    @Param("projectUuid") String projectUuid,
    @Param("projectBranchUuid") String projectBranchUuid,
    @Param("now") long now);

  void removeProjectBranchFromAppBranch(@Param("applicationBranchUuid") String applicationBranchUuid, @Param("projectBranchUuid") String projectBranchUuid);

  Set<BranchDto> selectProjectBranchesFromAppBranchUuid(@Param("applicationBranchUuid") String applicationBranchUuid);

  Set<BranchDto> selectProjectBranchesFromAppBranchKey(@Param("applicationUuid") String applicationUuid, @Param("applicationBranchKey") String applicationBranchKey);

  int countApplicationProjects(@Param("applicationUuid") String applicationUuid);

  Set<ProjectDto> selectApplicationsFromProjectBranch(@Param("projectUuid") String projectUuid, @Param("branchKey") String branchKey);

  Set<ProjectDto> selectApplicationsFromProjects(@Param("projectUuids") Collection<String> projectUuids);

  void removeAllProjectBranchesOfAppBranch(@Param("applicationBranchUuid") String applicationBranchUuid);
}
