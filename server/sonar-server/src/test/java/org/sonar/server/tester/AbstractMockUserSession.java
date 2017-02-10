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
package org.sonar.server.tester;

import com.google.common.collect.HashMultimap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sonar.server.user.AbstractUserSession;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

public abstract class AbstractMockUserSession<T extends AbstractMockUserSession> extends AbstractUserSession {
  private final Class<T> clazz;
  private HashMultimap<String, String> projectUuidByPermission = HashMultimap.create();
  private HashMultimap<String, String> permissionsByOrganizationUuid = HashMultimap.create();
  private Map<String, String> projectUuidByComponentUuid = newHashMap();
  private List<String> projectPermissionsCheckedByUuid = newArrayList();
  private boolean systemAdministrator = false;

  protected AbstractMockUserSession(Class<T> clazz) {
    this.clazz = clazz;
  }

  public T addProjectUuidPermissions(String projectPermission, String... projectUuids) {
    this.projectPermissionsCheckedByUuid.add(projectPermission);
    this.projectUuidByPermission.putAll(projectPermission, newArrayList(projectUuids));
    for (String projectUuid : projectUuids) {
      this.projectUuidByComponentUuid.put(projectUuid, projectUuid);
    }
    return clazz.cast(this);
  }

  public T addComponentUuidPermission(String projectPermission, String projectUuid, String componentUuid) {
    this.projectUuidByComponentUuid.put(componentUuid, projectUuid);
    addProjectUuidPermissions(projectPermission, projectUuid);
    return clazz.cast(this);
  }

  @Override
  protected boolean hasOrganizationPermissionImpl(String organizationUuid, String permission) {
    return permissionsByOrganizationUuid.get(organizationUuid).contains(permission);
  }

  @Override
  protected Optional<String> componentUuidToProjectUuid(String componentUuid) {
    return Optional.ofNullable(projectUuidByComponentUuid.get(componentUuid));
  }

  @Override
  protected boolean hasProjectUuidPermission(String permission, String projectUuid) {
    return projectPermissionsCheckedByUuid.contains(permission) && projectUuidByPermission.get(permission).contains(projectUuid);
  }

  public T addOrganizationPermission(String organizationUuid, String permission) {
    permissionsByOrganizationUuid.put(organizationUuid, permission);
    return clazz.cast(this);
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
