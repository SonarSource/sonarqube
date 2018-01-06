/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.core.permission.ProjectPermissions;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationFlags;

import static com.google.common.collect.Maps.newHashMap;
import static org.apache.commons.lang.StringUtils.defaultIfEmpty;

/**
 * Implementation of {@link UserSession} used in web server
 */
public class ServerUserSession extends AbstractUserSession {
  @CheckForNull
  private final UserDto userDto;
  private final DbClient dbClient;
  private final OrganizationFlags organizationFlags;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final Supplier<Collection<GroupDto>> groups = Suppliers.memoize(this::loadGroups);
  private final Supplier<Boolean> isSystemAdministratorSupplier = Suppliers.memoize(this::loadIsSystemAdministrator);
  private final Map<String, String> projectUuidByComponentUuid = newHashMap();
  private Map<String, Set<OrganizationPermission>> permissionsByOrganizationUuid;
  private Map<String, Set<String>> permissionsByProjectUuid;

  ServerUserSession(DbClient dbClient, OrganizationFlags organizationFlags,
    DefaultOrganizationProvider defaultOrganizationProvider, @Nullable UserDto userDto) {
    this.dbClient = dbClient;
    this.organizationFlags = organizationFlags;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
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
    return userDto == null ? null : userDto.getId();
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
  protected boolean hasPermissionImpl(OrganizationPermission permission, String organizationUuid) {
    if (permissionsByOrganizationUuid == null) {
      permissionsByOrganizationUuid = new HashMap<>();
    }
    Set<OrganizationPermission> permissions = permissionsByOrganizationUuid.computeIfAbsent(organizationUuid, this::loadOrganizationPermissions);
    return permissions.contains(permission);
  }

  private Set<OrganizationPermission> loadOrganizationPermissions(String organizationUuid) {
    Set<String> permissionKeys;
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (userDto != null && userDto.getId() != null) {
        permissionKeys = dbClient.authorizationDao().selectOrganizationPermissions(dbSession, organizationUuid, userDto.getId());
      } else {
        permissionKeys = dbClient.authorizationDao().selectOrganizationPermissionsOfAnonymous(dbSession, organizationUuid);
      }
    }
    return permissionKeys.stream()
      .map(OrganizationPermission::fromKey)
      .collect(MoreCollectors.toSet(permissionKeys.size()));
  }

  @Override
  protected Optional<String> componentUuidToProjectUuid(String componentUuid) {
    String projectUuid = projectUuidByComponentUuid.get(componentUuid);
    if (projectUuid != null) {
      return Optional.of(projectUuid);
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      com.google.common.base.Optional<ComponentDto> component = dbClient.componentDao().selectByUuid(dbSession, componentUuid);
      if (!component.isPresent()) {
        return Optional.empty();
      }
      // if component is part of a branch, then permissions must be
      // checked on the project (represented by its main branch)
      projectUuid = defaultIfEmpty(component.get().getMainBranchProjectUuid(), component.get().projectUuid());
      projectUuidByComponentUuid.put(componentUuid, projectUuid);
      return Optional.of(projectUuid);
    }
  }

  @Override
  protected boolean hasProjectUuidPermission(String permission, String projectUuid) {
    if (permissionsByProjectUuid == null) {
      permissionsByProjectUuid = new HashMap<>();
    }
    Set<String> permissions = permissionsByProjectUuid.computeIfAbsent(projectUuid, this::loadProjectPermissions);
    return permissions.contains(permission);
  }

  private Set<String> loadProjectPermissions(String projectUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      com.google.common.base.Optional<ComponentDto> component = dbClient.componentDao().selectByUuid(dbSession, projectUuid);
      if (!component.isPresent()) {
        return Collections.emptySet();
      }
      if (component.get().isPrivate()) {
        return loadDbPermissions(dbSession, projectUuid);
      }
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();
      builder.addAll(ProjectPermissions.PUBLIC_PERMISSIONS);
      builder.addAll(loadDbPermissions(dbSession, projectUuid));
      return builder.build();
    }
  }

  private Set<String> loadDbPermissions(DbSession dbSession, String projectUuid) {
    if (userDto != null && userDto.getId() != null) {
      return dbClient.authorizationDao().selectProjectPermissions(dbSession, projectUuid, userDto.getId());
    }
    return dbClient.authorizationDao().selectProjectPermissionsOfAnonymous(dbSession, projectUuid);
  }

  @Override
  protected List<ComponentDto> doKeepAuthorizedComponents(String permission, Collection<ComponentDto> components) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Set<String> projectUuids = components.stream()
        .map(c -> defaultIfEmpty(c.getMainBranchProjectUuid(), c.projectUuid()))
        .collect(MoreCollectors.toSet(components.size()));
      Set<String> authorizedProjectUuids = dbClient.authorizationDao().keepAuthorizedProjectUuids(dbSession, projectUuids, getUserId(), permission);

      return components.stream()
        .filter(c -> authorizedProjectUuids.contains(c.projectUuid()) || authorizedProjectUuids.contains(c.getMainBranchProjectUuid()))
        .collect(MoreCollectors.toList(components.size()));
    }
  }

  @Override
  public boolean isSystemAdministrator() {
    return isSystemAdministratorSupplier.get();
  }

  private boolean loadIsSystemAdministrator() {
    if (isRoot()) {
      return true;
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (!organizationFlags.isEnabled(dbSession)) {
        String uuidOfDefaultOrg = defaultOrganizationProvider.get().getUuid();
        return hasPermission(OrganizationPermission.ADMINISTER, uuidOfDefaultOrg);
      }
      // organization feature is enabled -> requires to be root
      return false;
    }
  }
}
