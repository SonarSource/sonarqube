/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.common.almsettings.bitbucketserver;

import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import org.sonar.alm.client.bitbucketserver.BitbucketServerRestClient;
import org.sonar.alm.client.bitbucketserver.Branch;
import org.sonar.alm.client.bitbucketserver.BranchesList;
import org.sonar.alm.client.bitbucketserver.Repository;
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
import org.sonar.server.common.project.ProjectCreationRequest;
import org.sonar.server.common.project.ProjectCreator;
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class BitbucketServerProjectCreator implements DevOpsProjectCreator {

  private final AlmSettingDto almSettingDto;
  private final BitbucketServerRestClient bitbucketServerRestClient;
  private final DbClient dbClient;
  private final DevOpsProjectDescriptor devOpsProjectDescriptor;
  private final UserSession userSession;
  private final ProjectCreator projectCreator;
  private final ProjectKeyGenerator projectKeyGenerator;

  public BitbucketServerProjectCreator(AlmSettingDto almSettingDto, BitbucketServerRestClient bitbucketServerRestClient,
    DbClient dbClient, DevOpsProjectDescriptor devOpsProjectDescriptor, UserSession userSession, ProjectCreator projectCreator,
    ProjectKeyGenerator projectKeyGenerator) {
    this.almSettingDto = almSettingDto;
    this.bitbucketServerRestClient = bitbucketServerRestClient;
    this.dbClient = dbClient;
    this.devOpsProjectDescriptor = devOpsProjectDescriptor;
    this.userSession = userSession;
    this.projectCreator = projectCreator;
    this.projectKeyGenerator = projectKeyGenerator;
  }

  @Override
  public boolean isScanAllowedUsingPermissionsFromDevopsPlatform() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  @Override
  public ComponentCreationData createProjectAndBindToDevOpsPlatform(DbSession dbSession, CreationMethod creationMethod, Boolean monorepo, @Nullable String projectKey,
    @Nullable String projectName, boolean allowExisting) {

    String pat = findPersonalAccessTokenOrThrow(dbSession);
    String url = requireNonNull(almSettingDto.getUrl(), "DevOps Platform url cannot be null");
    String bitbucketRepo = devOpsProjectDescriptor.repositoryIdentifier();
    String bitbucketProject = getBitbucketProjectOrThrow();

    Repository repository = bitbucketServerRestClient.getRepo(url, pat, bitbucketProject, bitbucketRepo);
    String defaultBranchName = getDefaultBranchName(url, pat, bitbucketProject, bitbucketRepo);

    ProjectCreationRequest request = new ProjectCreationRequest(
      getProjectKey(projectKey, repository),
      getProjectName(projectName, repository),
      defaultBranchName,
      creationMethod,
      null,
      false,
      allowExisting);

    ComponentCreationData componentCreationData = projectCreator.getOrCreateProject(dbSession, request);
    ProjectDto projectDto = Optional.ofNullable(componentCreationData.projectDto()).orElseThrow();
    createProjectAlmSettingDto(dbSession, repository, projectDto, almSettingDto, monorepo);

    return componentCreationData;
  }

  private String findPersonalAccessTokenOrThrow(DbSession dbSession) {
    String userUuid = requireNonNull(userSession.getUuid(), "User UUID cannot be null.");
    Optional<AlmPatDto> almPatDto = dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto);

    return almPatDto.map(AlmPatDto::getPersonalAccessToken)
      .orElseThrow(() -> new IllegalArgumentException(format("personal access token for '%s' is missing", almSettingDto.getKey())));
  }

  private String getBitbucketProjectOrThrow() {
    if (devOpsProjectDescriptor.projectIdentifier() == null) {
      throw new IllegalArgumentException(String.format("The BitBucket project, in which the repository %s is located, is mandatory",
        devOpsProjectDescriptor.repositoryIdentifier()));
    }
    return devOpsProjectDescriptor.projectIdentifier();
  }

  private String getDefaultBranchName(String url, String pat, String project, String repo) {
    BranchesList branches = bitbucketServerRestClient.getBranches(url, pat, project, repo);
    Optional<Branch> defaultBranch = branches.findDefaultBranch();
    return defaultBranch.map(Branch::getName).orElse(null);
  }

  private String getProjectKey(@Nullable String projectKey, Repository repo) {
    return Optional.ofNullable(projectKey).orElseGet(() -> projectKeyGenerator.generateUniqueProjectKey(repo.getProject().getKey(), repo.getSlug()));
  }

  private static String getProjectName(@Nullable String projectName, Repository repository) {
    return Optional.ofNullable(projectName).orElse(repository.getName());
  }

  private void createProjectAlmSettingDto(DbSession dbSession, Repository repository, ProjectDto projectDto, AlmSettingDto almSettingDto,
    Boolean isMonorepo) {
    ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto()
      .setAlmSettingUuid(almSettingDto.getUuid())
      .setAlmRepo(repository.getProject().getKey())
      .setAlmSlug(repository.getSlug())
      .setProjectUuid(projectDto.getUuid())
      .setMonorepo(isMonorepo);

    dbClient.projectAlmSettingDao().insertOrUpdate(dbSession, projectAlmSettingDto, almSettingDto.getKey(), projectDto.getName(), projectDto.getKey());
  }

}
