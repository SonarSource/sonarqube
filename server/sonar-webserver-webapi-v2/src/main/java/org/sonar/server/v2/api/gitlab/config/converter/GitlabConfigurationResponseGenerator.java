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
package org.sonar.server.v2.api.gitlab.config.converter;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.sonar.server.common.gitlab.config.GitlabConfiguration;
import org.sonar.server.common.gitlab.config.GitlabConfigurationService;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.gitlab.config.response.GitlabConfigurationRestResponse;
import org.sonar.server.v2.api.gitlab.config.response.GitlabConfigurationRestResponseForAdmins;
import org.sonar.server.v2.api.gitlab.config.response.GitlabConfigurationRestResponseForLoggedInUsers;
import org.sonar.server.v2.api.model.ProvisioningType;

public class GitlabConfigurationResponseGenerator {

  private final UserSession userSession;
  private final GitlabConfigurationService gitlabConfigurationService;

  public GitlabConfigurationResponseGenerator(UserSession userSession, GitlabConfigurationService gitlabConfigurationService) {
    this.userSession = userSession;
    this.gitlabConfigurationService = gitlabConfigurationService;
  }

  public GitlabConfigurationRestResponse toResponse(GitlabConfiguration configuration) {
    if (userSession.isSystemAdministrator()) {
      return toAdminResponse(configuration);
    }
    return toReducedResponse(configuration);
  }
  
  private GitlabConfigurationRestResponseForAdmins toAdminResponse(GitlabConfiguration configuration) {
    Optional<String> configurationError = gitlabConfigurationService.validate(configuration);
    return new GitlabConfigurationRestResponseForAdmins(
      configuration.id(),
      configuration.enabled(),
      configuration.applicationId(),
      configuration.url(),
      configuration.synchronizeGroups(),
      sortGroups(configuration.allowedGroups()),
      configuration.allowAllGroups(),
      configuration.allowUsersToSignUp(),
      toProvisioningType(configuration),
      StringUtils.isNotEmpty(configuration.provisioningToken()),
      configurationError.orElse(null));
  }

  private static GitlabConfigurationRestResponseForLoggedInUsers toReducedResponse(GitlabConfiguration configuration) {
    return new GitlabConfigurationRestResponseForLoggedInUsers(
      configuration.id(),
      configuration.enabled(),
      toProvisioningType(configuration));
  }

  private static ProvisioningType toProvisioningType(GitlabConfiguration configuration) {
    return ProvisioningType.valueOf(configuration.provisioningType().name());
  }

  private static List<String> sortGroups(Set<String> groups) {
    return groups.stream().sorted().toList();
  }
}
