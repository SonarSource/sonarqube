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

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceDao;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Objects.requireNonNull;

/**
 * Implementation of {@link UserSession} used in web server
 */
public class ServerUserSession extends AbstractUserSession {
  @CheckForNull
  private final UserDto userDto;
  private final DbClient dbClient;
  private final ResourceDao resourceDao;
  private final Supplier<List<GroupDto>> groups;
  private List<String> globalPermissions = null;
  private SetMultimap<String, String> projectKeyByPermission = HashMultimap.create();
  private SetMultimap<String, String> projectUuidByPermission = HashMultimap.create();
  private SetMultimap<String, String> permissionsByOrganizationUuid;
  private Map<String, String> projectUuidByComponentUuid = newHashMap();
  private List<String> projectPermissionsCheckedByUuid = new ArrayList<>();

  private ServerUserSession(DbClient dbClient, @Nullable UserDto userDto) {
    this.userDto = userDto;
    this.dbClient = dbClient;
    this.resourceDao = dbClient.resourceDao();
    this.groups = Suppliers.memoize(this::loadGroups);
  }

  public static ServerUserSession createForUser(DbClient dbClient, UserDto userDto) {
    requireNonNull(userDto, "UserDto must not be null");
    return new ServerUserSession(dbClient, userDto);
  }

  public static ServerUserSession createForAnonymous(DbClient dbClient) {
    return new ServerUserSession(dbClient, null);
  }

  private List<GroupDto> loadGroups() {
    if (this.userDto == null) {
      return Collections.emptyList();
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.groupDao().selectByUserLogin(dbSession, userDto.getLogin());
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
  public Collection<GroupDto> getGroups() {
    return groups.get();
  }

  @Override
  public boolean isLoggedIn() {
    return userDto != null;
  }

  @Override
  public boolean isRoot() {
    return userDto != null && userDto.isRoot();
  }

  @Override
  public boolean hasOrganizationPermission(String organizationUuid, String permission) {
    if (permissionsByOrganizationUuid == null) {
      permissionsByOrganizationUuid = HashMultimap.create();
    }
    Set<String> permissions;
    if (permissionsByOrganizationUuid.containsKey(organizationUuid)) {
      permissions = permissionsByOrganizationUuid.get(organizationUuid);
    } else {
      permissions = loadOrganizationPermissions(organizationUuid);
      permissionsByOrganizationUuid.putAll(organizationUuid, permissions);
    }
    return permissions.contains(permission);
  }

  private Set<String> loadOrganizationPermissions(String organizationUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (userDto != null && userDto.getId() != null) {
        return dbClient.authorizationDao().selectOrganizationPermissions(dbSession, organizationUuid, userDto.getId());
      }
      return dbClient.authorizationDao().selectOrganizationPermissionsOfAnonymous(dbSession, organizationUuid);
    }
  }

  @Override
  public List<String> globalPermissions() {
    if (globalPermissions == null) {
      List<String> permissionKeys = dbClient.authorizationDao().selectGlobalPermissions(getLogin());
      globalPermissions = ImmutableList.copyOf(permissionKeys);
    }
    return globalPermissions;
  }

  @Override
  public boolean hasComponentUuidPermission(String permission, String componentUuid) {
    if (isRoot()) {
      return true;
    }

    String projectUuid = projectUuidByComponentUuid.get(componentUuid);
    if (projectUuid == null) {
      try (DbSession dbSession = dbClient.openSession(false)) {
        Optional<ComponentDto> component = dbClient.componentDao().selectByUuid(dbSession, componentUuid);
        if (!component.isPresent()) {
          return false;
        }
        projectUuid = component.get().projectUuid();
        if (hasOrganizationPermission(component.get().getOrganizationUuid(), permission)) {
          projectUuidByComponentUuid.put(componentUuid, projectUuid);
          return true;
        }
      }
    }
    boolean hasComponentPermission = hasProjectPermissionByUuid(permission, projectUuid);
    if (hasComponentPermission) {
      projectUuidByComponentUuid.put(componentUuid, projectUuid);
      return true;
    }
    return false;
  }

  // To keep private
  private boolean hasProjectPermissionByUuid(String permission, String projectUuid) {
    if (!projectPermissionsCheckedByUuid.contains(permission)) {
      try (DbSession dbSession = dbClient.openSession(false)) {
        Collection<String> projectUuids = dbClient.authorizationDao().selectAuthorizedRootProjectsUuids(dbSession, getUserId(), permission);
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

}
