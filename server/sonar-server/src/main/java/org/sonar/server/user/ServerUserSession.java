/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.user;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ResourceDao;
import org.sonar.db.component.ResourceDto;
import org.sonar.db.user.AuthorizationDao;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

/**
 * Part of the current HTTP session
 */
public class ServerUserSession extends AbstractUserSession<ServerUserSession> {
  private Map<String, String> projectKeyByComponentKey = newHashMap();

  private final DbClient dbClient;
  private final AuthorizationDao authorizationDao;
  private final ResourceDao resourceDao;

  private ServerUserSession(DbClient dbClient, @Nullable UserDto userDto) {
    super(ServerUserSession.class);
    this.dbClient = dbClient;
    this.authorizationDao = dbClient.authorizationDao();
    this.resourceDao = dbClient.resourceDao();
    this.globalPermissions = null;
    if(userDto != null){
      this.setLogin(userDto.getLogin());
      this.setName(userDto.getName());
      this.setUserId(userDto.getId().intValue());
      this.userGroups.addAll(getUserGroups(userDto.getLogin()));
    }
  }

  public static ServerUserSession createForUser(DbClient dbClient, UserDto userDto){
    requireNonNull(userDto, "UserDto must not be null");
    return new ServerUserSession(dbClient, userDto);
  }

  public static ServerUserSession createForAnonymous(DbClient dbClient){
    return new ServerUserSession(dbClient, null);
  }

  private Set<String> getUserGroups(String userLogin) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      return new HashSet<>(dbClient.groupDao().selectByUserLogin(dbSession, userLogin).stream().map(GroupDto::getName).collect(Collectors.toSet()));
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  @Override
  public List<String> globalPermissions() {
    if (globalPermissions == null) {
      List<String> permissionKeys = authorizationDao.selectGlobalPermissions(login);
      globalPermissions = new ArrayList<>();
      for (String permissionKey : permissionKeys) {
        globalPermissions.add(permissionKey);
      }
    }
    return globalPermissions;
  }

  private boolean hasProjectPermission(String permission, String projectKey) {
    if (!projectPermissionsCheckedByKey.contains(permission)) {
      Collection<String> projectKeys = authorizationDao.selectAuthorizedRootProjectsKeys(userId, permission);
      for (String key : projectKeys) {
        projectKeyByPermission.put(permission, key);
      }
      projectPermissionsCheckedByKey.add(permission);
    }
    return projectKeyByPermission.get(permission).contains(projectKey);
  }

  // To keep private
  private boolean hasProjectPermissionByUuid(String permission, String projectUuid) {
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
    if (hasPermission(permission)) {
      return true;
    }

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
    if (hasPermission(permission)) {
      return true;
    }

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
