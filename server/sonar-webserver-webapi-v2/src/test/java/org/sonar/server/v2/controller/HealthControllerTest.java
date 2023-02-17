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
package org.sonar.server.v2.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.health.Health;
import org.sonar.server.health.HealthChecker;
import org.sonar.server.platform.NodeInformation;
import org.sonar.server.user.SystemPasscode;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.common.RestResponseEntityExceptionHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.sonar.server.user.SystemPasscodeImpl.PASSCODE_HTTP_HEADER;
import static org.sonar.server.v2.WebApiEndpoints.HEALTH_ENDPOINT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class HealthControllerTest {

  private static final String PASSCODE = "1234";
  private static final Health HEALTH_RESULT = Health.builder().
    setStatus(Health.Status.YELLOW)
    .addCause("One cause")
    .build();
  @Mock
  private HealthChecker healthChecker;
  @Mock
  private UserSession userSession;
  @Mock
  private SystemPasscode systemPasscode;
  @Mock
  private NodeInformation nodeInformation;

  private HealthController level4HealthController;

  private HealthController safeModeHealthController;

  private MockMvc level4mockMvc;

  private MockMvc safeModeMockMvc;

  @Before
  public void setUp() {
    level4HealthController = new HealthController(healthChecker, systemPasscode, nodeInformation, userSession);
    level4mockMvc = MockMvcBuilders.standaloneSetup(level4HealthController)
      .setControllerAdvice(RestResponseEntityExceptionHandler.class)
      .build();

    safeModeHealthController = new HealthController(healthChecker, systemPasscode);
    safeModeMockMvc = MockMvcBuilders.standaloneSetup(safeModeHealthController)
      .setControllerAdvice(RestResponseEntityExceptionHandler.class)
      .build();

    when(healthChecker.checkNode()).thenReturn(HEALTH_RESULT);
  }

  @Test
  public void getHealth_inSafeModeWithoutUserSessionAndPasscode_returnsForbidden() throws Exception {
    safeModeMockMvc.perform(get(HEALTH_ENDPOINT))
      .andExpect(status().isForbidden());
  }

  @Test
  public void getHealth_inSafeModeWithValidPasscode_succeeds() throws Exception {
    when(systemPasscode.isValidPasscode(PASSCODE)).thenReturn(true);

    safeModeMockMvc.perform(get(HEALTH_ENDPOINT).header(PASSCODE_HTTP_HEADER, PASSCODE))
      .andExpect(status().isOk());
  }

  @Test
  public void getHealth_should_returnForbiddenWithNoCredentials() throws Exception {
    level4mockMvc.perform(get(HEALTH_ENDPOINT))
      .andExpect(status().isForbidden());
  }

  @Test
  public void getHealth_should_returnForbiddenWithWrongPasscodeAndNoAdminCredentials() throws Exception {
    when(systemPasscode.isValidPasscode(PASSCODE)).thenReturn(false);
    when(userSession.isSystemAdministrator()).thenReturn(false);
    level4mockMvc.perform(get(HEALTH_ENDPOINT).header(PASSCODE_HTTP_HEADER, PASSCODE))
      .andExpect(status().isForbidden());
  }

  @Test
  public void getHealth_withValidPasscodeAndStandaloneNode_returnHealth() throws Exception {
    when(systemPasscode.isValidPasscode(PASSCODE)).thenReturn(true);
    when(nodeInformation.isStandalone()).thenReturn(true);
    level4mockMvc.perform(get(HEALTH_ENDPOINT).header(PASSCODE_HTTP_HEADER, PASSCODE))
      .andExpect(status().isOk())
      .andExpect(content().json("""
        {
           "status":"YELLOW",
           "causes":[
              "One cause"
           ]
        }"""));
  }

  @Test
  public void getHealth_asSysadminAndStandaloneNode_returnHealth() throws Exception {
    when(userSession.isSystemAdministrator()).thenReturn(true);
    when(nodeInformation.isStandalone()).thenReturn(true);
    level4mockMvc.perform(get(HEALTH_ENDPOINT))
      .andExpect(status().isOk())
      .andExpect(content().json("""
        {
           "status":"YELLOW",
           "causes":[
              "One cause"
           ]
        }"""));
  }

  @Test
  public void getHealth_whenUnauthorizedExceptionThrown_returnHttpUnauthorized() throws Exception {
    when(userSession.isSystemAdministrator()).thenThrow(new UnauthorizedException("unauthorized"));
    level4mockMvc.perform(get(HEALTH_ENDPOINT))
      .andExpect(status().isUnauthorized());
  }

  @Test
  public void getHealth_should_returnServerErrorForCluster() throws Exception {
    when(systemPasscode.isValidPasscode(PASSCODE)).thenReturn(true);
    when(nodeInformation.isStandalone()).thenReturn(false);
    level4mockMvc.perform(get(HEALTH_ENDPOINT).header(PASSCODE_HTTP_HEADER, PASSCODE))
      .andExpect(status().isNotImplemented())
      .andExpect(content().string("Unsupported in cluster mode"));
  }
}
