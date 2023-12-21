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
public class DelegatingManagedServices implements ManagedInstanceService, ManagedProjectService {

  private static final IllegalStateException NOT_MANAGED_INSTANCE_EXCEPTION = new IllegalStateException("This instance is not managed.");
  private final Set<ManagedInstanceService> delegates;

  public DelegatingManagedServices(Set<ManagedInstanceService> delegates) {
    this.delegates = delegates;
  }

  public final boolean isInstanceExternallyManaged() {
    return delegates.stream().anyMatch(ManagedInstanceService::isInstanceExternallyManaged);
  }

  @Override
  public String getProviderName() {
    return findManagedInstanceService()
      .map(ManagedInstanceService::getProviderName)
      .orElseThrow(() -> NOT_MANAGED_INSTANCE_EXCEPTION);
  }

  @Override
  public Map<String, Boolean> getUserUuidToManaged(DbSession dbSession, Set<String> userUuids) {
    return findManagedInstanceService()
      .map(managedInstanceService -> managedInstanceService.getUserUuidToManaged(dbSession, userUuids))
      .orElse(returnNonManagedForAll(userUuids));
  }

  @Override
  public Map<String, Boolean> getGroupUuidToManaged(DbSession dbSession, Set<String> groupUuids) {
    return findManagedInstanceService()
      .map(managedInstanceService -> managedInstanceService.getGroupUuidToManaged(dbSession, groupUuids))
      .orElse(returnNonManagedForAll(groupUuids));
  }

  @Override
  public String getManagedUsersSqlFilter(boolean filterByManaged) {
    return findManagedInstanceService()
      .map(managedInstanceService -> managedInstanceService.getManagedUsersSqlFilter(filterByManaged))
      .orElseThrow(() -> NOT_MANAGED_INSTANCE_EXCEPTION);
  }

  @Override
  public String getManagedGroupsSqlFilter(boolean filterByManaged) {
    return findManagedInstanceService()
      .map(managedInstanceService -> managedInstanceService.getManagedGroupsSqlFilter(filterByManaged))
      .orElseThrow(() -> NOT_MANAGED_INSTANCE_EXCEPTION);
  }

  @Override
  public boolean isUserManaged(DbSession dbSession, String userUuid) {
    return findManagedInstanceService()
      .map(managedInstanceService -> managedInstanceService.isUserManaged(dbSession, userUuid))
      .orElse(false);
  }

  @Override
  public boolean isGroupManaged(DbSession dbSession, String groupUuid) {
    return findManagedInstanceService()
      .map(managedInstanceService -> managedInstanceService.isGroupManaged(dbSession, groupUuid))
      .orElse(false);
  }

  @Override
  public void queueSynchronisationTask() {
    findManagedInstanceService()
      .ifPresent(ManagedInstanceService::queueSynchronisationTask);
  }

  private Optional<ManagedInstanceService> findManagedInstanceService() {
    Set<ManagedInstanceService> managedInstanceServices = delegates.stream()
      .filter(ManagedInstanceService::isInstanceExternallyManaged)
      .collect(toSet());

    checkState(managedInstanceServices.size() < 2,
      "The instance can't be managed by more than one identity provider and %s were found.", managedInstanceServices.size());
    return managedInstanceServices.stream().collect(MoreCollectors.toOptional());
  }

  private static Map<String, Boolean> returnNonManagedForAll(Set<String> resourcesUuid) {
    return resourcesUuid.stream().collect(toMap(identity(), any -> false));
  }

  @Override
  public Map<String, Boolean> getProjectUuidToManaged(DbSession dbSession, Set<String> projectUuids) {
    return findManagedProjectService()
      .map(managedProjectService -> managedProjectService.getProjectUuidToManaged(dbSession, projectUuids))
      .orElse(returnNonManagedForAll(projectUuids));
  }

  @Override
  public boolean isProjectManaged(DbSession dbSession, String projectUuid) {
    return findManagedProjectService()
      .map(managedProjectService -> managedProjectService.isProjectManaged(dbSession, projectUuid))
      .orElse(false);
  }

  @Override
  public void queuePermissionSyncTask(String submitterUuid, String componentUuid, String projectUuid) {
    findManagedProjectService()
      .ifPresent(managedProjectService -> managedProjectService.queuePermissionSyncTask(submitterUuid, componentUuid, projectUuid));
  }

  @Override
  public boolean isProjectVisibilitySynchronizationActivated() {
    return findManagedProjectService()
      .map(ManagedProjectService::isProjectVisibilitySynchronizationActivated)
      .orElse(false);
  }

  private Optional<ManagedProjectService> findManagedProjectService() {
    return findManagedInstanceService()
      .filter(ManagedProjectService.class::isInstance)
      .map(ManagedProjectService.class::cast);
  }
}
