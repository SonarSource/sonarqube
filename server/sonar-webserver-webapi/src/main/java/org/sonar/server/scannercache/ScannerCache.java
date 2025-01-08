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
package org.sonar.server.scannercache;

import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbInputStream;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.scannercache.ScannerAnalysisCacheDao;

@ServerSide
public class ScannerCache {
  private final DbClient dbClient;
  private final ScannerAnalysisCacheDao dao;
  private final ProjectDao projectDao;
  private final BranchDao branchDao;

  public ScannerCache(DbClient dbClient, ScannerAnalysisCacheDao dao, ProjectDao projectDao, BranchDao branchDao) {
    this.dbClient = dbClient;
    this.dao = dao;
    this.projectDao = projectDao;
    this.branchDao = branchDao;
  }

  @CheckForNull
  public DbInputStream get(String branchUuid) {
    try (DbSession session = dbClient.openSession(false)) {
      return dao.selectData(session, branchUuid);
    }
  }

  /**
   * clear all the cache.
   */
  public void clear() {
    doClear(null, null);
  }

  /**
   * clear the project cache, that is the cache of all the branches of the project.
   *
   * @param projectKey the key of the project.
   */
  public void clearProject(String projectKey) {
    doClear(projectKey, null);
  }

  /**
   * clear the branch cache, that is the cache of this branch only.
   *
   * @param projectKey the key of the project.
   * @param branchKey  the key of the specific branch.
   */
  public void clearBranch(String projectKey, String branchKey) {
    doClear(projectKey, branchKey);
  }

  private void doClear(@Nullable final String projectKey, @Nullable final String branchKey) {
    try (final DbSession session = dbClient.openSession(true)) {
      if (projectKey == null) {
        dao.removeAll(session);
      } else {
        Optional<ProjectDto> projectDto = projectDao.selectProjectByKey(session, projectKey);
        projectDto.stream().flatMap(pDto -> {
            if (branchKey == null) {
              return branchDao.selectByProject(session, pDto).stream();
            } else {
              return branchDao.selectByKeys(session, pDto.getUuid(), Set.of(branchKey)).stream();
            }
          })
          .map(BranchDto::getUuid)
          .forEach(bUuid -> dao.remove(session, bUuid));
      }
      session.commit();
    }
  }
}
