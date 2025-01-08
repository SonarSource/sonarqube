/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.db.DbSession;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@RunWith(MockitoJUnitRunner.class)
public class DelegatingManagedServicesTest {

  private static final DelegatingManagedServices NO_MANAGED_SERVICES = new DelegatingManagedServices(emptySet());

  @Mock
  private DbSession dbSession;

  @Test
  public void getProviderName_whenNotManaged_shouldThrow() {
    assertThatThrownBy(NO_MANAGED_SERVICES::getProviderName)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("This instance is not managed.");
  }

  @Test
  public void getProviderName_whenManaged_shouldReturnName() {
    DelegatingManagedServices managedInstanceService = new DelegatingManagedServices(Set.of(new AlwaysManagedInstanceService()));

    assertThat(managedInstanceService.getProviderName()).isEqualTo("Always");
  }

  @Test
  public void isInstanceExternallyManaged_whenNoManagedInstanceService_returnsFalse() {
    assertThat(NO_MANAGED_SERVICES.isInstanceExternallyManaged()).isFalse();
  }

  @Test
  public void isInstanceExternallyManaged_whenAllManagedInstanceServiceReturnsFalse_returnsFalse() {
    Set<ManagedInstanceService> delegates = Set.of(new NeverManagedInstanceService(), new NeverManagedInstanceService());
    DelegatingManagedServices managedInstanceService = new DelegatingManagedServices(delegates);

    assertThat(managedInstanceService.isInstanceExternallyManaged()).isFalse();
  }

  @Test
  public void isInstanceExternallyManaged_whenOneManagedInstanceServiceReturnsTrue_returnsTrue() {
    Set<ManagedInstanceService> delegates = Set.of(new NeverManagedInstanceService(), new AlwaysManagedInstanceService());
    DelegatingManagedServices managedInstanceService = new DelegatingManagedServices(delegates);

    assertThat(managedInstanceService.isInstanceExternallyManaged()).isTrue();
  }

  @Test
  public void getUserUuidToManaged_whenNoDelegates_setAllUsersAsNonManaged() {
    Set<String> userUuids = Set.of("a", "b");

    Map<String, Boolean> userUuidToManaged = NO_MANAGED_SERVICES.getUserUuidToManaged(dbSession, userUuids);

    assertThat(userUuidToManaged).containsExactlyInAnyOrderEntriesOf(Map.of("a", false, "b", false));
  }

  @Test
  public void getUserUuidToManaged_delegatesToRightService_andPropagateAnswer() {
    Set<String> userUuids = Set.of("a", "b");
    Map<String, Boolean> serviceResponse = Map.of("a", false, "b", true);

    ManagedInstanceService anotherManagedInstanceService = getManagedInstanceService(userUuids, serviceResponse);
    DelegatingManagedServices managedInstanceService = new DelegatingManagedServices(Set.of(new NeverManagedInstanceService(), anotherManagedInstanceService));

    Map<String, Boolean> userUuidToManaged = managedInstanceService.getUserUuidToManaged(dbSession, userUuids);

    assertThat(userUuidToManaged).containsExactlyInAnyOrderEntriesOf(serviceResponse);
  }

  @Test
  public void getGroupUuidToManaged_whenNoDelegates_setAllUsersAsNonManaged() {
    Set<String> groupUuids = Set.of("a", "b");
    DelegatingManagedServices managedInstanceService = NO_MANAGED_SERVICES;

    Map<String, Boolean> groupUuidToManaged = managedInstanceService.getGroupUuidToManaged(dbSession, groupUuids);

    assertThat(groupUuidToManaged).containsExactlyInAnyOrderEntriesOf(Map.of("a", false, "b", false));
  }

  @Test
  public void isUserManaged_delegatesToRightService_andPropagateAnswer() {
    DelegatingManagedServices managedInstanceService = new DelegatingManagedServices(Set.of(new NeverManagedInstanceService(), new AlwaysManagedInstanceService()));

    assertThat(managedInstanceService.isUserManaged(dbSession, "whatever")).isTrue();
  }

  @Test
  public void isUserManaged_whenNoDelegates_returnsFalse() {
    DelegatingManagedServices managedInstanceService = new DelegatingManagedServices(Set.of());

    assertThat(managedInstanceService.isUserManaged(dbSession, "whatever")).isFalse();
  }

  @Test
  public void isGroupManaged_delegatesToRightService_andPropagateAnswer() {
    DelegatingManagedServices managedInstanceService = new DelegatingManagedServices(Set.of(new NeverManagedInstanceService(), new AlwaysManagedInstanceService()));

    assertThat(managedInstanceService.isGroupManaged(dbSession, "whatever")).isTrue();
  }

  @Test
  public void isGroupManaged_whenNoDelegates_returnsFalse() {
    DelegatingManagedServices managedInstanceService = new DelegatingManagedServices(Set.of());

    assertThat(managedInstanceService.isGroupManaged(dbSession, "whatever")).isFalse();
  }

  @Test
  public void getGroupUuidToManaged_delegatesToRightService_andPropagateAnswer() {
    Set<String> groupUuids = Set.of("a", "b");
    Map<String, Boolean> serviceResponse = Map.of("a", false, "b", true);

    ManagedInstanceService anotherManagedInstanceService = getManagedInstanceService(groupUuids, serviceResponse);
    DelegatingManagedServices managedInstanceService = new DelegatingManagedServices(Set.of(new NeverManagedInstanceService(), anotherManagedInstanceService));

    Map<String, Boolean> groupUuidToManaged = managedInstanceService.getGroupUuidToManaged(dbSession, groupUuids);

    assertThat(groupUuidToManaged).containsExactlyInAnyOrderEntriesOf(serviceResponse);
  }

  @Test
  public void getGroupUuidToManaged_ifMoreThanOneDelegatesActivated_throws() {
    Set<ManagedInstanceService> managedInstanceServices = Set.of(new AlwaysManagedInstanceService(), new AlwaysManagedInstanceService());
    DelegatingManagedServices delegatingManagedServices = new DelegatingManagedServices(managedInstanceServices);
    assertThatIllegalStateException()
      .isThrownBy(() -> delegatingManagedServices.getGroupUuidToManaged(dbSession, Set.of("a")))
      .withMessage("The instance can't be managed by more than one identity provider and 2 were found.");
  }

  @Test
  public void getUserUuidToManaged_ifMoreThanOneDelegatesActivated_throws() {
    Set<ManagedInstanceService> managedInstanceServices = Set.of(new AlwaysManagedInstanceService(), new AlwaysManagedInstanceService());
    DelegatingManagedServices delegatingManagedServices = new DelegatingManagedServices(managedInstanceServices);
    assertThatIllegalStateException()
      .isThrownBy(() -> delegatingManagedServices.getUserUuidToManaged(dbSession, Set.of("a")))
      .withMessage("The instance can't be managed by more than one identity provider and 2 were found.");
  }

  @Test
  public void getManagedUsersSqlFilter_whenNoDelegates_throws() {
    Set<ManagedInstanceService> managedInstanceServices = emptySet();
    DelegatingManagedServices delegatingManagedServices = new DelegatingManagedServices(managedInstanceServices);
    assertThatIllegalStateException()
      .isThrownBy(() -> delegatingManagedServices.getManagedUsersSqlFilter(true))
      .withMessage("This instance is not managed.");
  }

  @Test
  public void getManagedUsersSqlFilter_delegatesToRightService_andPropagateAnswer() {
    AlwaysManagedInstanceService alwaysManagedInstanceService = new AlwaysManagedInstanceService();
    DelegatingManagedServices managedInstanceService = new DelegatingManagedServices(Set.of(new NeverManagedInstanceService(), alwaysManagedInstanceService));

    assertThat(managedInstanceService.getManagedUsersSqlFilter(true)).isNotNull().isEqualTo(alwaysManagedInstanceService.getManagedUsersSqlFilter(
      true));
  }

  @Test
  public void getManagedGroupsSqlFilter_whenNoDelegates_throws() {
    Set<ManagedInstanceService> managedInstanceServices = emptySet();
    DelegatingManagedServices delegatingManagedServices = new DelegatingManagedServices(managedInstanceServices);
    assertThatIllegalStateException()
      .isThrownBy(() -> delegatingManagedServices.getManagedGroupsSqlFilter(true))
      .withMessage("This instance is not managed.");
  }

  @Test
  public void getManagedGroupsSqlFilter_delegatesToRightService_andPropagateAnswer() {
    AlwaysManagedInstanceService alwaysManagedInstanceService = new AlwaysManagedInstanceService();
    DelegatingManagedServices managedInstanceService = new DelegatingManagedServices(Set.of(new NeverManagedInstanceService(), alwaysManagedInstanceService));

    assertThat(managedInstanceService.getManagedGroupsSqlFilter(true)).isNotNull().isEqualTo(alwaysManagedInstanceService.getManagedGroupsSqlFilter(
      true));
  }

  @Test
  public void queueSynchronisationTask_whenManagedNoInstanceServices_doesNotFail() {
    assertThatNoException().isThrownBy(NO_MANAGED_SERVICES::queueSynchronisationTask);
  }

  @Test
  public void queueSynchronisationTask_whenManagedInstanceServices_shouldDelegatesToRightService() {
    NeverManagedInstanceService neverManagedInstanceService = spy(new NeverManagedInstanceService());
    AlwaysManagedInstanceService alwaysManagedInstanceService = spy(new AlwaysManagedInstanceService());
    Set<ManagedInstanceService> delegates = Set.of(neverManagedInstanceService, alwaysManagedInstanceService);
    DelegatingManagedServices managedInstanceService = new DelegatingManagedServices(delegates);

    managedInstanceService.queueSynchronisationTask();
    verify(neverManagedInstanceService, never()).queueSynchronisationTask();
    verify(alwaysManagedInstanceService).queueSynchronisationTask();
  }

  private ManagedInstanceService getManagedInstanceService(Set<String> userUuids, Map<String, Boolean> uuidToManaged) {
    ManagedInstanceService anotherManagedInstanceService = mock(ManagedInstanceService.class);
    when(anotherManagedInstanceService.isInstanceExternallyManaged()).thenReturn(true);
    when(anotherManagedInstanceService.getGroupUuidToManaged(dbSession, userUuids)).thenReturn(uuidToManaged);
    when(anotherManagedInstanceService.getUserUuidToManaged(dbSession, userUuids)).thenReturn(uuidToManaged);
    return anotherManagedInstanceService;
  }

  @Test
  public void getProjectUuidToManaged_whenNoDelegates_setAllProjectsAsNonManaged() {
    Set<String> projectUuids = Set.of("a", "b");
    DelegatingManagedServices managedInstanceService = NO_MANAGED_SERVICES;

    Map<String, Boolean> projectUuidToManaged = managedInstanceService.getProjectUuidToManaged(dbSession, projectUuids);

    assertThat(projectUuidToManaged).containsExactlyInAnyOrderEntriesOf(Map.of("a", false, "b", false));
  }

  @Test
  public void getProjectUuidToManaged_delegatesToRightService_andPropagateAnswer() {
    Set<String> projectUuids = Set.of("a", "b");
    Map<String, Boolean> serviceResponse = Map.of("a", false, "b", true);

    ManagedInstanceService anotherManagedProjectService = getManagedProjectService(projectUuids, serviceResponse);
    DelegatingManagedServices managedInstanceService = new DelegatingManagedServices(Set.of(new NeverManagedInstanceService(), anotherManagedProjectService));

    Map<String, Boolean> projectUuidToManaged = managedInstanceService.getProjectUuidToManaged(dbSession, projectUuids);

    assertThat(projectUuidToManaged).containsExactlyInAnyOrderEntriesOf(serviceResponse);
  }

  private ManagedInstanceService getManagedProjectService(Set<String> projectUuids, Map<String, Boolean> uuidsToManaged) {
    ManagedInstanceService anotherManagedProjectService = mock(ManagedInstanceService.class, withSettings().extraInterfaces(ManagedProjectService.class));
    when(anotherManagedProjectService.isInstanceExternallyManaged()).thenReturn(true);
    doReturn(uuidsToManaged).when((ManagedProjectService) anotherManagedProjectService).getProjectUuidToManaged(dbSession, projectUuids);
    return anotherManagedProjectService;
  }

  @Test
  public void isProjectManaged_whenManagedInstanceServices_shouldDelegatesToRightService() {
    DelegatingManagedServices managedInstanceService = new DelegatingManagedServices(Set.of(new NeverManagedInstanceService(), new AlwaysManagedInstanceService()));

    assertThat(managedInstanceService.isProjectManaged(dbSession, "whatever")).isTrue();
  }

  @Test
  public void isProjectManaged_whenManagedNoInstanceServices_returnsFalse() {
    assertThat(NO_MANAGED_SERVICES.isProjectManaged(dbSession, "whatever")).isFalse();
  }

  @Test
  public void queuePermissionSyncTask_whenManagedNoInstanceServices_doesNotFail() {
    assertThatNoException().isThrownBy(() -> NO_MANAGED_SERVICES.queuePermissionSyncTask("userUuid", "componentUuid", "projectUuid"));
  }

  @Test
  public void queuePermissionSyncTask_whenManagedInstanceServices_shouldDelegatesToRightService() {
    NeverManagedInstanceService neverManagedInstanceService = spy(new NeverManagedInstanceService());
    AlwaysManagedInstanceService alwaysManagedInstanceService = spy(new AlwaysManagedInstanceService());
    Set<ManagedInstanceService> delegates = Set.of(neverManagedInstanceService, alwaysManagedInstanceService);
    DelegatingManagedServices managedInstanceService = new DelegatingManagedServices(delegates);

    managedInstanceService.queuePermissionSyncTask("userUuid", "componentUuid", "projectUuid");
    verify(neverManagedInstanceService, never()).queuePermissionSyncTask(anyString(), anyString(), anyString());
    verify(alwaysManagedInstanceService).queuePermissionSyncTask("userUuid", "componentUuid", "projectUuid");
  }

  @Test
  public void isProjectVisibilitySynchronizationActivated_whenManagedInstanceServices_shouldDelegatesToRightService() {
    DelegatingManagedServices managedInstanceService = new DelegatingManagedServices(Set.of(new NeverManagedInstanceService(), new AlwaysManagedInstanceService()));

    assertThat(managedInstanceService.isProjectVisibilitySynchronizationActivated()).isTrue();
  }

  @Test
  public void isProjectVisibilitySynchronizationActivated_whenManagedNoInstanceServices_returnsFalse() {
    assertThat(NO_MANAGED_SERVICES.isProjectVisibilitySynchronizationActivated()).isFalse();
  }

  private static class NeverManagedInstanceService implements ManagedInstanceService, ManagedProjectService {

    @Override
    public boolean isInstanceExternallyManaged() {
      return false;
    }

    @Override
    public String getProviderName() {
      return "Never";
    }

    @Override
    public Map<String, Boolean> getUserUuidToManaged(DbSession dbSession, Set<String> userUuids) {
      return null;
    }

    @Override
    public Map<String, Boolean> getGroupUuidToManaged(DbSession dbSession, Set<String> groupUuids) {
      return null;
    }

    @Override
    public String getManagedUsersSqlFilter(boolean filterByManaged) {
      return null;
    }

    @Override
    public String getManagedGroupsSqlFilter(boolean filterByManaged) {
      return null;
    }

    @Override
    public boolean isUserManaged(DbSession dbSession, String userUuid) {
      return false;
    }

    @Override
    public boolean isGroupManaged(DbSession dbSession, String groupUuid) {
      return false;
    }

    @Override
    public void queueSynchronisationTask() {

    }

    @Override
    public Map<String, Boolean> getProjectUuidToManaged(DbSession dbSession, Set<String> projectUuids) {
      return null;
    }

    @Override
    public boolean isProjectManaged(DbSession dbSession, String projectUuid) {
      return false;
    }

    @Override
    public void queuePermissionSyncTask(String submitterUuid, String componentUuid, String projectUuid) {

    }

    @Override
    public boolean isProjectVisibilitySynchronizationActivated() {
      return false;
    }
  }

  private static class AlwaysManagedInstanceService implements ManagedInstanceService, ManagedProjectService {

    @Override
    public boolean isInstanceExternallyManaged() {
      return true;
    }

    @Override
    public String getProviderName() {
      return "Always";
    }

    @Override
    public Map<String, Boolean> getUserUuidToManaged(DbSession dbSession, Set<String> userUuids) {
      return null;
    }

    @Override
    public Map<String, Boolean> getGroupUuidToManaged(DbSession dbSession, Set<String> groupUuids) {
      return null;
    }

    @Override
    public String getManagedUsersSqlFilter(boolean filterByManaged) {
      return "any filter";
    }

    @Override
    public String getManagedGroupsSqlFilter(boolean filterByManaged) {
      return "any filter";
    }

    @Override
    public boolean isUserManaged(DbSession dbSession, String userUuid) {
      return true;
    }

    @Override
    public boolean isGroupManaged(DbSession dbSession, String groupUuid) {
      return true;
    }

    @Override
    public void queueSynchronisationTask() {

    }

    @Override
    public Map<String, Boolean> getProjectUuidToManaged(DbSession dbSession, Set<String> projectUuids) {
      return null;
    }

    @Override
    public boolean isProjectManaged(DbSession dbSession, String projectUuid) {
      return true;
    }

    @Override
    public void queuePermissionSyncTask(String submitterUuid, String componentUuid, String projectUuid) {

    }

    @Override
    public boolean isProjectVisibilitySynchronizationActivated() {
      return true;
    }
  }

}
