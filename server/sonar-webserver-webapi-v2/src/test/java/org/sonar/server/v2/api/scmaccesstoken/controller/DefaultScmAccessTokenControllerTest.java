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
package org.sonar.server.v2.api.scmaccesstoken.controller;

import com.google.gson.Gson;
import java.util.Optional;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.sonar.core.scm.ScmAccessToken;
import org.sonar.core.scm.ScmAccessTokenProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.ControllerTester;
import org.sonar.server.v2.api.scmaccesstoken.response.ScmAccessTokenRestResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.user.ServiceIdentity.AGENTIC_SHARED;
import static org.sonar.server.user.ServiceIdentity.REMEDIATION_TO_SQS;
import static org.sonar.server.v2.WebApiEndpoints.SCM_ACCESS_TOKEN_ENDPOINT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DefaultScmAccessTokenControllerTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private final ScmAccessTokenProvider scmAccessTokenProvider = mock(ScmAccessTokenProvider.class);

  private final MockMvc mockMvc = ControllerTester.getMockMvc(
    new DefaultScmAccessTokenController(userSession, scmAccessTokenProvider));

  private static final Gson gson = new Gson();

  @Test
  void generateScmAccessToken_whenNotSystemAdministrator_returnsForbidden() throws Exception {
    userSession.logIn();

    mockMvc
      .perform(post(SCM_ACCESS_TOKEN_ENDPOINT).param("project", "my-project"))
      .andExpect(status().isForbidden());
  }

  @Test
  void generateScmAccessToken_whenProviderCannotMintAToken_returnsNotFound() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(scmAccessTokenProvider.mint("unbound-project")).thenReturn(Optional.empty());

    mockMvc
      .perform(post(SCM_ACCESS_TOKEN_ENDPOINT).param("project", "unbound-project"))
      .andExpect(status().isNotFound());
  }

  @Test
  void generateScmAccessToken_whenTokenIsMinted_returnsIt() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(scmAccessTokenProvider.mint("my-project")).thenReturn(Optional.of(
      new ScmAccessToken("gitlab", "sonarqube-remediation-agent", "glpat-abc123", "2026-08-06")));

    MvcResult mvcResult = mockMvc
      .perform(post(SCM_ACCESS_TOKEN_ENDPOINT).param("project", "my-project"))
      .andExpect(status().isOk())
      .andReturn();

    ScmAccessTokenRestResponse response = gson.fromJson(mvcResult.getResponse().getContentAsString(), ScmAccessTokenRestResponse.class);
    assertThat(response).isEqualTo(new ScmAccessTokenRestResponse("gitlab", "sonarqube-remediation-agent", "glpat-abc123", "2026-08-06"));
  }

  @Test
  void generateScmAccessToken_whenCalledByAgenticSharedService_returnsTokenWithoutSystemAdminPermission() throws Exception {
    UserSession serviceSession = mock(UserSession.class);
    when(serviceSession.getServiceIdentity()).thenReturn(Optional.of(AGENTIC_SHARED));
    when(scmAccessTokenProvider.mint("my-project")).thenReturn(Optional.of(
      new ScmAccessToken("gitlab", "sonarqube-remediation-agent", "glpat-abc123", "2026-08-06")));
    MockMvc serviceMockMvc = ControllerTester.getMockMvc(
      new DefaultScmAccessTokenController(serviceSession, scmAccessTokenProvider));

    serviceMockMvc.perform(post(SCM_ACCESS_TOKEN_ENDPOINT).param("project", "my-project"))
      .andExpect(status().isOk());

    verify(serviceSession, never()).checkIsSystemAdministrator();
  }

  @Test
  void generateScmAccessToken_whenCalledByAnotherService_stillChecksSystemAdminPermission() throws Exception {
    UserSession serviceSession = mock(UserSession.class);
    when(serviceSession.getServiceIdentity()).thenReturn(Optional.of(REMEDIATION_TO_SQS));
    when(scmAccessTokenProvider.mint("my-project")).thenReturn(Optional.of(
      new ScmAccessToken("gitlab", "sonarqube-remediation-agent", "glpat-abc123", "2026-08-06")));
    MockMvc serviceMockMvc = ControllerTester.getMockMvc(
      new DefaultScmAccessTokenController(serviceSession, scmAccessTokenProvider));

    serviceMockMvc.perform(post(SCM_ACCESS_TOKEN_ENDPOINT).param("project", "my-project"))
      .andExpect(status().isOk());

    verify(serviceSession).checkIsSystemAdministrator();
  }
}
