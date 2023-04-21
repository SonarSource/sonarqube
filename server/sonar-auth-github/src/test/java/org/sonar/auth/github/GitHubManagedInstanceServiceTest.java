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
package org.sonar.auth.github;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserQuery;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GitHubManagedInstanceServiceTest {

  @Mock
  private GitHubSettings gitHubSettings;

  @Mock
  private UserDao userDao;

  @Mock
  private DbSession dbSession;

  @Captor
  private ArgumentCaptor<UserQuery> userQueryCaptor;

  @InjectMocks
  private GitHubManagedInstanceService gitHubManagedInstanceService;

  @Test
  public void isInstanceExternallyManaged_whenFalse_returnsFalse() {
    when(gitHubSettings.isProvisioningEnabled()).thenReturn(false);
    assertThat(gitHubManagedInstanceService.isInstanceExternallyManaged()).isFalse();
  }

  @Test
  public void isInstanceExternallyManaged_whenTrue_returnsTrue() {
    when(gitHubSettings.isProvisioningEnabled()).thenReturn(true);
    assertThat(gitHubManagedInstanceService.isInstanceExternallyManaged()).isTrue();
  }

  @Test
  public void getManagedUsersSqlFilter_whenTrue_returnsFilterByGithub() {
    String managedUsersSqlFilter = gitHubManagedInstanceService.getManagedUsersSqlFilter(true);
    assertThat(managedUsersSqlFilter).isEqualTo("external_identity_provider = 'github'");
  }

  @Test
  public void getManagedUsersSqlFilter_whenFalse_returnsFilterByNotGithub() {
    String managedUsersSqlFilter = gitHubManagedInstanceService.getManagedUsersSqlFilter(false);
    assertThat(managedUsersSqlFilter).isEqualTo("external_identity_provider <> 'github'");
  }

  @Test
  public void getUserUuidToManaged_whenNoUsers_returnsFalseForAllInput() {
    Set<String> uuids = Set.of("uuid1", "uuid2");
    Map<String, Boolean> userUuidToManaged = gitHubManagedInstanceService.getUserUuidToManaged(dbSession, uuids);

    assertThat(userUuidToManaged)
      .hasSize(2)
      .containsEntry("uuid1", false)
      .containsEntry("uuid2", false);
  }

  @Test
  public void getUserUuidToManaged_whenOneUserManaged_returnsTrueForIt() {
    String managedUserUuid = "managedUserUuid";
    Set<String> uuids = Set.of("uuid1", managedUserUuid);

    UserDto user2dto = mock(UserDto.class);
    when(user2dto.getUuid()).thenReturn(managedUserUuid);

    when(userDao.selectUsers(eq(dbSession), userQueryCaptor.capture())).thenReturn(List.of(user2dto));

    Map<String, Boolean> userUuidToManaged = gitHubManagedInstanceService.getUserUuidToManaged(dbSession, uuids);

    assertThat(userUuidToManaged)
      .hasSize(2)
      .containsEntry("uuid1", false)
      .containsEntry(managedUserUuid, true);
  }

  @Test
  public void getUserUuidToManaged_sendsTheRightQueryToUserDao() {
    Set<String> uuids = Set.of("uuid1", "uuid2");

    when(userDao.selectUsers(eq(dbSession), userQueryCaptor.capture())).thenReturn(emptyList());

    gitHubManagedInstanceService.getUserUuidToManaged(dbSession, uuids);

    UserQuery capturedQuery = userQueryCaptor.getValue();
    UserQuery expectedQuery = UserQuery.builder()
      .userUuids(uuids)
      .isManagedClause(gitHubManagedInstanceService.getManagedUsersSqlFilter(true))
      .build();
    assertThat(capturedQuery).usingRecursiveComparison().isEqualTo(expectedQuery);
  }

}
