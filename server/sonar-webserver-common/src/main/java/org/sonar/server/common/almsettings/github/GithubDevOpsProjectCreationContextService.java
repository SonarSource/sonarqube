/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.common.almsettings.github;

import org.sonar.api.server.ServerSide;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.auth.github.security.AccessToken;
import org.sonar.auth.github.security.UserAccessToken;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.common.almsettings.DevOpsProjectCreationContext;
import org.sonar.server.common.almsettings.DevOpsProjectDescriptor;
import org.sonar.server.common.almsettings.DevOpsProjectCreationContextService;
import org.sonar.server.user.UserSession;

import static java.util.Objects.requireNonNull;

@ServerSide
public class GithubDevOpsProjectCreationContextService implements DevOpsProjectCreationContextService {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final GithubApplicationClient githubApplicationClient;

  public GithubDevOpsProjectCreationContextService(DbClient dbClient, UserSession userSession, GithubApplicationClient githubApplicationClient) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.githubApplicationClient = githubApplicationClient;
  }

  @Override
  public DevOpsProjectCreationContext create(AlmSettingDto almSettingDto, DevOpsProjectDescriptor devOpsProjectDescriptor) {
    AccessToken accessToken = getAccessToken(almSettingDto);
    return createDevOpsProject(almSettingDto, devOpsProjectDescriptor, accessToken);
  }

  private AccessToken getAccessToken(AlmSettingDto almSettingDto) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String userUuid = requireNonNull(userSession.getUuid(), "User UUID cannot be null.");
      return dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto)
        .map(AlmPatDto::getPersonalAccessToken)
        .map(UserAccessToken::new)
        .orElseThrow(() -> new IllegalArgumentException("No personal access token found"));
    }
  }

  public DevOpsProjectCreationContext createDevOpsProject(AlmSettingDto almSettingDto, DevOpsProjectDescriptor devOpsProjectDescriptor, AccessToken accessToken) {
    GithubApplicationClient.Repository repository = fetchRepository(almSettingDto, devOpsProjectDescriptor, accessToken);
    return new DevOpsProjectCreationContext(repository.getName(), repository.getFullName(), repository.getFullName(),
      !repository.isPrivate(), repository.getDefaultBranch(), almSettingDto, userSession);
  }

  private GithubApplicationClient.Repository fetchRepository(AlmSettingDto almSettingDto, DevOpsProjectDescriptor devOpsProjectDescriptor, AccessToken accessToken) {
    String url = requireNonNull(almSettingDto.getUrl(), "DevOps Platform url cannot be null");
    return githubApplicationClient.getRepository(url, accessToken, devOpsProjectDescriptor.repositoryIdentifier())
      .orElseThrow(() -> new IllegalStateException(
        String.format("Impossible to find the repository '%s' on GitHub, using the devops config %s", devOpsProjectDescriptor.repositoryIdentifier(), almSettingDto.getKey())));
  }


}
