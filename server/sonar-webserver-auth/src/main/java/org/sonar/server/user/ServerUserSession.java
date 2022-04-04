/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.user;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.StringUtils.defaultIfEmpty;
import static org.sonar.api.web.UserRole.PUBLIC_PERMISSIONS;

/**
 * Implementation of {@link UserSession} used in web server
 */
public class ServerUserSession extends AbstractUserSession {
  @CheckForNull
  private final UserDto userDto;
  private final DbClient dbClient;
  private final Map<String, String> projectUuidByComponentUuid = new HashMap<>();
  private Collection<GroupDto> groups;
  private Boolean isSystemAdministrator;
  private Set<GlobalPermission> permissions;
  private Map<String, Set<String>> permissionsByProjectUuid;

  public ServerUserSession(DbClient dbClient, @Nullable UserDto userDto) {
    this.dbClient = dbClient;
    this.userDto = userDto;
  }

  private Collection<GroupDto> loadGroups() {
    if (this.userDto == null) {
      return Collections.emptyList();
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.groupDao().selectByUserLogin(dbSession, userDto.getLogin());
    }
  }

  @Override
  @CheckForNull
  public Long getLastSonarlintConnectionDate() {
    return userDto == null ? null : userDto.getLastSonarlintConnectionDate();
  }

  @Override
  @CheckForNull
  public String getLogin() {
    return userDto == null ? null : userDto.getLogin();
  }

  @Override
  @CheckForNull
  public String getUuid() {
    return userDto == null ? null : userDto.getUuid();
  }

  @Override
  @CheckForNull
  public String getName() {
    return userDto == null ? null : userDto.getName();
  }

  @Override
  public Collection<GroupDto> getGroups() {
    if (groups == null) {
      groups = loadGroups();
    }
    return groups;
  }

  @Override
  public boolean shouldResetPassword() {
    return userDto != null && userDto.isResetPassword();
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
  public Optional<IdentityProvider> getIdentityProvider() {
    return ofNullable(userDto).map(d -> computeIdentity(d).getIdentityProvider());
  }

  @Override
  public Optional<ExternalIdentity> getExternalIdentity() {
    return ofNullable(userDto).map(d -> computeIdentity(d).getExternalIdentity());
  }

  @Override
  protected boolean hasPermissionImpl(GlobalPermission permission) {
    if (permissions == null) {
      permissions = loadGlobalPermissions();
    }
    return permissions.contains(permission);
  }

  @Override
  protected Optional<String> componentUuidToProjectUuid(String componentUuid) {
    String projectUuid = projectUuidByComponentUuid.get(componentUuid);
    if (projectUuid != null) {
      return of(projectUuid);
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<ComponentDto> component = dbClient.componentDao().selectByUuid(dbSession, componentUuid);
      if (!component.isPresent()) {
        return Optional.empty();
      }
      // if component is part of a branch, then permissions must be
      // checked on the project (represented by its main branch)
      projectUuid = defaultIfEmpty(component.get().getMainBranchProjectUuid(), component.get().projectUuid());
      projectUuidByComponentUuid.put(componentUuid, projectUuid);
      return of(projectUuid);
    }
  }

  @Override
  protected boolean hasProjectUuidPermission(String permission, String projectUuid) {
    if (permissionsByProjectUuid == null) {
      permissionsByProjectUuid = new HashMap<>();
    }
    Set<String> projectPermissions = permissionsByProjectUuid.computeIfAbsent(projectUuid, this::loadProjectPermissions);
    return projectPermissions.contains(permission);
  }

  /**
   * Also applies to views
   */
  private Set<String> loadProjectPermissions(String projectUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<ComponentDto> component = dbClient.componentDao().selectByUuid(dbSession, projectUuid);
      if (!component.isPresent()) {
        return Collections.emptySet();
      }
      if (component.get().isPrivate()) {
        return loadDbPermissions(dbSession, projectUuid);
      }
      Set<String> projectPermissions = new HashSet<>();
      projectPermissions.addAll(PUBLIC_PERMISSIONS);
      projectPermissions.addAll(loadDbPermissions(dbSession, projectUuid));
      return Collections.unmodifiableSet(projectPermissions);
    }
  }

  private Set<GlobalPermission> loadGlobalPermissions() {
    Set<String> permissionKeys;
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (userDto != null && userDto.getUuid() != null) {
        permissionKeys = dbClient.authorizationDao().selectGlobalPermissions(dbSession, userDto.getUuid());
      } else {
        permissionKeys = dbClient.authorizationDao().selectGlobalPermissionsOfAnonymous(dbSession);
      }
    }
    return permissionKeys.stream()
      .map(GlobalPermission::fromKey)
      .collect(MoreCollectors.toSet(permissionKeys.size()));
  }

  private Set<String> loadDbPermissions(DbSession dbSession, String projectUuid) {
    if (userDto != null && userDto.getUuid() != null) {
      return dbClient.authorizationDao().selectProjectPermissions(dbSession, projectUuid, userDto.getUuid());
    }
    return dbClient.authorizationDao().selectProjectPermissionsOfAnonymous(dbSession, projectUuid);
  }

  @Override
  protected List<ComponentDto> doKeepAuthorizedComponents(String permission, Collection<ComponentDto> components) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Set<String> projectUuids = components.stream()
        .map(c -> defaultIfEmpty(c.getMainBranchProjectUuid(), c.projectUuid()))
        .collect(MoreCollectors.toSet(components.size()));
      Set<String> authorizedProjectUuids = dbClient.authorizationDao().keepAuthorizedProjectUuids(dbSession, projectUuids, getUuid(), permission);

      return components.stream()
        .filter(c -> authorizedProjectUuids.contains(c.projectUuid()) || authorizedProjectUuids.contains(c.getMainBranchProjectUuid()))
        .collect(MoreCollectors.toList(components.size()));
    }
  }

  @Override
  public boolean isSystemAdministrator() {
    if (isSystemAdministrator == null) {
      isSystemAdministrator = loadIsSystemAdministrator();
    }
    return isSystemAdministrator;
  }

  private boolean loadIsSystemAdministrator() {
    if (isRoot()) {
      return true;
    }
    return hasPermission(GlobalPermission.ADMINISTER);
  }
}
