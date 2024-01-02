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
package org.sonar.server.batch;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.FilePathWithHashDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.scanner.protocol.input.FileData;
import org.sonar.scanner.protocol.input.ProjectRepositories;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.user.UserSession;

import static org.sonar.server.exceptions.BadRequestException.checkRequest;

@ServerSide
public class ProjectDataLoader {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public ProjectDataLoader(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  public ProjectRepositories load(ProjectDataQuery query) {
    try (DbSession session = dbClient.openSession(false)) {
      String projectKey = query.getProjectKey();
      String branch = query.getBranch();
      String pullRequest = query.getPullRequest();
      ComponentDto project = componentFinder.getByKey(session, projectKey);
      checkRequest(project.isRootProject(), "Key '%s' belongs to a component which is not a Project", projectKey);
      boolean hasScanPerm = userSession.hasComponentPermission(UserRole.SCAN, project) || userSession.hasPermission(GlobalPermission.SCAN);
      checkPermission(hasScanPerm);
      ComponentDto branchComponent = (branch == null && pullRequest == null) ? project
        : componentFinder.getByKeyAndOptionalBranchOrPullRequest(session, projectKey, branch, pullRequest);

      List<FilePathWithHashDto> files = searchFilesWithHashAndRevision(session, branchComponent);
      ProjectRepositories repository = new ProjectRepositories();
      addFileData(repository, files);
      return repository;
    }
  }

  private List<FilePathWithHashDto> searchFilesWithHashAndRevision(DbSession session, @Nullable ComponentDto branchComponent) {
    if (branchComponent == null) {
      return Collections.emptyList();
    }
    return dbClient.componentDao().selectEnabledFilesFromProject(session, branchComponent.uuid());
  }

  private static void addFileData(ProjectRepositories data, List<FilePathWithHashDto> files) {
    for (FilePathWithHashDto file : files) {
      FileData fileData = new FileData(file.getSrcHash(), file.getRevision());
      data.addFileData(file.getPath(), fileData);
    }
  }

  private static void checkPermission(boolean hasScanPerm) {
    if (!hasScanPerm) {
      throw new ForbiddenException("You're not authorized to push analysis results to the SonarQube server. " +
        "Please contact your SonarQube administrator.");
    }
  }

}
