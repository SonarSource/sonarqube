/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.v2.api.system.controller;

import com.google.gson.Gson;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.health.Health;
import org.sonar.server.health.HealthChecker;
import org.sonar.server.platform.NodeInformation;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.SystemPasscode;
import org.sonar.server.v2.api.ControllerTester;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static java.net.HttpURLConnection.HTTP_NOT_IMPLEMENTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.user.SystemPasscodeImpl.PASSCODE_HTTP_HEADER;
import static org.sonar.server.v2.WebApiEndpoints.HEALTH_ENDPOINT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class HealthControllerTest {

  private static final String VALID_PASSCODE = "valid_passcode";
  private static final String INVALID_PASSCODE = "invalid_passcode";
  private static final Health HEALTH_RESULT = Health.builder().
    setStatus(Health.Status.YELLOW)
    .addCause("One cause")
    .build();

  private final HealthChecker healthChecker = mock(HealthChecker.class);
  private final SystemPasscode systemPasscode = mock(SystemPasscode.class);
  private final NodeInformation nodeInformation = mock(NodeInformation.class);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private final MockMvc mockMvc = ControllerTester.getMockMvc(new HealthController(healthChecker, systemPasscode, nodeInformation,userSession));


  private static final Gson gson = new Gson();

  @Test
  public void getSystemHealth_whenValidPasscodeAndStandaloneMode_shouldSucceed() throws Exception {
    when(systemPasscode.isValidPasscode(VALID_PASSCODE)).thenReturn(true);
    when(nodeInformation.isStandalone()).thenReturn(true);
    when(healthChecker.checkNode()).thenReturn(HEALTH_RESULT);

    MvcResult mvcResult = mockMvc.perform(get(HEALTH_ENDPOINT).header(PASSCODE_HTTP_HEADER, VALID_PASSCODE))
      .andExpect(status().isOk())
      .andReturn();

    Health actualHealth = gson.fromJson(mvcResult.getResponse().getContentAsString(), Health.class);
    assertThat(actualHealth).isEqualTo(HEALTH_RESULT);
  }

  @Test
  public void getSystemHealth_whenAdminCredentialAndStandaloneMode_shouldSucceed() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(nodeInformation.isStandalone()).thenReturn(true);
    when(healthChecker.checkNode()).thenReturn(HEALTH_RESULT);

    MvcResult mvcResult = mockMvc.perform(get(HEALTH_ENDPOINT))
      .andExpect(status().isOk())
      .andReturn();

    Health actualHealth = gson.fromJson(mvcResult.getResponse().getContentAsString(), Health.class);
    assertThat(actualHealth).isEqualTo(HEALTH_RESULT);
  }

  @Test
  public void getSystemHealth_whenNoCredentials_shouldReturnForbidden() throws Exception {
    mockMvc.perform(get(HEALTH_ENDPOINT))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void getSystemHealth_whenInvalidPasscodeAndNoAdminCredentials_shouldReturnForbidden() throws Exception {
    userSession.logIn();
    when(systemPasscode.isValidPasscode(INVALID_PASSCODE)).thenReturn(false);

    mockMvc.perform(get(HEALTH_ENDPOINT).header(PASSCODE_HTTP_HEADER, INVALID_PASSCODE))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void getSystemHealth_whenValidPasscodeAndClusterMode_shouldReturnNotImplemented() throws Exception {
    when(systemPasscode.isValidPasscode(VALID_PASSCODE)).thenReturn(true);
    when(nodeInformation.isStandalone()).thenReturn(false);

    mockMvc.perform(get(HEALTH_ENDPOINT).header(PASSCODE_HTTP_HEADER, VALID_PASSCODE))
      .andExpectAll(
        status().is(HTTP_NOT_IMPLEMENTED),
        content().json("{\"message\":\"Unsupported in cluster mode\"}"));
  }

}
