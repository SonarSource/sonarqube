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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.alm.client.github.AppInstallationToken;
import org.sonar.alm.client.github.GithubApplicationClient;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.alm.client.github.config.GithubAppConfiguration;
import org.sonar.alm.client.github.security.AccessToken;
import org.sonar.api.server.ServerSide;
import org.sonar.auth.github.GitHubSettings;
import org.sonar.auth.github.GithubPermissionConverter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.almintegration.ws.ProjectKeyGenerator;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static org.sonar.core.ce.CeTaskCharacteristics.DEVOPS_PLATFORM_PROJECT_IDENTIFIER;
import static org.sonar.core.ce.CeTaskCharacteristics.DEVOPS_PLATFORM_URL;

@ServerSide
public class GithubProjectCreatorFactory implements DevOpsProjectCreatorFactory {
  private static final Logger LOG = LoggerFactory.getLogger(GithubProjectCreatorFactory.class);

  private final DbClient dbClient;
  private final GithubGlobalSettingsValidator githubGlobalSettingsValidator;
  private final GithubApplicationClient githubApplicationClient;
  private final ProjectDefaultVisibility projectDefaultVisibility;
  private final ProjectKeyGenerator projectKeyGenerator;
  private final UserSession userSession;
  private final ComponentUpdater componentUpdater;
  private final GitHubSettings gitHubSettings;
  private final GithubPermissionConverter githubPermissionConverter;

  public GithubProjectCreatorFactory(DbClient dbClient, GithubGlobalSettingsValidator githubGlobalSettingsValidator,
    GithubApplicationClient githubApplicationClient, ProjectDefaultVisibility projectDefaultVisibility, ProjectKeyGenerator projectKeyGenerator, UserSession userSession,
    ComponentUpdater componentUpdater, GitHubSettings gitHubSettings, GithubPermissionConverter githubPermissionConverter) {
    this.dbClient = dbClient;
    this.githubGlobalSettingsValidator = githubGlobalSettingsValidator;
    this.githubApplicationClient = githubApplicationClient;
    this.projectDefaultVisibility = projectDefaultVisibility;
    this.projectKeyGenerator = projectKeyGenerator;
    this.userSession = userSession;
    this.componentUpdater = componentUpdater;
    this.gitHubSettings = gitHubSettings;
    this.githubPermissionConverter = githubPermissionConverter;
  }

  @Override
  public Optional<DevOpsProjectCreator> getDevOpsProjectCreator(DbSession dbSession, Map<String, String> characteristics) {
    String githubApiUrl = characteristics.get(DEVOPS_PLATFORM_URL);
    String githubRepository = characteristics.get(DEVOPS_PLATFORM_PROJECT_IDENTIFIER);
    if (githubApiUrl == null || githubRepository == null) {
      return Optional.empty();
    }
    DevOpsProjectDescriptor devOpsProjectDescriptor = new DevOpsProjectDescriptor(ALM.GITHUB, githubApiUrl, githubRepository);

    Optional<DevOpsProjectCreator> githubProjectCreator = dbClient.almSettingDao().selectByAlm(dbSession, ALM.GITHUB).stream()
      .filter(almSettingDto -> devOpsProjectDescriptor.url().equals(almSettingDto.getUrl()))
      .map(almSettingDto -> findInstallationIdAndCreateDevOpsProjectCreator(dbSession, devOpsProjectDescriptor, almSettingDto))
      .flatMap(Optional::stream)
      .findFirst();

    if (githubProjectCreator.isPresent()) {
      return githubProjectCreator;
    }

    throw new IllegalStateException(format("The project %s could not be created. It was auto-detected as a %s project "
                                           + "and no valid DevOps platform configuration were found to access %s. Please check with a SonarQube administrator.",
      devOpsProjectDescriptor.projectIdentifier(), devOpsProjectDescriptor.alm(), devOpsProjectDescriptor.url()));
  }

  private Optional<DevOpsProjectCreator> findInstallationIdAndCreateDevOpsProjectCreator(DbSession dbSession, DevOpsProjectDescriptor devOpsProjectDescriptor,
    AlmSettingDto almSettingDto) {
    GithubAppConfiguration githubAppConfiguration = githubGlobalSettingsValidator.validate(almSettingDto);
    return findInstallationIdToAccessRepo(githubAppConfiguration, devOpsProjectDescriptor.projectIdentifier())
      .map(installationId -> generateAppInstallationToken(githubAppConfiguration, installationId))
      .map(appInstallationToken -> createGithubProjectCreator(dbSession, devOpsProjectDescriptor, almSettingDto, appInstallationToken));
  }

  private GithubProjectCreator createGithubProjectCreator(DbSession dbSession, DevOpsProjectDescriptor devOpsProjectDescriptor, AlmSettingDto almSettingDto,
    AppInstallationToken appInstallationToken) {
    LOG.info("DevOps configuration {} auto-detected for project {}", almSettingDto.getKey(), devOpsProjectDescriptor.projectIdentifier());
    Optional<AppInstallationToken> authAppInstallationToken = getAuthAppInstallationTokenIfNecessary(devOpsProjectDescriptor);

    GithubProjectCreationParameters githubProjectCreationParameters = new GithubProjectCreationParameters(devOpsProjectDescriptor,
      almSettingDto, projectDefaultVisibility.get(dbSession).isPrivate(), gitHubSettings.isProvisioningEnabled(), userSession, appInstallationToken,
      authAppInstallationToken.orElse(null));
    return new GithubProjectCreator(dbClient, githubApplicationClient, githubPermissionConverter, projectKeyGenerator, componentUpdater,
      githubProjectCreationParameters);
  }

  public Optional<DevOpsProjectCreator> getDevOpsProjectCreator(DbSession dbSession, AlmSettingDto almSettingDto, AccessToken accessToken,
    DevOpsProjectDescriptor devOpsProjectDescriptor) {

    Optional<AppInstallationToken> authAppInstallationToken = getAuthAppInstallationTokenIfNecessary(devOpsProjectDescriptor);
    GithubProjectCreationParameters githubProjectCreationParameters = new GithubProjectCreationParameters(devOpsProjectDescriptor,
      almSettingDto, projectDefaultVisibility.get(dbSession).isPrivate(), gitHubSettings.isProvisioningEnabled(), userSession, accessToken, authAppInstallationToken.orElse(null));
    return Optional.of(
      new GithubProjectCreator(dbClient, githubApplicationClient, githubPermissionConverter, projectKeyGenerator, componentUpdater, githubProjectCreationParameters)
    );
  }

  private Optional<AppInstallationToken> getAuthAppInstallationTokenIfNecessary(DevOpsProjectDescriptor devOpsProjectDescriptor) {
    if (gitHubSettings.isProvisioningEnabled()) {
      GithubAppConfiguration githubAppConfiguration = new GithubAppConfiguration(Long.parseLong(gitHubSettings.appId()), gitHubSettings.privateKey(), gitHubSettings.apiURL());
      long installationId = findInstallationIdToAccessRepo(githubAppConfiguration, devOpsProjectDescriptor.projectIdentifier())
        .orElseThrow(() -> new IllegalStateException(format("GitHub auto-provisioning is activated. However the repo %s is not in the scope of the authentication application. "
                                                            + "The permissions can't be checked, and the project can not be breated.",
          devOpsProjectDescriptor.projectIdentifier())));
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
