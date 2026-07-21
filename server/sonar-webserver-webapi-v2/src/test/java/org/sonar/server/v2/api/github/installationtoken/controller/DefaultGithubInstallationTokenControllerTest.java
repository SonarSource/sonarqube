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
package org.sonar.server.v2.api.github.installationtoken.controller;

import com.google.gson.Gson;
import java.util.Optional;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.sonar.core.scm.github.GithubInstallationToken;
import org.sonar.core.scm.github.GithubInstallationTokenProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.v2.api.ControllerTester;
import org.sonar.server.v2.api.github.installationtoken.response.GithubInstallationTokenRestResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.v2.WebApiEndpoints.GITHUB_INSTALLATION_TOKEN_ENDPOINT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DefaultGithubInstallationTokenControllerTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private final GithubInstallationTokenProvider tokenProvider = mock(GithubInstallationTokenProvider.class);

  private final MockMvc mockMvc = ControllerTester.getMockMvc(
    new DefaultGithubInstallationTokenController(userSession, tokenProvider));

  private static final Gson gson = new Gson();

  @Test
  void generateInstallationToken_whenNotSystemAdministrator_returnsForbidden() throws Exception {
    userSession.logIn();

    mockMvc
      .perform(post(GITHUB_INSTALLATION_TOKEN_ENDPOINT).param("project", "my-project"))
      .andExpect(status().isForbidden());
  }

  @Test
  void generateInstallationToken_whenProviderCannotMintAToken_returnsNotFound() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(tokenProvider.mint("unbound-project")).thenReturn(Optional.empty());

    mockMvc
      .perform(post(GITHUB_INSTALLATION_TOKEN_ENDPOINT).param("project", "unbound-project"))
      .andExpect(status().isNotFound());
  }

  @Test
  void generateInstallationToken_whenTokenIsMinted_returnsIt() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(tokenProvider.mint("my-project")).thenReturn(Optional.of(
      new GithubInstallationToken("ghs_abc123", "2026-07-15T15:00:00Z")));

    MvcResult mvcResult = mockMvc
      .perform(post(GITHUB_INSTALLATION_TOKEN_ENDPOINT).param("project", "my-project"))
      .andExpect(status().isOk())
      .andReturn();

    GithubInstallationTokenRestResponse response = gson.fromJson(mvcResult.getResponse().getContentAsString(), GithubInstallationTokenRestResponse.class);
    assertThat(response).isEqualTo(new GithubInstallationTokenRestResponse("ghs_abc123", "2026-07-15T15:00:00Z"));
  }
}
