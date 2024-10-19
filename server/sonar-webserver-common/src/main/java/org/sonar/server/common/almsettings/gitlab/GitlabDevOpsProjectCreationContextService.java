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
package org.sonar.server.common.almsettings.gitlab;

import java.util.Optional;
import org.sonar.alm.client.gitlab.GitLabBranch;
import org.sonar.alm.client.gitlab.GitlabApplicationClient;
import org.sonar.alm.client.gitlab.GitlabServerException;
import org.sonar.alm.client.gitlab.Project;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.common.almsettings.DevOpsProjectCreationContext;
import org.sonar.server.common.almsettings.DevOpsProjectDescriptor;
import org.sonar.server.common.almsettings.DevOpsProjectCreationContextService;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

@ServerSide
public class GitlabDevOpsProjectCreationContextService implements DevOpsProjectCreationContextService {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final GitlabApplicationClient gitlabApplicationClient;

  public GitlabDevOpsProjectCreationContextService(DbClient dbClient, UserSession userSession, GitlabApplicationClient gitlabApplicationClient) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.gitlabApplicationClient = gitlabApplicationClient;
  }

  @Override
  public DevOpsProjectCreationContext create(AlmSettingDto almSettingDto, DevOpsProjectDescriptor devOpsProjectDescriptor) {
    String url = requireNonNull(almSettingDto.getUrl(), "DevOps Platform url cannot be null");
    Long gitlabProjectId = getGitlabProjectId(devOpsProjectDescriptor);
    String pat = findPersonalAccessTokenOrThrow(almSettingDto);
    Project gitlabProject = fetchGitlabProject(url, pat, gitlabProjectId);
    String defaultBranchName = getDefaultBranchOnGitlab(url, pat, gitlabProjectId).orElse(null);

    return new DevOpsProjectCreationContext(gitlabProject.getName(), gitlabProject.getPathWithNamespace(),
      String.valueOf(gitlabProjectId), gitlabProject.getVisibility().equals("public"), defaultBranchName, almSettingDto, userSession);

  }

  private static Long getGitlabProjectId(DevOpsProjectDescriptor devOpsProjectDescriptor) {
    try {
      return Long.parseLong(devOpsProjectDescriptor.repositoryIdentifier());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(format("GitLab project identifier must be a number, was '%s'", devOpsProjectDescriptor.repositoryIdentifier()));
    }
  }

  private String findPersonalAccessTokenOrThrow(AlmSettingDto almSettingDto) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String userUuid = requireNonNull(userSession.getUuid(), "User UUID cannot be null.");
      Optional<AlmPatDto> almPatDto = dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto);
      return almPatDto.map(AlmPatDto::getPersonalAccessToken)
        .orElseThrow(() -> new IllegalArgumentException(format("Personal access token for '%s' is missing", almSettingDto.getKey())));
    }
  }

  private Project fetchGitlabProject(String gitlabUrl, String pat, Long gitlabProjectId) {
    try {
      return gitlabApplicationClient.getProject(
        gitlabUrl,
        pat,
        gitlabProjectId);
    } catch (GitlabServerException e) {
      throw new IllegalStateException(format("Failed to fetch GitLab project with ID '%s' from '%s'", gitlabProjectId, gitlabUrl), e);
    }
  }

  private Optional<String> getDefaultBranchOnGitlab(String gitlabUrl, String pat, long gitlabProjectId) {
    Optional<GitLabBranch> almMainBranch = gitlabApplicationClient.getBranches(gitlabUrl, pat, gitlabProjectId).stream().filter(GitLabBranch::isDefault).findFirst();
    return almMainBranch.map(GitLabBranch::getName);
  }

}
