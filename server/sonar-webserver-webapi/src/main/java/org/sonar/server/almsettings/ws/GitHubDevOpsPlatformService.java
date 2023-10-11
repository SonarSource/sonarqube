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
package org.sonar.server.almsettings.ws;

import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.alm.client.github.AppInstallationToken;
import org.sonar.alm.client.github.GithubApplicationClient;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.alm.client.github.config.GithubAppConfiguration;
import org.sonar.alm.client.github.security.AccessToken;
import org.sonar.api.server.ServerSide;
import org.sonar.auth.github.GitHubSettings;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.almintegration.ws.ProjectKeyGenerator;
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.component.ComponentCreationParameters;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.component.NewComponent;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.db.project.CreationMethod.Category.ALM_IMPORT;
import static org.sonar.db.project.CreationMethod.SCANNER_API_DEVOPS_AUTO_CONFIG;
import static org.sonar.db.project.CreationMethod.getCreationMethod;
import static org.sonar.server.component.NewComponent.newComponentBuilder;

@ServerSide
public class GitHubDevOpsPlatformService implements DevOpsPlatformService {
  private static final Logger LOG = LoggerFactory.getLogger(GitHubDevOpsPlatformService.class);

  public static final String DEVOPS_PLATFORM_URL = "devOpsPlatformUrl";
  public static final String DEVOPS_PLATFORM_PROJECT_IDENTIFIER = "devOpsPlatformProjectIdentifier";

  private final DbClient dbClient;
  private final GithubGlobalSettingsValidator githubGlobalSettingsValidator;
  private final GithubApplicationClient githubApplicationClient;
  private final ProjectDefaultVisibility projectDefaultVisibility;
  private final ProjectKeyGenerator projectKeyGenerator;
  private final UserSession userSession;
  private final ComponentUpdater componentUpdater;
  private final GitHubSettings gitHubSettings;

  public GitHubDevOpsPlatformService(DbClient dbClient, GithubGlobalSettingsValidator githubGlobalSettingsValidator,
    GithubApplicationClient githubApplicationClient, ProjectDefaultVisibility projectDefaultVisibility, ProjectKeyGenerator projectKeyGenerator, UserSession userSession,
    ComponentUpdater componentUpdater, GitHubSettings gitHubSettings) {
    this.dbClient = dbClient;
    this.githubGlobalSettingsValidator = githubGlobalSettingsValidator;
    this.githubApplicationClient = githubApplicationClient;
    this.projectDefaultVisibility = projectDefaultVisibility;
    this.projectKeyGenerator = projectKeyGenerator;
    this.userSession = userSession;
    this.componentUpdater = componentUpdater;
    this.gitHubSettings = gitHubSettings;
  }

  @Override
  public ALM getDevOpsPlatform() {
    return ALM.GITHUB;
  }

  @Override
  public Optional<DevOpsProjectDescriptor> getDevOpsProjectDescriptor(Map<String, String> characteristics) {
    String githubApiUrl = characteristics.get(DEVOPS_PLATFORM_URL);
    String githubRepository = characteristics.get(DEVOPS_PLATFORM_PROJECT_IDENTIFIER);
    if (githubApiUrl != null && githubRepository != null) {
      return Optional.of(new DevOpsProjectDescriptor(ALM.GITHUB, githubApiUrl, githubRepository));
    }
    return Optional.empty();
  }

  @Override
  public Optional<AlmSettingDto> getValidAlmSettingDto(DbSession dbSession, DevOpsProjectDescriptor devOpsProjectDescriptor) {
    Optional<AlmSettingDto> configurationToUse = dbClient.almSettingDao().selectByAlm(dbSession, getDevOpsPlatform()).stream()
      .filter(almSettingDto -> devOpsProjectDescriptor.url().equals(almSettingDto.getUrl()))
      .filter(almSettingDto -> findInstallationIdToAccessRepo(almSettingDto, devOpsProjectDescriptor.projectIdentifier()).isPresent())
      .findFirst();
    if (configurationToUse.isPresent()) {
      LOG.info("DevOps configuration {} auto-detected", configurationToUse.get().getKey());
    } else {
      LOG.info("Could not auto-detect a DevOps configuration for project {} (api url {})",
        devOpsProjectDescriptor.projectIdentifier(), devOpsProjectDescriptor.url());
    }
    return configurationToUse;
  }

  @Override
  public ComponentCreationData createProjectAndBindToDevOpsPlatform(DbSession dbSession, String projectKey, AlmSettingDto almSettingDto,
    DevOpsProjectDescriptor devOpsProjectDescriptor) {
    GithubAppConfiguration githubAppConfiguration = githubGlobalSettingsValidator.validate(almSettingDto);
    GithubApplicationClient.Repository repository = findInstallationIdToAccessRepo(almSettingDto, devOpsProjectDescriptor.projectIdentifier())
      .flatMap(installationId -> findRepositoryOnGithub(devOpsProjectDescriptor.projectIdentifier(), githubAppConfiguration, installationId))
      .orElseThrow(() -> new IllegalStateException(format("Impossible to find the repository %s on GitHub, using the devops config %s.",
        devOpsProjectDescriptor.projectIdentifier(), almSettingDto.getKey())));

    return createProjectAndBindToDevOpsPlatform(dbSession, projectKey, almSettingDto, repository, SCANNER_API_DEVOPS_AUTO_CONFIG);
  }

  private Optional<Long> findInstallationIdToAccessRepo(AlmSettingDto almSettingDto, String repositoryKey) {
    try {
      GithubAppConfiguration githubAppConfiguration = githubGlobalSettingsValidator.validate(almSettingDto);
      return githubApplicationClient.getInstallationId(githubAppConfiguration, repositoryKey);
    } catch (Exception exception) {
      LOG.info(format("Could not use DevOps configuration '%s' to access repo %s. Error: %s", almSettingDto.getKey(), repositoryKey, exception.getMessage()));
      return Optional.empty();
    }
  }

  private Optional<GithubApplicationClient.Repository> findRepositoryOnGithub(String organizationAndRepository,
    GithubAppConfiguration githubAppConfiguration, long installationId) {
    AppInstallationToken accessToken = generateAppInstallationToken(githubAppConfiguration, installationId);
    return githubApplicationClient.getRepository(githubAppConfiguration.getApiEndpoint(), accessToken, organizationAndRepository);
  }

  private AppInstallationToken generateAppInstallationToken(GithubAppConfiguration githubAppConfiguration, long installationId) {
    return githubApplicationClient.createAppInstallationToken(githubAppConfiguration, installationId)
      .orElseThrow(() -> new IllegalStateException(format("Error while generating token for GitHub Api Url %s (installation id: %s)",
        githubAppConfiguration.getApiEndpoint(), installationId)));
  }

  @Override
  public ComponentCreationData createProjectAndBindToDevOpsPlatform(DbSession dbSession, AlmSettingDto almSettingDto, AccessToken accessToken,
    DevOpsProjectDescriptor devOpsProjectDescriptor) {
    String url = requireNonNull(almSettingDto.getUrl(), "DevOps Platform url cannot be null");
    GithubApplicationClient.Repository repository = githubApplicationClient.getRepository(url, accessToken, devOpsProjectDescriptor.projectIdentifier())
      .orElseThrow(() -> new NotFoundException(String.format("GitHub repository '%s' not found", devOpsProjectDescriptor)));

    CreationMethod creationMethod = getCreationMethod(ALM_IMPORT, userSession.isAuthenticatedBrowserSession());
    return createProjectAndBindToDevOpsPlatform(dbSession, null, almSettingDto, repository, creationMethod);
  }

  private ComponentCreationData createProjectAndBindToDevOpsPlatform(DbSession dbSession, @Nullable String projectKey, AlmSettingDto almSettingDto,
    GithubApplicationClient.Repository repository, CreationMethod creationMethod) {
    ComponentCreationData componentCreationData = createProject(dbSession, projectKey, repository, creationMethod);
    ProjectDto projectDto = Optional.ofNullable(componentCreationData.projectDto()).orElseThrow();
    createProjectAlmSettingDto(dbSession, repository, projectDto, almSettingDto);
    return componentCreationData;
  }

  private ComponentCreationData createProject(DbSession dbSession, @Nullable String projectKey, GithubApplicationClient.Repository repository, CreationMethod creationMethod) {
    boolean visibility = projectDefaultVisibility.get(dbSession).isPrivate();
    NewComponent projectComponent = newComponentBuilder()
      .setKey(Optional.ofNullable(projectKey).orElse(getUniqueProjectKey(repository)))
      .setName(repository.getName())
      .setPrivate(visibility)
      .setQualifier(PROJECT)
      .build();
    ComponentCreationParameters componentCreationParameters = ComponentCreationParameters.builder()
      .newComponent(projectComponent)
      .userLogin(userSession.getLogin())
      .userUuid(userSession.getUuid())
      .mainBranchName(repository.getDefaultBranch())
      .isManaged(gitHubSettings.isProvisioningEnabled())
      .creationMethod(creationMethod)
      .build();
    return componentUpdater.createWithoutCommit(dbSession, componentCreationParameters);
  }

  private String getUniqueProjectKey(GithubApplicationClient.Repository repository) {
    return projectKeyGenerator.generateUniqueProjectKey(repository.getFullName());
  }

  private void createProjectAlmSettingDto(DbSession dbSession, GithubApplicationClient.Repository repo, ProjectDto projectDto, AlmSettingDto almSettingDto) {
    ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto()
      .setAlmSettingUuid(almSettingDto.getUuid())
      .setAlmRepo(repo.getFullName())
      .setAlmSlug(null)
      .setProjectUuid(projectDto.getUuid())
      .setSummaryCommentEnabled(true)
      .setMonorepo(false);
    dbClient.projectAlmSettingDao().insertOrUpdate(dbSession, projectAlmSettingDto, almSettingDto.getKey(), projectDto.getName(), projectDto.getKey());
  }

}
