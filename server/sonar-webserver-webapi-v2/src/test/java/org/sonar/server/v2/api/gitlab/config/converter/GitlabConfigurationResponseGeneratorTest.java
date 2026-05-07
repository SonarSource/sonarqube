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

import java.util.Optional;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.common.gitlab.config.GitlabConfiguration;
import org.sonar.server.common.gitlab.config.GitlabConfigurationService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.v2.api.gitlab.config.response.GitlabConfigurationRestResponse;
import org.sonar.server.v2.api.gitlab.config.response.GitlabConfigurationRestResponseForAdmins;
import org.sonar.server.v2.api.gitlab.config.response.GitlabConfigurationRestResponseForLoggedInUsers;
import org.sonar.server.v2.api.model.ProvisioningType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GitlabConfigurationResponseGeneratorTest {

  private static final GitlabConfiguration CONFIGURATION = new GitlabConfiguration(
    "gitlab-configuration",
    true,
    "application-id",
    "https://gitlab.example.com",
    "secret",
    true,
    Set.of("group1", "group2"),
    true,
    true,
    org.sonar.server.common.gitlab.config.ProvisioningType.AUTO_PROVISIONING,
    "provisioning-token");

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private final GitlabConfigurationService service = mock(GitlabConfigurationService.class);
  private final GitlabConfigurationResponseGenerator underTest = new GitlabConfigurationResponseGenerator(userSession, service);

  @Test
  public void toResponse_whenAdmin_returnsFullResource() {
    userSession.logIn().setSystemAdministrator();
    when(service.validate(CONFIGURATION)).thenReturn(Optional.empty());

    GitlabConfigurationRestResponse response = underTest.toResponse(CONFIGURATION);

    assertThat(response).isInstanceOf(GitlabConfigurationRestResponseForAdmins.class);
    GitlabConfigurationRestResponseForAdmins admin = (GitlabConfigurationRestResponseForAdmins) response;
    assertThat(admin.id()).isEqualTo("gitlab-configuration");
    assertThat(admin.enabled()).isTrue();
    assertThat(admin.applicationId()).isEqualTo("application-id");
    assertThat(admin.url()).isEqualTo("https://gitlab.example.com");
    assertThat(admin.allowedGroups()).containsExactly("group1", "group2");
    assertThat(admin.provisioningType()).isEqualTo(ProvisioningType.AUTO_PROVISIONING);
    assertThat(admin.isProvisioningTokenSet()).isTrue();
    assertThat(admin.errorMessage()).isNull();
  }

  @Test
  public void toResponse_whenAdminAndValidationFails_passesErrorMessage() {
    userSession.logIn().setSystemAdministrator();
    when(service.validate(CONFIGURATION)).thenReturn(Optional.of("Invalid configuration"));

    GitlabConfigurationRestResponseForAdmins admin = (GitlabConfigurationRestResponseForAdmins) underTest.toResponse(CONFIGURATION);

    assertThat(admin.errorMessage()).isEqualTo("Invalid configuration");
  }

  @Test
  public void toResponse_whenNonAdminLoggedIn_returnsReducedResource() {
    userSession.logIn().setNonSystemAdministrator();

    GitlabConfigurationRestResponse response = underTest.toResponse(CONFIGURATION);

    assertThat(response).isInstanceOf(GitlabConfigurationRestResponseForLoggedInUsers.class);
    GitlabConfigurationRestResponseForLoggedInUsers reduced = (GitlabConfigurationRestResponseForLoggedInUsers) response;
    assertThat(reduced.id()).isEqualTo("gitlab-configuration");
    assertThat(reduced.enabled()).isTrue();
    assertThat(reduced.provisioningType()).isEqualTo(ProvisioningType.AUTO_PROVISIONING);
  }

  @Test
  public void toResponse_whenNonAdminAndConfigDisabled_returnsEnabledFalse() {
    userSession.logIn().setNonSystemAdministrator();
    GitlabConfiguration disabled = new GitlabConfiguration(
      "gitlab-configuration", false, "app", "url", "secret", true,
      Set.of("group1"), false, true, org.sonar.server.common.gitlab.config.ProvisioningType.JIT, "token");

    GitlabConfigurationRestResponseForLoggedInUsers reduced = (GitlabConfigurationRestResponseForLoggedInUsers) underTest.toResponse(disabled);

    assertThat(reduced.enabled()).isFalse();
    assertThat(reduced.provisioningType()).isEqualTo(ProvisioningType.JIT);
  }

}
