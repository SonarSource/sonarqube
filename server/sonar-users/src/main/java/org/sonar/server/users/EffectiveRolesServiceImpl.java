/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.users;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonarsource.organizations.server.DefaultOrganizationProvider;
import org.sonarsource.users.api.EffectiveRolesBatchQuery;
import org.sonarsource.users.api.EffectiveRolesQuery;
import org.sonarsource.users.api.EffectiveRolesService;
import org.sonarsource.users.api.model.EffectiveRole;
import org.sonarsource.users.api.model.EffectiveRoleBatch;
import org.sonarsource.users.api.model.Principal;
import org.sonarsource.users.api.model.PrincipalType;
import org.sonarsource.users.api.model.ResourceType;

@ServerSide
public class EffectiveRolesServiceImpl implements EffectiveRolesService {
  public static final String ADMIN_ROLE = "admin";
  public static final String MEMBER_ROLE = "member";
  private final DbClient dbClient;

  public EffectiveRolesServiceImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public List<EffectiveRole> getEffectiveRoles(EffectiveRolesQuery query) {
    EffectiveRolesBatchQuery batchQuery = new EffectiveRolesBatchQuery(
      List.of(query.principal().id()),
      query.principal().type(),
      query.resourceId() != null ? List.of(query.resourceId()) : List.of(),
      query.resourceType(),
      null
    );

    return getEffectiveRolesBatch(batchQuery).stream()
      .flatMap(batch -> batch.effectiveRoles().stream())
      .toList();
  }

  @Override
  public List<EffectiveRoleBatch> getEffectiveRolesBatch(EffectiveRolesBatchQuery query) {
    validatePrincipalType(query.principalType());

    try (DbSession dbSession = dbClient.openSession(false)) {
      return switch (query.resourceType()) {
        case ORGANIZATION -> getOrganizationRolesBatch(dbSession, query);
        case PROJECT -> getProjectRolesBatch(dbSession, query);
        case SQC -> throw new IllegalArgumentException("Resource type SQC is not yet supported");
        case UNKNOWN -> throw new IllegalArgumentException("Resource type UNKNOWN is not supported");
      };
    }
  }

  @Override
  public boolean isOrganizationAdmin(String userId, String organizationId) {
    return hasOrganizationRole(userId, organizationId, ADMIN_ROLE);
  }

  @Override
  public boolean isProjectAdmin(String userId, String projectId) {
    return hasProjectRole(userId, projectId, ADMIN_ROLE);
  }

  @Override
  public boolean hasOrganizationRole(String userId, String organizationId, String role) {
    return getEffectiveRoles(new EffectiveRolesQuery(
      new Principal(PrincipalType.USER, userId), organizationId, ResourceType.ORGANIZATION
    )).contains(new EffectiveRole(role));
  }

  @Override
  public boolean hasProjectRole(String userId, String projectId, String role) {
    return getEffectiveRoles(new EffectiveRolesQuery(
      new Principal(PrincipalType.USER, userId), projectId, ResourceType.PROJECT
    )).contains(new EffectiveRole(role));
  }

  private List<EffectiveRoleBatch> getOrganizationRolesBatch(DbSession dbSession, EffectiveRolesBatchQuery query) {
    Map<String, Set<String>> rolesPerPrincipal =
      dbClient.authorizationDao().selectGlobalPermissionsBatch(dbSession, query.principalIds());

    // Temporary solution to add the "member" role to existing users (at runtime).
    // See: https://sonarsource.atlassian.net/browse/EA-211
    dbClient.userDao().selectByUuids(dbSession, query.principalIds())
      .forEach(user -> rolesPerPrincipal
        .computeIfAbsent(user.getUuid(), k -> new HashSet<>())
        .add(MEMBER_ROLE));

    return transformToEffectiveRoleBatches(rolesPerPrincipal, DefaultOrganizationProvider.ID.toString(), query);
  }

  private List<EffectiveRoleBatch> getProjectRolesBatch(DbSession dbSession, EffectiveRolesBatchQuery query) {
    List<String> resourceIds = query.resourceIds();
    if (resourceIds == null || resourceIds.isEmpty()) {
      throw new IllegalArgumentException("Resource IDs are required for PROJECT resource type");
    }

    List<EffectiveRoleBatch> result = new ArrayList<>();
    for (String resourceId : resourceIds) {
      Map<String, Set<String>> rolesPerPrincipal =
        dbClient.authorizationDao().selectEntityPermissionsBatch(dbSession, resourceId, query.principalIds());

      result.addAll(transformToEffectiveRoleBatches(rolesPerPrincipal, resourceId, query));
    }
    return result;
  }

  private static List<EffectiveRoleBatch> transformToEffectiveRoleBatches(
    Map<String, Set<String>> rolesPerPrincipal,
    String resourceId,
    EffectiveRolesBatchQuery query) {

    List<EffectiveRoleBatch> result = new ArrayList<>();
    rolesPerPrincipal.forEach((principalId, roles) -> {
      List<EffectiveRole> effectiveRoles = roles.stream()
        .filter(role -> query.role() == null || query.role().equals(role))
        .map(EffectiveRole::new)
        .toList();
      if (!effectiveRoles.isEmpty()) {
        result.add(new EffectiveRoleBatch(principalId, query.principalType(), resourceId, query.resourceType(), effectiveRoles));
      }
    });
    return result;
  }

  private static void validatePrincipalType(PrincipalType principalType) {
    if (principalType != PrincipalType.USER) {
      throw new IllegalArgumentException("Only USER principal type is supported");
    }
  }
}
