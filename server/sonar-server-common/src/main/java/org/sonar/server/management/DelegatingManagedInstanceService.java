/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.management;

import com.google.common.collect.MoreCollectors;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Priority;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbSession;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.sonar.api.utils.Preconditions.checkState;

@ServerSide
@Priority(ManagedInstanceService.DELEGATING_INSTANCE_PRIORITY)
public class DelegatingManagedInstanceService implements ManagedInstanceService {

  private final Set<ManagedInstanceService> delegates;

  public DelegatingManagedInstanceService(Set<ManagedInstanceService> delegates) {
    this.delegates = delegates;
  }

  public final boolean isInstanceExternallyManaged() {
    return delegates.stream().anyMatch(ManagedInstanceService::isInstanceExternallyManaged);
  }

  @Override
  public Map<String, Boolean> getUserUuidToManaged(DbSession dbSession, Set<String> userUuids) {
    return findManagedInstanceService()
      .map(managedInstanceService -> managedInstanceService.getUserUuidToManaged(dbSession, userUuids))
      .orElse(returnNonManagedForAllGroups(userUuids));
  }

  @Override
  public Map<String, Boolean> getGroupUuidToManaged(DbSession dbSession, Set<String> groupUuids) {
    return findManagedInstanceService()
      .map(managedInstanceService -> managedInstanceService.getGroupUuidToManaged(dbSession, groupUuids))
      .orElse(returnNonManagedForAllGroups(groupUuids));
  }

  private Optional<ManagedInstanceService> findManagedInstanceService() {
    Set<ManagedInstanceService> managedInstanceServices = delegates.stream()
      .filter(ManagedInstanceService::isInstanceExternallyManaged)
      .collect(toSet());

    checkState(managedInstanceServices.size() < 2,
      "The instance can't be managed by more than one identity provider and %s were found.", managedInstanceServices.size());
    return managedInstanceServices.stream().collect(MoreCollectors.toOptional());
  }

  private static Map<String, Boolean> returnNonManagedForAllGroups(Set<String> resourcesUuid) {
    return resourcesUuid.stream().collect(toMap(identity(), any -> false));
  }
}
