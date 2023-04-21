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
import java.util.function.Function;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserQuery;
import org.sonar.server.management.ManagedInstanceService;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@ServerSide
@ComputeEngineSide
public class GitHubManagedInstanceService implements ManagedInstanceService {

  private final GitHubSettings gitHubSettings;
  private final UserDao userDao;

  public GitHubManagedInstanceService(GitHubSettings gitHubSettings, UserDao userDao) {
    this.gitHubSettings = gitHubSettings;
    this.userDao = userDao;
  }

  @Override
  public boolean isInstanceExternallyManaged() {
    return gitHubSettings.isProvisioningEnabled();
  }

  @Override
  public Map<String, Boolean> getUserUuidToManaged(DbSession dbSession, Set<String> userUuids) {
    Set<String> gitHubUserUuids = findManagedUserUuids(dbSession, userUuids);

    return userUuids.stream()
      .collect(toMap(Function.identity(), gitHubUserUuids::contains));
  }

  private Set<String> findManagedUserUuids(DbSession dbSession, Set<String> userUuids) {
    List<UserDto> userDtos = findManagedUsers(dbSession, userUuids);

    return userDtos.stream()
      .map(UserDto::getUuid)
      .collect(toSet());
  }

  private List<UserDto> findManagedUsers(DbSession dbSession, Set<String> userUuids) {
    UserQuery managedUsersQuery = UserQuery.builder()
      .userUuids(userUuids)
      .isManagedClause(getManagedUsersSqlFilter(true))
      .build();

    return userDao.selectUsers(dbSession, managedUsersQuery);
  }

  @Override
  public Map<String, Boolean> getGroupUuidToManaged(DbSession dbSession, Set<String> groupUuids) {
    throw new IllegalStateException("Not implemented.");
  }

  @Override
  public String getManagedUsersSqlFilter(boolean filterByManaged) {
    String operator = filterByManaged ? "=" : "<>";
    return String.format("external_identity_provider %s '%s'", operator, GitHubIdentityProvider.KEY);
  }

  @Override
  public String getManagedGroupsSqlFilter(boolean filterByManaged) {
    throw new IllegalStateException("Not implemented.");
  }
}
