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
package org.sonar.server.tester;

import com.google.common.collect.HashMultimap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.ProjectPermissions;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.server.user.AbstractUserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.newHashMap;

public abstract class AbstractMockUserSession<T extends AbstractMockUserSession> extends AbstractUserSession {
  private final Class<T> clazz;
  private HashMultimap<String, String> projectUuidByPermission = HashMultimap.create();
  private final HashMultimap<String, OrganizationPermission> permissionsByOrganizationUuid = HashMultimap.create();
  private Map<String, String> projectUuidByComponentUuid = newHashMap();
  private Set<String> projectPermissionsCheckedByUuid = new HashSet<>();
  private boolean systemAdministrator = false;

  protected AbstractMockUserSession(Class<T> clazz) {
    this.clazz = clazz;
  }

  public T addPermission(OrganizationPermission permission, String organizationUuid) {
    permissionsByOrganizationUuid.put(organizationUuid, permission);
    return clazz.cast(this);
  }

  @Override
  protected boolean hasPermissionImpl(OrganizationPermission permission, String organizationUuid) {
    return permissionsByOrganizationUuid.get(organizationUuid).contains(permission);
  }

  /**
   * Use this method to register public root component and non root components the UserSession must be aware of.
   * (ie. this method can be used to emulate the content of the DB)
   */
  public T registerComponents(ComponentDto... components) {
    Arrays.stream(components)
      .forEach(component -> {
        if (component.projectUuid().equals(component.uuid()) && !component.isPrivate()) {
          this.projectUuidByPermission.put(UserRole.USER, component.uuid());
          this.projectUuidByPermission.put(UserRole.CODEVIEWER, component.uuid());
          this.projectPermissionsCheckedByUuid.add(UserRole.USER);
          this.projectPermissionsCheckedByUuid.add(UserRole.CODEVIEWER);
        }
        this.projectUuidByComponentUuid.put(component.uuid(), component.projectUuid());
      });
    return clazz.cast(this);
  }

  public T addProjectPermission(String permission, ComponentDto... components) {
    Arrays.stream(components).forEach(component -> {
      checkArgument(
        component.isPrivate() || !ProjectPermissions.PUBLIC_PERMISSIONS.contains(permission),
        "public component %s can't be granted public permission %s", component.uuid(), permission);
    });
    registerComponents(components);
    this.projectPermissionsCheckedByUuid.add(permission);
    Arrays.stream(components)
      .forEach(component -> this.projectUuidByPermission.put(permission, component.projectUuid()));
    return clazz.cast(this);
  }

  @Override
  protected Optional<String> componentUuidToProjectUuid(String componentUuid) {
    return Optional.ofNullable(projectUuidByComponentUuid.get(componentUuid));
  }

  @Override
  protected boolean hasProjectUuidPermission(String permission, String projectUuid) {
    return projectPermissionsCheckedByUuid.contains(permission) && projectUuidByPermission.get(permission).contains(projectUuid);
  }

  public T setSystemAdministrator(boolean b) {
    this.systemAdministrator = b;
    return clazz.cast(this);
  }

  @Override
  public boolean isSystemAdministrator() {
    return isRoot() || systemAdministrator;
  }
}
