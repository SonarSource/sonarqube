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
package org.sonar.server.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.component.ResourceDao;
import org.sonar.db.component.ResourceDto;
import org.sonar.db.user.AuthorizationDao;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

/**
 * Part of the current HTTP session
 */
public class ServerUserSession extends AbstractUserSession<ServerUserSession>
    implements UserSession {

  private static final Logger LOG = Loggers.get(ServerUserSession.class);

  private Map<String, String> projectKeyByComponentKey = newHashMap();

  private final AuthorizationDao authorizationDao;
  private final ResourceDao resourceDao;

  ServerUserSession(AuthorizationDao authorizationDao, ResourceDao resourceDao) {
    super(ServerUserSession.class);
    this.globalPermissions = null;
    this.authorizationDao = authorizationDao;
    this.resourceDao = resourceDao;
    // Do not forget that when forceAuthentication is set to true, the Anyone group should not be set (but this will be check when
    // authentication will be done in Java)
    this.userGroups = newHashSet(DefaultGroups.ANYONE);
  }

  @Override
  public List<String> globalPermissions() {
    if (globalPermissions == null) {
      List<String> permissionKeys = authorizationDao.selectGlobalPermissions(login);
      globalPermissions = new ArrayList<>();
      for (String permissionKey : permissionKeys) {
        if (!GlobalPermissions.ALL.contains(permissionKey)) {
          LOG.warn("Ignoring unknown permission {} for user {}", permissionKey, login);
        } else {
          globalPermissions.add(permissionKey);
        }
      }
    }
    return globalPermissions;
  }

  @Override
  public boolean hasProjectPermission(String permission, String projectKey) {
    if (!projectPermissionsCheckedByKey.contains(permission)) {
      Collection<String> projectKeys = authorizationDao.selectAuthorizedRootProjectsKeys(userId, permission);
      for (String key : projectKeys) {
        projectKeyByPermission.put(permission, key);
      }
      projectPermissionsCheckedByKey.add(permission);
    }
    return projectKeyByPermission.get(permission).contains(projectKey);
  }

  @Override
  public boolean hasProjectPermissionByUuid(String permission, String projectUuid) {
    if (!projectPermissionsCheckedByUuid.contains(permission)) {
      Collection<String> projectUuids = authorizationDao.selectAuthorizedRootProjectsUuids(userId, permission);
      addProjectPermission(permission, projectUuids);
    }
    return projectUuidByPermission.get(permission).contains(projectUuid);
  }

  private void addProjectPermission(String permission, Collection<String> authorizedProjectUuids) {
    for (String key : authorizedProjectUuids) {
      projectUuidByPermission.put(permission, key);
    }
    projectPermissionsCheckedByUuid.add(permission);
  }

  @Override
  public boolean hasComponentPermission(String permission, String componentKey) {
    String projectKey = projectKeyByComponentKey.get(componentKey);
    if (projectKey == null) {
      ResourceDto project = resourceDao.getRootProjectByComponentKey(componentKey);
      if (project == null) {
        return false;
      }
      projectKey = project.getKey();
    }
    boolean hasComponentPermission = hasProjectPermission(permission, projectKey);
    if (hasComponentPermission) {
      projectKeyByComponentKey.put(componentKey, projectKey);
      return true;
    }
    return false;
  }

  @Override
  public boolean hasComponentUuidPermission(String permission, String componentUuid) {
    String projectUuid = projectUuidByComponentUuid.get(componentUuid);
    if (projectUuid == null) {
      ResourceDto project = resourceDao.selectResource(componentUuid);
      if (project == null) {
        return false;
      }
      projectUuid = project.getProjectUuid();
    }
    boolean hasComponentPermission = hasProjectPermissionByUuid(permission, projectUuid);
    if (hasComponentPermission) {
      projectUuidByComponentUuid.put(componentUuid, projectUuid);
      return true;
    }
    return false;
  }

}
