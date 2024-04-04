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
import org.jetbrains.annotations.Nullable;
import org.sonar.alm.client.gitlab.GitLabBranch;
import org.sonar.alm.client.gitlab.GitlabApplicationClient;
import org.sonar.alm.client.gitlab.GitlabServerException;
import org.sonar.alm.client.gitlab.Project;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.common.almintegration.ProjectKeyGenerator;
import org.sonar.server.common.almsettings.DevOpsProjectCreator;
import org.sonar.server.common.almsettings.DevOpsProjectDescriptor;
import org.sonar.server.common.project.ProjectCreator;
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class GitlabProjectCreator implements DevOpsProjectCreator {

  private final DbClient dbClient;
  private final ProjectKeyGenerator projectKeyGenerator;
  private final ProjectCreator projectCreator;
  private final AlmSettingDto almSettingDto;
  private final DevOpsProjectDescriptor devOpsProjectDescriptor;
  private final GitlabApplicationClient gitlabApplicationClient;
  private final UserSession userSession;

  public GitlabProjectCreator(DbClient dbClient, ProjectKeyGenerator projectKeyGenerator, ProjectCreator projectCreator, AlmSettingDto almSettingDto,
    DevOpsProjectDescriptor devOpsProjectDescriptor, GitlabApplicationClient gitlabApplicationClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.projectKeyGenerator = projectKeyGenerator;
    this.projectCreator = projectCreator;
    this.almSettingDto = almSettingDto;
    this.devOpsProjectDescriptor = devOpsProjectDescriptor;
    this.gitlabApplicationClient = gitlabApplicationClient;
    this.userSession = userSession;
  }

  @Override
  public boolean isScanAllowedUsingPermissionsFromDevopsPlatform() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  @Override
  public ComponentCreationData createProjectAndBindToDevOpsPlatform(DbSession dbSession, CreationMethod creationMethod, Boolean monorepo, @Nullable String projectKey,
    @Nullable String projectName) {

    String pat = findPersonalAccessTokenOrThrow(dbSession, almSettingDto);

    String gitlabUrl = requireNonNull(almSettingDto.getUrl(), "DevOps Platform gitlabUrl cannot be null");

    Long gitlabProjectId = getGitlabProjectId();
    Project gitlabProject = fetchGitlabProject(gitlabUrl, pat, gitlabProjectId);

    Optional<String> almDefaultBranch = getDefaultBranchOnGitlab(gitlabUrl, pat, gitlabProjectId);
    ComponentCreationData componentCreationData = projectCreator.createProject(
      dbSession,
      getProjectKey(projectKey, gitlabProject),
      getProjectName(projectName, gitlabProject),
      almDefaultBranch.orElse(null),
      creationMethod);
    ProjectDto projectDto = Optional.ofNullable(componentCreationData.projectDto()).orElseThrow();

    createProjectAlmSettingDto(dbSession, gitlabProjectId.toString(), projectDto, almSettingDto, monorepo);
    return componentCreationData;
  }

  private String findPersonalAccessTokenOrThrow(DbSession dbSession, AlmSettingDto almSettingDto) {
    String userUuid = requireNonNull(userSession.getUuid(), "User UUID cannot be null.");
    Optional<AlmPatDto> almPatDto = dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto);
    return almPatDto.map(AlmPatDto::getPersonalAccessToken)
      .orElseThrow(() -> new IllegalArgumentException(format("personal access token for '%s' is missing", almSettingDto.getKey())));
  }

  private Long getGitlabProjectId() {
    try {
      return Long.parseLong(devOpsProjectDescriptor.repositoryIdentifier());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(format("GitLab project identifier must be a number, was '%s'", devOpsProjectDescriptor.repositoryIdentifier()));
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

  private String getProjectKey(@Nullable String projectKey, Project gitlabProject) {
    return Optional.ofNullable(projectKey).orElseGet(() -> projectKeyGenerator.generateUniqueProjectKey(gitlabProject.getPathWithNamespace()));
  }

  private static String getProjectName(@Nullable String projectName, Project gitlabProject) {
    return Optional.ofNullable(projectName).orElse(gitlabProject.getName());
  }

  private void createProjectAlmSettingDto(DbSession dbSession, String gitlabProjectId, ProjectDto projectDto, AlmSettingDto almSettingDto, Boolean monorepo) {
    ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto()
      .setAlmSettingUuid(almSettingDto.getUuid())
      .setAlmRepo(gitlabProjectId)
      .setProjectUuid(projectDto.getUuid())
      .setMonorepo(monorepo);
    dbClient.projectAlmSettingDao().insertOrUpdate(dbSession, projectAlmSettingDto, almSettingDto.getKey(), projectDto.getName(), projectDto.getKey());
  }

}
