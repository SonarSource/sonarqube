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
package org.sonar.server.common.almsettings.bitbucketcloud;

import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient;
import org.sonar.alm.client.bitbucket.bitbucketcloud.Repository;
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
import static java.util.Optional.ofNullable;

public class BitbucketCloudProjectCreator implements DevOpsProjectCreator {

  private final DbClient dbClient;
  private final AlmSettingDto almSettingDto;
  private final DevOpsProjectDescriptor devOpsProjectDescriptor;
  private final UserSession userSession;
  private final BitbucketCloudRestClient bitbucketCloudRestClient;
  private final ProjectCreator projectCreator;
  private final ProjectKeyGenerator projectKeyGenerator;


  public BitbucketCloudProjectCreator(DbClient dbClient, AlmSettingDto almSettingDto, DevOpsProjectDescriptor devOpsProjectDescriptor, UserSession userSession,
    BitbucketCloudRestClient bitbucketCloudRestClient, ProjectCreator projectCreator, ProjectKeyGenerator projectKeyGenerator) {
    this.dbClient = dbClient;
    this.almSettingDto = almSettingDto;
    this.devOpsProjectDescriptor = devOpsProjectDescriptor;
    this.userSession = userSession;
    this.bitbucketCloudRestClient = bitbucketCloudRestClient;
    this.projectCreator = projectCreator;
    this.projectKeyGenerator = projectKeyGenerator;
  }

  @Override
  public boolean isScanAllowedUsingPermissionsFromDevopsPlatform() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  @Override
  public ComponentCreationData createProjectAndBindToDevOpsPlatform(DbSession dbSession, CreationMethod creationMethod, Boolean monorepo, @Nullable String projectKey,
    @Nullable String projectName) {

    String pat = findPersonalAccessTokenOrThrow(dbSession, almSettingDto);
    String workspace = ofNullable(almSettingDto.getAppId())
      .orElseThrow(() -> new IllegalArgumentException(String.format("workspace for alm setting %s is missing", almSettingDto.getKey())));

    Repository repo = bitbucketCloudRestClient.getRepo(pat, workspace, devOpsProjectDescriptor.repositoryIdentifier());

    ComponentCreationData componentCreationData = projectCreator.createProject(
      dbSession,
      getProjectKey(workspace, projectKey, repo),
      getProjectName(projectName, repo),
      repo.getMainBranch().getName(),
      creationMethod);
    ProjectDto projectDto = Optional.ofNullable(componentCreationData.projectDto()).orElseThrow();

    createProjectAlmSettingDto(dbSession, repo.getSlug(), projectDto, almSettingDto, monorepo);
    return componentCreationData;
  }

  private String findPersonalAccessTokenOrThrow(DbSession dbSession, AlmSettingDto almSettingDto) {
    String userUuid = requireNonNull(userSession.getUuid(), "User UUID cannot be null.");
    Optional<AlmPatDto> almPatDto = dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto);
    return almPatDto.map(AlmPatDto::getPersonalAccessToken)
      .orElseThrow(() -> new IllegalArgumentException(format("personal access token for '%s' is missing", almSettingDto.getKey())));
  }

  private String getProjectKey(String workspace, @Nullable String projectKey, Repository repository) {
    return Optional.ofNullable(projectKey).orElseGet(() -> projectKeyGenerator.generateUniqueProjectKey(workspace, repository.getSlug()));
  }

  private static String getProjectName(@Nullable String projectName, Repository repository) {
    return Optional.ofNullable(projectName).orElse(repository.getName());
  }

  private void createProjectAlmSettingDto(DbSession dbSession, String repoSlug, ProjectDto projectDto, AlmSettingDto almSettingDto, Boolean monorepo) {
    ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto()
      .setAlmSettingUuid(almSettingDto.getUuid())
      .setAlmRepo(repoSlug)
      .setProjectUuid(projectDto.getUuid())
      .setMonorepo(monorepo);
    dbClient.projectAlmSettingDao().insertOrUpdate(dbSession, projectAlmSettingDto, almSettingDto.getKey(), projectDto.getName(), projectDto.getKey());
  }

}
