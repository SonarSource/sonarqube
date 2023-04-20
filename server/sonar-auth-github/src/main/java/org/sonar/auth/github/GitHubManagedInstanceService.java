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

import java.util.Map;
import java.util.Set;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbSession;
import org.sonar.server.management.ManagedInstanceService;

@ServerSide
@ComputeEngineSide
public class GitHubManagedInstanceService implements ManagedInstanceService {

  private final GitHubSettings gitHubSettings;

  public GitHubManagedInstanceService(GitHubSettings gitHubSettings) {
    this.gitHubSettings = gitHubSettings;
  }

  @Override
  public boolean isInstanceExternallyManaged() {
    return gitHubSettings.isProvisioningEnabled();
  }

  @Override
  public Map<String, Boolean> getUserUuidToManaged(DbSession dbSession, Set<String> userUuids) {
    throw new IllegalStateException("Not implemented.");
  }

  @Override
  public Map<String, Boolean> getGroupUuidToManaged(DbSession dbSession, Set<String> groupUuids) {
    throw new IllegalStateException("Not implemented.");
  }

  @Override
  public String getManagedUsersSqlFilter(boolean filterByManaged) {
    throw new IllegalStateException("Not implemented.");
  }

  @Override
  public String getManagedGroupsSqlFilter(boolean filterByManaged) {
    throw new IllegalStateException("Not implemented.");
  }
}
