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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonarsource.organizations.server.DefaultOrganizationProvider;
import org.sonarsource.users.api.EffectiveRolesBatchQuery;
import org.sonarsource.users.api.EffectiveRolesQuery;
import org.sonarsource.users.api.EffectiveRolesService;
import org.sonarsource.users.api.model.EffectiveRole;
import org.sonarsource.users.api.model.PrincipalType;

@ServerSide
public class EffectiveRolesServiceImpl implements EffectiveRolesService {
  private final DbClient dbClient;

  public EffectiveRolesServiceImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public Set<String> getEffectiveRoles(EffectiveRolesQuery query) {
    EffectiveRolesBatchQuery batchQuery = new EffectiveRolesBatchQuery(
      List.of(query.principal().id()),
      query.principal().type(),
      query.resourceId() != null ? List.of(query.resourceId()) : List.of(),
      query.resourceType(),
      null
    );

    return getEffectiveRolesBatch(batchQuery).stream()
      .map(EffectiveRole::role)
      .collect(Collectors.toSet());
  }

  @Override
  public Set<EffectiveRole> getEffectiveRolesBatch(EffectiveRolesBatchQuery query) {
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

  private Set<EffectiveRole> getOrganizationRolesBatch(DbSession dbSession, EffectiveRolesBatchQuery query) {
    Map<String, Set<String>> rolesPerPrincipal =
      dbClient.authorizationDao().selectGlobalPermissionsBatch(dbSession, query.principalIds());

    return transformToEffectiveRoles(rolesPerPrincipal, DefaultOrganizationProvider.ID.toString(), query);
  }

  private Set<EffectiveRole> getProjectRolesBatch(DbSession dbSession, EffectiveRolesBatchQuery query) {
    List<String> resourceIds = query.resourceIds();
    if (resourceIds == null || resourceIds.isEmpty()) {
      throw new IllegalArgumentException("Resource IDs are required for PROJECT resource type");
    }

    Set<EffectiveRole> result = new HashSet<>();
    for (String resourceId : resourceIds) {
      Map<String, Set<String>> rolesPerPrincipal =
        dbClient.authorizationDao().selectEntityPermissionsBatch(dbSession, resourceId, query.principalIds());

      result.addAll(transformToEffectiveRoles(rolesPerPrincipal, resourceId, query));
    }
    return result;
  }

  private static Set<EffectiveRole> transformToEffectiveRoles(
    Map<String, Set<String>> rolesPerPrincipal,
    String resourceId,
    EffectiveRolesBatchQuery query) {

    Set<EffectiveRole> result = new HashSet<>();
    rolesPerPrincipal.forEach((principalId, roles) ->
      roles.stream()
        .filter(role -> query.role() == null || query.role().equals(role))
        .forEach(role -> result.add(
          new EffectiveRole(principalId, query.principalType(), resourceId, query.resourceType(), role)))
    );
    return result;
  }

  private static void validatePrincipalType(PrincipalType principalType) {
    if (principalType != PrincipalType.USER) {
      throw new IllegalArgumentException("Only USER principal type is supported");
    }
  }
}
