/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.component;

import java.util.Set;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentKeyUpdaterDao;
import org.sonar.db.component.ResourceDto;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.es.ProjectIndexers;
import org.sonar.server.project.Project;
import org.sonar.server.project.ProjectLifeCycleListeners;
import org.sonar.server.project.RekeyedProject;
import org.sonar.server.user.UserSession;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.sonar.core.component.ComponentKeys.isValidProjectKey;
import static org.sonar.db.component.ComponentKeyUpdaterDao.checkIsProjectOrModule;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;

@ServerSide
public class ComponentService {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final ProjectIndexers projectIndexers;
  private final ProjectLifeCycleListeners projectLifeCycleListeners;

  public ComponentService(DbClient dbClient, UserSession userSession, ProjectIndexers projectIndexers, ProjectLifeCycleListeners projectLifeCycleListeners) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.projectIndexers = projectIndexers;
    this.projectLifeCycleListeners = projectLifeCycleListeners;
  }

  public void updateKey(DbSession dbSession, ComponentDto projectOrModule, String newKey) {
    userSession.checkComponentPermission(UserRole.ADMIN, projectOrModule);
    checkIsProjectOrModule(projectOrModule);
    checkProjectOrModuleKeyFormat(newKey);
    dbClient.componentKeyUpdaterDao().updateKey(dbSession, projectOrModule.uuid(), newKey);
    projectIndexers.commitAndIndex(dbSession, singletonList(projectOrModule), ProjectIndexer.Cause.PROJECT_KEY_UPDATE);
    if (isMainProject(projectOrModule)) {
      Project newProject = new Project(projectOrModule.uuid(), newKey, projectOrModule.name(), projectOrModule.description(), projectOrModule.getTags());
      projectLifeCycleListeners.onProjectsRekeyed(singleton(new RekeyedProject(newProject, projectOrModule.getDbKey())));
    }
  }

  private static boolean isMainProject(ComponentDto projectOrModule) {
    return projectOrModule.isRootProject() && projectOrModule.getMainBranchProjectUuid() == null;
  }

  public void bulkUpdateKey(DbSession dbSession, ComponentDto projectOrModule, String stringToReplace, String replacementString) {
    Set<ComponentKeyUpdaterDao.RekeyedResource> rekeyedProjects = dbClient.componentKeyUpdaterDao().bulkUpdateKey(
      dbSession, projectOrModule.uuid(), stringToReplace, replacementString,
      ComponentService::isMainProject);
    projectIndexers.commitAndIndex(dbSession, singletonList(projectOrModule), ProjectIndexer.Cause.PROJECT_KEY_UPDATE);
    if (!rekeyedProjects.isEmpty()) {
      projectLifeCycleListeners.onProjectsRekeyed(rekeyedProjects.stream()
        .map(ComponentService::toRekeyedProject)
        .collect(MoreCollectors.toSet(rekeyedProjects.size())));
    }
  }

  private static boolean isMainProject(ComponentKeyUpdaterDao.RekeyedResource rekeyedResource) {
    ResourceDto resource = rekeyedResource.getResource();
    String resourceKey = resource.getKey();
    return Scopes.PROJECT.equals(resource.getScope())
      && Qualifiers.PROJECT.equals(resource.getQualifier())
      && !resourceKey.contains(ComponentDto.BRANCH_KEY_SEPARATOR)
      && !resourceKey.contains(ComponentDto.PULL_REQUEST_SEPARATOR);
  }

  private static RekeyedProject toRekeyedProject(ComponentKeyUpdaterDao.RekeyedResource rekeyedResource) {
    ResourceDto resource = rekeyedResource.getResource();
    Project project = new Project(resource.getUuid(), resource.getKey(), resource.getName(), resource.getDescription(), emptyList());
    return new RekeyedProject(project, rekeyedResource.getOldKey());
  }

  private static void checkProjectOrModuleKeyFormat(String key) {
    checkRequest(isValidProjectKey(key), "Malformed key for '%s'. It cannot be empty nor contain whitespaces.", key);
  }

}
