/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.permission;

import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.core.platform.EditionProvider.Edition;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.permission.UserPermissionDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentTypes;

@Immutable
public class PermissionServiceImpl implements PermissionService {
  /**
   * This particular order seems to be important for some web services.
   * Maybe the UI is relying on it?
   * That's why we are not relying on {@link ProjectPermission#values()}.
   */
  private static final List<ProjectPermission> ALL_PROJECT_PERMISSIONS = List.of(
    ProjectPermission.ADMIN,
    ProjectPermission.CODEVIEWER,
    ProjectPermission.ISSUE_ADMIN,
    ProjectPermission.SECURITYHOTSPOT_ADMIN,
    ProjectPermission.ARCHITECTURE_ADMIN,
    ProjectPermission.SCAN,
    ProjectPermission.USER);

  private static final List<GlobalPermission> ALL_GLOBAL_PERMISSIONS = List.of(GlobalPermission.values());

  private final List<GlobalPermission> globalPermissions;
  private final List<ProjectPermission> projectPermissions;
  private final DbClient dbClient;

  /**
   * IoC constructor — used by Spring in production.
   * architectureadmin is available in Developer edition and above.
   */
  @Inject
  public PermissionServiceImpl(ComponentTypes componentTypes, PlatformEditionProvider platformEditionProvider, DbClient dbClient) {
    this(componentTypes, platformEditionProvider.get().map(e -> e == Edition.COMMUNITY).orElse(true), dbClient);
  }

  @VisibleForTesting
  public PermissionServiceImpl(ComponentTypes componentTypes, DbClient dbClient) {
    this(componentTypes, true, dbClient);
  }

  /**
   * Backward-compatible constructor for tests that do not exercise {@link #findGroupPermissions} or
   * {@link #findUserPermissions}. Defaults to Community Build behavior (architectureadmin excluded).
   * Calling the DB-touching methods on an instance created with this constructor throws {@link NullPointerException}.
   */
  @VisibleForTesting
  public PermissionServiceImpl(ComponentTypes componentTypes) {
    this(componentTypes, true, null);
  }

  /**
   * Backward-compatible constructor for tests that control the edition but do not exercise the DB-touching methods.
   */
  @VisibleForTesting
  public PermissionServiceImpl(ComponentTypes componentTypes, PlatformEditionProvider platformEditionProvider) {
    this(componentTypes, platformEditionProvider.get().map(e -> e == Edition.COMMUNITY).orElse(true), null);
  }

  private PermissionServiceImpl(ComponentTypes componentTypes, boolean isCommunityBuild, @Nullable DbClient dbClient) {
    globalPermissions = List.copyOf(ALL_GLOBAL_PERMISSIONS.stream()
      .filter(s -> !s.equals(GlobalPermission.APPLICATION_CREATOR) || componentTypes.isQualifierPresent(ComponentQualifiers.APP))
      .filter(s -> !s.equals(GlobalPermission.PORTFOLIO_CREATOR) || componentTypes.isQualifierPresent(ComponentQualifiers.VIEW))
      .toList());
    projectPermissions = List.copyOf(ALL_PROJECT_PERMISSIONS.stream()
      .filter(p -> p != ProjectPermission.ARCHITECTURE_ADMIN || !isCommunityBuild)
      .toList());
    this.dbClient = dbClient;
  }

  @Override
  public List<GlobalPermission> getGlobalPermissions() {
    return globalPermissions;
  }

  @Override
  public List<ProjectPermission> getAllProjectPermissions() {
    return projectPermissions;
  }

  @Override
  public List<GroupPermissionDto> findGroupPermissions(DbSession dbSession, List<GroupDto> groups, @Nullable EntityDto entity) {
    if (groups.isEmpty()) {
      return List.of();
    }
    List<String> uuids = groups.stream().map(GroupDto::getUuid).toList();
    Set<String> allowedKeys = allowedPermissionKeys(entity);
    return dbClient.groupPermissionDao().selectByGroupUuids(dbSession, uuids, entity != null ? entity.getUuid() : null)
      .stream()
      .filter(p -> allowedKeys.contains(p.getRole()))
      .toList();
  }

  @Override
  public List<UserPermissionDto> findUserPermissions(DbSession dbSession, List<UserDto> users, @Nullable EntityDto entity) {
    if (users.isEmpty()) {
      return List.of();
    }
    List<String> userUuids = users.stream().map(UserDto::getUuid).toList();
    PermissionQuery.Builder queryBuilder = PermissionQuery.builder().withAtLeastOnePermission();
    if (entity != null) {
      queryBuilder.setEntityUuid(entity.getUuid());
    }
    Set<String> allowedKeys = allowedPermissionKeys(entity);
    return dbClient.userPermissionDao().selectUserPermissionsByQuery(dbSession, queryBuilder.build(), userUuids)
      .stream()
      .filter(p -> allowedKeys.contains(p.getPermission()))
      .toList();
  }

  private Set<String> allowedPermissionKeys(@Nullable EntityDto entity) {
    if (entity != null) {
      return getAllProjectPermissions().stream().map(ProjectPermission::getKey).collect(Collectors.toUnmodifiableSet());
    }
    return getGlobalPermissions().stream().map(GlobalPermission::getKey).collect(Collectors.toUnmodifiableSet());
  }
}
