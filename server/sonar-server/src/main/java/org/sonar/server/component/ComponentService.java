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

package org.sonar.server.component;

import com.google.common.collect.ImmutableMap;
import org.sonar.api.ServerComponent;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.resource.ResourceKeyUpdaterDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.permission.InternalPermissionService;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;

import java.util.Date;

public class ComponentService implements ServerComponent {

  private final DbClient dbClient;

  private final ResourceKeyUpdaterDao resourceKeyUpdaterDao;
  private final InternalPermissionService permissionService;
  private final PreviewCache previewCache;

  public ComponentService(DbClient dbClient, ResourceKeyUpdaterDao resourceKeyUpdaterDao, InternalPermissionService permissionService, PreviewCache previewCache) {
    this.dbClient = dbClient;
    this.resourceKeyUpdaterDao = resourceKeyUpdaterDao;
    this.permissionService = permissionService;
    this.previewCache = previewCache;
  }

  public ComponentDto getByKey(String key) {
    DbSession session = dbClient.openSession(false);
    try {
      return dbClient.componentDao().getByKey(session, key);
    } finally {
      session.close();
    }
  }

  @CheckForNull
  public ComponentDto getNullableByKey(String key) {
    DbSession session = dbClient.openSession(false);
    try {
      return dbClient.componentDao().getNullableByKey(session, key);
    } finally {
      session.close();
    }
  }

  public ComponentDto getByKey(DbSession session, String key) {
    return dbClient.componentDao().getByKey(session, key);
  }

  public void updateKey(String projectOrModuleKey, String newKey) {
    UserSession.get().checkComponentPermission(UserRole.ADMIN, projectOrModuleKey);

    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto projectOrModule = getByKey(projectOrModuleKey);
      ComponentDto oldRootProject = dbClient.componentDao().getRootProjectByKey(projectOrModuleKey, session);

      resourceKeyUpdaterDao.updateKey(projectOrModule.getId(), newKey);
      session.commit();

      ComponentDto newRootProject = dbClient.componentDao().getRootProjectByKey(newKey, session);
      updateIssuesIndex(session, oldRootProject.key(), newRootProject.key());

      previewCache.reportResourceModification(newRootProject.key());

      session.commit();
    } finally {
      session.close();
    }
  }

  public void bulkUpdateKey(String projectKey, String stringToReplace, String replacementString) {
    UserSession.get().checkProjectPermission(UserRole.ADMIN, projectKey);

    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto project = getByKey(projectKey);

      resourceKeyUpdaterDao.bulkUpdateKey(project.getId(), stringToReplace, replacementString);
      session.commit();

      ComponentDto newProject = dbClient.componentDao().getById(project.getId(), session);
      updateIssuesIndex(session, projectKey, newProject.key());

      previewCache.reportResourceModification(newProject.key());

      session.commit();
    } finally {
      session.close();
    }
  }

  private void updateIssuesIndex(DbSession session, String oldKey, String newKey) {
    // Remove permission on old project key
    permissionService.synchronizePermissions(session, oldKey);

    // Add permission on new project key
    permissionService.synchronizePermissions(session, newKey);

    // Reindex issues on new project key
    dbClient.issueDao().synchronizeAfter(session, new Date(0),
      ImmutableMap.of("project", newKey));
  }

}
