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

import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.alm.client.github.GithubPermissionConverter;
import org.sonar.api.server.ServerSide;
import org.sonar.auth.github.AppInstallationToken;
import org.sonar.auth.github.GitHubSettings;
import org.sonar.auth.github.GithubAppConfiguration;
import org.sonar.auth.github.client.GithubApplicationClient;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.common.almintegration.ProjectKeyGenerator;
import org.sonar.server.common.almsettings.DefaultDevOpsProjectCreator;
import org.sonar.server.common.almsettings.DevOpsProjectCreationContext;
import org.sonar.server.common.almsettings.DevOpsProjectCreator;
import org.sonar.server.common.almsettings.DevOpsProjectCreatorFactory;
import org.sonar.server.common.almsettings.DevOpsProjectDescriptor;
import org.sonar.server.common.permission.PermissionUpdater;
import org.sonar.server.common.permission.UserPermissionChange;
import org.sonar.server.common.project.ProjectCreator;
import org.sonar.server.exceptions.BadConfigurationException;
import org.sonar.server.management.ManagedProjectService;
import org.sonar.server.permission.PermissionService;

import static java.lang.String.format;
import static org.sonar.core.ce.CeTaskCharacteristics.DEVOPS_PLATFORM_PROJECT_IDENTIFIER;
import static org.sonar.core.ce.CeTaskCharacteristics.DEVOPS_PLATFORM_URL;

@ServerSide
public class GithubProjectCreatorFactory implements DevOpsProjectCreatorFactory {
  private static final Logger LOG = LoggerFactory.getLogger(GithubProjectCreatorFactory.class);

  private final DbClient dbClient;
  private final GithubGlobalSettingsValidator githubGlobalSettingsValidator;
  private final GithubApplicationClient githubApplicationClient;
  private final ProjectKeyGenerator projectKeyGenerator;
  private final ProjectCreator projectCreator;
  private final GitHubSettings gitHubSettings;
  private final GithubPermissionConverter githubPermissionConverter;
  private final PermissionUpdater<UserPermissionChange> permissionUpdater;
  private final PermissionService permissionService;
  private final ManagedProjectService managedProjectService;
  private final GithubDevOpsProjectCreationContextService githubDevOpsProjectService;

  public GithubProjectCreatorFactory(DbClient dbClient, GithubGlobalSettingsValidator githubGlobalSettingsValidator,
    GithubApplicationClient githubApplicationClient, ProjectKeyGenerator projectKeyGenerator,
    ProjectCreator projectCreator, GitHubSettings gitHubSettings, GithubPermissionConverter githubPermissionConverter,
    PermissionUpdater<UserPermissionChange> permissionUpdater, PermissionService permissionService, ManagedProjectService managedProjectService,
    GithubDevOpsProjectCreationContextService githubDevOpsProjectService) {
    this.dbClient = dbClient;
    this.githubGlobalSettingsValidator = githubGlobalSettingsValidator;
    this.githubApplicationClient = githubApplicationClient;
    this.projectKeyGenerator = projectKeyGenerator;
    this.projectCreator = projectCreator;
    this.gitHubSettings = gitHubSettings;
    this.githubPermissionConverter = githubPermissionConverter;
    this.permissionUpdater = permissionUpdater;
    this.permissionService = permissionService;
    this.managedProjectService = managedProjectService;
    this.githubDevOpsProjectService = githubDevOpsProjectService;
  }

  @Override
  public Optional<DevOpsProjectCreator> getDevOpsProjectCreator(DbSession dbSession, Map<String, String> characteristics) {
    String githubApiUrl = characteristics.get(DEVOPS_PLATFORM_URL);
    String githubRepository = characteristics.get(DEVOPS_PLATFORM_PROJECT_IDENTIFIER);
    if (githubApiUrl == null || githubRepository == null) {
      return Optional.empty();
    }
    DevOpsProjectDescriptor devOpsProjectDescriptor = new DevOpsProjectDescriptor(ALM.GITHUB, githubApiUrl, githubRepository, null);

    return dbClient.almSettingDao().selectByAlm(dbSession, ALM.GITHUB).stream()
      .filter(almSettingDto -> devOpsProjectDescriptor.url().equals(almSettingDto.getUrl()))
      .map(almSettingDto -> findInstallationIdAndCreateDevOpsProjectCreator(devOpsProjectDescriptor, almSettingDto))
      .flatMap(Optional::stream)
      .findFirst();

  }

  private Optional<DevOpsProjectCreator> findInstallationIdAndCreateDevOpsProjectCreator(DevOpsProjectDescriptor devOpsProjectDescriptor,
    AlmSettingDto almSettingDto) {
    GithubAppConfiguration githubAppConfiguration = githubGlobalSettingsValidator.validate(almSettingDto);
    return findInstallationIdToAccessRepo(githubAppConfiguration, devOpsProjectDescriptor.repositoryIdentifier())
      .map(installationId -> generateAppInstallationToken(githubAppConfiguration, installationId))
      .map(appInstallationToken -> createGithubProjectCreator(devOpsProjectDescriptor, almSettingDto, appInstallationToken));
  }

  private DevOpsProjectCreator createGithubProjectCreator(DevOpsProjectDescriptor devOpsProjectDescriptor, AlmSettingDto almSettingDto,
    AppInstallationToken appInstallationToken) {
    LOG.info("DevOps configuration {} auto-detected for project {}", almSettingDto.getKey(), devOpsProjectDescriptor.repositoryIdentifier());

    DevOpsProjectCreationContext devOpsProjectCreationContext = githubDevOpsProjectService.createDevOpsProject(almSettingDto, devOpsProjectDescriptor, appInstallationToken);
    return createDefaultDevOpsProjectCreator(devOpsProjectCreationContext);
  }

  @Override
  public Optional<DevOpsProjectCreator> getDevOpsProjectCreator(AlmSettingDto almSettingDto, DevOpsProjectDescriptor devOpsProjectDescriptor) {
    if (almSettingDto.getAlm() != ALM.GITHUB) {
      return Optional.empty();
    }
    DevOpsProjectCreationContext devOpsProjectCreationContext = githubDevOpsProjectService.create(almSettingDto, devOpsProjectDescriptor);
    return Optional.of(createDefaultDevOpsProjectCreator(devOpsProjectCreationContext));
  }

  private DefaultDevOpsProjectCreator createDefaultDevOpsProjectCreator(DevOpsProjectCreationContext devOpsProjectCreationContext) {
    Optional<AppInstallationToken> authAppInstallationToken = getAuthAppInstallationTokenIfNecessary(devOpsProjectCreationContext);

    return new GithubProjectCreator(dbClient, devOpsProjectCreationContext, projectKeyGenerator,
      gitHubSettings, projectCreator, permissionService, permissionUpdater,
      managedProjectService, githubApplicationClient, githubPermissionConverter, authAppInstallationToken.orElse(null));
  }

  private Optional<AppInstallationToken> getAuthAppInstallationTokenIfNecessary(DevOpsProjectCreationContext devOpsProjectCreationContext) {
    if (gitHubSettings.isProvisioningEnabled()) {
      GithubAppConfiguration githubAppConfiguration = new GithubAppConfiguration(Long.parseLong(gitHubSettings.appId()), gitHubSettings.privateKey(), gitHubSettings.apiURL());
      long installationId = findInstallationIdToAccessRepo(githubAppConfiguration, devOpsProjectCreationContext.devOpsPlatformIdentifier())
        .orElseThrow(() -> new BadConfigurationException("PROJECT",
          format("GitHub auto-provisioning is activated. However the repo %s is not in the scope of the authentication application. "
              + "The permissions can't be checked, and the project can not be created.",
            devOpsProjectCreationContext.devOpsPlatformIdentifier())));
      return Optional.of(generateAppInstallationToken(githubAppConfiguration, installationId));
    }
    return Optional.empty();
  }

  private Optional<Long> findInstallationIdToAccessRepo(GithubAppConfiguration githubAppConfiguration, String repositoryKey) {
    return githubApplicationClient.getInstallationId(githubAppConfiguration, repositoryKey);
  }

  private AppInstallationToken generateAppInstallationToken(GithubAppConfiguration githubAppConfiguration, long installationId) {
    return githubApplicationClient.createAppInstallationToken(githubAppConfiguration, installationId)
      .orElseThrow(() -> new IllegalStateException(format("Error while generating token for GitHub Api Url %s (installation id: %s)",
        githubAppConfiguration.getApiEndpoint(), installationId)));
  }

}
