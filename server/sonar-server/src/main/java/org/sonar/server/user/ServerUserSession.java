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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.security.DefaultGroups;
import org.sonar.core.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ResourceDao;
import org.sonar.db.component.ResourceDto;
import org.sonar.db.permission.PermissionDao;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Objects.requireNonNull;

/**
 * Part of the current HTTP session
 */
public class ServerUserSession extends AbstractUserSession {
  private Map<String, String> projectKeyByComponentKey = newHashMap();

  @CheckForNull
  private final UserDto userDto;
  private final DbClient dbClient;
  private final PermissionDao permissionDao;
  private final ResourceDao resourceDao;
  private final Set<String> userGroups;
  private List<String> globalPermissions = null;
  private HashMultimap<String, String> projectKeyByPermission = HashMultimap.create();
  private HashMultimap<String, String> projectUuidByPermission = HashMultimap.create();
  private Map<String, String> projectUuidByComponentUuid = newHashMap();
  private List<String> projectPermissionsCheckedByKey = new ArrayList<>();
  private List<String> projectPermissionsCheckedByUuid = new ArrayList<>();

  private ServerUserSession(DbClient dbClient, @Nullable UserDto userDto) {
    this.userDto = userDto;
    this.dbClient = dbClient;
    this.permissionDao = dbClient.permissionDao();
    this.resourceDao = dbClient.resourceDao();
    this.userGroups = loadUserGroups();
  }

  public static ServerUserSession createForUser(DbClient dbClient, UserDto userDto) {
    requireNonNull(userDto, "UserDto must not be null");
    return new ServerUserSession(dbClient, userDto);
  }

  public static ServerUserSession createForAnonymous(DbClient dbClient) {
    return new ServerUserSession(dbClient, null);
  }

  private Set<String> loadUserGroups() {
    if (this.userDto == null) {
      return Collections.singleton(DefaultGroups.ANYONE);
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      return Stream.concat(
        Stream.of(DefaultGroups.ANYONE),
        dbClient.groupDao().selectByUserLogin(dbSession, userDto.getLogin()).stream().map(GroupDto::getName))
        .collect(Collectors.toSet());
    }
  }

  @Override
  @CheckForNull
  public String getLogin() {
    return userDto == null ? null : userDto.getLogin();
  }

  @Override
  @CheckForNull
  public String getName() {
    return userDto == null ? null : userDto.getName();
  }

  @Override
  @CheckForNull
  public Integer getUserId() {
    return userDto == null ? null : userDto.getId().intValue();
  }

  @Override
  public Set<String> getUserGroups() {
    return userGroups;
  }

  @Override
  public boolean isLoggedIn() {
    return userDto != null;
  }

  @Override
  public Locale locale() {
    return Locale.ENGLISH;
  }

  @Override
  public List<String> globalPermissions() {
    if (globalPermissions == null) {
      List<String> permissionKeys = permissionDao.selectGlobalPermissions(getLogin());
      globalPermissions = ImmutableList.copyOf(permissionKeys);
    }
    return globalPermissions;
  }

  private boolean hasProjectPermission(String permission, String projectKey) {
    if (!projectPermissionsCheckedByKey.contains(permission)) {
      try (DbSession dbSession = dbClient.openSession(false)) {
        Collection<String> projectKeys = permissionDao.selectAuthorizedRootProjectsKeys(dbSession, getUserId(), permission);
        for (String key : projectKeys) {
          projectKeyByPermission.put(permission, key);
        }
        projectPermissionsCheckedByKey.add(permission);
      }
    }
    return projectKeyByPermission.get(permission).contains(projectKey);
  }

  // To keep private
  private boolean hasProjectPermissionByUuid(String permission, String projectUuid) {
    if (!projectPermissionsCheckedByUuid.contains(permission)) {
      try (DbSession dbSession = dbClient.openSession(false)) {
        Collection<String> projectUuids = permissionDao.selectAuthorizedRootProjectsUuids(dbSession, getUserId(), permission);
        addProjectPermission(permission, projectUuids);
      }
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
