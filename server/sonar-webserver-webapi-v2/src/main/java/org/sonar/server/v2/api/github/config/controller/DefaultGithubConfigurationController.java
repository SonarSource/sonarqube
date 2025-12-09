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
package org.sonar.server.v2.api.github.config.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.server.common.github.config.GithubConfiguration;
import org.sonar.server.common.github.config.GithubConfigurationService;
import org.sonar.server.common.github.config.UpdateGithubConfigurationRequest;
import org.sonar.server.common.gitlab.config.ProvisioningType;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.github.config.request.GithubConfigurationCreateRestRequest;
import org.sonar.server.v2.api.github.config.request.GithubConfigurationUpdateRestRequest;
import org.sonar.server.v2.api.github.config.resource.GithubConfigurationResource;
import org.sonar.server.v2.api.github.config.response.GithubConfigurationSearchRestResponse;
import org.sonar.server.v2.api.response.PageRestResponse;

import static org.sonar.api.utils.Preconditions.checkArgument;
import static org.sonar.server.common.github.config.GithubConfigurationService.UNIQUE_GITHUB_CONFIGURATION_ID;

public class DefaultGithubConfigurationController implements GithubConfigurationController {

  private final UserSession userSession;
  private final GithubConfigurationService githubConfigurationService;

  public DefaultGithubConfigurationController(UserSession userSession, GithubConfigurationService githubConfigurationService) {
    this.userSession = userSession;
    this.githubConfigurationService = githubConfigurationService;
  }

  @Override
  public GithubConfigurationResource getGithubConfiguration(String id) {
    userSession.checkIsSystemAdministrator();
    return getGithubConfigurationResource(id);
  }

  @Override
  public GithubConfigurationSearchRestResponse searchGithubConfiguration() {
    userSession.checkIsSystemAdministrator();

    List<GithubConfigurationResource> githubConfigurationResources = githubConfigurationService.findConfigurations()
      .stream()
      .map(this::toGithubConfigurationResource)
      .toList();

    PageRestResponse pageRestResponse = new PageRestResponse(1, 1000, githubConfigurationResources.size());
    return new GithubConfigurationSearchRestResponse(githubConfigurationResources, pageRestResponse);
  }

  @Override
  public GithubConfigurationResource createGithubConfiguration(GithubConfigurationCreateRestRequest createRequest) {
    userSession.checkIsSystemAdministrator();
    GithubConfiguration createdConfiguration = githubConfigurationService.createConfiguration(toGithubConfiguration(createRequest));
    return toGithubConfigurationResource(createdConfiguration);
  }

  private static GithubConfiguration toGithubConfiguration(GithubConfigurationCreateRestRequest createRestRequest) {
    return new GithubConfiguration(
      UNIQUE_GITHUB_CONFIGURATION_ID,
      createRestRequest.enabled(),
      createRestRequest.clientId(),
      createRestRequest.clientSecret(),
      createRestRequest.applicationId(),
      createRestRequest.privateKey(),
      createRestRequest.synchronizeGroups(),
      createRestRequest.apiUrl(),
      createRestRequest.webUrl(),
      Set.copyOf(createRestRequest.allowedOrganizations()),
      toProvisioningType(createRestRequest.provisioningType()),
      createRestRequest.allowUsersToSignUp() != null && createRestRequest.allowUsersToSignUp(),
      createRestRequest.projectVisibility() != null && createRestRequest.projectVisibility(),
      createRestRequest.userConsentRequiredAfterUpgrade() != null && createRestRequest.userConsentRequiredAfterUpgrade());
  }

  private GithubConfigurationResource getGithubConfigurationResource(String id) {
    return toGithubConfigurationResource(githubConfigurationService.getConfiguration(id));
  }

  @Override
  public GithubConfigurationResource updateGithubConfiguration(String id, GithubConfigurationUpdateRestRequest updateRequest) {
    userSession.checkIsSystemAdministrator();
    UpdateGithubConfigurationRequest updateGithubConfigurationRequest = toUpdateGithubConfigurationRequest(id, updateRequest);
    return toGithubConfigurationResource(githubConfigurationService.updateConfiguration(updateGithubConfigurationRequest));
  }

  private static UpdateGithubConfigurationRequest toUpdateGithubConfigurationRequest(String id, GithubConfigurationUpdateRestRequest updateRequest) {
    return UpdateGithubConfigurationRequest.builder()
      .githubConfigurationId(id)
      .enabled(updateRequest.getEnabled().toNonNullUpdatedValue())
      .clientId(updateRequest.getClientId().toNonNullUpdatedValue())
      .clientSecret(updateRequest.getClientSecret().toNonNullUpdatedValue())
      .applicationId(updateRequest.getApplicationId().toNonNullUpdatedValue())
      .privateKey(updateRequest.getPrivateKey().toNonNullUpdatedValue())
      .synchronizeGroups(updateRequest.getSynchronizeGroups().toNonNullUpdatedValue())
      .apiUrl(updateRequest.getApiUrl().toNonNullUpdatedValue())
      .webUrl(updateRequest.getWebUrl().toNonNullUpdatedValue())
      .allowedOrganizations(updateRequest.getAllowedOrganizations().map(DefaultGithubConfigurationController::getOrganizations).toNonNullUpdatedValue())
      .provisioningType(updateRequest.getProvisioningType().map(DefaultGithubConfigurationController::toProvisioningType).toNonNullUpdatedValue())
      .allowUsersToSignUp(updateRequest.getAllowUsersToSignUp().toNonNullUpdatedValue())
      .projectVisibility(updateRequest.getProjectVisibility().toNonNullUpdatedValue())
      .userConsentRequiredAfterUpgrade(updateRequest.getUserConsentRequiredAfterUpgrade().toNonNullUpdatedValue())
      .build();
  }

  private static Set<String> getOrganizations(@Nullable List<String> orgs) {
    checkArgument(orgs != null, "allowedOrganizations must not be null");
    return new HashSet<>(orgs);
  }

  private GithubConfigurationResource toGithubConfigurationResource(GithubConfiguration configuration) {
    Optional<String> configurationError = githubConfigurationService.validate(configuration);
    return new GithubConfigurationResource(
      configuration.id(),
      configuration.enabled(),
      configuration.applicationId(),
      configuration.synchronizeGroups(),
      configuration.apiUrl(),
      configuration.webUrl(),
      sortGroups(configuration.allowedOrganizations()),
      toRestProvisioningType(configuration),
      configuration.allowUsersToSignUp(),
      configuration.provisionProjectVisibility(),
      configuration.userConsentRequiredAfterUpgrade(),
      configurationError.orElse(null));
  }

  private static org.sonar.server.v2.api.model.ProvisioningType toRestProvisioningType(GithubConfiguration configuration) {
    return org.sonar.server.v2.api.model.ProvisioningType.valueOf(configuration.provisioningType().name());
  }

  private static ProvisioningType toProvisioningType(org.sonar.server.v2.api.model.ProvisioningType provisioningType) {
    return ProvisioningType.valueOf(provisioningType.name());
  }

  private static List<String> sortGroups(Set<String> groups) {
    return groups.stream().sorted().toList();
  }

  @Override
  public void deleteGithubConfiguration(String id) {
    userSession.checkIsSystemAdministrator();
    githubConfigurationService.deleteConfiguration(id);
  }
}
