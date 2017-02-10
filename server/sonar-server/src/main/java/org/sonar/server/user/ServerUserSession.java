/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationFlags;

import static com.google.common.collect.Maps.newHashMap;

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
  private SetMultimap<String, String> permissionsByOrganizationUuid;
  private SetMultimap<String, String> permissionsByProjectUuid;

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
  protected boolean hasOrganizationPermissionImpl(String organizationUuid, String permission) {
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
      projectUuid = component.get().projectUuid();
      projectUuidByComponentUuid.put(componentUuid, projectUuid);
      return Optional.of(projectUuid);
    }
  }

  @Override
  protected boolean hasProjectUuidPermission(String permission, String projectUuid) {
    if (permissionsByProjectUuid == null) {
      permissionsByProjectUuid = HashMultimap.create();
    }
    Set<String> permissions;
    if (permissionsByProjectUuid.containsKey(projectUuid)) {
      permissions = permissionsByProjectUuid.get(projectUuid);
    } else {
      permissions = loadProjectPermissions(projectUuid);
      permissionsByProjectUuid.putAll(projectUuid, permissions);
    }
    return permissions.contains(permission);
  }

  private Set<String> loadProjectPermissions(String projectUuid) {
    Set<String> permissions;
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (userDto != null && userDto.getId() != null) {
        permissions = dbClient.authorizationDao().selectProjectPermissions(dbSession, projectUuid, userDto.getId());
      } else {
        permissions = dbClient.authorizationDao().selectProjectPermissionsOfAnonymous(dbSession, projectUuid);
      }
    }
    return permissions;
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
        return hasOrganizationPermission(uuidOfDefaultOrg, GlobalPermissions.SYSTEM_ADMIN);
      }
      // organization feature is enabled -> requires to be root
      return false;
    }
  }
}
