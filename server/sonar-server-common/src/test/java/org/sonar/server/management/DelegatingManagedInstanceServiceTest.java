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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DelegatingManagedInstanceServiceTest {

  @Mock
  private DbSession dbSession;

  @Test
  public void isInstanceExternallyManaged_whenNoManagedInstanceService_returnsFalse() {
    DelegatingManagedInstanceService managedInstanceService = new DelegatingManagedInstanceService(emptySet());
    assertThat(managedInstanceService.isInstanceExternallyManaged()).isFalse();
  }

  @Test
  public void isInstanceExternallyManaged_whenAllManagedInstanceServiceReturnsFalse_returnsFalse() {
    Set<ManagedInstanceService> delegates = Set.of(new NeverManagedInstanceService(), new NeverManagedInstanceService());
    DelegatingManagedInstanceService managedInstanceService = new DelegatingManagedInstanceService(delegates);

    assertThat(managedInstanceService.isInstanceExternallyManaged()).isFalse();
  }

  @Test
  public void isInstanceExternallyManaged_whenOneManagedInstanceServiceReturnsTrue_returnsTrue() {
    Set<ManagedInstanceService> delegates = Set.of(new NeverManagedInstanceService(), new AlwaysManagedInstanceService());
    DelegatingManagedInstanceService managedInstanceService = new DelegatingManagedInstanceService(delegates);

    assertThat(managedInstanceService.isInstanceExternallyManaged()).isTrue();
  }

  @Test
  public void getUserUuidToManaged_whenNoDelegates_setAllUsersAsNonManaged() {
    Set<String> userUuids = Set.of("a", "b");
    DelegatingManagedInstanceService managedInstanceService = new DelegatingManagedInstanceService(emptySet());

    Map<String, Boolean> userUuidToManaged = managedInstanceService.getUserUuidToManaged(dbSession, userUuids);

    assertThat(userUuidToManaged).containsExactlyInAnyOrderEntriesOf(Map.of("a", false, "b", false));
  }

  @Test
  public void getUserUuidToManaged_delegatesToRightService_andPropagateAnswer() {
    Set<String> userUuids = Set.of("a", "b");
    Map<String, Boolean> serviceResponse = Map.of("a", false, "b", true);

    ManagedInstanceService anotherManagedInstanceService = getManagedInstanceService(userUuids, serviceResponse);
    DelegatingManagedInstanceService managedInstanceService = new DelegatingManagedInstanceService(Set.of(new NeverManagedInstanceService(), anotherManagedInstanceService));

    Map<String, Boolean> userUuidToManaged = managedInstanceService.getUserUuidToManaged(dbSession, userUuids);

    assertThat(userUuidToManaged).containsExactlyInAnyOrderEntriesOf(serviceResponse);
  }

  @Test
  public void getGroupUuidToManaged_whenNoDelegates_setAllUsersAsNonManaged() {
    Set<String> groupUuids = Set.of("a", "b");
    DelegatingManagedInstanceService managedInstanceService = new DelegatingManagedInstanceService(emptySet());

    Map<String, Boolean> groupUuidToManaged = managedInstanceService.getGroupUuidToManaged(dbSession, groupUuids);

    assertThat(groupUuidToManaged).containsExactlyInAnyOrderEntriesOf(Map.of("a", false, "b", false));
  }

  @Test
  public void getGroupUuidToManaged_delegatesToRightService_andPropagateAnswer() {
    Set<String> groupUuids = Set.of("a", "b");
    Map<String, Boolean> serviceResponse = Map.of("a", false, "b", true);

    ManagedInstanceService anotherManagedInstanceService = getManagedInstanceService(groupUuids, serviceResponse);
    DelegatingManagedInstanceService managedInstanceService = new DelegatingManagedInstanceService(Set.of(new NeverManagedInstanceService(), anotherManagedInstanceService));

    Map<String, Boolean> groupUuidToManaged = managedInstanceService.getGroupUuidToManaged(dbSession, groupUuids);

    assertThat(groupUuidToManaged).containsExactlyInAnyOrderEntriesOf(serviceResponse);
  }

  @Test
  public void getGroupUuidToManaged_ifMoreThanOneDelegatesActivated_throws() {
    Set<ManagedInstanceService> managedInstanceServices = Set.of(new AlwaysManagedInstanceService(), new AlwaysManagedInstanceService());
    DelegatingManagedInstanceService delegatingManagedInstanceService = new DelegatingManagedInstanceService(managedInstanceServices);
    assertThatIllegalStateException()
      .isThrownBy(() -> delegatingManagedInstanceService.getGroupUuidToManaged(dbSession, Set.of("a")))
      .withMessage("The instance can't be managed by more than one identity provider and 2 were found.");
  }

  @Test
  public void getUserUuidToManaged_ifMoreThanOneDelegatesActivated_throws() {
    Set<ManagedInstanceService> managedInstanceServices = Set.of(new AlwaysManagedInstanceService(), new AlwaysManagedInstanceService());
    DelegatingManagedInstanceService delegatingManagedInstanceService = new DelegatingManagedInstanceService(managedInstanceServices);
    assertThatIllegalStateException()
      .isThrownBy(() -> delegatingManagedInstanceService.getUserUuidToManaged(dbSession, Set.of("a")))
      .withMessage("The instance can't be managed by more than one identity provider and 2 were found.");
  }

  private ManagedInstanceService getManagedInstanceService(Set<String> userUuids, Map<String, Boolean> uuidToManaged) {
    ManagedInstanceService anotherManagedInstanceService = mock(ManagedInstanceService.class);
    when(anotherManagedInstanceService.isInstanceExternallyManaged()).thenReturn(true);
    when(anotherManagedInstanceService.getGroupUuidToManaged(dbSession, userUuids)).thenReturn(uuidToManaged);
    when(anotherManagedInstanceService.getUserUuidToManaged(dbSession, userUuids)).thenReturn(uuidToManaged);
    return anotherManagedInstanceService;
  }

  private static class NeverManagedInstanceService implements ManagedInstanceService {

    @Override
    public boolean isInstanceExternallyManaged() {
      return false;
    }

    @Override
    public Map<String, Boolean> getUserUuidToManaged(DbSession dbSession, Set<String> userUuids) {
      return null;
    }

    @Override
    public Map<String, Boolean> getGroupUuidToManaged(DbSession dbSession, Set<String> groupUuids) {
      return null;
    }
  }

  private static class AlwaysManagedInstanceService implements ManagedInstanceService {

    @Override
    public boolean isInstanceExternallyManaged() {
      return true;
    }

    @Override
    public Map<String, Boolean> getUserUuidToManaged(DbSession dbSession, Set<String> userUuids) {
      return null;
    }

    @Override
    public Map<String, Boolean> getGroupUuidToManaged(DbSession dbSession, Set<String> groupUuids) {
      return null;
    }
  }

}
