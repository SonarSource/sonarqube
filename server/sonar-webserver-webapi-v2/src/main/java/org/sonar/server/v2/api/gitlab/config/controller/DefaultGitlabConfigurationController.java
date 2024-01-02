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
package org.sonar.server.v2.api.gitlab.config.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sonar.server.common.gitlab.config.GitlabConfiguration;
import org.sonar.server.common.gitlab.config.GitlabConfigurationService;
import org.sonar.server.common.gitlab.config.ProvisioningType;
import org.sonar.server.common.gitlab.config.UpdateGitlabConfigurationRequest;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.gitlab.config.request.GitlabConfigurationCreateRestRequest;
import org.sonar.server.v2.api.gitlab.config.request.GitlabConfigurationUpdateRestRequest;
import org.sonar.server.v2.api.gitlab.config.resource.GitlabConfigurationResource;
import org.sonar.server.v2.api.gitlab.config.response.GitlabConfigurationSearchRestResponse;
import org.sonar.server.v2.api.response.PageRestResponse;

import static org.sonar.server.common.gitlab.config.GitlabConfigurationService.UNIQUE_GITLAB_CONFIGURATION_ID;

public class DefaultGitlabConfigurationController implements GitlabConfigurationController {

  private final UserSession userSession;
  private final GitlabConfigurationService gitlabConfigurationService;

  public DefaultGitlabConfigurationController(UserSession userSession, GitlabConfigurationService gitlabConfigurationService) {
    this.userSession = userSession;
    this.gitlabConfigurationService = gitlabConfigurationService;
  }

  @Override
  public GitlabConfigurationResource getGitlabConfiguration(String id) {
    userSession.checkIsSystemAdministrator();
    return getGitlabConfigurationResource(id);
  }

  @Override
  public GitlabConfigurationSearchRestResponse searchGitlabConfiguration() {
    userSession.checkIsSystemAdministrator();

    List<GitlabConfigurationResource> gitlabConfigurationResources = gitlabConfigurationService.findConfigurations()
      .stream()
      .map(this::toGitLabConfigurationResource)
      .toList();

    PageRestResponse pageRestResponse = new PageRestResponse(1, 1000, gitlabConfigurationResources.size());
    return new GitlabConfigurationSearchRestResponse(gitlabConfigurationResources, pageRestResponse);
  }

  @Override
  public GitlabConfigurationResource create(GitlabConfigurationCreateRestRequest createRequest) {
    userSession.checkIsSystemAdministrator();
    GitlabConfiguration createdConfiguration = gitlabConfigurationService.createConfiguration(toGitlabConfiguration(createRequest));
    return toGitLabConfigurationResource(createdConfiguration);
  }


  private static GitlabConfiguration toGitlabConfiguration(GitlabConfigurationCreateRestRequest createRestRequest) {
    return new GitlabConfiguration(
      UNIQUE_GITLAB_CONFIGURATION_ID,
      createRestRequest.enabled(),
      createRestRequest.applicationId(),
      createRestRequest.url(),
      createRestRequest.secret(),
      createRestRequest.synchronizeGroups(),
      toProvisioningType(createRestRequest.provisioningType()),
      createRestRequest.allowUsersToSignUp() != null && createRestRequest.allowUsersToSignUp(),
      createRestRequest.provisioningToken(),
      createRestRequest.provisioningGroups() == null ? Set.of() : Set.copyOf(createRestRequest.provisioningGroups()));
  }

  private GitlabConfigurationResource getGitlabConfigurationResource(String id) {
    return toGitLabConfigurationResource(gitlabConfigurationService.getConfiguration(id));
  }

  @Override
  public GitlabConfigurationResource updateGitlabConfiguration(String id, GitlabConfigurationUpdateRestRequest updateRequest) {
    userSession.checkIsSystemAdministrator();
    UpdateGitlabConfigurationRequest updateGitlabConfigurationRequest = toUpdateGitlabConfigurationRequest(id, updateRequest);
    return toGitLabConfigurationResource(gitlabConfigurationService.updateConfiguration(updateGitlabConfigurationRequest));
  }

  private static UpdateGitlabConfigurationRequest toUpdateGitlabConfigurationRequest(String id,
    GitlabConfigurationUpdateRestRequest updateRequest) {
    return UpdateGitlabConfigurationRequest.builder()
      .gitlabConfigurationId(id)
      .enabled(updateRequest.getEnabled().toNonNullUpdatedValue())
      .applicationId(updateRequest.getApplicationId().toNonNullUpdatedValue())
      .url(updateRequest.getUrl().toNonNullUpdatedValue())
      .secret(updateRequest.getSecret().toNonNullUpdatedValue())
      .synchronizeGroups(updateRequest.getSynchronizeGroups().toNonNullUpdatedValue())
      .provisioningType(updateRequest.getProvisioningType().map(DefaultGitlabConfigurationController::toProvisioningType).toNonNullUpdatedValue())
      .allowUserToSignUp(updateRequest.getAllowUsersToSignUp().toNonNullUpdatedValue())
      .provisioningToken(updateRequest.getProvisioningToken().toUpdatedValue())
      .provisioningGroups(updateRequest.getProvisioningGroups().map(DefaultGitlabConfigurationController::getGroups).toNonNullUpdatedValue())
      .build();
  }

  private static Set<String> getGroups(List<String> groups) {
    return new HashSet<>(groups);
  }

  private GitlabConfigurationResource toGitLabConfigurationResource(GitlabConfiguration configuration) {
    Optional<String> configurationError = gitlabConfigurationService.validate(configuration);
    return new GitlabConfigurationResource(
      configuration.id(),
      configuration.enabled(),
      configuration.applicationId(),
      configuration.url(),
      configuration.synchronizeGroups(),
      toRestProvisioningType(configuration),
      configuration.allowUsersToSignUp(),
      sortGroups(configuration.provisioningGroups()),
      configurationError.orElse(null)
    );
  }

  private static org.sonar.server.v2.api.gitlab.config.resource.ProvisioningType toRestProvisioningType(GitlabConfiguration configuration) {
    return org.sonar.server.v2.api.gitlab.config.resource.ProvisioningType.valueOf(configuration.provisioningType().name());
  }

  private static ProvisioningType toProvisioningType(org.sonar.server.v2.api.gitlab.config.resource.ProvisioningType provisioningType) {
    return ProvisioningType.valueOf(provisioningType.name());
  }

  private static List<String> sortGroups(Set<String> groups) {
    return groups.stream().sorted().toList();
  }

  @Override
  public void deleteGitlabConfiguration(String id) {
    userSession.checkIsSystemAdministrator();
    gitlabConfigurationService.deleteConfiguration(id);
  }
}
