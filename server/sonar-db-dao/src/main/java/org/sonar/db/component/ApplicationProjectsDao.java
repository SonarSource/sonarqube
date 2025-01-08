/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;

public class ApplicationProjectsDao implements Dao {
  private final System2 system2;
  private final UuidFactory uuidFactory;

  public ApplicationProjectsDao(System2 system2, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  public void addProject(DbSession dbSession, String applicationUuid, String projectUuid) {
    getMapper(dbSession).addProject(uuidFactory.create(), applicationUuid, projectUuid, system2.now());
  }

  public void removeApplicationProjectsByApplicationAndProject(DbSession dbSession, String applicationUuid, String projectUuid) {
    getMapper(dbSession).removeApplicationBranchProjectBranchesByApplicationAndProject(applicationUuid, projectUuid);
    getMapper(dbSession).removeApplicationProjectsByApplicationAndProject(applicationUuid, projectUuid);
  }

  public int countApplicationProjects(DbSession dbSession, String applicationUuid) {
    return getMapper(dbSession).countApplicationProjects(applicationUuid);
  }

  public Set<ProjectDto> selectProjects(DbSession dbSession, String applicationUuid) {
    return getMapper(dbSession).selectProjects(applicationUuid);
  }

  public void addProjectBranchToAppBranch(DbSession dbSession, BranchDto applicationBranch, BranchDto projectBranch) {
    getMapper(dbSession).addProjectBranchToAppBranch(
      uuidFactory.create(),
      applicationBranch.getProjectUuid(),
      applicationBranch.getUuid(),
      projectBranch.getProjectUuid(),
      projectBranch.getUuid(),
      system2.now());
  }

  public void addProjectBranchToAppBranch(DbSession dbSession, String applicationUuid, String applicationBranchUuid, String projectUuid, String projectBranchUuid) {
    getMapper(dbSession).addProjectBranchToAppBranch(
      uuidFactory.create(),
      applicationUuid,
      applicationBranchUuid,
      projectUuid,
      projectBranchUuid,
      system2.now());
  }

  public void removeProjectBranchFromAppBranch(DbSession dbSession, String applicationBranchUuid, String projectBranchUuid) {
    getMapper(dbSession).removeProjectBranchFromAppBranch(applicationBranchUuid, projectBranchUuid);
  }

  public Set<BranchDto> selectProjectBranchesFromAppBranchUuid(DbSession dbSession, String applicationBranchUuid) {
    return getMapper(dbSession).selectProjectBranchesFromAppBranchUuid(applicationBranchUuid);
  }

  public Set<BranchDto> selectProjectBranchesFromAppBranchKey(DbSession dbSession, String applicationUuid, String applicationBranchKey) {
    return getMapper(dbSession).selectProjectBranchesFromAppBranchKey(applicationUuid, applicationBranchKey);
  }

  public Set<ProjectDto> selectApplicationsFromProjectBranch(DbSession dbSession, String projectUuid, String branchKey) {
    return getMapper(dbSession).selectApplicationsFromProjectBranch(projectUuid, branchKey);
  }

  public Set<ProjectDto> selectApplicationsFromProjects(DbSession dbSession, Collection<String> projectUuids) {
    return getMapper(dbSession).selectApplicationsFromProjects(projectUuids);
  }

  public List<BranchDto> selectProjectsMainBranchesOfApplication(DbSession dbSession, String applicationUuid) {
    return getMapper(dbSession).selectProjectsMainBranchesOfApplication(applicationUuid);
  }

  private static ApplicationProjectsMapper getMapper(DbSession session) {
    return session.getMapper(ApplicationProjectsMapper.class);
  }
}
