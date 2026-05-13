/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.server.common.gitlab.config.GitlabConfiguration;
import org.sonar.server.common.gitlab.config.GitlabConfigurationService;
import org.sonar.server.common.gitlab.config.ProvisioningType;
import org.sonar.server.common.gitlab.config.UpdateGitlabConfigurationRequest;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.gitlab.config.converter.GitlabConfigurationResponseGenerator;
import org.sonar.server.v2.api.gitlab.config.request.GitlabConfigurationCreateRestRequest;
import org.sonar.server.v2.api.gitlab.config.request.GitlabConfigurationUpdateRestRequest;
import org.sonar.server.v2.api.gitlab.config.response.GitlabConfigurationRestResponse;
import org.sonar.server.v2.api.gitlab.config.response.GitlabConfigurationSearchRestResponse;
import org.sonar.server.v2.api.response.PageRestResponse;

import static org.sonar.api.utils.Preconditions.checkArgument;
import static org.sonar.server.common.gitlab.config.GitlabConfigurationService.UNIQUE_GITLAB_CONFIGURATION_ID;

public class DefaultGitlabConfigurationController implements GitlabConfigurationController {

  private final UserSession userSession;
  private final GitlabConfigurationService gitlabConfigurationService;
  private final GitlabConfigurationResponseGenerator gitlabConfigurationResponseGenerator;

  public DefaultGitlabConfigurationController(UserSession userSession, GitlabConfigurationService gitlabConfigurationService,
    GitlabConfigurationResponseGenerator gitlabConfigurationResponseGenerator) {
    this.userSession = userSession;
    this.gitlabConfigurationService = gitlabConfigurationService;
    this.gitlabConfigurationResponseGenerator = gitlabConfigurationResponseGenerator;
  }

  @Override
  public GitlabConfigurationRestResponse getGitlabConfiguration(String id) {
    userSession.checkLoggedIn();
    return gitlabConfigurationResponseGenerator.toResponse(gitlabConfigurationService.getConfiguration(id));
  }

  @Override
  public GitlabConfigurationSearchRestResponse searchGitlabConfiguration() {
    userSession.checkLoggedIn();

    List<GitlabConfigurationRestResponse> responses = gitlabConfigurationService.findConfigurations()
      .stream()
      .map(gitlabConfigurationResponseGenerator::toResponse)
      .toList();

    PageRestResponse pageRestResponse = new PageRestResponse(1, 1000, responses.size());
    return new GitlabConfigurationSearchRestResponse(responses, pageRestResponse);
  }

  @Override
  public GitlabConfigurationRestResponse create(GitlabConfigurationCreateRestRequest createRequest) {
    userSession.checkIsSystemAdministrator();
    GitlabConfiguration createdConfiguration = gitlabConfigurationService.createConfiguration(toGitlabConfiguration(createRequest));
    return gitlabConfigurationResponseGenerator.toResponse(createdConfiguration);
  }

  private static GitlabConfiguration toGitlabConfiguration(GitlabConfigurationCreateRestRequest createRestRequest) {
    return new GitlabConfiguration(
      UNIQUE_GITLAB_CONFIGURATION_ID,
      createRestRequest.enabled(),
      createRestRequest.applicationId(),
      createRestRequest.url(),
      createRestRequest.secret(),
      createRestRequest.synchronizeGroups(),
      Set.copyOf(createRestRequest.allowedGroups()),
      createRestRequest.allowAllGroups() != null && createRestRequest.allowAllGroups(),
      createRestRequest.allowUsersToSignUp() != null && createRestRequest.allowUsersToSignUp(),
      toProvisioningType(createRestRequest.provisioningType()),
      createRestRequest.provisioningToken());
  }

  @Override
  public GitlabConfigurationRestResponse updateGitlabConfiguration(String id, GitlabConfigurationUpdateRestRequest updateRequest) {
    userSession.checkIsSystemAdministrator();
    UpdateGitlabConfigurationRequest updateGitlabConfigurationRequest = toUpdateGitlabConfigurationRequest(id, updateRequest);
    return gitlabConfigurationResponseGenerator.toResponse(gitlabConfigurationService.updateConfiguration(updateGitlabConfigurationRequest));
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
      .allowedGroups(updateRequest.getAllowedGroups().map(DefaultGitlabConfigurationController::getGroups).toNonNullUpdatedValue())
      .allowAllGroups(updateRequest.getAllowAllGroups().toNonNullUpdatedValue())
      .provisioningType(updateRequest.getProvisioningType().map(DefaultGitlabConfigurationController::toProvisioningType).toNonNullUpdatedValue())
      .allowUserToSignUp(updateRequest.getAllowUsersToSignUp().toNonNullUpdatedValue())
      .provisioningToken(updateRequest.getProvisioningToken().toUpdatedValue())
      .build();
  }

  private static Set<String> getGroups(@Nullable List<String> groups) {
    checkArgument(groups != null, "allowedGroups must not be null");
    return new HashSet<>(groups);
  }

  private static ProvisioningType toProvisioningType(org.sonar.server.v2.api.model.ProvisioningType provisioningType) {
    return ProvisioningType.valueOf(provisioningType.name());
  }

  @Override
  public void deleteGitlabConfiguration(String id) {
    userSession.checkIsSystemAdministrator();
    gitlabConfigurationService.deleteConfiguration(id);
  }
}
